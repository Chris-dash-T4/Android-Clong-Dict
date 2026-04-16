package edu.cmu.androidstuco.clongdict

import android.graphics.Typeface
import java.lang.StringBuilder
import java.util.stream.IntStream

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
    }
}