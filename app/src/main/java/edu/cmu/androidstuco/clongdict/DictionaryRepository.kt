package edu.cmu.androidstuco.clongdict

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import edu.cmu.androidstuco.clongdict.data.State
import edu.cmu.androidstuco.clongdict.obj.NewDictEntry

object DictionaryRepository {
    /**
     * Loads [path] into [State.entries] (keyed by document id) and refreshes [LingUtils.dataset]
     * for search adapters. Invokes [onComplete] on the caller thread when the task finishes
     * (success or failure).
     */
    @JvmStatic
    fun loadCollection(db: FirebaseFirestore, path: String, onComplete: Runnable) {
        State.entries.clear()
        db.collection(path).get().addOnCompleteListener { task ->
            if (task.isSuccessful && task.result != null) {
                val list = ArrayList<NewDictEntry>()
                for (doc: DocumentSnapshot in task.result!!) {
                    val data = doc.data ?: continue
                    fun get(key: String): String {
                        val v = data[key] ?: return ""
                        return v.toString()
                    }
                    val lex = get("part_of_speech").ifEmpty { get("lex_category") }
                    val e = NewDictEntry(
                        ConWord(get("word")),
                        get("pronunciation"),
                        lex,
                        get("definition"),
                        get("etymology")
                    )
                    State.entries[doc.id] = e
                    list.add(e)
                }
                LingUtils.dataset = list
            } else {
                LingUtils.dataset = ArrayList()
            }
            onComplete.run()
        }
    }
}
