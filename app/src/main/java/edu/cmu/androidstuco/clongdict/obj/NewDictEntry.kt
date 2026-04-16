package edu.cmu.androidstuco.clongdict.obj

import edu.cmu.androidstuco.clongdict.ConWord

// TODO make `definition` into an FString
class NewDictEntry(var word: ConWord, var pronunciation: String, var lexCategory: String, var definition: String, var etymology: String) {

    fun setWord(word: String) {
        this.word = ConWord(word)
    }

    override fun toString(): String {
        return word.toString() + "$" + this.pronunciation + "$" + this.lexCategory + "$" + this.definition.replace('$', '€') + "$" + etymology
    }

    fun toStringArray(): Array<String> {
        return arrayOf(word.toString(), pronunciation, lexCategory, definition.replace('$', '€'), etymology)
    }
}