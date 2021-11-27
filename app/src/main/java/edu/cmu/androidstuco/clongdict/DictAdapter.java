package edu.cmu.androidstuco.clongdict;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * NOTE: most of this code was unceremoniously yoinked from the Android GitHub page
 */
public class DictAdapter extends RecyclerView.Adapter<DictAdapter.ViewHolder> {
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
        private final TextView etymView;

        public ViewHolder(View v) {
            super(v);
            // Define click listener for the ViewHolder's View.
            wordView = (TextView) v.findViewById(R.id.dictWordTV);
            defView = (TextView) v.findViewById(R.id.dictDefTV);
            pronView = (TextView) v.findViewById(R.id.dictPronTV);
            etymView = (TextView) v.findViewById(R.id.dictEtymTV);
            v.setOnClickListener(new View.OnClickListener() {
                @SuppressLint("ResourceType")
                @Override
                public void onClick(View v) {
                    //Log.d(TAG, "Element " + getAdapterPosition() + " clicked.");
                    //System.out.println(getAdapterPosition() + " clicked");

                    // thank you stackoverflow
                    Context c0 = v.getContext();
                    MainActivity a = null;
                    while (c0 instanceof ContextWrapper && a==null) {
                        if (c0 instanceof MainActivity) a = (MainActivity) c0;
                        else c0 = ((ContextWrapper)c0).getBaseContext();
                    }
                    if (a==null) Snackbar.make(v,"thing is not", Snackbar.LENGTH_SHORT);//.show();
                    else Toast.makeText(a,getAdapterPosition()+" clicked", Toast.LENGTH_LONG);//.show();
                    FragmentManager fm = a.getSupportFragmentManager();//.getFragments();
                    List<Fragment> fs = fm.getFragments();
                    if (fs==null || fs.size()<1) Snackbar.make(v,"what",Snackbar.LENGTH_SHORT).show();
                    // TODO get entryFragment textviews
                    Bundle b0 = new Bundle();
                    b0.putString("word",wordView.getText().toString());
                    b0.putString("pron",pronView.getText().toString());
                    b0.putString("def" , defView.getText().toString());
                    b0.putString("etym",etymView.getText().toString());
                    SecondFragment snd = new SecondFragment();
                    snd.setArguments(b0);
                    FragmentTransaction txn = fm.beginTransaction();
                    txn.setReorderingAllowed(true);
                    txn.replace(R.id.fragment_container_view_tag,snd,null);
                    ((FloatingActionButton) a.findViewById(R.id.fab)).setImageResource(0x0108003e); // Pencil, ic_menu_edit
                    txn.commit();
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
    }
    // END_INCLUDE(recyclerViewSampleViewHolder)

    /**
     * Initialize the dataset of the Adapter.
     *
     * @param dataSet DictEntry[] containing the data to populate views to be used by RecyclerView.
     */
    public DictAdapter(DictEntry[] dataSet) {
        mDataSet = new ArrayList<>(Arrays.asList(dataSet));
        mDataSet.add(0,new DictEntry("ArrayList Moment","sæm.pl", DictEntry.PartOfSpeech.UNDEFINED,"Западный текст",""));
    }

    public DictAdapter(FirebaseFirestore db,String path) {
        mDataSet = new ArrayList<>();
        db.collection(path).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                for (DocumentSnapshot doc :
                        task.getResult()) {
                    DictEntry.PartOfSpeech pos = DictEntry.PartOfSpeech.UNDEFINED;
                    if (((String)doc.getData().get("part_of_speech")).toLowerCase(Locale.ROOT).contains("n"))
                        pos = DictEntry.PartOfSpeech.NOUN;
                    if (((String)doc.getData().get("part_of_speech")).toLowerCase(Locale.ROOT).contains("v"))
                        pos = DictEntry.PartOfSpeech.VERB;
                    if (((String)doc.getData().get("part_of_speech")).toLowerCase(Locale.ROOT).contains("par"))
                        pos = DictEntry.PartOfSpeech.PARTICLE;
                    DictEntry e = new DictEntry((String) doc.getData().get("word"),
                            (String) doc.getData().get("pronunciation"),
                            pos,
                            (String) doc.getData().get("definition"),
                            (String) doc.getData().get("etymology")
                    );
                    mDataSet.add(e);
                }
                DictAdapter.this.notifyDataSetChanged();
            }
        });
    }

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
