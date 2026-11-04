use super::*;

/// # Safety
#[unsafe(no_mangle)]
pub unsafe extern "C" fn Java_com_urbanspork_jni_xchacha8poly1305_Cipher_init<'local>(
    env: EnvUnowned<'local>,
    class: JClass<'local>,
    key: JByteArray<'local>,
) -> JObject<'local> {
    unsafe { init::<XChaCha8Poly1305>(env, class, key) }
}

/// # Safety
#[unsafe(no_mangle)]
pub unsafe extern "C" fn Java_com_urbanspork_jni_xchacha8poly1305_Cipher_dispose<'local>(
    env: EnvUnowned<'local>,
    _this: JObject<'local>,
    ptr: jlong,
) {
    unsafe { dispose::<XChaCha8Poly1305>(env, ptr) }
}

/// # Safety
#[unsafe(no_mangle)]
pub unsafe extern "C" fn Java_com_urbanspork_jni_xchacha8poly1305_Cipher_encrypt0<'local>(
    env: EnvUnowned<'local>,
    _this: JObject<'local>,
    ptr: jlong,
    nonce: JByteArray<'local>,
    aad: JByteArray<'local>,
    plaintext: JByteArray<'local>,
) {
    unsafe { encrypt::<XChaCha8Poly1305>(env, ptr, nonce, aad, plaintext) }
}

/// # Safety
#[unsafe(no_mangle)]
pub unsafe extern "C" fn Java_com_urbanspork_jni_xchacha8poly1305_Cipher_decrypt0<'local>(
    env: EnvUnowned<'local>,
    _this: JObject<'local>,
    ptr: jlong,
    nonce: JByteArray<'local>,
    aad: JByteArray<'local>,
    ciphertext: JByteArray<'local>,
) {
    unsafe { decrypt::<XChaCha8Poly1305>(env, ptr, nonce, aad, ciphertext) }
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn Java_com_urbanspork_jni_xchacha8poly1305_Cipher_encryptDirect0<'local>(
    env: EnvUnowned<'local>,
    _this: JObject<'local>,
    ptr: jlong,
    nonce: JByteArray<'local>,
    aad: JByteArray<'local>,
    plaintext: JByteBuffer<'local>,
) {
    unsafe { encrypt_direct::<XChaCha8Poly1305>(env, ptr, nonce, aad, plaintext) }
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn Java_com_urbanspork_jni_xchacha8poly1305_Cipher_decryptDirect0<'local>(
    env: EnvUnowned<'local>,
    _this: JObject<'local>,
    ptr: jlong,
    nonce: JByteArray<'local>,
    aad: JByteArray<'local>,
    ciphertext: JByteBuffer<'local>,
) {
    unsafe { decrypt_direct::<XChaCha8Poly1305>(env, ptr, nonce, aad, ciphertext) }
}
