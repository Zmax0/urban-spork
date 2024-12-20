use super::*;

/// # Safety
#[no_mangle]
pub unsafe extern "C" fn Java_com_urbanspork_jni_xchacha8poly1305_Cipher_init<'local>(
    env: JNIEnv<'local>,
    class: JClass,
    key: JByteArray<'local>,
) -> JObject<'local> {
    init::<XChaCha8Poly1305>(env, class, key)
}

/// # Safety
#[no_mangle]
pub unsafe extern "C" fn Java_com_urbanspork_jni_xchacha8poly1305_Cipher_encrypt<'local>(
    env: JNIEnv<'local>,
    this: JObject<'local>,
    nonce: JByteArray<'local>,
    aad: JByteArray<'local>,
    plaintext: JByteArray<'local>,
) {
    encrypt::<XChaCha8Poly1305>(env, this, nonce, aad, plaintext)
}

/// # Safety
#[no_mangle]
pub unsafe extern "C" fn Java_com_urbanspork_jni_xchacha8poly1305_Cipher_decrypt<'local>(
    env: JNIEnv<'local>,
    this: JObject<'local>,
    nonce: JByteArray<'local>,
    aad: JByteArray<'local>,
    ciphertext: JByteArray<'local>,
) {
    decrypt::<XChaCha8Poly1305>(env, this, nonce, aad, ciphertext)
}