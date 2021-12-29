package edu.cmu.androidstuco.clongdict;

import org.json.*;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.File;
import java.util.Locale;

import edu.cmu.androidstuco.clongdict.databinding.FragmentFirstBinding;

public class FirstFragment extends Fragment {

    private FragmentFirstBinding binding;

    protected RecyclerView mRecyclerView;
    protected DictAdapter mAdapter = null;
    protected RecyclerView.LayoutManager mLayoutManager;
    protected DictEntry[] mDataset;

    private static final int DATASET_COUNT = 6;
    private static String jsonifiedDict;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentFirstBinding.inflate(inflater, container, false);
        View rootView = binding.getRoot();

        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recView1);


        // LinearLayoutManager is used here, this will layout the elements in a similar fashion
        // to the way ListView would layout elements. The RecyclerView.LayoutManager defines how
        // elements are laid out.
        mLayoutManager = new LinearLayoutManager(getActivity());
        MainActivity a = (MainActivity) this.getActivity();
        mAdapter = new DictAdapter(a.db,"huoxinde-jazk"); // TODO make not hardcoded
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(mLayoutManager);
        int pos;
        if (getArguments() != null && (pos = getArguments().getInt("pos",-1)) >= 0)
            mRecyclerView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mRecyclerView.scrollToPosition(Math.min(pos+6,mAdapter.getItemCount()-1));
                }
            }, 500);
        return rootView;

    }

    @SuppressLint("ResourceType")
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ((FloatingActionButton) this.getActivity().findViewById(R.id.fab)).setImageResource(0x0108002b); //Plus symbol, ic_input_add
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /*
    private void initDataset() {
        mDataset = new DictEntry[DATASET_COUNT];
        MainActivity a = (MainActivity) this.getActivity();
        a.db.collection("huoxinde-jazk") // TODO make variable
            .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                int i = 0;
                for (QueryDocumentSnapshot doc :
                        task.getResult()) {
                    DictEntry.PartOfSpeech pos = DictEntry.PartOfSpeech.UNDEFINED;
                    if (((String)doc.getData().get("part_of_speech")).toLowerCase(Locale.ROOT).contains("n"))
                        pos = DictEntry.PartOfSpeech.NOUN;
                    if (((String)doc.getData().get("part_of_speech")).toLowerCase(Locale.ROOT).contains("v"))
                        pos = DictEntry.PartOfSpeech.VERB;
                    if (((String)doc.getData().get("part_of_speech")).toLowerCase(Locale.ROOT).contains("par"))
                        pos = DictEntry.PartOfSpeech.PARTICLE;
                    if (i>DATASET_COUNT) break;
                    mDataset[i] = new DictEntry((String) doc.getData().get("word"),
                            (String) doc.getData().get("pronunciation"),
                            pos,
                            (String) doc.getData().get("definition"),
                            (String) doc.getData().get("etymology")
                    );
                }
            }
        });
        /*
        // TODO get JSON from file not from text
        // File jsonData = new File(this.getActivity().getFilesDir(),"lang.json");
        jsonifiedDict = LingUtils.json;
        JSONTokener tokener = new JSONTokener(jsonifiedDict);
        int remaining = DATASET_COUNT;
        try {
            JSONArray jsonArray = (JSONArray) tokener.nextValue();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject entry = jsonArray.getJSONObject(i);
                DictEntry.PartOfSpeech pos = DictEntry.PartOfSpeech.UNDEFINED;
                if (entry.getString("part_speech").toLowerCase(Locale.ROOT).contains("n"))
                    pos = DictEntry.PartOfSpeech.NOUN;
                if (entry.getString("part_speech").toLowerCase(Locale.ROOT).contains("v"))
                    pos = DictEntry.PartOfSpeech.VERB;
                if (entry.getString("part_speech").toLowerCase(Locale.ROOT).contains("par"))
                    pos = DictEntry.PartOfSpeech.PARTICLE;
                mDataset[i] = new DictEntry(entry.getString("word"),
                                            entry.getString("pronunciation"),
                                            pos,
                                            entry.getString("definition"),
                                            entry.getString("etymology")
                                            );
                remaining--;
                if (remaining <= 0) {
                    break;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }*-/
        for (int i = 0; i < DATASET_COUNT; i++) {
            mDataset[i] = new DictEntry("word","ipa", DictEntry.PartOfSpeech.UNDEFINED, "def", "etym");
        }
    }
    */
}