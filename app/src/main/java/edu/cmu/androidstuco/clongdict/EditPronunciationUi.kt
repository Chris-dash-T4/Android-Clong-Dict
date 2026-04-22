package edu.cmu.androidstuco.clongdict

import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import edu.cmu.androidstuco.clongdict.databinding.ActivityEditBinding
import edu.cmu.androidstuco.clongdict.rust.ClongImeNative
import kotlin.math.max
import kotlin.math.min

/**
 * Edit screen: **Auto IPA** recomputes the engine line from the headword; **Use IPA** copies it
 * into the field (replace all). Long-press **Use IPA** for insert-at-cursor / replace-selection / append.
 */
object EditPronunciationUi {
    @JvmStatic
    fun attach(activity: AppCompatActivity, binding: ActivityEditBinding) {
        Controller(activity, binding).attach()
    }
}

private class Controller(
    private val activity: AppCompatActivity,
    private val binding: ActivityEditBinding,
) {
    /** NFC-trimmed IPA comparable to [PronunciationCompare.normalizeForCompare] on the engine line. */
    private var baselineNormalized: String? = null

    /** Last raw IPA string returned by the engine (what we paste into the field). */
    private var lastEngineIpa: String? = null

    fun attach() {
        binding.pronunciationSuggestBtn.setOnClickListener { onSuggestFromHeadword() }
        binding.pronunciationApplyBtn.setOnClickListener { applyReplaceAll() }
        binding.pronunciationApplyBtn.setOnLongClickListener { anchor ->
            showInsertAppendMenu(anchor)
            true
        }
        binding.pronunciation.addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    refreshDeviationUi()
                }
            },
        )
    }

    private fun onSuggestFromHeadword() {
        val word = binding.word.text?.toString()?.trim().orEmpty()
        if (word.isEmpty()) {
            Toast.makeText(activity, R.string.pronunciation_need_headword, Toast.LENGTH_SHORT).show()
            return
        }
        val ipa = ConWord.renderWithEngine(activity, word, "ipa")
        if (ipa == null) {
            val err = ClongImeNative.nativeGetLastError()
            Toast.makeText(
                activity,
                if (err.isNullOrBlank()) activity.getString(R.string.pronunciation_engine_error_generic) else err,
                Toast.LENGTH_LONG,
            ).show()
            return
        }
        lastEngineIpa = ipa
        baselineNormalized = PronunciationCompare.normalizeForCompare(ipa)
        binding.pronunciationApplyBtn.isEnabled = true
        binding.pronunciationBaselineCaption.text =
            activity.getString(R.string.pronunciation_baseline_caption, ipa)
        binding.pronunciationBaselineCaption.visibility = View.VISIBLE
        binding.pronunciationDeviationHint.visibility = View.VISIBLE
        binding.pronunciationDeviationStrip.visibility = View.VISIBLE
        if (binding.pronunciation.text?.isBlank() != false) {
            binding.pronunciation.setText(ipa)
        }
        refreshDeviationUi()
    }

    private fun applyReplaceAll() {
        val ipa = lastEngineIpa ?: return
        binding.pronunciation.setText(ipa)
        binding.pronunciation.setSelection(ipa.length)
        refreshDeviationUi()
    }

    private fun showInsertAppendMenu(anchor: View) {
        val ipa = lastEngineIpa
        if (ipa == null) {
            Toast.makeText(activity, R.string.pronunciation_need_auto_first, Toast.LENGTH_SHORT).show()
            return
        }
        PopupMenu(activity, anchor, Gravity.END).apply {
            menuInflater.inflate(R.menu.pronunciation_apply_menu, menu)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_insert_ipa_at_cursor -> {
                        insertAtCursorOrReplaceSelection(ipa)
                        true
                    }
                    R.id.menu_append_ipa -> {
                        appendIpa(ipa)
                        true
                    }
                    else -> false
                }
            }
            show()
        }
    }

    private fun insertAtCursorOrReplaceSelection(ipa: String) {
        val et = binding.pronunciation
        val cur = et.text?.toString().orEmpty()
        val start = min(et.selectionStart, et.selectionEnd).coerceIn(0, cur.length)
        val end = max(et.selectionStart, et.selectionEnd).coerceIn(0, cur.length)
        val newText = cur.replaceRange(start, end, ipa)
        et.setText(newText)
        val newPos = (start + ipa.length).coerceIn(0, newText.length)
        et.setSelection(newPos)
        refreshDeviationUi()
    }

    private fun appendIpa(ipa: String) {
        val et = binding.pronunciation
        val cur = et.text?.toString().orEmpty()
        val sep = when {
            cur.isEmpty() -> ""
            cur.endsWith('\n') || cur.endsWith(' ') -> ""
            else -> " "
        }
        val newText = cur + sep + ipa
        et.setText(newText)
        et.setSelection(newText.length)
        refreshDeviationUi()
    }

    private fun refreshDeviationUi() {
        val base = baselineNormalized
        if (base == null) {
            binding.pronunciationDeviationStrip.visibility = View.GONE
            binding.pronunciationDeviationHint.visibility = View.GONE
            return
        }
        binding.pronunciationDeviationStrip.visibility = View.VISIBLE
        val entered = binding.pronunciation.text?.toString().orEmpty()
        if (entered.isBlank()) {
            binding.pronunciationDeviationStrip.text =
                activity.getString(R.string.pronunciation_enter_to_compare)
            return
        }
        binding.pronunciationDeviationStrip.text =
            PronunciationCompare.buildDeviationStrip(activity, entered, base)
    }
}
