package edu.cmu.androidstuco.clongdict;

import android.content.Context;
import android.content.ContextWrapper;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.snackbar.Snackbar;

import edu.cmu.androidstuco.clongdict.databinding.FragmentSecondBinding;

public class SecondFragment extends Fragment {

    private FragmentSecondBinding binding;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentSecondBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = this.getArguments();
        if (args != null) {
            binding.wordDisplay.setText(args.getString("word"));
            binding.pronunciation.setText(args.getString("pron"));
            binding.definition.setText(args.getString("def"));
            binding.etymology.setText(args.getString("etym"));
        }
        //else binding.wordDisplay.setText(Integer.toString(this.getId()));
        binding.buttonSecond.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Context c0 = view.getContext();
                MainActivity a = null;
                while (c0 instanceof ContextWrapper && a==null) {
                    if (c0 instanceof MainActivity) a = (MainActivity) c0;
                    else c0 = ((ContextWrapper)c0).getBaseContext();
                }
                FragmentManager fm = a.getSupportFragmentManager();
                // Return to list fragment
                fm.beginTransaction().replace(R.id.fragment_container_view_tag,FirstFragment.class,null).commit();
            }
        });

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}