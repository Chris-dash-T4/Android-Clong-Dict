#![allow(non_snake_case)] // JNI `Java_*` exports must match JVM name mangling exactly

mod disk_cache;

use jni::objects::{JClass, JObject, JObjectArray, JString, JValue};
use jni::sys::jobject;
use jni::JNIEnv;
use ndarray::Array2;
use reqwest::blocking::Client;
use serde_json::json;
use std::collections::HashMap;
use std::path::PathBuf;
use std::sync::{LazyLock, Mutex};
use anyhow::Result;

static CACHE: LazyLock<Mutex<HashMap<String, Vec<f64>>>> =
    LazyLock::new(|| Mutex::new(HashMap::new()));
static EMBEDDING_API_URL: &str = "https://fahmiaziz-api-embedding.hf.space/api/v1/embeddings";
static EMBEDDING_MODEL: &str = "qwen3-0.6b";
static MAX_BATCH_SIZE: usize = 256;
static MAX_CACHE_SIZE: usize = 65536;
static TIMEOUT: std::time::Duration = std::time::Duration::from_secs(MAX_BATCH_SIZE as u64);

/// Absolute path to the embedding cache file (e.g. `context.filesDir/embedding_cache.bin`).
/// Unset until JNI [nativeSetEmbeddingCachePath]; disk read/write is skipped until then.
static EMBEDDING_DISK_PATH: LazyLock<Mutex<Option<PathBuf>>> =
    LazyLock::new(|| Mutex::new(None));

fn embedding_disk_path() -> Option<PathBuf> {
    EMBEDDING_DISK_PATH.lock().unwrap().clone()
}

fn jni_throw_illegal_argument(env: &mut JNIEnv, msg: &str) {
    if let Err(e) = env.throw_new("java/lang/IllegalArgumentException", msg) {
        eprintln!("JNI ThrowNew(IllegalArgumentException) failed: {e}");
    }
}

fn jni_throw_runtime(env: &mut JNIEnv, err: anyhow::Error) {
    let msg = format!("{err:#}");
    if let Err(e) = env.throw_new("java/lang/RuntimeException", msg) {
        eprintln!("JNI ThrowNew(RuntimeException) failed: {e}");
    }
}

#[no_mangle]
pub extern "system" fn Java_edu_cmu_androidstuco_clongdict_rust_SemanticSearchHelper_nativeSetEmbeddingCachePath(
    mut env: JNIEnv,
    _class: JClass,
    path: JString,
) {
    let s: String = env.get_string(&path).expect("embedding cache path utf-8").into();
    *EMBEDDING_DISK_PATH.lock().unwrap() = Some(PathBuf::from(s));
}

#[no_mangle]
pub extern "system" fn Java_edu_cmu_androidstuco_clongdict_rust_SemanticSearchHelper_nativeSemanticSearch(
    mut env: JNIEnv,
    _class: JClass,
    query: JString,
    documents: JObjectArray,
    ids: JObjectArray,
) -> jobject {
    let query: String = env.get_string(&query).expect("query utf").into();
    let documents = jobject_array_to_strings(&mut env, &documents);
    let ids = jobject_array_to_strings(&mut env, &ids);
    if documents.len() != ids.len() {
        jni_throw_illegal_argument(
            &mut env,
            "documents and ids arrays must have the same length",
        );
        return JObject::null().into_raw();
    }
    let before = atomic_get_cache_size();
    let results = match semantic_search(&query, &documents, &ids) {
        Ok(v) => v,
        Err(e) => {
            jni_throw_runtime(&mut env, e);
            return JObject::null().into_raw();
        }
    };
    let after = atomic_get_cache_size();
    if after > before {
        spawn_disk_write_thread();
    }
    let out = env
        .new_double_array(results.len() as jni::sys::jsize)
        .expect("new_double_array");
    if !results.is_empty() {
        env.set_double_array_region(&out, 0, &results)
            .expect("set_double_array_region");
    }
    out.into_raw()
}

#[no_mangle]
pub extern "system" fn Java_edu_cmu_androidstuco_clongdict_rust_SemanticSearchHelper_nativeGetEmbeddings(
    mut env: JNIEnv,
    _class: JClass,
    documents: JObjectArray,
    ids: JObjectArray,
) -> jni::sys::jsize {
    let documents = jobject_array_to_strings(&mut env, &documents);
    let ids = jobject_array_to_strings(&mut env, &ids);
    if documents.len() != ids.len() {
        jni_throw_illegal_argument(
            &mut env,
            "documents and ids arrays must have the same length",
        );
        return -1;
    }
    let before = atomic_get_cache_size();
    // Ignore the embeddings, we just need to fetch them from the API
    if let Err(e) = get_embeddings_with_progress(&mut env, &documents, &ids) {
        jni_throw_runtime(&mut env, e);
        return -1 as jni::sys::jsize;
    }
    let after = atomic_get_cache_size();
    let delta = after as isize - before as isize;
    if delta > 0 {
        spawn_disk_write_thread();
    }
    delta as jni::sys::jsize
}

fn atomic_get_cache_size() -> usize {
    let cache = CACHE.lock().unwrap();
    cache.len()
}

