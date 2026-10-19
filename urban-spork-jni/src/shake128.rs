use super::*;

use shake::digest::{ExtendableOutput, Update, XofReader};

/// # Safety
#[unsafe(no_mangle)]
pub unsafe extern "C" fn Java_com_urbanspork_jni_shake128_Shake128_squeeze0<'local>(
    mut env: EnvUnowned<'local>,
    _class: JClass<'local>,
    input: JByteArray<'local>,
    offset: jlong,
    out: JByteArray<'local>,
) {
    env.with_env(|env| -> Result<()> {
        unsafe {
            crate::try_catch!(
                {
                    if offset < 0 {
                        throw_invalid_argument(env, format!("invalid SHAKE128 offset: {}", offset))?;
                        return Err(Error::JavaException);
                    }
                    let input_guard = input.get_elements(env, ReleaseMode::NoCopyBack)?;
                    let data = slice::from_raw_parts(
                        input_guard.as_ptr() as *const u8,
                        input_guard.len(),
                    );
                    let out_guard = out.get_elements(env, ReleaseMode::CopyBack)?;
                    let dest = slice::from_raw_parts_mut(
                        out_guard.as_ptr() as *mut u8,
                        out_guard.len(),
                    );
                    let mut hasher = shake::Shake128::default();
                    hasher.update(data);
                    let mut reader = hasher.finalize_xof();
                    let mut skip = offset as usize;
                    let mut scratch = [0u8; 4096];
                    while skip > 0 {
                        let n = skip.min(scratch.len());
                        reader.read(&mut scratch[..n]);
                        skip -= n;
                    }
                    reader.read(dest);
                    Ok(())
                },
                env
            )
        }
    })
    .resolve::<ThrowRuntimeExAndDefault>()
}
