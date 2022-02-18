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
        mAdapter = new DictAdapter(a.db,ConWord.lang); // TODO make not hardcoded
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

}