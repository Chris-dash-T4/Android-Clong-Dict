package edu.cmu.androidstuco.clongdict;

import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Trace;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.text.style.UnderlineSpan;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
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

    private Spannable format(String s) {
        String[] conwords = s.replace("@","").split("[{}]");
        String s0 = "";
        for (int i = 0; i < conwords.length; i+=2) {
            s0 += conwords[i];
        }
        SpannableStringBuilder out = new SpannableStringBuilder(s0.replace("*",""));
        String[] format = s0.split("\\*");
        int prev = 0;
        for (int i = 1; i < format.length; i+=2) {
            int a = prev+format[i-1].length();
            int b = a+format[i].length();
            out.setSpan(new StyleSpan(Typeface.ITALIC),a,b, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            prev = b;
        }
        prev = 0;
        for (int i = 1; i < conwords.length; i+=2) {
            int a = prev+conwords[i-1].length();
            int b = a+conwords[i].length();
            out.insert(a,conwords[i]);
            out.setSpan(new TypefaceSpan(ConWord.clongTypeface),a,b, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            prev = b;
        }
        return out;
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = this.getArguments();
        if (args != null) {
            binding.wordDisplay.setText(args.getString("word"));
            binding.wordDisplay.setTypeface(ConWord.clongTypeface);
            binding.pronunciation.setText(args.getString("pron"));
            binding.partOfSpeech.setText(args.getString("lexcat"));
            Spannable formatDef = format(args.getString("def"));
            binding.definition.setText(formatDef);
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
                Bundle b0 = new Bundle();
                if (args.getInt("pos",-1) >= 0) b0.putInt("pos",args.getInt("pos"));
                // Return to list fragment
                fm.beginTransaction().replace(R.id.fragment_container_view_tag,FirstFragment.class,b0).commit();
            }
        });

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}