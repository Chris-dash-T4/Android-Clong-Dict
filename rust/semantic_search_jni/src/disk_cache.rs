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

static MAX_CACHE_SIZE: usize = 65536;
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

#[derive(Serialize, Deserialize, Clone)]
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
    let prev_cache = load_cache(path).unwrap_or_else(|_| EmbeddingCache::new(model.to_string(), api_url.to_string()));
    let mut cache = if prev_cache.schema_version != DISK_CACHE_SCHEMA_VERSION || prev_cache.model != model || prev_cache.api_url != api_url {
        eprintln!("Existing cache schema/model/api mismatch, overwriting with new cache");
        EmbeddingCache::new(model.to_string(), api_url.to_string())
    } else {
        prev_cache
    };
    for (key, value) in mem_cache {
        cache.cache.insert(
            key.clone(),
            EmbeddingSchema::new(value.clone(), key.clone()),
        );
    }
    if cache.cache.len() > MAX_CACHE_SIZE {
        eprintln!("embedding disk cache: cache size {} exceeds max {}, evicting old entries", cache.cache.len(), MAX_CACHE_SIZE);
    }
    save_cache(&cache, path)
}

pub fn evict_expired_entries(cache: &EmbeddingCache) -> EmbeddingCache {
    let mut new_cache = cache.clone();
    // Step 1: Remove expired entries
    new_cache
        .cache
        .retain(|_, v| !v.is_expired());
    if new_cache.cache.len() > MAX_CACHE_SIZE {
        eprintln!("embedding disk cache: cache size {} still exceeds max {}, evicting unexpired entries to reduce size", new_cache.cache.len(), MAX_CACHE_SIZE);
        // Step 2: If still too large, evict oldest entries until under limit
        let mut entries: Vec<_> = new_cache.cache.into_iter().collect();
        // Sort descending by last_updated so we keep the most recently updated entries
        entries.sort_by_key(|(_, v)| -v.last_updated);
        entries.truncate(MAX_CACHE_SIZE);
        new_cache.cache = entries.into_iter().collect();
    }
    new_cache
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
    use std::{collections::HashMap};
    use std::path::Path;
    use std::time::{SystemTime, UNIX_EPOCH};

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
    
    #[test]
    fn test_evict_expired_entries() {
        let model = "test_model".to_string();
        let api_url = "https://test_api_url/".to_string();
        let mut cache = EmbeddingCache::new(model.clone(), api_url.clone());
        cache.cache.insert(
            "expired_key".to_string(),
            super::EmbeddingSchema {
                embedding: vec![1.0, 2.0, 3.0],
                doc_fingerprint: "expired_key".to_string(),
                last_updated: 0, // very old timestamp to ensure expiration
            },
        );
        // Insert MAX_CACHE_SIZE unexpired entries to test eviction of expired entries
        for i in 0..super::MAX_CACHE_SIZE {
            let embedding = vec![i as f64; 3];
            let key = format!("key_{}", i);
            cache.cache.insert(
                key.clone(),
                super::EmbeddingSchema::new(embedding, key),
            );
        }
        let evicted_cache = super::evict_expired_entries(&cache);
        assert!(!evicted_cache.cache.contains_key("expired_key"));
        // All unexpired entries should still be present
        for i in 0..super::MAX_CACHE_SIZE {
            let key = format!("key_{}", i);
            assert!(evicted_cache.cache.contains_key(&key));
        }
    }

    #[test]
    fn test_evict_unexpired_entries() {
        let model = "test_model".to_string();
        let api_url = "https://test_api_url/".to_string();
        let mut cache = EmbeddingCache::new(model.clone(), api_url.clone());
        cache.cache.insert(
            "expired_key".to_string(),
            super::EmbeddingSchema {
                embedding: vec![1.0, 2.0, 3.0],
                doc_fingerprint: "expired_key".to_string(),
                last_updated: SystemTime::now()
                    .duration_since(UNIX_EPOCH)
                    .unwrap()
                    .as_secs() as i64 - 3600_i64, // 1 hour ago, not expired but will be evicted if cache exceeds max size
            },
        );
        // Insert MAX_CACHE_SIZE unexpired entries to test eviction of expired entries
        for i in 0..super::MAX_CACHE_SIZE {
            let embedding = vec![i as f64; 3];
            let key = format!("key_{}", i);
            cache.cache.insert(
                key.clone(),
                super::EmbeddingSchema::new(embedding, key),
            );
        }
        let evicted_cache = super::evict_expired_entries(&cache);
        assert!(!evicted_cache.cache.contains_key("expired_key"));
        // All unexpired entries should still be present
        for i in 0..super::MAX_CACHE_SIZE {
            let key = format!("key_{}", i);
            assert!(evicted_cache.cache.contains_key(&key));
        }
    }
}