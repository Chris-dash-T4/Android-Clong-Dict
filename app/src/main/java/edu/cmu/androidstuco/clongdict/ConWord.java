package edu.cmu.androidstuco.clongdict;

import android.graphics.Typeface;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;

import java.util.stream.IntStream;

public class ConWord implements CharSequence {
    public static CharSequence alphabet = null;
    public static CharSequence ignored = null;
    public static Typeface clongTypeface;
    public static String lang = null; //"huoxinde-jazk"; // May change depending on defaults
    private String word;
    private CharSequence sortString;
    private boolean refreshSorts = false;

    public ConWord(String s) {
        word=s;
        sortString = null;
    }

    public CharSequence getSortString() {
        if (sortString == null || refreshSorts) {
            StringBuilder s0 = new StringBuilder();
            if (alphabet == null) return null;
            for (char c : word.toCharArray()) {
                int v = alphabet.toString().indexOf(c);
                if (v >= 0) s0.append((char) v);
                else if (ignored.toString().indexOf(c) == -1) s0.append((char) alphabet.length()+c);
            }
            //compute sortString
            sortString = s0;
            if (refreshSorts) refreshSorts = false;
        }
        return sortString;
    }

    @Override
    public int length() {
        return word.length();
    }

    @Override
    public char charAt(int i) {
        return word.charAt(i);
    }

    @NonNull
    @Override
    public CharSequence subSequence(int i, int j) {
        return new ConWord(word.substring(i,j));
    }

    @NonNull
    @Override
    public IntStream chars() {
        // TODO
        return null;
    }

    @NonNull
    @Override
    public IntStream codePoints() {
        // TODO
        return null;
    }

    //TODO

    @Override
    public String toString() {
        return word;
    }

    public void updateAlphabet() {
        refreshSorts = true;
    }
}
