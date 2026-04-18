package edu.cmu.androidstuco.clongdict.rust

/**
 * JNI entry points for `rust/clong_ime_jni` ([conlang-ime] headless engine).
 *
 * Typical usage: load schema once from assets or network, then [nativeRender] per headword.
 *
 * [conlang-ime]: sibling repo `clong-ime`, dependency path in `rust/clong_ime_jni/Cargo.toml`
 */
object ClongImeNative {
    init {
        System.loadLibrary("clong_ime_jni")
    }

    /** Empty when the last JNI call succeeded. */
    @JvmStatic
    external fun nativeGetLastError(): String

    /**
     * Parses TOML schema; returns an opaque handle or 0 on failure
     * (see [nativeGetLastError]). Caller must [nativeEngineDestroy].
     */
    @JvmStatic
    external fun nativeEngineCreate(schemaToml: String): Long

    @JvmStatic
    external fun nativeEngineDestroy(handle: Long)

    /**
     * Renders [word] with the given output format name (e.g. `font`, `ipa`, `latex`,
     * or `romanization`). Returns null on failure ([nativeGetLastError]).
     */
    @JvmStatic
    external fun nativeRender(handle: Long, word: String, format: String): String?
}
