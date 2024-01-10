package edu.cmu.androidstuco.clongdict.obj

import edu.cmu.androidstuco.clongdict.ConWord

// TODO make `definition` into an FString
class NewDictEntry(val word : ConWord, val pronunciation : String, val lexCategory: String, val definition : String, val etymology : String) {


    override fun toString(): String {
        return word.toString() + "$" + pronunciation + "$" + this.lexCategory + "$" + definition.replace('$', '€') + "$" + etymology
    }
}