fn spawn_disk_write_thread() {
    let Some(path) = embedding_disk_path() else {
        eprintln!("embedding disk cache: path not set; call SemanticSearchHelper.setEmbeddingCachePath (files dir)");
        return;
    };
    let snapshot = CACHE.lock().unwrap().clone();
    if (snapshot.len() > MAX_CACHE_SIZE) {
        eprintln!("embedding disk cache: cache size {} exceeds max {}", snapshot.len(), MAX_CACHE_SIZE);
        // TODO: evict some entries instead of writing an excessively large cache file
    }
    let _ = std::thread::Builder::new()
        .name("embedding-disk-cache".to_string())
        .spawn(move || {
            if let Err(e) = disk_cache::write_inmemory_cache(
                &snapshot,
                path.as_path(),
                EMBEDDING_MODEL,
                EMBEDDING_API_URL,
            ) {
                eprintln!("embedding disk cache write failed: {e}");
            }
        })
        .map_err(|e| eprintln!("failed to spawn embedding disk cache thread: {e}"));
}

fn jobject_array_to_strings(env: &mut JNIEnv, array: &JObjectArray) -> Vec<String> {
    let len = env.get_array_length(array).expect("documents array length");
    let mut out = Vec::with_capacity(len as usize);
    for i in 0..len {
        let el = env
            .get_object_array_element(array, i)
            .expect("document string element");
        let js = JString::from(el);
        out.push(env.get_string(&js).expect("document utf").into());
    }
    out
}

/**
 * Performs semantic search for [query] and returns the results.
 * [documents] and [ids] must have the same length.
 * Caller must ensure that id1 == id2 => document1 == document2. Converse is not required.
 */
fn semantic_search(query: &str, documents: &[String], ids: &[String]) -> Result<Vec<f64>> {
    if documents.is_empty() {
        return Ok(Vec::new());
    }
    let document_embeddings = get_embeddings(documents, ids, None)?;
    let query_embedding = get_embeddings(&[query.to_string()], &[String::from("query-literal:") + query], None)?;
    let similarities = document_embeddings.dot(&query_embedding.t());
    Ok(similarities.iter().copied().collect())
}

fn get_embeddings_with_progress(
    env: &mut JNIEnv,
    documents: &[String],
    ids: &[String],
) -> Result<Array2<f64>> {
    // Helper class is also the caller, if it doesn't exist, there are bigger problems.
    let helper_class = env.find_class("edu/cmu/androidstuco/clongdict/rust/SemanticSearchHelper")?;

    let mut progress_fn = |current: usize, total: usize| {
        let args = [
            JValue::Int(total as jni::sys::jint),
            JValue::Int(current as jni::sys::jint),
        ];
        if let Err(e) = env.call_static_method(
            &helper_class,
            "progressUpdate",
            "(II)V",
            &args,
        ) {
            eprintln!("progressUpdate JNI call failed: {e}");
        }
    };

    get_embeddings(documents, ids, Some(&mut progress_fn))
}

