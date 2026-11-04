use super::*;

/// # Safety
#[unsafe(no_mangle)]
pub unsafe extern "C" fn Java_com_urbanspork_jni_aes256_Cipher_init<'local>(
    env: EnvUnowned<'local>,
    class: JClass<'local>,
    key: JByteArray<'local>,
) -> JObject<'local> {
    unsafe { init::<Aes256>(env, class, key) }
}

/// # Safety
#[unsafe(no_mangle)]
pub unsafe extern "C" fn Java_com_urbanspork_jni_aes256_Cipher_dispose<'local>(
    env: EnvUnowned<'local>,
    _this: JObject<'local>,
    ptr: jlong,
) {
    unsafe { dispose::<Aes256>(env, ptr) }
}

/// # Safety
#[unsafe(no_mangle)]
pub unsafe extern "C" fn Java_com_urbanspork_jni_aes256_Cipher_encrypt0<'local>(
    env: EnvUnowned<'local>,
    _this: JObject<'local>,
    ptr: jlong,
    buffer: JByteArray<'local>,
) {
    unsafe { encrypt_block::<Aes256>(env, ptr, buffer) }
}

/// # Safety
#[unsafe(no_mangle)]
pub unsafe extern "C" fn Java_com_urbanspork_jni_aes256_Cipher_decrypt0<'local>(
    env: EnvUnowned<'local>,
    _this: JObject<'local>,
    ptr: jlong,
    buffer: JByteArray<'local>,
) {
    unsafe { decrypt_block::<Aes256>(env, ptr, buffer) }
}
