pub mod aes128;
pub mod aes128gcm;
pub mod aes256;
pub mod aes256gcm;
pub mod chacha20poly1305;
pub mod chacha8poly1305;
pub mod xchacha20poly1305;
pub mod xchacha8poly1305;

use aes::cipher::typenum::Unsigned;
use aes::cipher::{Block, BlockCipherDecrypt, BlockCipherEncrypt, BlockSizeUser};
use aes::{Aes128, Aes256};
use aes_gcm::{Aes128Gcm, Aes256Gcm};
// the local chacha20poly1305 module shadows the crate of the same name, hence the leading `::`
use ::chacha20poly1305::aead::{AeadCore, AeadInOut, Nonce, Tag};
use ::chacha20poly1305::{
    ChaCha8Poly1305, ChaCha20Poly1305, KeyInit, XChaCha8Poly1305, XChaCha20Poly1305,
};
use core::slice;
use jni::errors::{Error, Result, ThrowRuntimeExAndDefault};
use jni::objects::{JByteArray, JByteBuffer, JClass, JObject, Reference, ReleaseMode};
use jni::strings::JNIString;
use jni::sys::jlong;
use jni::{Env, EnvUnowned, jni_sig, jni_str};
use std::sync::Mutex;

#[macro_export]
macro_rules! try_catch {
    ($body:expr, $env:ident) => {
        try_catch!($body, $env, ())
    };
    ($body:expr, $env:ident, $default_exp:expr) => {
        match (|| $body)() {
            Ok(res) => Ok(res),
            Err(Error::JavaException) => Ok($default_exp),
            Err(e) => {
                let _ = $env.throw_new(
                    jni_str!("java/lang/RuntimeException"),
                    JNIString::new(e.to_string()),
                );
                Ok($default_exp)
            }
        }
    };
}

fn throw_invalid_argument(env: &mut Env, message: impl AsRef<str>) -> Result<()> {
    env.throw_new(
        jni_str!("java/lang/IllegalArgumentException"),
        JNIString::new(message),
    )
}

fn throw_failure(env: &mut Env, message: impl AsRef<str>) -> Result<()> {
    env.throw_new(
        jni_str!("java/lang/RuntimeException"),
        JNIString::new(message),
    )
}

unsafe fn lock_cipher<'a, Cipher>(ptr: jlong) -> std::sync::MutexGuard<'a, Cipher> {
    unsafe { &*(ptr as *const Mutex<Cipher>) }
        .lock()
        .unwrap_or_else(|e| e.into_inner())
}

pub(crate) unsafe fn init<'local, Cipher>(
    mut env: EnvUnowned<'local>,
    class: JClass<'local>,
    key: JByteArray<'local>,
) -> JObject<'local>
where
    Cipher: KeyInit + Send + 'static,
{
    env.with_env(|env| -> Result<JObject<'local>> {
        unsafe {
            try_catch!(
                {
                    let key_guard = key.get_elements(env, ReleaseMode::NoCopyBack)?;
                    let key =
                        slice::from_raw_parts(key_guard.as_ptr() as *const u8, key_guard.len());
                    match Cipher::new_from_slice(key) {
                        Ok(cipher) => {
                            let j_cipher = env.new_object(class, jni_sig!("()V"), &[])?;
                            env.set_rust_field(&j_cipher, jni_str!("ptr"), cipher)?;
                            Ok(j_cipher)
                        }
                        Err(_) => {
                            throw_invalid_argument(
                                env,
                                format!("invalid key size: {}", key.len()),
                            )?;
                            Ok(JObject::null())
                        }
                    }
                },
                env,
                JObject::null()
            )
        }
    })
    .resolve::<ThrowRuntimeExAndDefault>()
}

