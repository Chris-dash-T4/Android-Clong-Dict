//! JNI bridge for `conlang-ime` (headless). Loaded by Android as `libclong_ime_jni.so`.
//!
//! Java class: `edu.cmu.androidstuco.clongdict.rust.ClongImeNative`

use std::ffi::c_void;
use std::sync::Mutex;

use conlang_ime::{Engine, Renderer, Schema};
use jni::objects::{JClass, JString};
use jni::sys::{jlong, JNI_VERSION_1_6};
use jni::JNIEnv;

static LAST_ERROR: Mutex<String> = Mutex::new(String::new());

fn set_last_error(msg: impl Into<String>) {
    if let Ok(mut g) = LAST_ERROR.lock() {
        *g = msg.into();
    }
}

fn clear_last_error() {
    if let Ok(mut g) = LAST_ERROR.lock() {
        g.clear();
    }
}

#[no_mangle]
pub extern "system" fn Java_edu_cmu_androidstuco_clongdict_rust_ClongImeNative_nativeGetLastError(
    env: JNIEnv,
    _class: JClass,
) -> jni::sys::jstring {
    let msg = LAST_ERROR.lock().map(|g| g.clone()).unwrap_or_default();
    env.new_string(msg)
        .map(|s| s.into_raw())
        .unwrap_or(std::ptr::null_mut())
}

#[no_mangle]
pub extern "system" fn Java_edu_cmu_androidstuco_clongdict_rust_ClongImeNative_nativeEngineCreate(
    mut env: JNIEnv,
    _class: JClass,
    schema_toml: JString,
) -> jlong {
    clear_last_error();
    let toml: String = match env.get_string(&schema_toml) {
        Ok(s) => s.into(),
        Err(e) => {
            set_last_error(format!("JNI get_string (schema): {e}"));
            return 0;
        }
    };
    let schema = match Schema::from_str(&toml) {
        Ok(s) => s,
        Err(e) => {
            set_last_error(format!("{e}"));
            return 0;
        }
    };
    Box::into_raw(Box::new(schema)) as jlong
}

#[no_mangle]
pub unsafe extern "system" fn Java_edu_cmu_androidstuco_clongdict_rust_ClongImeNative_nativeEngineDestroy(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
) {
    if handle == 0 {
        return;
    }
    drop(Box::from_raw(handle as *mut Schema));
}

#[no_mangle]
pub extern "system" fn Java_edu_cmu_androidstuco_clongdict_rust_ClongImeNative_nativeRender(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    word: JString,
    format: JString,
) -> jni::sys::jstring {
    clear_last_error();
    if handle == 0 {
        set_last_error("invalid engine handle (0)");
        return std::ptr::null_mut();
    }
    let word: String = match env.get_string(&word) {
        Ok(s) => s.into(),
        Err(e) => {
            set_last_error(format!("JNI get_string (word): {e}"));
            return std::ptr::null_mut();
        }
    };
    let format: String = match env.get_string(&format) {
        Ok(s) => s.into(),
        Err(e) => {
            set_last_error(format!("JNI get_string (format): {e}"));
            return std::ptr::null_mut();
        }
    };
    let schema = unsafe { &*(handle as *const Schema) };
    let engine = Engine::new(schema);
    let renderer = Renderer::new(schema);
    let tokens = engine.tokenise(&word);
    let rendered = match renderer.render(&tokens, &format) {
        Ok(s) => s,
        Err(e) => {
            set_last_error(format!("{e}"));
            return std::ptr::null_mut();
        }
    };
    match env.new_string(rendered) {
        Ok(s) => s.into_raw(),
        Err(e) => {
            set_last_error(format!("JNI new_string: {e}"));
            std::ptr::null_mut()
        }
    }
}

#[no_mangle]
pub extern "system" fn JNI_OnLoad(_vm: jni::JavaVM, _: *mut c_void) -> jni::sys::jint {
    JNI_VERSION_1_6
}
