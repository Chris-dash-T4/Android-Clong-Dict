package edu.cmu.androidstuco.clongdict

import android.content.Context
import android.graphics.Typeface
import java.lang.StringBuilder
import java.util.stream.IntStream

import edu.cmu.androidstuco.clongdict.rust.ClongImeNative

class ConWord(private val word: String) : CharSequence, Comparable<ConWord> {
    private var sortString: CharSequence? = null
    private var refreshSorts = false
    override val length = word.length
    fun getSortString(): CharSequence? {
        if (sortString == null || refreshSorts) {
            val s0 = StringBuilder()
            if (alphabet == null) return null
            for (c in word.toCharArray()) {
                val v = alphabet.toString().indexOf(c)
                if (v >= 0) s0.append(v.toChar()) else if (ignored.toString().indexOf(c) == -1) s0.append(alphabet!!.length.toChar() + c.toInt())
            }
            //compute sortString
            sortString = s0
            if (refreshSorts) refreshSorts = false
        }
        return sortString
    }

    /**
     * Order words by the active conlang [alphabet]: [getSortString] keys are compared first
     * (same ordering as iterating letters in script order). If sort keys are unavailable, falls
     * back to plain Unicode [word] order so ordering stays total and deterministic.
     */
    override fun compareTo(other: ConWord): Int {
        val sa = getSortString()
        val sb = other.getSortString()
        if (sa != null && sb != null) {
            val c = lexCompareCharSequences(sa, sb)
            if (c != 0) return c
        } else if (sa != null) return -1
        else if (sb != null) return 1
        return word.compareTo(other.word)
    }

    private fun lexCompareCharSequences(a: CharSequence, b: CharSequence): Int {
        val n = minOf(a.length, b.length)
        for (i in 0 until n) {
            val d = a[i].code.compareTo(b[i].code)
            if (d != 0) return d
        }
        return a.length.compareTo(b.length)
    }

    override fun get(i: Int): Char {
        return word[i]
    }

    override fun subSequence(i: Int, j: Int): CharSequence {
        return ConWord(word.substring(i, j))
    }

    override fun chars(): IntStream? {
        // TODO
        return null
    }

    override fun codePoints(): IntStream? {
        // TODO
        return null
    }

    //TODO
    override fun toString(): String {
        return word
    }

    fun updateAlphabet() {
        refreshSorts = true
    }

    companion object {
        @JvmField
        var alphabet: CharSequence? = null
        @JvmField
        var ignored: CharSequence? = null
        @JvmField
        var clongTypeface: Typeface? = null
        @JvmField
        var lang: String? = null //"huoxinde-jazk"; // May change depending on defaults

        /**
         * Asset path under `assets/` for the IME TOML schema (e.g. `hxj.toml`).
         * Change before [createEngine] when switching conlangs; call [destroyEngine] first if replacing an existing engine.
         */
        @JvmField
        var imeSchemaAsset: String = "hxj.toml"

        /** 0 = no engine or failed init. Use from Java as `ConWord.engineHandle == 0`. */
        @JvmField
        var engineHandle: Long = 0

        /** Release the native schema/engine (e.g. before loading a different [imeSchemaAsset] or after a failed init). */
        @JvmStatic
        fun destroyEngine() {
            if (engineHandle != 0L) {
                ClongImeNative.nativeEngineDestroy(engineHandle)
                engineHandle = 0L
            }
        }

        /** Loads [imeSchemaAsset] from [Context.getAssets] and creates the native engine. */
        @JvmStatic
        fun createEngine(context: Context) {
            if (engineHandle != 0L) return
            val schemaToml = context.assets.open(imeSchemaAsset).bufferedReader().use { it.readText() }
            engineHandle = ClongImeNative.nativeEngineCreate(schemaToml)
        }

        /**
         * Renders [headword] with the native IME engine (e.g. [format] `"ipa"`, `"font"`).
         * Ensures [createEngine]; returns null if the engine is unavailable or render fails.
         */
        @JvmStatic
        fun renderWithEngine(context: Context, headword: String, format: String): String? {
            if (headword.isBlank()) return null
            createEngine(context.applicationContext)
            if (engineHandle == 0L) return null
            return ClongImeNative.nativeRender(engineHandle, headword, format)
        }
    }
}