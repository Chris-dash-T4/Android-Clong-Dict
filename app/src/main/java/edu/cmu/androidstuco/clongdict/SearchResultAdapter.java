package edu.cmu.androidstuco.clongdict;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * NOTE: Much of the starter code was unceremoniously yoinked from the Android GitHub page
 */
public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ViewHolder> {
    private static final String TAG = "DictAdapter";

    private ArrayList<DictEntry> mDataSet;

    // BEGIN_INCLUDE(recyclerViewSampleViewHolder)
    /**
     * Provide a reference to the type of views that you are using (custom ViewHolder)
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView wordView;
        private final TextView defView;
        private final TextView pronView;
        private final TextView posView;
        private final TextView etymView;

        public ViewHolder(View v) {
            super(v);
            // Define click listener for the ViewHolder's View.
            wordView = (TextView) v.findViewById(R.id.dictWordTV);
            wordView.setTypeface(ConWord.clongTypeface);
            defView = (TextView) v.findViewById(R.id.dictDefTV);
            pronView = (TextView) v.findViewById(R.id.dictPronTV);
            posView = (TextView) v.findViewById(R.id.dictPoSTV);
            etymView = (TextView) v.findViewById(R.id.dictEtymTV);
            v.setOnClickListener(new View.OnClickListener() {
                @SuppressLint("ResourceType")
                @Override
                public void onClick(View v) {

                    // thank you stackoverflow
                    Context c0 = v.getContext();
                    SearchActivity a = null;
                    while (c0 instanceof ContextWrapper && a==null) {
                        if (c0 instanceof SearchActivity) a = (SearchActivity) c0;
                        else c0 = ((ContextWrapper)c0).getBaseContext();
                    }

                    /** The following is from DictAdapter and does not work outside of MainActivity
                    FragmentManager fm = a.getSupportFragmentManager();
                    List<Fragment> fs = fm.getFragments();
                    //if (fs==null || fs.size()<1) Snackbar.make(v,"what",Snackbar.LENGTH_SHORT).show();

                    // Put the entry data in a bundle to send to the viewing fragment
                    Bundle b0 = new Bundle();
                    b0.putString("word",wordView.getText().toString());
                    b0.putString("pron",pronView.getText().toString());
                    b0.putString("def" , defView.getText().toString());
                    b0.putString("etym",etymView.getText().toString());
                    SecondFragment snd = new SecondFragment();
                    snd.setArguments(b0);
                    FragmentTransaction txn = fm.beginTransaction();
                    // allows for moving back to the list fragment w/o creating a new instance
                    txn.setReorderingAllowed(true);
                    txn.replace(R.id.fragment_container_view_tag,snd,null);
                    ((FloatingActionButton) a.findViewById(R.id.fab)).setImageResource(0x0108003e); // Pencil, ic_menu_edit
                    txn.commit();
                     */

                    Intent disp = new Intent(a,MainActivity.class);
                    disp.putExtra("display_mode",true);
                    disp.putExtra("word",wordView.getText().toString());
                    disp.putExtra("pron",pronView.getText().toString());
                    disp.putExtra("lexcat",posView.getText().toString());
                    disp.putExtra("def" , defView.getText().toString());
                    disp.putExtra("etym",etymView.getText().toString());
                    a.startActivity(disp);
                }
            });
        }

        public TextView getWordView() {
            return wordView;
        }

        public TextView getDefView() {
            return defView;
        }

        public TextView getPronView() {
            return pronView;
        }

        public TextView getEtymView() {
            return etymView;
        }

        public TextView getPosView() {
            return posView;
        }
    }
    // END_INCLUDE(recyclerViewSampleViewHolder)

    /**
     * Initialize the dataset of the Adapter.
     *
     * @param db FirebaseFirestore representing the database
     * @param path a String representing the collection in db that contains data for the current language
     * @param query a String representing the current search query
     */
    public SearchResultAdapter(FirebaseFirestore db, String path, String query) {
        mDataSet = new ArrayList<>();
        /*
        final String[] langInfo = new String[2];
        db.collection("languages").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                for (DocumentSnapshot doc :
                        task.getResult()) {
                    if (doc.get("path") == path) {
                        langInfo[0] = (String) doc.get("alphabet");
                        langInfo[1] = (String) doc.get("ignored");
                    }
                }
                sortData(db, path, query, langInfo[0], langInfo[1]);
            }
        });
        */
        db.collection(path).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                // Populates the dataSet with entries from Firebase
                for (DocumentSnapshot doc :
                        task.getResult()) {
                    DictEntry e = new DictEntry((String) doc.getData().get("word"),
                            (String) doc.getData().get("pronunciation"),
                            (String) doc.getData().get("part_of_speech"),
                            (String) doc.getData().get("definition"),
                            (String) doc.getData().get("etymology")
                    );
                    String ignored;
                    try {
                        ignored = ConWord.ignored.toString();
                        if (ConWord.ignored == null) throw new NullPointerException();
                    }
                    catch (Exception exp) {
                        ignored = "`´";
                    }
                    if (query == null)
                        mDataSet.add(e);
                    else if (e.getWord().toString().toLowerCase(Locale.ROOT)
                            .replaceAll("["+ignored+"]","").indexOf(query) != -1)
                        mDataSet.add(e);
                    else if (e.getDefinition().toLowerCase(Locale.ROOT)
                            .replaceAll("["+ignored+"]","").indexOf(query) != -1)
                        mDataSet.add(e);
                    else if (e.getEtymology().toLowerCase(Locale.ROOT)
                            .replaceAll("["+ignored+"]","").indexOf(query) != -1)
                        mDataSet.add(e);
                }
                /*
                Comparator<DictEntry> c = new Comparator<DictEntry>() {
                    @Override
                    public int compare(DictEntry e1, DictEntry e2) {
                        if (e1 == null || e2 == null) throw new NullPointerException();
                        // characters in ignored are explicitly removed from the counting
                        char[] w1 = e1.getWord().replaceAll("["+ignored+"]", "").toCharArray();
                        char[] w2 = e2.getWord().replaceAll("["+ignored+"]", "").toCharArray();
                        int minLen = Math.min(w1.length, w2.length);
                        for (int i = 0; i < minLen; i++) {
                            if (alphabet.indexOf(w1[i]) < alphabet.indexOf(w2[i])) return -1;
                            if (alphabet.indexOf(w1[i]) > alphabet.indexOf(w2[i])) return 1;
                            // characters not in the alphabet and not ignored will be sorted by unicode value
                            if (alphabet.indexOf(w1[i]) == -1) {
                                if (Character.getNumericValue(w1[i]) < Character.getNumericValue(w2[i]))
                                    return -1;
                                if (Character.getNumericValue(w1[i]) > Character.getNumericValue(w2[i]))
                                    return 1;
                            }
                            //@assert w1[i]===w2[i]
                        }
                        if (w1.length < w2.length) return -1;
                        if (w1.length > w2.length) return 1;
                        return 0;
                    }
                };
                mDataSet.sort(c);
                 */
                // Refresh the View to show the newly-loaded entries
                SearchResultAdapter.this.notifyDataSetChanged();
            }
        });
    }

    /**
     * Initialize the dataset of the Adapter by importing the data from a different adapter.
     *
     * @param query a String representing the current search query
     */
    public SearchResultAdapter(String query) {
        //LingUtils.dataset; // can't send objects thru intents? no problem.
        mDataSet = new ArrayList<>();
        for (int i=0; i<LingUtils.dataset.size(); i++) {
            DictEntry e = LingUtils.dataset.get(i);
            if (query == null) mDataSet.add(e);
            else if (e.getWord().toString().replaceAll("[" + ConWord.ignored + "]", "").contains(query)
                    || e.getDefinition().toLowerCase(Locale.ROOT).contains(query)
                    || e.getEtymology().toLowerCase(Locale.ROOT).contains(query)) {
                mDataSet.add(e);
            }
        }
    }

    /*
    private void sortData(FirebaseFirestore db, String path, String query, String alphabet, String ignored) {
    }
     */

    // BEGIN_INCLUDE(recyclerViewOnCreateViewHolder)
    // Create new views (invoked by the layout manager)
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        // Create a new view.
        View v = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.dict_entry, viewGroup, false);

        return new ViewHolder(v);
    }
    // END_INCLUDE(recyclerViewOnCreateViewHolder)

    // BEGIN_INCLUDE(recyclerViewOnBindViewHolder)
    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {
        //Log.d(TAG, "Element " + position + " set.");
        System.out.println("em: "+position+" set.");

        // Get element from your dataset at this position and replace the contents of the view
        // with that element
        viewHolder.getWordView().setText(mDataSet.get(position).getWord());
        viewHolder.getPronView().setText(mDataSet.get(position).getPronunciation());
        viewHolder.getPosView().setText(mDataSet.get(position).getPartOfSpeech());
        viewHolder.getDefView().setText(mDataSet.get(position).getDefinition());
        viewHolder.getEtymView().setText(mDataSet.get(position).getEtymology());
    }
    // END_INCLUDE(recyclerViewOnBindViewHolder)

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataSet.size();
    }

    public void pushElement(DictEntry e) {
        this.mDataSet.add(0,e);
        return;
    }
}
