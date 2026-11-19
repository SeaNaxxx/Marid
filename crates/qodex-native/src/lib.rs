//! Qodex native performance module (Phase 2 baseline).
//!
//! Ships as a JNI `cdylib` (`libqodex_native.so` in `jniLibs/arm64-v8a/`),
//! loaded in-process via `System.loadLibrary("qodex_native")`. This avoids the
//! W^X trap entirely: shared libraries are mapped by the Android linker, never
//! `execve()`'d from writable app storage.
//!
//! Scope guard (architecture directive): this crate NEVER owns agent
//! orchestration or UI logic — it is a leaf accelerator for workspace
//! traversal and pattern matching only.

use std::fmt::Write as _;

use ignore::WalkBuilder;
use jni::objects::{JClass, JString};
use jni::sys::jstring;
use jni::JNIEnv;
use regex::RegexBuilder;

const BINARY_SNIFF_BYTES: usize = 8 * 1024;
const MAX_OUTPUT_CHARS: usize = 1 << 20;

fn to_string(env: &mut JNIEnv, value: &JString) -> String {
    // jni 0.21: JavaStr has no Display; convert via its owned-String From impl.
    // get_string needs &mut JNIEnv (the UTF-8 char release mutates the frame).
    env.get_string(value).map(String::from).unwrap_or_default()
}

fn return_jstring(env: &mut JNIEnv, value: &str) -> jstring {
    let trimmed: String = if value.len() > MAX_OUTPUT_CHARS {
        value.chars().take(MAX_OUTPUT_CHARS).collect()
    } else {
        value.to_string()
    };
    env.new_string(&trimmed)
        .map(|s| s.into_raw())
        .unwrap_or(std::ptr::null_mut())
}

/// Newline-delimited file paths under `root`, honoring .gitignore/.ignore.
#[no_mangle]
pub extern "system" fn Java_com_qodex_app_native_QodexNative_walkFiles(
    mut env: JNIEnv,
    _class: JClass,
    root: JString,
    max_entries: i32,
) -> jstring {
    let root = to_string(&mut env, &root);
    if root.is_empty() || !std::path::Path::new(&root).is_dir() {
        return return_jstring(&mut env, "");
    }
    let mut out = String::new();
    let mut count = 0i64;
    let limit = max_entries.clamp(0, 100_000) as i64;
    for entry in WalkBuilder::new(&root).build().flatten() {
        if !entry.file_type().map(|t| t.is_file()).unwrap_or(false) {
            continue;
        }
        let _ = writeln!(out, "{}", entry.path().display());
        count += 1;
        if count >= limit {
            break;
        }
    }
    return_jstring(&mut env, &out)
}

/// JSON array of `path:line:text` matches for `pattern` under `root`.
/// Line-oriented scan (single-threaded scaffold): correctness first, parallel
/// walker is a documented follow-up.
#[no_mangle]
pub extern "system" fn Java_com_qodex_app_native_QodexNative_searchFiles(
    mut env: JNIEnv,
    _class: JClass,
    root: JString,
    pattern: JString,
    max_results: i32,
) -> jstring {
    let root = to_string(&mut env, &root);
    let pattern = to_string(&mut env, &pattern);
    if root.is_empty() || pattern.is_empty() {
        return return_jstring(&mut env, "[]");
    }
    let re = match RegexBuilder::new(&pattern)
        .size_limit(1 << 20)
        .build()
    {
        Ok(re) => re,
        Err(_) => return return_jstring(&mut env, "[]"),
    };
    let mut out = String::from("[");
    let mut count = 0i64;
    let limit = max_results.clamp(0, 10_000) as i64;
    'outer: for entry in WalkBuilder::new(&root).build().flatten() {
        if !entry.file_type().map(|t| t.is_file()).unwrap_or(false) {
            continue;
        }
        let path = entry.path();
        let Ok(content) = std::fs::read(path) else {
            continue;
        };
        let head = &content[..content.len().min(BINARY_SNIFF_BYTES)];
        if head.contains(&0u8) {
            continue;
        }
        let text = String::from_utf8_lossy(&content);
        for (index, line) in text.lines().enumerate() {
            if re.is_match(line) {
                if count > 0 {
                    out.push(',');
                }
                out.push_str(&match_json(path.to_string_lossy().as_ref()));
                out.push(':');
                out.push_str(&(index + 1).to_string());
                out.push(':');
                out.push_str(&match_json(line.trim()));
                count += 1;
                if count >= limit {
                    break 'outer;
                }
            }
        }
    }
    out.push(']');
    return_jstring(&mut env, &out)
}

/// Minimal JSON string escaping: quotes, backslashes, and control characters.
fn match_json(value: &str) -> String {
    let mut out = String::with_capacity(value.len() + 2);
    out.push('"');
    for c in value.chars() {
        match c {
            '"' => out.push_str("\\\""),
            '\\' => out.push_str("\\\\"),
            '\n' => out.push_str("\\n"),
            '\r' => out.push_str("\\r"),
            '\t' => out.push_str("\\t"),
            c if (c as u32) < 0x20 => {
                let _ = write!(out, "\\u{:04x}", c as u32);
            }
            c => out.push(c),
        }
    }
    out.push('"');
    out
}
