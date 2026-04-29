#![allow(non_snake_case)] // JNI `Java_*` exports must match JVM name mangling exactly

use jni::objects::{JClass, JObjectArray, JString};
use jni::sys::jobject;
use jni::JNIEnv;
use ndarray::Array2;
use reqwest::blocking::Client;
use serde_json::json;
use std::collections::HashMap;
use std::sync::{LazyLock, Mutex};

static CACHE: LazyLock<Mutex<HashMap<String, Vec<f64>>>> =
    LazyLock::new(|| Mutex::new(HashMap::new()));
static EMBEDDING_API_URL: &str = "https://fahmiaziz-api-embedding.hf.space/api/v1/embeddings";
static EMBEDDING_MODEL: &str = "qwen3-0.6b";

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
        let empty = env.new_double_array(0).expect("new_double_array empty");
        return empty.into_raw();
    }
    let results = semantic_search(&query, &documents, &ids);
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
        return -1;
    }
    let mut delta = 0;
    // The improved atomic version (whole operation under one lock):
    {
        let cache = CACHE.lock().unwrap();
        let before = cache.len();
        // Ignore the embeddings, we just need to fetch them from the API
        let _embeddings = {
            // Drop the cache lock before calling get_embeddings; get_embeddings will also lock CACHE internally!
            drop(cache);
            get_embeddings(&documents, &ids)
        };
        // reacquire lock to get 'after' size
        let after = CACHE.lock().unwrap().len();
        delta += after as isize - before as isize;
    }
    delta as jni::sys::jsize
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
fn semantic_search(query: &str, documents: &[String], ids: &[String]) -> Vec<f64> {
    if documents.is_empty() {
        return Vec::new();
    }
    let document_embeddings = get_embeddings(documents, ids);
    let query_embedding = get_embeddings(&[query.to_string()], &[String::from("query-literal:") + query]);
    let similarities = document_embeddings.dot(&query_embedding.t());
    similarities.iter().copied().collect()
}

fn get_embeddings(documents: &[String], ids: &[String]) -> Array2<f64> {
    assert_eq!(
        documents.len(),
        ids.len(),
        "documents and ids must have the same length"
    );
    if documents.is_empty() {
        return Array2::zeros((0, 0));
    }
    let mut cache = CACHE.lock().unwrap();
    let needs_fetch: Vec<(&String, &String)> = documents
        .iter()
        .zip(ids.iter())
        .filter(|(_document, id)| !cache.contains_key(id.as_str()))
        .collect();
    if !needs_fetch.is_empty() {
        // Fetch embeddings from API and cache them
        // Inline API request to fetch embeddings
        let client = Client::new();
        let input: Vec<&str> = needs_fetch.iter().map(|(document, _)| document.as_str()).collect();
        let payload = json!({
            "input": input,
            "model": EMBEDDING_MODEL,
            "options": {
                "normalize_embeddings": true
            }
        });

        let response = client
            .post(EMBEDDING_API_URL)
            .json(&payload)
            .send()
            .unwrap();

        let response_json: serde_json::Value = response.json().unwrap();

        // Assume the API returns { "data": [ { "embedding": [f64, ...] }, ... ] }
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
        for ((_, id), emb) in needs_fetch.into_iter().zip(new_embeddings.into_iter()) {
            cache.insert(id.clone(), emb);
        }
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
    Array2::from_shape_vec((n_rows, n_cols), flat).expect("shape should match data")
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn get_embeddings_empty_yields_zero_matrix() {
        let m = get_embeddings(&[], &[]);
        assert_eq!(m.shape(), &[0, 0]);
    }

    /// Requires network and a live API; run with `cargo test -p semantic_search_jni -- --ignored`.
    #[test]
    #[ignore = "calls live embedding HTTP API"]
    fn test_get_embeddings() {
        let embeddings = get_embeddings(
            &["Hello, world!".to_string()],
            &[String::from("hello-world")],
        );
        assert_eq!(embeddings.shape(), [1, 1024]);
    }

    #[test]
    #[ignore = "calls live embedding HTTP API"]
    fn test_cache_delta() {
        let _ = get_embeddings(
            &["Hello, world!".to_string()],
            &[String::from("hello-world")],
        );
        let before = CACHE.lock().unwrap().len();
        let documents = vec![
            "Hello, world!".to_string(),
            "Hello, universe!".to_string(),
        ];
        let _ = get_embeddings(
            &documents,
            &[
                String::from("hello-world"),
                String::from("hello-universe"),
            ],
        );
        let after = CACHE.lock().unwrap().len();
        assert_eq!(after - before, 1);
    }

    #[test]
    #[ignore = "calls live embedding HTTP API"]
    fn test_semantic_search() {
        let embeddings = get_embeddings(
            &["Hello, world!".to_string()],
            &[String::from("hello-world")],
        );
        let query_embedding = get_embeddings(&["Hello, world!".to_string()], &[String::from("query-literal:Hello, world!")]);
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
        let similarities = semantic_search(&"Hello, world!", &documents, &ids);
        assert_eq!(similarities.len(), 2);
    }
}