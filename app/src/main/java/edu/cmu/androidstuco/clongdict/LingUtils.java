package edu.cmu.androidstuco.clongdict;

import java.util.ArrayList;
import java.util.HashMap;

import edu.cmu.androidstuco.clongdict.obj.NewDictEntry;

/** Shared dictionary rows for search and phoneme helpers. */
public class LingUtils {
    public static ArrayList<NewDictEntry> dataset;
    /** When true, ConWord sort keys are refreshed on the next dictionary list build. */
    public static boolean resetAlph = false;
    public static HashMap<Character,Character> phoneConverter;

    public static void initPhonemeConverter() {
        phoneConverter = new HashMap<>();
        char[][] conversions = {
                {'6','ą'},
        };
        for (char[] pair :
                conversions) {
            phoneConverter.put(pair[0], pair[1]);
        }
    }

    public static String SAMPAtoIPA(String in) {
        String out = in;
        for (Character sampa :
                phoneConverter.keySet()) {
            Character ipa = phoneConverter.get(sampa.charValue());
            out.replace(sampa.charValue(), ipa.charValue());
        }
        return out;
    }
}
