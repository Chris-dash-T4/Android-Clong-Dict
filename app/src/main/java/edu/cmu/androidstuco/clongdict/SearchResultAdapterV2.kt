package edu.cmu.androidstuco.clongdict

import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import edu.cmu.androidstuco.clongdict.obj.NewDictEntry
import java.util.ArrayList
import java.util.Locale
import java.util.NavigableSet
import java.util.TreeSet

/**
 * Search results backed by a [fullDataSet] cache; [setQuery] only re-filters in memory instead of
 * reloading from Firestore on every intent.
 */
class SearchResultAdapterV2 : RecyclerView.Adapter<SearchResultAdapterV2.ViewHolder> {

    private val mDataSet = ArrayList<NewDictEntry>()
    private lateinit var fullDataSet: NavigableSet<NewDictEntry>
    private var query: String? = null

    /**
     * Headword order follows [Comparable] on [ConWord] (conlang alphabet via [ConWord.getSortString],
     * not raw UTF-16 string order). Remaining fields use normal string order; last tie-break is
     * [System.identityHashCode] so [TreeSet] never merges two distinct instances.
     */
    private val dictEntryLexOrder: Comparator<NewDictEntry> =
        compareBy<NewDictEntry> { it.word }
            .thenBy { it.pronunciation }
            .thenBy { it.lexCategory }
            .thenBy { it.definition }
            .thenBy { it.etymology }
            .thenComparingInt { System.identityHashCode(it) }

    /** Firestore collection id when this adapter loads from Firebase; null when using [LingUtils.dataset]. */
    private var backingCollectionPath: String? = null

    /** Used by [SearchActivity] to decide whether to recycle the adapter or rebuild after a language switch. */
    fun getBackingCollectionPath(): String? = backingCollectionPath

    /**
     * In-memory source: a snapshot of [LingUtils.dataset] at construction time (copied into a
     * [TreeSet]). External mutations to the list after that are not reflected unless the adapter
     * is recreated or entries are added via [pushElement].
     */
    constructor(query: String?) : super() {
        backingCollectionPath = null
        fullDataSet = TreeSet(dictEntryLexOrder)
        LingUtils.dataset?.let { fullDataSet.addAll(it) }
        this.query = query
        applyFilter()
    }

    /**
     * Load the collection once into [fullDataSet], then apply the initial [query] filter.
     */
    constructor(db: FirebaseFirestore, path: String, query: String?) : super() {
        backingCollectionPath = path
        this.query = query
        fullDataSet = TreeSet(dictEntryLexOrder)
        val mainHandler = Handler(Looper.getMainLooper())
        db.collection(path).get().addOnCompleteListener { task ->
            if (!task.isSuccessful || task.result == null) return@addOnCompleteListener
            val snap = task.result!!
            val rows = ArrayList<NewDictEntry>()
            for (doc: DocumentSnapshot in snap) {
                val data = doc.data ?: continue
                fun getKey(key: String): String {
                    val v = data[key] ?: return ""
                    return v.toString()
                }
                val lex = getKey("part_of_speech").ifEmpty { getKey("lex_category") }
                rows.add(
                    NewDictEntry(
                        ConWord(getKey("word")),
                        getKey("pronunciation"),
                        lex,
                        getKey("definition"),
                        getKey("etymology")
                    )
                )
            }
            mainHandler.post {
                fullDataSet.clear()
                fullDataSet.addAll(rows)
                applyFilter()
            }
        }
    }

    /**
     * Update the search string and re-filter from [fullDataSet] only (no Firestore / full rescan).
     */
    fun setQuery(query: String?) {
        this.query = query
        applyFilter()
    }

    private fun applyFilter() {
        mDataSet.clear()
        val q = query
        for (e in fullDataSet) {
            if (matchesQuery(e, q)) {
                mDataSet.add(e)
            }
        }
        notifyDataSetChanged()
    }

    private fun matchesQuery(e: NewDictEntry, query: String?): Boolean {
        if (query == null) return true
        val normalizedQuery = query.lowercase(Locale.ROOT)
        val ig = ConWord.ignored
        val wordMatches = if (ig != null && ig.isNotEmpty()) {
            e.word.toString().replace("[$ig]".toRegex(), "")
                .lowercase(Locale.ROOT).contains(normalizedQuery)
        } else {
            e.word.toString().lowercase(Locale.ROOT).contains(normalizedQuery)
        }
        return wordMatches ||
            e.definition.lowercase(Locale.ROOT).contains(normalizedQuery) ||
            e.etymology.lowercase(Locale.ROOT).contains(normalizedQuery)
    }

    @SuppressLint("ResourceType")
    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        private val wordView: TextView = v.findViewById(R.id.dictWordTV)
        private val defView: TextView = v.findViewById(R.id.dictDefTV)
        private val pronView: TextView = v.findViewById(R.id.dictPronTV)
        private val posView: TextView = v.findViewById(R.id.dictPoSTV)
        private val etymView: TextView = v.findViewById(R.id.dictEtymTV)

        init {
            ConWord.clongTypeface?.let { wordView.typeface = it }
            v.setOnClickListener {
                var c0: Context = v.context
                var a: SearchActivity? = null
                while (c0 is ContextWrapper && a == null) {
                    if (c0 is SearchActivity) a = c0
                    else c0 = c0.baseContext
                }
                if (a == null) return@setOnClickListener
                val disp = Intent(a, MainActivity::class.java)
                disp.putExtra("display_mode", true)
                disp.putExtra("word", wordView.text.toString())
                disp.putExtra("pron", pronView.text.toString())
                disp.putExtra("lexcat", posView.text.toString())
                disp.putExtra("def", defView.text.toString())
                disp.putExtra("etym", etymView.text.toString())
                a.startActivity(disp)
            }
        }

        fun getWordView(): TextView = wordView
        fun getDefView(): TextView = defView
        fun getPronView(): TextView = pronView
        fun getPosView(): TextView = posView
        fun getEtymView(): TextView = etymView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.dict_entry, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val row = mDataSet[position]
        holder.getWordView().text = row.word
        holder.getPronView().text = row.pronunciation
        holder.getPosView().text = row.lexCategory
        holder.getDefView().text = row.definition
        holder.getEtymView().text = row.etymology
    }

    override fun getItemCount(): Int = mDataSet.size

    fun pushElement(e: NewDictEntry) {
        fullDataSet.add(e)
        if (matchesQuery(e, query)) {
            applyFilter()
        }
    }
}
