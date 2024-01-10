package edu.cmu.androidstuco.clongdict

import java.lang.StringBuilder
import edu.cmu.androidstuco.clongdict.ConWord
import java.util.stream.IntStream
import android.graphics.Typeface

class ConWord(private val word: String) : CharSequence {
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