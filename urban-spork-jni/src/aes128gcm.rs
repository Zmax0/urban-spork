use super::*;

/// # Safety
#[unsafe(no_mangle)]
pub unsafe extern "C" fn Java_com_urbanspork_jni_aes128gcm_Cipher_init<'local>(
    env: EnvUnowned<'local>,
    class: JClass<'local>,
    key: JByteArray<'local>,
) -> JObject<'local> {
    unsafe { init::<Aes128Gcm>(env, class, key) }
}

/// # Safety
#[unsafe(no_mangle)]
pub unsafe extern "C" fn Java_com_urbanspork_jni_aes128gcm_Cipher_dispose<'local>(
    env: EnvUnowned<'local>,
    _this: JObject<'local>,
    ptr: jlong,
) {
    unsafe { dispose::<Aes128Gcm>(env, ptr) }
}

/// # Safety
#[unsafe(no_mangle)]
pub unsafe extern "C" fn Java_com_urbanspork_jni_aes128gcm_Cipher_encrypt0<'local>(
    env: EnvUnowned<'local>,
    _this: JObject<'local>,
    ptr: jlong,
    nonce: JByteArray<'local>,
    aad: JByteArray<'local>,
    plaintext: JByteArray<'local>,
) {
    unsafe { encrypt::<Aes128Gcm>(env, ptr, nonce, aad, plaintext) }
}

/// # Safety
#[unsafe(no_mangle)]
pub unsafe extern "C" fn Java_com_urbanspork_jni_aes128gcm_Cipher_decrypt0<'local>(
    env: EnvUnowned<'local>,
    _this: JObject<'local>,
    ptr: jlong,
    nonce: JByteArray<'local>,
    aad: JByteArray<'local>,
    ciphertext: JByteArray<'local>,
) {
    unsafe { decrypt::<Aes128Gcm>(env, ptr, nonce, aad, ciphertext) }
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn Java_com_urbanspork_jni_aes128gcm_Cipher_encryptDirect0<'local>(
    env: EnvUnowned<'local>,
    _this: JObject<'local>,
    ptr: jlong,
    nonce: JByteArray<'local>,
    aad: JByteArray<'local>,
    plaintext: JByteBuffer<'local>,
) {
    unsafe { encrypt_direct::<Aes128Gcm>(env, ptr, nonce, aad, plaintext) }
}

#[unsafe(no_mangle)]
pub unsafe extern "C" fn Java_com_urbanspork_jni_aes128gcm_Cipher_decryptDirect0<'local>(
    env: EnvUnowned<'local>,
    _this: JObject<'local>,
    ptr: jlong,
    nonce: JByteArray<'local>,
    aad: JByteArray<'local>,
    ciphertext: JByteBuffer<'local>,
) {
    unsafe { decrypt_direct::<Aes128Gcm>(env, ptr, nonce, aad, ciphertext) }
}
