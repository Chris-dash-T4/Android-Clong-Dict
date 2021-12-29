package edu.cmu.androidstuco.clongdict;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

/**
 * This class is not currently in use anywhere, but may be implemented later for data parsing, exports, etc.
 */
public class LingUtils {
    public static ArrayList<DictEntry> dataset;
    public static HashMap<Character,Character> phoneConverter;

    public static void initPhonemeConverter() {
        phoneConverter = new HashMap<>();
        char[][] conversions = {
                {'6','ą'},
        };
        for (char[] pair :
                conversions) {
            phoneConverter.put(new Character(pair[0]),new Character(pair[1]));
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
