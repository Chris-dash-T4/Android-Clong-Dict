package edu.cmu.androidstuco.clongdict.obj

import edu.cmu.androidstuco.clongdict.ConWord

class FString(val repr : String) {
    enum class Format {
        Normal,
        Bold,
        Italic,
        BoldItalic,
        Conlang
    }

    val outlinks : List<ConWord>;
    val text : List<Pair<String,Format>>;

    init {
        var outlinksPart = mutableListOf<ConWord>();
        var textPart = mutableListOf<Pair<String,Format>>();
        val sep = """((?<!\\)[*_]+|@\{|\})""".toRegex()
        var matchResult = sep.find(repr)
        var style = Format.Normal
        var prevStart = 0
        var beforeConWord = style;
        while (matchResult != null) {
            val endOfText = matchResult!!.range.first
            val nextStart = matchResult!!.range.last + 1
            val length = matchResult!!.range.count()

            val update : Pair<Format,Boolean> = when (Pair(Pair(length,matchResult!!.value == "@{"),style)) {
                // found `@{`, open new conword
                Pair(Pair(2,true),Format.Normal), Pair(Pair(2,true),Format.Bold), Pair(Pair(2,true),Format.Italic), Pair(Pair(2,true),Format.BoldItalic) -> Pair(Format.Conlang,true)
                // found single `*` or `_`; toggle italics
                Pair(Pair(1,false),Format.Normal) -> Pair(Format.Italic,true)
                Pair(Pair(1,false),Format.Bold) -> Pair(Format.BoldItalic,true)
                Pair(Pair(1,false),Format.Italic) -> Pair(Format.Normal,true)
                Pair(Pair(1,false),Format.BoldItalic) -> Pair(Format.Bold,true)
                // assume there aren't any stray `_` or `*` characters within a conword entry
                Pair(Pair(1,false),Format.Conlang) -> if (matchResult!!.value == "}") Pair(beforeConWord,true) else Pair(Format.Conlang,false)
                // found double `**` or `__`; toggle bold
                Pair(Pair(2,false),Format.Normal) -> Pair(Format.Bold,true)
                Pair(Pair(2,false),Format.Italic) -> Pair(Format.BoldItalic,true)
                Pair(Pair(2,false),Format.Bold) -> Pair(Format.Normal,true)
                Pair(Pair(2,false),Format.BoldItalic) -> Pair(Format.Italic,true)
                // found triple `***` or `___`; toggle bold+italic
                Pair(Pair(3,false),Format.Normal) -> Pair(Format.BoldItalic,true)
                Pair(Pair(3,false),Format.Italic) -> Pair(Format.Bold,true)
                Pair(Pair(3,false),Format.Bold) -> Pair(Format.Italic,true)
                Pair(Pair(3,false),Format.BoldItalic) -> Pair(Format.Normal,true)
                else -> Pair(style,false)
            }
            if (update.second) {
                val sub = repr.subSequence(prevStart,endOfText) as String
                textPart.add(Pair(sub,style))
                if (style == Format.Conlang)

                style = update.first
                prevStart = nextStart
            }

            matchResult = matchResult.next();
        }

        outlinks = outlinksPart;
        text = textPart;
    }

    override fun toString() : String {
        val fmt = this.text.map { val (s,f) = it; when (f) {
            Format.Normal -> s
            Format.Bold -> "*${s}*"
            Format.Italic -> "__${s}__"
            Format.BoldItalic -> "***${s}***"
            Format.Conlang -> "@{${s}}"
        }}
        val out = fmt.joinToString("")
        return out
    }
}