fn get_embeddings(
    documents: &[String],
    ids: &[String],
    mut progress_callback: Option<&mut dyn FnMut(usize, usize)>,
) -> Result<Array2<f64>> {
    assert_eq!(
        documents.len(),
        ids.len(),
        "documents and ids must have the same length"
    );
    if documents.is_empty() {
        return Ok(Array2::zeros((0, 0)));
    }
    let mut cache = CACHE.lock().unwrap();
    if let Some(ref cache_path) = embedding_disk_path() {
        let disk_cache = disk_cache::read_disk_cache(
            cache_path.as_path(),
            EMBEDDING_MODEL,
            EMBEDDING_API_URL,
        );
        match disk_cache {
            Ok(mem_cache) => {
                cache.extend(mem_cache);
            }
            Err(e) => {
                eprintln!("Error reading disk cache: {e}");
                eprintln!("Checking for backup cache...");
                let backup_cache_path = cache_path.with_extension("bak");
                let backup_cache = disk_cache::read_disk_cache(
                    &backup_cache_path,
                    EMBEDDING_MODEL,
                    EMBEDDING_API_URL,
                );
                match backup_cache {
                    Ok(mem_cache) => {
                        cache.extend(mem_cache);
                    }
                    Err(e) => {
                        eprintln!("Error reading backup cache: {e}");
                        eprintln!("Using in-memory cache only.");
                    }
                }
            }
        }
    }
    let needs_fetch: Vec<(&String, &String)> = documents
        .iter()
        .zip(ids.iter())
        .filter(|(_document, id)| !cache.contains_key(id.as_str()))
        .collect();
    let mut n_fetched = documents.len() - needs_fetch.len();
    if !needs_fetch.is_empty() {
        // Indicate initial progress before starting fetches
        if let Some(cb) = progress_callback.as_mut() {
            cb(n_fetched, documents.len());
        }
        // Fetch embeddings from API and cache them
        // Inline API request to fetch embeddings
        let client = Client::new();
        let input: Vec<&str> = needs_fetch.iter().map(|(document, _)| document.as_str()).collect();
        let num_batches = (input.len() + MAX_BATCH_SIZE - 1) / MAX_BATCH_SIZE;
        for batch_idx in 0..num_batches {
            let start = batch_idx * MAX_BATCH_SIZE;
            let end = (start + MAX_BATCH_SIZE).min(input.len());
            let batch = &input[start..end];
            let batch_pairs = &needs_fetch[start..end];
            let payload = json!({
                "input": batch,
                "model": EMBEDDING_MODEL,
                "options": {
                    "normalize_embeddings": true
                }
            });
            let response = client
                .post(EMBEDDING_API_URL)
                .json(&payload)
                .timeout(TIMEOUT)
                .send()?;
            let response_json: serde_json::Value = response.json()?;
            let new_embeddings: Vec<Vec<f64>> = response_json["data"]
                .as_array()
                .expect("data should be an array")
                .iter()
                .map(|item| {
                    item["embedding"]
                        .as_array()
                        .expect("embedding should be array")
                        .iter()
                        .map(|v| v.as_f64().expect("embedding values should be f64"))
                        .collect::<Vec<f64>>()
                })
                .collect();
            if new_embeddings.len() != batch_pairs.len() {
                return Err(anyhow::anyhow!("API must return one row per batched input, got {} rows for {} inputs", new_embeddings.len(), batch_pairs.len()));
            }
            for (idx, emb) in new_embeddings.into_iter().enumerate() {
                let (_, id) = batch_pairs[idx];
                cache.insert(id.to_string(), emb);
            }
            n_fetched += batch.len();
            if n_fetched < documents.len() {
                // Emit progress update after each batch
                if let Some(cb) = progress_callback.as_mut() {
                    cb(n_fetched, documents.len());
                }
                // Dispatch a disk write after each batch to persist progress and mitigate data loss on crashes
                drop(cache);
                spawn_disk_write_thread();
                cache = CACHE.lock().unwrap();
            }
        }
    }
    // Final progress update to indicate completion
    if let Some(cb) = progress_callback.as_mut() {
        cb(n_fetched, documents.len());
    }
    let n_rows = documents.len();
    let n_cols = cache
        .get(ids[0].as_str())
        .expect("embedding present after fetch")
        .len();
    let mut flat: Vec<f64> = Vec::with_capacity(n_rows * n_cols);
    for id in ids {
        flat.extend(
            cache
                .get(id.as_str())
                .expect("embedding in cache")
                .iter()
                .copied(),
        );
    }
    Ok(Array2::from_shape_vec((n_rows, n_cols), flat).expect("shape should match data"))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn get_embeddings_empty_yields_zero_matrix() {
        let m = get_embeddings(&[], &[], None).unwrap();
        assert_eq!(m.shape(), &[0, 0]);
    }

    /// Requires network and a live API; run with `cargo test -p semantic_search_jni -- --ignored`.
    #[test]
    #[ignore = "calls live embedding HTTP API"]
    fn test_get_embeddings() {
        let embeddings = get_embeddings(
            &["Hello, world!".to_string()],
            &[String::from("hello-world")],
            None,
        )
        .unwrap();
        assert_eq!(embeddings.shape(), [1, 1024]);
    }

    #[test]
    #[ignore = "calls live embedding HTTP API"]
    fn test_cache_delta() {
        // Unique ids so parallel `cargo test -- --ignored` runs do not share CACHE keys.
        let suffix = std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .unwrap()
            .as_nanos();
        let id_world = format!("hello-world-{suffix}");
        let id_universe = format!("hello-universe-{suffix}");
        if let Some(path) = embedding_disk_path() {
            let _ = std::fs::remove_file(path);
        }
        CACHE.lock().unwrap().clear();
        let _ = get_embeddings(
            &["Hello, world!".to_string()],
            &[id_world.clone()],
            None,
        )
        .unwrap();
        let before = CACHE.lock().unwrap().len();
        let documents = vec![
            "Hello, world!".to_string(),
            "Hello, universe!".to_string(),
        ];
        let _ = get_embeddings(
            &documents,
            &[id_world.clone(), id_universe],
            None,
        )
        .unwrap();
        let after = CACHE.lock().unwrap().len();
        assert_eq!(after - before, 1);
    }

    #[test]
    #[ignore = "calls live embedding HTTP API"]
    fn test_semantic_search() {
        let embeddings = get_embeddings(
            &["Hello, world!".to_string()],
            &[String::from("hello-world")],
            None,
        )
        .unwrap();
        let query_embedding = get_embeddings(
            &["Hello, world!".to_string()],
            &[String::from("query-literal:Hello, world!")],
            None,
        )
        .unwrap();
        let similarities = embeddings.dot(&query_embedding.t());
        assert_eq!(similarities.shape(), [1, 1]);
    }

    #[test]
    #[ignore = "calls live embedding HTTP API"]
    fn test_semantic_search_function() {
        let documents = vec![
            "Hello, world!".to_string(),
            "Hello, universe!".to_string(),
        ];
        let ids = vec![
            String::from("hello-world"),
            String::from("hello-universe"),
        ];
        let similarities = semantic_search(&"Hello, world!", &documents, &ids).unwrap();
        assert_eq!(similarities.len(), 2);
    }
}