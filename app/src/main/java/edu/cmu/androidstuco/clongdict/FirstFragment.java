package edu.cmu.androidstuco.clongdict;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import edu.cmu.androidstuco.clongdict.databinding.FragmentFirstBinding;

public class FirstFragment extends Fragment {

    private FragmentFirstBinding binding;

    protected RecyclerView mRecyclerView;
    protected NewDictAdapter mAdapter = null;
    protected RecyclerView.LayoutManager mLayoutManager;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentFirstBinding.inflate(inflater, container, false);
        View rootView = binding.getRoot();

        mRecyclerView = rootView.findViewById(R.id.recView1);
        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);

        MainActivity a = (MainActivity) requireActivity();
        DictionaryRepository.loadCollection(a.db, ConWord.lang, () -> {
            if (!isAdded()) return;
            mAdapter = new NewDictAdapter();
            mRecyclerView.setAdapter(mAdapter);
            int pos;
            if (getArguments() != null && (pos = getArguments().getInt("pos", -1)) >= 0
                    && mAdapter.getItemCount() > 0) {
                mRecyclerView.postDelayed(() ->
                        mRecyclerView.scrollToPosition(Math.min(pos + 6, mAdapter.getItemCount() - 1)),
                        500);
            }
        });

        return rootView;
    }

    @SuppressLint("ResourceType")
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ((FloatingActionButton) this.requireActivity().findViewById(R.id.fab)).setImageResource(android.R.drawable.ic_input_add);
    }

    @Override
    public void onResume() {
        super.onResume();
        MainActivity a = (MainActivity) getActivity();
        if (a == null || mRecyclerView == null || mAdapter == null) return;
        if (a.pendingDictionaryScrollPos < 0) return;
        int pos = a.pendingDictionaryScrollPos;
        a.pendingDictionaryScrollPos = -1;
        if (mAdapter.getItemCount() <= 0) return;
        int target = Math.min(pos + 6, mAdapter.getItemCount() - 1);
        mRecyclerView.postDelayed(() -> mRecyclerView.scrollToPosition(target), 100);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}
