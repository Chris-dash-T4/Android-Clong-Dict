package edu.cmu.androidstuco.clongdict

import android.content.Context
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.BackgroundColorSpan
import android.text.style.StyleSpan
import androidx.core.content.ContextCompat
import java.text.Normalizer

/** Normalize IPA for comparison and for the deviation mirror line. */
object PronunciationCompare {
    fun normalizeForCompare(s: String): String {
        var t = s.trim()
        if (t.length >= 2 && t.startsWith("/") && t.endsWith("/")) {
            t = t.substring(1, t.length - 1).trim()
        }
        // Strip extraneous whitespace
        var collapsed = t.replace("\\s+".toRegex(), " ")
        // Remove syllable boundaries
        collapsed = collapsed.replace("\\.".toRegex(), "")
        return Normalizer.normalize(collapsed, Normalizer.Form.NFC)
    }

    /**
     * Builds a line showing [enteredRaw] after [normalizeForCompare], with an amber background on
     * code points that are not on one longest-common-subsequence alignment with [baselineNormalized]
     * (which must already be normalized).
     */
    fun buildDeviationStrip(context: Context, enteredRaw: String, baselineNormalized: String): CharSequence {
        val enteredNorm = normalizeForCompare(enteredRaw)
        if (enteredNorm.isEmpty()) {
            return SpannableStringBuilder("")
        }
        if (enteredNorm == baselineNormalized) {
            return SpannableStringBuilder(context.getString(R.string.pronunciation_matches_suggestion)).apply {
                setSpan(StyleSpan(Typeface.ITALIC), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        val a = enteredNorm.toCodePointArray()
        val b = baselineNormalized.toCodePointArray()
        val onLcs = markLcsOnPath(a, b)
        val color = ContextCompat.getColor(context, R.color.pronunciation_deviation_bg)
        val out = SpannableStringBuilder(enteredNorm)
        var cp = 0
        while (cp < onLcs.size) {
            if (!onLcs[cp]) {
                val startCp = cp
                while (cp < onLcs.size && !onLcs[cp]) cp++
                val start = enteredNorm.offsetByCodePoints(0, startCp)
                val end = enteredNorm.offsetByCodePoints(0, cp)
                out.setSpan(BackgroundColorSpan(color), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            } else {
                cp++
            }
        }
        return out
    }

    private fun String.toCodePointArray(): IntArray {
        val n = codePointCount(0, length)
        val out = IntArray(n)
        var i = 0
        var j = 0
        while (j < n) {
            val cp = codePointAt(i)
            out[j++] = cp
            i += Character.charCount(cp)
        }
        return out
    }

    /** Marks code point indices in [a] that lie on one LCS alignment with [b]. */
    private fun markLcsOnPath(a: IntArray, b: IntArray): BooleanArray {
        val n = a.size
        val m = b.size
        val dp = Array(n + 1) { IntArray(m + 1) }
        for (i in 1..n) {
            for (j in 1..m) {
                dp[i][j] = if (a[i - 1] == b[j - 1]) {
                    dp[i - 1][j - 1] + 1
                } else {
                    maxOf(dp[i - 1][j], dp[i][j - 1])
                }
            }
        }
        val onPath = BooleanArray(n)
        var i = n
        var j = m
        while (i > 0 && j > 0) {
            when {
                a[i - 1] == b[j - 1] && dp[i][j] == dp[i - 1][j - 1] + 1 -> {
                    onPath[i - 1] = true
                    i--
                    j--
                }
                dp[i - 1][j] >= dp[i][j - 1] -> i--
                else -> j--
            }
        }
        return onPath
    }
}