pub(crate) unsafe fn encrypt<'local, Cipher>(
    mut env: EnvUnowned<'local>,
    ptr: jlong,
    nonce: JByteArray<'local>,
    aad: JByteArray<'local>,
    plaintext: JByteArray<'local>,
) where
    Cipher: AeadInOut + Send + 'static,
{
    env.with_env(|env| -> Result<()> {
        unsafe {
            try_catch!(
                {
                    let nonce_guard = nonce.get_elements(env, ReleaseMode::NoCopyBack)?;
                    let nonce_slice =
                        slice::from_raw_parts(nonce_guard.as_ptr() as *const u8, nonce_guard.len());
                    // the guard must outlive the slice, the associated data is used after this block
                    let aad_guard = if aad.is_null() {
                        None
                    } else {
                        Some(aad.get_elements(env, ReleaseMode::NoCopyBack)?)
                    };
                    let aad = match &aad_guard {
                        Some(guard) => {
                            slice::from_raw_parts(guard.as_ptr() as *const u8, guard.len())
                        }
                        None => &[],
                    };
                    let plaintext_guard = plaintext.get_elements(env, ReleaseMode::CopyBack)?;
                    let plaintext_slice = slice::from_raw_parts_mut(
                        plaintext_guard.as_ptr() as *mut u8,
                        plaintext_guard.len(),
                    );
                    encrypt_inout::<Cipher>(env, ptr, nonce_slice, aad, plaintext_slice)
                },
                env
            )
        }
    })
    .resolve::<ThrowRuntimeExAndDefault>()
}

pub(crate) unsafe fn decrypt<'local, Cipher>(
    mut env: EnvUnowned<'local>,
    ptr: jlong,
    nonce: JByteArray<'local>,
    aad: JByteArray<'local>,
    ciphertext: JByteArray<'local>,
) where
    Cipher: AeadInOut + Send + 'static,
{
    env.with_env(|env| -> Result<()> {
        unsafe {
            try_catch!(
                {
                    let nonce_guard = nonce.get_elements(env, ReleaseMode::NoCopyBack)?;
                    let nonce_slice =
                        slice::from_raw_parts(nonce_guard.as_ptr() as *const u8, nonce_guard.len());
                    // the guard must outlive the slice, the associated data is used after this block
                    let aad_guard = if aad.is_null() {
                        None
                    } else {
                        Some(aad.get_elements(env, ReleaseMode::NoCopyBack)?)
                    };
                    let aad = match &aad_guard {
                        Some(guard) => {
                            slice::from_raw_parts(guard.as_ptr() as *const u8, guard.len())
                        }
                        None => &[],
                    };
                    let ciphertext_guard = ciphertext.get_elements(env, ReleaseMode::CopyBack)?;
                    let ciphertext_slice = slice::from_raw_parts_mut(
                        ciphertext_guard.as_ptr() as *mut u8,
                        ciphertext_guard.len(),
                    );
                    decrypt_inout::<Cipher>(env, ptr, nonce_slice, aad, ciphertext_slice)
                },
                env
            )
        }
    })
    .resolve::<ThrowRuntimeExAndDefault>()
}

unsafe fn direct_region<'local>(
    env: &mut Env,
    buffer: &JByteBuffer<'local>,
) -> Result<(*mut u8, usize)> {
    let address = match env.get_direct_buffer_address(buffer) {
        Ok(address) => address,
        Err(Error::NullPtr(_)) => {
            throw_invalid_argument(env, "a direct ByteBuffer is required")?;
            return Err(Error::JavaException);
        }
        Err(e) => return Err(e),
    };
    let capacity = env.get_direct_buffer_capacity(buffer)?;
    Ok((address, capacity))
}

fn check_nonce<'a, Cipher>(env: &mut Env, nonce: &'a [u8]) -> Result<&'a Nonce<Cipher>>
where
    Cipher: AeadInOut,
{
    let nonce_size = <Cipher as AeadCore>::NonceSize::USIZE;
    if nonce.len() != nonce_size {
        throw_invalid_argument(env, format!("invalid nonce size: {}", nonce.len()))?;
        return Err(Error::JavaException);
    }
    Ok(nonce.try_into().expect("nonce size checked above"))
}

