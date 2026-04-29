package edu.cmu.androidstuco.clongdict.rust

import java.security.MessageDigest
import edu.cmu.androidstuco.clongdict.obj.NewDictEntry
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast

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
        if (toastContext != null) {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(toastContext, "Semantic search started", Toast.LENGTH_SHORT).show()
            }
        }
        val raw = nativeSemanticSearch(query, documentStrings.toTypedArray(), ids.toTypedArray())
        val scores = raw?.toList().orEmpty()
        if (scores.size != entries.size) return emptyList()
        return scores.zip(entries).map { (score, entry) -> score to entry }
    }

    fun getEmbeddings(documents: Iterable<NewDictEntry>, toastContext: Context?): Int {
        if (toastContext != null) {
            Toast.makeText(toastContext, "Getting embeddings", Toast.LENGTH_SHORT).show()
        }
        val documentStrings = documents.map { dictEntryToDocumentString(it) }
        val ids = documents.map { dictEntryToIdString(it) }
        return nativeGetEmbeddings(documentStrings.toTypedArray(), ids.toTypedArray())
    }
}
