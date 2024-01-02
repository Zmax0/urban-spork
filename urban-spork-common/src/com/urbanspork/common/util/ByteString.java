package com.urbanspork.common.util;

import java.util.HexFormat;

public interface ByteString {
    /**
     * Convert byte array to string using the similar C-family languages byte string syntax .
     *
     * @param bytes target byte array
     * @return string
     */
    static String valueOf(byte[] bytes) {
        if (bytes == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (byte aByte : bytes) {
            builder.append(valueOf(aByte));
        }
        return builder.toString();
    }

    /**
     * Convert byte to string using the similar C-family languages byte string syntax.
     *
     * @param b target byte
     * @return string
     */
    static String valueOf(byte b) {
        if (b == 9) {
            return "\\t";
        } else if (b == 10) {
            return "\\n";
        } else if (b == 13) {
            return "\\r";
        } else if (b == 32) {
            return " ";
        } else if (32 < b && b < 127) {
            return String.valueOf((char) b);
        } else {
            return "\\x" + HexFormat.of().toHexDigits(b);
        }
    }
}
