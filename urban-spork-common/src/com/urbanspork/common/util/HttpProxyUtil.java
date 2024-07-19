package com.urbanspork.common.util;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpMethod;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

// https://tools.ietf.org/html/rfc7230
public class HttpProxyUtil {
    private static final boolean[] SP_LENIENT_BYTES;
    private static final boolean[] LATIN_WHITESPACE;

    static {
        SP_LENIENT_BYTES = new boolean[256];
        SP_LENIENT_BYTES[128 + ' '] = true;
        SP_LENIENT_BYTES[128 + 0x09] = true;
        SP_LENIENT_BYTES[128 + 0x0B] = true;
        SP_LENIENT_BYTES[128 + 0x0C] = true;
        SP_LENIENT_BYTES[128 + 0x0D] = true;
        LATIN_WHITESPACE = new boolean[256];
        for (byte b = Byte.MIN_VALUE; b < Byte.MAX_VALUE; b++) {
            LATIN_WHITESPACE[128 + b] = Character.isWhitespace(b);
        }
    }

    public record Option(HttpMethod method, InetSocketAddress address) {}

    public static Option parseOption(ByteBuf msg) {
        final int startContent = msg.readerIndex();
        final int end = startContent + msg.readableBytes();
        final int aStart = findNonSPLenient(msg, startContent, end);
        final int aEnd = findSPLenient(msg, aStart, end);
        final int bStart = findNonSPLenient(msg, aEnd, end);
        final int bEnd = findSPLenient(msg, bStart, end);
        HttpMethod method = HttpMethod.valueOf(getString(msg, aStart, aEnd - aStart));
        String uri = getString(msg, bStart, bEnd - bStart);
        String host;
        int port;
        if (HttpMethod.CONNECT == method) {
            int hEnd = uri.lastIndexOf(":");
            int pEnd = uri.length();
            host = getString(msg, bStart, hEnd);
            port = getInt(msg, bStart + hEnd + 1, pEnd - hEnd - 1);
        } else {
            int hStart = uri.indexOf("//") + 2;
            int hEnd = uri.lastIndexOf(":");
            int hv6End = uri.lastIndexOf("]");
            if (hEnd < hStart || hEnd < hv6End) { // without port
                hEnd = uri.length();
                host = getString(msg, bStart + hStart, hEnd - hStart);
                port = 80;
            } else { // with port
                int pStart = hEnd + 1;
                int pEnd = uri.indexOf("/", pStart + 1, uri.length());
                if (pEnd < pStart) { //  end without "/"
                    pEnd = uri.length();
                }
                host = getString(msg, bStart + hStart, hEnd - hStart);
                port = getInt(msg, bStart + pStart, pEnd - pStart);
            }
        }
        return new Option(method, InetSocketAddress.createUnresolved(host, port));
    }

    private static int findNonSPLenient(ByteBuf buf, int offset, int end) {
        for (int result = offset; result < end; ++result) {
            byte c = buf.getByte(result);
            if (isSPLenient(c)) {
                continue;
            }
            if (isWhitespace(c)) {
                throw new IllegalArgumentException("Invalid separator");
            }
            return result;
        }
        return end;
    }

    private static boolean isSPLenient(byte c) {
        return SP_LENIENT_BYTES[c + 128];
    }

    private static boolean isWhitespace(byte b) {
        return LATIN_WHITESPACE[b + 128];
    }

    private static int findSPLenient(ByteBuf buf, int offset, int end) {
        for (int result = offset; result < end; ++result) {
            if (isSPLenient(buf.getByte(result))) {
                return result;
            }
        }
        return end;
    }

    private static String getString(ByteBuf buf, int start, int length) {
        return buf.getCharSequence(start, length, StandardCharsets.US_ASCII).toString();
    }

    private static int getInt(ByteBuf buf, int start, int length) {
        CharSequence charSequence = buf.getCharSequence(start, length, StandardCharsets.US_ASCII);
        return Integer.parseInt(charSequence, 0, charSequence.length(), 10);
    }
}