unsafe fn encrypt_inout<Cipher>(
    env: &mut Env,
    ptr: jlong,
    nonce: &[u8],
    aad: &[u8],
    payload: &mut [u8],
) -> Result<()>
where
    Cipher: AeadInOut + Send + 'static,
{
    let tag_size = <Cipher as AeadCore>::TagSize::USIZE;
    let nonce = check_nonce::<Cipher>(env, nonce)?;
    if payload.len() < tag_size {
        throw_invalid_argument(env, format!("invalid plaintext size: {}", payload.len()))?;
        return Err(Error::JavaException);
    }
    // the tag is only ever written over, never read: a direct buffer may hold stale bytes in that tail
    let (buffer, tag) = payload.split_at_mut(payload.len() - tag_size);
    let cipher = unsafe { lock_cipher::<Cipher>(ptr) };
    match cipher.encrypt_inout_detached(nonce, aad, buffer.into()) {
        Ok(new_tag) => tag.copy_from_slice(new_tag.as_slice()),
        Err(_) => throw_failure(env, "encrypt failed")?,
    }
    Ok(())
}

unsafe fn decrypt_inout<Cipher>(
    env: &mut Env,
    ptr: jlong,
    nonce: &[u8],
    aad: &[u8],
    payload: &mut [u8],
) -> Result<()>
where
    Cipher: AeadInOut + Send + 'static,
{
    let tag_size = <Cipher as AeadCore>::TagSize::USIZE;
    let nonce = check_nonce::<Cipher>(env, nonce)?;
    if payload.len() < tag_size {
        throw_invalid_argument(env, format!("invalid ciphertext size: {}", payload.len()))?;
        return Err(Error::JavaException);
    }
    let (buffer, tag) = payload.split_at_mut(payload.len() - tag_size);
    let tag: &Tag<Cipher> = (&*tag).try_into().expect("tag size checked above");
    let cipher = unsafe { lock_cipher::<Cipher>(ptr) };
    if cipher
        .decrypt_inout_detached(nonce, aad, buffer.into(), tag)
        .is_err()
    {
        throw_failure(env, "decrypt failed")?;
    }
    Ok(())
}

pub(crate) unsafe fn encrypt_direct<'local, Cipher>(
    mut env: EnvUnowned<'local>,
    ptr: jlong,
    nonce: JByteArray<'local>,
    aad: JByteArray<'local>,
    plaintext: JByteBuffer<'local>,
) where
    Cipher: AeadInOut + Send + 'static,
{
    env.with_env(|env| -> Result<()> {
        unsafe {
            try_catch!(
                {
                    let nonce_guard = nonce.get_elements(env, ReleaseMode::NoCopyBack)?;
                    let nonce_slice =
                        slice::from_raw_parts(nonce_guard.as_ptr() as *const u8, nonce_guard.len());
                    // the guard must outlive the slice, the associated data is used after this block
                    let aad_guard = if aad.is_null() {
                        None
                    } else {
                        Some(aad.get_elements(env, ReleaseMode::NoCopyBack)?)
                    };
                    let aad = match &aad_guard {
                        Some(guard) => {
                            slice::from_raw_parts(guard.as_ptr() as *const u8, guard.len())
                        }
                        None => &[],
                    };
                    let (address, capacity) = direct_region(env, &plaintext)?;
                    let payload = slice::from_raw_parts_mut(address, capacity);
                    encrypt_inout::<Cipher>(env, ptr, nonce_slice, aad, payload)
                },
                env
            )
        }
    })
    .resolve::<ThrowRuntimeExAndDefault>()
}

