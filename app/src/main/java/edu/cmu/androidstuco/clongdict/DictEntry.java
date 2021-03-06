package edu.cmu.androidstuco.clongdict;

import android.content.Intent;

public class DictEntry {
    // TODO make partOfSpeech user-determined.
    @Deprecated
    protected enum PartOfSpeech {
        NOUN,
        VERB,
        PARTICLE,
        UNDEFINED
    }
    private ConWord word;
    private String pronunciation;
    private String lexCat;
    private String definition;
    private String etymology;

    public DictEntry(String word, String pronunc, String lexCat, String def, String etym) {
        this.word=new ConWord(word);
        this.pronunciation=pronunc;
        this.lexCat=lexCat;
        this.definition=def;
        this.etymology=etym;
    }

    public ConWord getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = new ConWord(word);
    }

    public String getPronunciation() {
        return pronunciation;
    }

    public void setPronunciation(String pronunciation) {
        this.pronunciation = pronunciation;
    }

    public String getLexCat() {
        return lexCat;
    }

    public String getPartOfSpeech() {
        return lexCat;
        /*
        switch (lexCat) {
            case NOUN:
                return "n.";
            case VERB:
                return "v.";
            case PARTICLE:
                return "part.";
            default:
                return "unk.";
        }
        */
    }

    public void setLexCat(String lexCat) {
        this.lexCat = lexCat;
    }

    public String getDefinition() {
        return definition;
    }

    public void setDefinition(String definition) {
        this.definition = definition;
    }

    public String getEtymology() {
        return etymology;
    }

    public void setEtymology(String etymology) {
        this.etymology = etymology;
    }

    public String toString() {
        return this.word+"$"+this.pronunciation+"$"+this.lexCat+"$"+this.definition.replace('$','€')+"$"+this.etymology;
    }

    public String[] toStringArray() {
        return new String[]{this.word.toString(), this.pronunciation, this.lexCat, this.definition, this.etymology};
    }

}
