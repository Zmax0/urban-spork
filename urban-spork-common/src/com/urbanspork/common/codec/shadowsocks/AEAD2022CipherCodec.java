package com.urbanspork.common.codec.shadowsocks;

import com.urbanspork.common.codec.aead.CipherCodec;

/**
 * AEAD-2022 Cipher Codec
 *
 * @author Zmax0
 * @see <a href=https://shadowsocks.org/doc/sip022.html>SIP022 AEAD-2022 Ciphers</a>
 */
public record AEAD2022CipherCodec(byte[] secret, int saltSize, CipherCodec cipherCodec) {
    /*
        +----------------+
        |  length chunk  |
        +----------------+
        | u16 big-endian |
        +----------------+

        +---------------+
        | payload chunk |
        +---------------+
        |   variable    |
        +---------------+

        Request stream:
        +--------+------------------------+---------------------------+------------------------+---------------------------+---+
        |  salt  | encrypted header chunk |  encrypted header chunk   | encrypted length chunk |  encrypted payload chunk  |...|
        +--------+------------------------+---------------------------+------------------------+---------------------------+---+
        | 16/32B |     11B + 16B tag      | variable length + 16B tag |  2B length + 16B tag   | variable length + 16B tag |...|
        +--------+------------------------+---------------------------+------------------------+---------------------------+---+

        Response stream:
        +--------+------------------------+---------------------------+------------------------+---------------------------+---+
        |  salt  | encrypted header chunk |  encrypted payload chunk  | encrypted length chunk |  encrypted payload chunk  |...|
        +--------+------------------------+---------------------------+------------------------+---------------------------+---+
        | 16/32B |    27/43B + 16B tag    | variable length + 16B tag |  2B length + 16B tag   | variable length + 16B tag |...|
        +--------+------------------------+---------------------------+------------------------+---------------------------+---+
     */
}