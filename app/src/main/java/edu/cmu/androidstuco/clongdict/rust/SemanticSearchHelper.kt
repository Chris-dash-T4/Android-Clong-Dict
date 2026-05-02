package edu.cmu.androidstuco.clongdict.rust

import java.io.File
import java.security.MessageDigest
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast

import edu.cmu.androidstuco.clongdict.obj.NewDictEntry
import edu.cmu.androidstuco.clongdict.SearchResultAdapterV2

/**
 * JNI entry points for `rust/clong_ime_jni` ([conlang-ime] headless engine).
 *
 * Typical usage: load schema once from assets or network, then [nativeRender] per headword.
 *
 * [conlang-ime]: sibling repo `clong-ime`, dependency path in `rust/clong_ime_jni/Cargo.toml`
 */
object SemanticSearchHelper {
    init {
        System.loadLibrary("semantic_search_jni")
    }

    @Volatile
    private var diskCachePathInitialized: Boolean = false

    /**
     * Must be called once with an app [Context] so Rust can read/write the embedding cache under
     * private app storage (not the process CWD, which is invalid on Android).
     */
    @Synchronized
    fun ensureEmbeddingDiskCachePath(context: Context) {
        if (diskCachePathInitialized) return
        val f = File(context.applicationContext.filesDir, "embedding_cache.bin")
        nativeSetEmbeddingCachePath(f.absolutePath)
        diskCachePathInitialized = true
    }

    @JvmStatic
    private external fun nativeSetEmbeddingCachePath(absolutePath: String)

    /**
     * Performs semantic search for [query] and returns the results.
     */
    /** Null if JNI refused the call (e.g. bad arrays); otherwise one score per document. */
    @JvmStatic
    external fun nativeSemanticSearch(query: String, documents: Array<String>, ids: Array<String>): DoubleArray?

    @JvmStatic
    external fun nativeGetEmbeddings(documents: Array<String>, ids: Array<String>): Int

    private fun dictEntryToDocumentString(entry: NewDictEntry): String {
        if (entry.definition.isEmpty()) {
            return entry.definition.toString()
        }
        return "${entry.definition.toString()}\n(Source: ${entry.etymology.toString()})"
    }

    private fun dictEntryToIdString(entry: NewDictEntry): String {
        val md5 = MessageDigest.getInstance("MD5")
        md5.update(entry.definition.toByteArray())
        md5.update(entry.etymology.toByteArray())
        val digest = md5.digest()
        val versionHash = digest.map { it.toInt().and(0xFF).toString(16).padStart(2, '0') }.joinToString("")
        return entry.id.toString()+":"+versionHash
    }

    fun semanticSearch(query: String, documents: Iterable<NewDictEntry>, toastContext: Context?): List<Pair<Double, NewDictEntry>> {
        val entries = documents.toList()
        if (entries.isEmpty()) return emptyList()
        val documentStrings = entries.map { dictEntryToDocumentString(it) }
        val ids = entries.map { dictEntryToIdString(it) }
        SearchResultAdapterV2.showToastSync("Starting semantic search...", Toast.LENGTH_SHORT)
        val raw = nativeSemanticSearch(
            query,
            documentStrings.toTypedArray(),
            ids.toTypedArray(),
        )
        val scores = raw?.toList().orEmpty()
        if (scores.size != entries.size) return emptyList()
        return scores.zip(entries).map { (score, entry) -> score to entry }
    }

    fun getEmbeddings(documents: Iterable<NewDictEntry>, toastContext: Context?): Int {
        SearchResultAdapterV2.showToastSync("Getting embeddings", Toast.LENGTH_SHORT)
        val documentStrings = documents.map { dictEntryToDocumentString(it) }
        val ids = documents.map { dictEntryToIdString(it) }
        try {
            val result = nativeGetEmbeddings(documentStrings.toTypedArray(), ids.toTypedArray())
            if (result < 0) {
                throw RuntimeException("Failed to get embeddings")
            }
            return result
        } catch (e: Exception) {
            SearchResultAdapterV2.showToastSync("Error getting embeddings: ${e.message}", Toast.LENGTH_LONG + 1)
            return -1
        }
    }

    @JvmStatic
    private fun progressUpdate(total: Int, current: Int) {
        SearchResultAdapterV2.showToastSync("Progress: $current/$total", Toast.LENGTH_SHORT)
    }
}
