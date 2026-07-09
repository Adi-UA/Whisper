//! JNI bridge: exposes `generate_phrase` to Java via the JNI calling convention.

use jni::objects::{JClass, JObjectArray, JString};
use jni::sys::jlong;
use jni::JNIEnv;

use crate::{generate_phrase, wordlist};

/// Java signature:
/// ```java
/// public static native String generatePhrase(String groupId, long rotation, String[] history);
/// ```
#[no_mangle]
pub extern "system" fn Java_dev_adi_1ua_whisper_WordGen_generatePhrase<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    group_id: JString<'local>,
    rotation: jlong,
    history: JObjectArray<'local>,
) -> JString<'local> {
    let group_id: String = env.get_string(&group_id)
        .expect("invalid group_id string")
        .into();

    let history_len = env.get_array_length(&history).unwrap_or(0);
    let mut history_vec: Vec<String> = Vec::with_capacity(history_len as usize);
    for i in 0..history_len {
        let item: JString = env.get_object_array_element(&history, i)
            .expect("failed to get history element")
            .into();
        let s: String = env.get_string(&item)
            .expect("invalid history string")
            .into();
        history_vec.push(s);
    }

    let words: Vec<String> = wordlist::DEFAULT_WORDS.iter().map(|s| s.to_string()).collect();
    let phrase = generate_phrase(&words, &history_vec, &group_id, rotation as u64);

    env.new_string(&phrase).expect("failed to create Java string")
}
