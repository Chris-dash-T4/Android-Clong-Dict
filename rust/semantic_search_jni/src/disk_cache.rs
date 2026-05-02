#![allow(dead_code)] // wired from JNI / lib once persistence is hooked up

use std::collections::HashMap;
use std::fs::File;
use std::io::{BufReader, BufWriter, Cursor, Read, Write};
use std::path::Path;
use std::time::{SystemTime, UNIX_EPOCH};

use anyhow::{anyhow, Context, Result};
use serde::{Deserialize, Serialize};

/// Bump when `EmbeddingCache` / bincode layout changes (keep in sync with readers).
pub const DISK_CACHE_SCHEMA_VERSION: u32 = 1;

static EXPIRATION_TIME: i64 = 60 * 60 * 24 * 30; // 30 days

/// zstd compression level (3 is a reasonable default for speed vs size).
const ZSTD_LEVEL: i32 = 3;

#[derive(Serialize, Deserialize, Debug, Clone)]
struct EmbeddingSchema {
    embedding: Vec<f64>,
    doc_fingerprint: String,
    last_updated: i64,
}

impl EmbeddingSchema {
    fn new(embedding: Vec<f64>, doc_fingerprint: String) -> Self {
        Self {
            embedding,
            doc_fingerprint,
            last_updated: SystemTime::now()
                .duration_since(UNIX_EPOCH)
                .unwrap()
                .as_secs() as i64,
        }
    }

    fn is_expired(&self) -> bool {
        SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .unwrap()
            .as_secs() as i64
            - self.last_updated
            > EXPIRATION_TIME
    }
}

#[derive(Serialize, Deserialize)]
pub struct EmbeddingCache {
    cache: HashMap<String, EmbeddingSchema>,
    model: String,
    api_url: String,
    schema_version: u32,
}

impl EmbeddingCache {
    fn new(model: String, api_url: String) -> Self {
        Self {
            cache: HashMap::new(),
            model,
            api_url,
            schema_version: DISK_CACHE_SCHEMA_VERSION,
        }
    }
}

fn serialize_cache(cache: &EmbeddingCache) -> Result<Vec<u8>> {
    bincode::serialize(cache).context("bincode serialize EmbeddingCache")
}

fn deserialize_cache(bytes: &[u8]) -> Result<EmbeddingCache> {
    bincode::deserialize(bytes).context("bincode deserialize EmbeddingCache")
}

pub fn load_cache(path: &Path) -> Result<EmbeddingCache> {
    let file = File::open(path)?;
    let mut reader = BufReader::new(file);
    let mut compressed = Vec::new();
    reader.read_to_end(&mut compressed)?;
    let raw = zstd::decode_all(Cursor::new(&compressed)).context("zstd decompress cache")?;
    deserialize_cache(&raw)
}

pub fn save_cache(cache: &EmbeddingCache, path: &Path) -> Result<()> {
    let raw = serialize_cache(cache)?;
    let compressed =
        zstd::encode_all(Cursor::new(&raw), ZSTD_LEVEL).context("zstd compress cache")?;

    let tmp_path = path.with_extension("tmp");
    let file = File::create(&tmp_path).context("create temp cache file")?;
    let mut writer = BufWriter::new(file);
    writer
        .write_all(&compressed)
        .context("write compressed cache")?;
    writer.flush().context("flush cache temp file")?;
    drop(writer);

    if path.exists() {
        let bak = path.with_extension("bak");
        let _ = std::fs::rename(path, &bak);
    }
    std::fs::rename(&tmp_path, path).context("rename cache tmp -> final")?;
    Ok(())
}

/// Write the in-memory cache to the disk cache
pub fn write_inmemory_cache(
    mem_cache: &HashMap<String, Vec<f64>>,
    path: &Path,
    model: &str,
    api_url: &str,
) -> Result<()> {
    let mut cache = EmbeddingCache::new(model.to_string(), api_url.to_string());
    for (key, value) in mem_cache {
        cache.cache.insert(
            key.clone(),
            EmbeddingSchema::new(value.clone(), key.clone()),
        );
    }
    save_cache(&cache, path)
}

/// Read the disk cache into an in-memory cache
pub fn read_disk_cache(
    path: &Path,
    model: &str,
    api_url: &str,
) -> Result<HashMap<String, Vec<f64>>> {
    let cache = load_cache(path)?;
    if cache.schema_version != DISK_CACHE_SCHEMA_VERSION {
        return Err(anyhow!("cache schema_version mismatch"));
    }
    if cache.model != model || cache.api_url != api_url {
        return Err(anyhow!("Cache model or API URL mismatch"));
    }
    let mut mem_cache = HashMap::new();
    for (key, value) in cache.cache {
        if value.is_expired() {
            continue;
        }
        mem_cache.insert(key, value.embedding);
    }
    Ok(mem_cache)
}

#[cfg(test)]
mod tests {
    use std::collections::HashMap;
    use std::path::Path;

    use super::{
        load_cache, save_cache, write_inmemory_cache, read_disk_cache, EmbeddingCache,
    };

    #[test]
    fn test_create_and_read_disk_cache() {
        let model = "test_model".to_string();
        let api_url = "https://test_api_url/".to_string();
        let cache = EmbeddingCache::new(model.clone(), api_url.clone());
        let cache_path = Path::new("test_cache.bin");
        save_cache(&cache, cache_path).unwrap();
        let cache = load_cache(cache_path).unwrap();
        assert_eq!(cache.model, model);
        assert_eq!(cache.api_url, api_url);
    }

    #[test]
    fn test_write_and_read_inmemory_cache() {
        let mut mem_cache = HashMap::new();
        mem_cache.insert("test_key".to_string(), vec![1.0, 2.0, 3.0]);
        let cache_path = Path::new("test_mem_cache.bin");
        write_inmemory_cache(&mem_cache, cache_path, "test_model", "https://test_api_url/").unwrap();
        let mem_cache =
            read_disk_cache(cache_path, "test_model", "https://test_api_url/").unwrap();
        assert_eq!(mem_cache.get("test_key").unwrap(), &vec![1.0, 2.0, 3.0]);
    }
}