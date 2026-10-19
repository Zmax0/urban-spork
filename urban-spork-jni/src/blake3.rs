use super::*;

/// # Safety
#[unsafe(no_mangle)]
pub unsafe extern "C" fn Java_com_urbanspork_jni_blake3_Blake3_hash0<'local>(
    mut env: EnvUnowned<'local>,
    _class: JClass<'local>,
    input: JByteArray<'local>,
    out: JByteArray<'local>,
) {
    env.with_env(|env| -> Result<()> {
        unsafe {
            crate::try_catch!(
                {
                    let out_len = out.len(env)?;
                    if out_len != ::blake3::OUT_LEN {
                        throw_invalid_argument(
                            env,
                            format!("invalid BLAKE3 output length: {}", out_len),
                        )?;
                        return Err(Error::JavaException);
                    }
                    let input_guard = input.get_elements(env, ReleaseMode::NoCopyBack)?;
                    let data = slice::from_raw_parts(
                        input_guard.as_ptr() as *const u8,
                        input_guard.len(),
                    );
                    let hash = ::blake3::hash(data);
                    let out_guard = out.get_elements(env, ReleaseMode::CopyBack)?;
                    let dest = slice::from_raw_parts_mut(
                        out_guard.as_ptr() as *mut u8,
                        out_guard.len(),
                    );
                    dest.copy_from_slice(hash.as_bytes());
                    Ok(())
                },
                env
            )
        }
    })
    .resolve::<ThrowRuntimeExAndDefault>()
}

/// # Safety
#[unsafe(no_mangle)]
pub unsafe extern "C" fn Java_com_urbanspork_jni_blake3_Blake3_deriveKey0<'local>(
    mut env: EnvUnowned<'local>,
    _class: JClass<'local>,
    context: JByteArray<'local>,
    material: JByteArray<'local>,
    out: JByteArray<'local>,
) {
    env.with_env(|env| -> Result<()> {
        unsafe {
            crate::try_catch!(
                {
                    let context_guard = context.get_elements(env, ReleaseMode::NoCopyBack)?;
                    let context_bytes = slice::from_raw_parts(
                        context_guard.as_ptr() as *const u8,
                        context_guard.len(),
                    );
                    let Some(context) = std::str::from_utf8(context_bytes).ok() else {
                        throw_invalid_argument(
                            env,
                            "BLAKE3 derive_key context must be valid UTF-8",
                        )?;
                        return Err(Error::JavaException);
                    };
                    let material_guard = material.get_elements(env, ReleaseMode::NoCopyBack)?;
                    let material_bytes = slice::from_raw_parts(
                        material_guard.as_ptr() as *const u8,
                        material_guard.len(),
                    );
                    let out_guard = out.get_elements(env, ReleaseMode::CopyBack)?;
                    let dest = slice::from_raw_parts_mut(
                        out_guard.as_ptr() as *mut u8,
                        out_guard.len(),
                    );
                    let mut hasher = ::blake3::Hasher::new_derive_key(context);
                    hasher.update(material_bytes);
                    hasher.finalize_xof().fill(dest);
                    Ok(())
                },
                env
            )
        }
    })
    .resolve::<ThrowRuntimeExAndDefault>()
}
