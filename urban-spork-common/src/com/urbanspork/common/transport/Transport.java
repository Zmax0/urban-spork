package com.urbanspork.common.transport;

import java.util.HashMap;
import java.util.Map;

public enum Transport {

    UDP(0, 1), TCP(1, 1), WS(1, 2);

    private final int index;
    private final int weight;
    private final String value;

    Transport(int index, int weight) {
        this.index = index;
        this.weight = weight;
        this.value = name().toLowerCase();
    }

    public int index() {
        return index;
    }

    public String value() {
        return value;
    }

    public static Transport[] regroup(Transport[] transports) {
        if (transports == null || transports.length == 0) {
            return transports;
        }
        Map<Integer, Transport> map = new HashMap<>();
        int max = 0;
        for (Transport transport : transports) {
            max = Math.max(max, transport.index());
            Transport old = map.get(transport.index);
            if (old == null || transport.weight > old.weight) {
                map.put(transport.index, transport);
            }
        }
        Transport[] res = new Transport[max + 1];
        for (Map.Entry<Integer, Transport> entry : map.entrySet()) {
            res[entry.getKey()] = entry.getValue();
        }
        return res;
    }
}
