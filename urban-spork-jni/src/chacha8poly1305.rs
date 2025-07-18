use super::*;

/// # Safety
#[unsafe(no_mangle)]
pub unsafe extern "C" fn Java_com_urbanspork_jni_chacha8poly1305_Cipher_init<'local>(
    env: JNIEnv<'local>,
    class: JClass,
    key: JByteArray<'local>,
) -> JObject<'local> {
    unsafe { init::<ChaCha8Poly1305>(env, class, key) }
}

/// # Safety
#[unsafe(no_mangle)]
pub unsafe extern "C" fn Java_com_urbanspork_jni_chacha8poly1305_Cipher_encrypt<'local>(
    env: JNIEnv<'local>,
    this: JObject<'local>,
    nonce: JByteArray<'local>,
    aad: JByteArray<'local>,
    plaintext: JByteArray<'local>,
) {
    unsafe { encrypt::<ChaCha8Poly1305>(env, this, nonce, aad, plaintext) }
}

/// # Safety
#[unsafe(no_mangle)]
pub unsafe extern "C" fn Java_com_urbanspork_jni_chacha8poly1305_Cipher_decrypt<'local>(
    env: JNIEnv<'local>,
    this: JObject<'local>,
    nonce: JByteArray<'local>,
    aad: JByteArray<'local>,
    ciphertext: JByteArray<'local>,
) {
    unsafe { decrypt::<ChaCha8Poly1305>(env, this, nonce, aad, ciphertext) }
}
