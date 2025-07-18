pub mod chacha8poly1305;
pub mod xchacha20poly1305;
pub mod xchacha8poly1305;

use chacha20poly1305::aead::generic_array::GenericArray;
use chacha20poly1305::aead::generic_array::typenum::Unsigned;
use chacha20poly1305::{AeadCore, AeadInPlace};
use chacha20poly1305::{ChaCha8Poly1305, KeyInit, XChaCha8Poly1305, XChaCha20Poly1305};
use core::slice;
use jni::{JNIEnv, errors::Error, objects::*};

const PTR: &str = "ptr";

#[macro_export]
macro_rules! try_catch {
    ($body:expr, $env:ident) => {
        try_catch!($body, $env, ())
    };
    ($body:expr, $env:ident, $default_exp:expr) => {
        match (|| $body)() {
            Ok(res) => res,
            Err(Error::JavaException) => $default_exp,
            Err(e) => {
                $env.throw_new("java/lang/RuntimeException", e.to_string())
                    .expect("throw JVM exception error");
                $default_exp
            }
        }
    };
}

pub(crate) unsafe extern "C" fn init<'local, Cipher>(
    mut env: JNIEnv<'local>,
    class: JClass,
    key: JByteArray<'local>,
) -> JObject<'local>
where
    Cipher: KeyInit + Send + 'static,
{
    unsafe {
        try_catch!(
            {
                let key = env.get_array_elements(&key, ReleaseMode::NoCopyBack)?;
                let key = slice::from_raw_parts(key.as_ptr() as *const u8, key.len());
                let cipher = Cipher::new(key.into());
                let j_cipher = env.new_object(class, "()V", &[])?;
                env.set_rust_field(&j_cipher, PTR, cipher)?;
                Ok(j_cipher)
            },
            env,
            JObject::null()
        )
    }
}

pub(crate) unsafe fn encrypt<'local, Cipher>(
    mut env: JNIEnv<'local>,
    this: JObject<'local>,
    nonce: JByteArray<'local>,
    aad: JByteArray<'local>,
    plaintext: JByteArray<'local>,
) where
    Cipher: AeadInPlace + Send + 'static,
{
    unsafe {
        try_catch!(
            {
                let nonce = env.get_array_elements(&nonce, ReleaseMode::NoCopyBack)?;
                let nonce = slice::from_raw_parts(nonce.as_ptr() as *const u8, nonce.len());
                let aad = if aad.is_null() {
                    &[]
                } else {
                    let aad = env.get_array_elements(&aad, ReleaseMode::NoCopyBack)?;
                    slice::from_raw_parts(aad.as_ptr() as *const u8, aad.len())
                };
                let plaintext = env.get_array_elements(&plaintext, ReleaseMode::CopyBack)?;
                let plaintext =
                    slice::from_raw_parts_mut(plaintext.as_ptr() as *mut u8, plaintext.len());
                let cipher: Cipher = env.take_rust_field(&this, PTR)?;
                let tag_size = <Cipher as AeadCore>::TagSize::USIZE;
                let (buffer, tag) = plaintext.split_at_mut(plaintext.len() - tag_size);
                if let Ok(_tag) = cipher.encrypt_in_place_detached(nonce.into(), aad, buffer) {
                    tag.copy_from_slice(_tag.as_slice());
                    env.set_rust_field(&this, PTR, cipher)?;
                } else {
                    env.throw_new("java/lang/RuntimeException", "encrypt failed")?;
                }
                Ok(())
            },
            env
        )
    }
}

pub(crate) unsafe fn decrypt<'local, Cipher>(
    mut env: JNIEnv<'local>,
    this: JObject<'local>,
    nonce: JByteArray<'local>,
    aad: JByteArray<'local>,
    ciphertext: JByteArray<'local>,
) where
    Cipher: AeadInPlace + Send + 'static,
{
    unsafe {
        try_catch!(
            {
                let nonce = env.get_array_elements(&nonce, ReleaseMode::NoCopyBack)?;
                let nonce = slice::from_raw_parts(nonce.as_ptr() as *const u8, nonce.len());
                let aad = if aad.is_null() {
                    &[]
                } else {
                    let aad = env.get_array_elements(&aad, ReleaseMode::NoCopyBack)?;
                    slice::from_raw_parts(aad.as_ptr() as *const u8, aad.len())
                };
                let ciphertext = env.get_array_elements(&ciphertext, ReleaseMode::CopyBack)?;
                let ciphertext =
                    slice::from_raw_parts_mut(ciphertext.as_ptr() as *mut u8, ciphertext.len());
                let cipher: Cipher = env.take_rust_field(&this, PTR)?;
                let tag_size = <Cipher as AeadCore>::TagSize::USIZE;
                let (buffer, tag) = ciphertext.split_at_mut(ciphertext.len() - tag_size);
                if cipher
                    .decrypt_in_place_detached(
                        nonce.into(),
                        aad,
                        buffer,
                        GenericArray::from_mut_slice(tag),
                    )
                    .is_err()
                {
                    env.throw_new("java/lang/RuntimeException", "decrypt failed")?;
                }
                env.set_rust_field(&this, PTR, cipher)?;
                Ok(())
            },
            env
        )
    }
}
