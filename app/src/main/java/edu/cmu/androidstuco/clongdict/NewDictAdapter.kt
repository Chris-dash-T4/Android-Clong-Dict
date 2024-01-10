package edu.cmu.androidstuco.clongdict

import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import edu.cmu.androidstuco.clongdict.data.State
import edu.cmu.androidstuco.clongdict.obj.NewDictEntry

class NewDictAdapter : RecyclerView.Adapter<NewDictAdapter.ViewHolder>() {
    /**
     * Provide a reference to the type of views that you are using (custom ViewHolder)
     */
    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val wordView: TextView = v.findViewById<View>(R.id.dictWordTV) as TextView
        val defView: TextView = v.findViewById<View>(R.id.dictDefTV) as TextView
        val pronView: TextView = v.findViewById<View>(R.id.dictPronTV) as TextView
        val posView: TextView = v.findViewById<View>(R.id.dictPoSTV) as TextView
        val etymView: TextView = v.findViewById<View>(R.id.dictEtymTV) as TextView

        init {
            // Define click listener for the ViewHolder's View.
            wordView.typeface = ConWord.clongTypeface
            v.setOnClickListener { v1 -> // thank you stackoverflow
                var c0: Context = v1.context
                while (c0 is ContextWrapper && c0 !is MainActivity) {
                    c0 = c0.baseContext
                }
                val a = c0 as MainActivity
                val fm = a.supportFragmentManager
                val fs = fm.fragments
                //if (fs==null || fs.size()<1) Snackbar.make(v,"what",Snackbar.LENGTH_SHORT).show();

                // Put the entry data in a bundle to send to the viewing fragment
                val b0 = Bundle()
                b0.putString("word", wordView.text.toString())
                b0.putString("pron", pronView.text.toString())
                b0.putString("lexcat", posView.text.toString())
                b0.putString("def", defView.text.toString())
                b0.putString("etym", etymView.text.toString())
                b0.putInt("pos", adapterPosition)
                val snd = SecondFragment()
                snd.arguments = b0
                val txn = fm.beginTransaction()
                // allows for moving back to the list fragment w/o creating a new instance
                txn.setReorderingAllowed(true)
                txn.replace(R.id.fragment_container_view_tag, snd, null)
                (a.findViewById<View>(R.id.fab) as FloatingActionButton).setImageResource(android.R.drawable.ic_menu_edit)
                txn.commit()
            }
        }
    }

    lateinit var dataset : Array<String>

    init {
        // ultimately we should maintain just a list of ID's or some such, that are pre-sorted and point to a hashmap
        val order : Comparator<String> = Comparator { x,y ->
            val (abcOrder,ignore) = State.alphabet
            val s1 = State.entries[x]?.word?.replace(Regex.fromLiteral("/[$ignore]*/g"),"")
            val s2 = State.entries[y]?.word?.replace(Regex.fromLiteral("/[$ignore]*/g"),"")
            if (s1 == null || s2 == null) return@Comparator 0

            val max = if (s2.length > s1.length) s2.length else s1.length
            for (i in (0..max)) {
                val c1 = s1[i]
                val c2 = s2[i]
                if (abcOrder.indexOf(c1) < abcOrder.indexOf(c2)) return@Comparator -1
                if (abcOrder.indexOf(c1) > abcOrder.indexOf(c2)) return@Comparator 1
            }
            return@Comparator when {
                   s2.length > s1.length -> -1
                   s1.length > s2.length -> 1
                   else -> 0
               }
        }
        /*
        var swaws = mutableListOf<String>();
        State.entries.keys.forEach { key ->
            val negativeInsertionPoint = swaws.binarySearch(key,order);
            assert(negativeInsertionPoint < 0)
            val insertionPoint = -(negativeInsertionPoint + 1)
            swaws.add(insertionPoint,key);
        }
         */
        //dataset = swaws.toTypedArray()
        dataset = State.entries.keys.toSortedSet(order).toTypedArray()
    }

    // BEGIN_INCLUDE(recyclerViewOnCreateViewHolder)
    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view.
        val v = LayoutInflater.from(viewGroup.context)
                .inflate(R.layout.dict_entry, viewGroup, false)
        return ViewHolder(v)
    }
    // END_INCLUDE(recyclerViewOnCreateViewHolder)

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

        // Get element from your dataset at this position and replace the contents of the view
        // with that element
        val current : NewDictEntry = State.entries[dataset[position]]!!

        viewHolder.wordView.text = current.word
        viewHolder.pronView.text = current.pronunciation
        viewHolder.posView.text = current.lexCategory
        viewHolder.defView.text = current.definition
        viewHolder.etymView.text = current.etymology
    }

    override fun getItemCount(): Int {
        return dataset.size;
    }

}