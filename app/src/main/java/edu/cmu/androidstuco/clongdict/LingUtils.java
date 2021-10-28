package edu.cmu.androidstuco.clongdict;

import java.util.HashMap;
import java.util.Objects;

public class LingUtils {
    public static HashMap<Char,Char> phoneConverter;
    public static final String json = "[{\"word\":\"ao´\",\"pronunciation\":\"áU\",\"part_speech\":\"A/N\",\"definition\":\"Blue. The primary color (RGB) with the most energy. Light with a wavelength around 470 nm.\",\"etymology\":\"Japanese: 青[あお]}\"},{\"word\":\"ao`xa´ju`\",\"pronunciation\":\"àU.Sá.j\\`u\",\"part_speech\":\"Noun\",\"definition\":\"Laser. Plasma or plasma-based weapon.\",\"etymology\":\"Unknown\"},{\"word\":\"a`bje´di´ni`cę`\",\"pronunciation\":\"à.bjé.dí.n\\`i.ts\\`9\",\"part_speech\":\"Verb\",\"definition\":\"To unite.\",\"etymology\":\"Russian: объединять}\"},{\"word\":\"a`pe´lse´\",\"pronunciation\":\"à.pél.sé\",\"part_speech\":\"A/N\",\"definition\":\"Butterscotch, golden-brown. The color of the Martian sky.\",\"etymology\":\"Russian: апельсин}\"},{\"word\":\"a`dżi´vao´\",\"pronunciation\":\"à.dZí.váU\",\"part_speech\":\"Verb\",\"definition\":\"To taste. To experience the flavor of (something), or to test the quality of such flavor.\",\"etymology\":\"Japanese: 味わう[あじわう]}\"},{\"word\":\"a`ta´m\",\"pronunciation\":\"à.tám\",\"part_speech\":\"Noun\",\"definition\":\"Head. The uppermost part of the human body, containing the brain and face.\",\"etymology\":\"Japanese: 頭[あたま]}\"},{\"word\":\"a`tti´va`\",\"pronunciation\":\"à.t:í.v\\`a\",\"part_speech\":\"Noun\",\"definition\":\"An asset or resource, wealth. A useful or otherwise desirable thing, or access to such things.\",\"etymology\":\"Russian: актив}\"}]";

    public static void initPhonemeConverter() {
        phoneConverter = new HashMap<>();
        char[][] conversions = {
                {'6','ą'},
        };
        for (char[] pair :
                conversions) {
            phoneConverter.put(new Char(pair[0]),new Char(pair[1]));
        }
    }

    public static String SAMPAtoIPA(String in) {
        String out = in;
        for (Char sampa :
                phoneConverter.keySet()) {
            Char ipa = phoneConverter.get(sampa);
            out.replace(sampa.c, ipa.c);
        }
        return out;
    }

    private static class Char {
        public char c;
        public Char(char c) {this.c=c;}

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Char aChar = (Char) o;
            return c == aChar.c;
        }

        @Override
        public int hashCode() {
            return Objects.hash(c);
        }
    }
}
