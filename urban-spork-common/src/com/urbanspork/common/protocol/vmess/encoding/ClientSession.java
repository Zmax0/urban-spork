package com.urbanspork.common.protocol.vmess.encoding;

import com.urbanspork.common.util.Dice;

import java.util.Arrays;

public class ClientSession extends Session {

    public ClientSession() {
        this(Dice.rollBytes(33));
    }

    ClientSession(byte[] bytes) {
        super(Arrays.copyOf(bytes, 16), Arrays.copyOfRange(bytes, 16, 32), bytes[32]);
    }
}