pub(crate) unsafe fn decrypt_direct<'local, Cipher>(
    mut env: EnvUnowned<'local>,
    ptr: jlong,
    nonce: JByteArray<'local>,
    aad: JByteArray<'local>,
    ciphertext: JByteBuffer<'local>,
) where
    Cipher: AeadInOut + Send + 'static,
{
    env.with_env(|env| -> Result<()> {
        unsafe {
            try_catch!(
                {
                    let nonce_guard = nonce.get_elements(env, ReleaseMode::NoCopyBack)?;
                    let nonce_slice =
                        slice::from_raw_parts(nonce_guard.as_ptr() as *const u8, nonce_guard.len());
                    // the guard must outlive the slice, the associated data is used after this block
                    let aad_guard = if aad.is_null() {
                        None
                    } else {
                        Some(aad.get_elements(env, ReleaseMode::NoCopyBack)?)
                    };
                    let aad = match &aad_guard {
                        Some(guard) => {
                            slice::from_raw_parts(guard.as_ptr() as *const u8, guard.len())
                        }
                        None => &[],
                    };
                    let (address, capacity) = direct_region(env, &ciphertext)?;
                    let payload = slice::from_raw_parts_mut(address, capacity);
                    decrypt_inout::<Cipher>(env, ptr, nonce_slice, aad, payload)
                },
                env
            )
        }
    })
    .resolve::<ThrowRuntimeExAndDefault>()
}

pub(crate) unsafe fn dispose<'local, Cipher>(mut env: EnvUnowned<'local>, ptr: jlong)
where
    Cipher: Send + 'static,
{
    env.with_env(|env| -> Result<()> {
        unsafe {
            try_catch!(
                {
                    let mbox = Box::from_raw(ptr as *mut Mutex<Cipher>);
                    drop(mbox.try_lock()?);
                    Ok(())
                },
                env
            )
        }
    })
    .resolve::<ThrowRuntimeExAndDefault>()
}

pub(crate) unsafe fn encrypt_block<'local, Cipher>(
    env: EnvUnowned<'local>,
    ptr: jlong,
    buffer: JByteArray<'local>,
) where
    Cipher: BlockCipherEncrypt + BlockCipherDecrypt + Send + 'static,
{
    unsafe { process_block::<Cipher>(env, ptr, buffer, true) }
}

pub(crate) unsafe fn decrypt_block<'local, Cipher>(
    env: EnvUnowned<'local>,
    ptr: jlong,
    buffer: JByteArray<'local>,
) where
    Cipher: BlockCipherEncrypt + BlockCipherDecrypt + Send + 'static,
{
    unsafe { process_block::<Cipher>(env, ptr, buffer, false) }
}

unsafe fn process_block<'local, Cipher>(
    mut env: EnvUnowned<'local>,
    ptr: jlong,
    buffer: JByteArray<'local>,
    encrypt: bool,
) where
    Cipher: BlockCipherEncrypt + BlockCipherDecrypt + Send + 'static,
{
    env.with_env(|env| -> Result<()> {
        unsafe {
            try_catch!(
                {
                    let block_size = <Cipher as BlockSizeUser>::BlockSize::USIZE;
                    let buffer_guard = buffer.get_elements(env, ReleaseMode::CopyBack)?;
                    let buffer = slice::from_raw_parts_mut(
                        buffer_guard.as_ptr() as *mut u8,
                        buffer_guard.len(),
                    );
                    if buffer.len() < block_size {
                        throw_invalid_argument(
                            env,
                            format!("invalid block size: {}", buffer.len()),
                        )?;
                        return Ok(());
                    }
                    let block: &mut Block<Cipher> = (&mut buffer[..block_size])
                        .try_into()
                        .expect("block size checked above");
                    let cipher = lock_cipher::<Cipher>(ptr);
                    if encrypt {
                        cipher.encrypt_block(block);
                    } else {
                        cipher.decrypt_block(block);
                    }
                    Ok(())
                },
                env
            )
        }
    })
    .resolve::<ThrowRuntimeExAndDefault>()
}
