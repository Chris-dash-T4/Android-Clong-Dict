package edu.cmu.androidstuco.clongdict.data

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.google.firebase.firestore.FirebaseFirestore
import edu.cmu.androidstuco.clongdict.ConWord
import edu.cmu.androidstuco.clongdict.R
import edu.cmu.androidstuco.clongdict.obj.NewDictEntry

data class State(val dataSource: FirebaseFirestore, val context: Context) {
    companion object {
        lateinit var languagesList : HashMap<String,String>
        lateinit var alphabet : Pair<CharSequence,CharSequence>
        lateinit var typeface: Typeface
        var language : String? = null
        var entries : HashMap<String, NewDictEntry> = HashMap()
        var initialized = false
    }

    init {
        // Language metadata is stored in firebase
        // not much is done with this now, but I plan to expand it so users can switch between languages
        dataSource.collection("languages")
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    for (document in task.result!!) {
                        //Toast.makeText(MainActivity.this,document.getId() + " => " + document.getData(), Toast.LENGTH_LONG).show();
                        languagesList.put(document.data["path"]!! as String, document.data["Name"]!! as String)
                        // TODO re-integrate with drawer and other such
                        if (language == document.data["language"]) {
                            alphabet = Pair(document.data["alphabet"]!! as CharSequence, document.data["ignored"]!! as CharSequence)
                            var fontFamily: Int = R.font.liberation_serif_bold_italic
                            if (language == "huoxinde-jazk") fontFamily = R.font.yu_martian_bold
                            typeface = Typeface.create(
                                    ResourcesCompat.getFont(context, fontFamily),
                                    Typeface.NORMAL
                            )
                        }
                    }
                    initialized = true
                } else {
                    System.err.println("Error getting documents.")
                    task.exception!!.printStackTrace()
                }
            };
    }

    fun loadLanguage(lang : String /* idr if it should be an int or a string */) {
        if (!languagesList.containsKey(lang)) { return; }

        language = lang;
        dataSource.collection(lang)
                .get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // TODO
                    } else {
                        System.err.println("Error getting documents.")
                        task.exception!!.printStackTrace()
                    }
                };
    }

    fun lookup(entry : String) : Pair<ConWord,String>? {


        return null;
    }
}
