package com.urbanspork.test;

import org.junit.jupiter.api.Assertions;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class TestUtil {

    private TestUtil() {}

    public static <T> void testEqualsAndHashcode(T t1, T t2) {
        Set<T> set = new HashSet<>();
        set.add(t1);
        set.add(t2);
        Assertions.assertEquals(t1, set.iterator().next());
        Assertions.assertTrue(set.contains(t1));
        Assertions.assertTrue(set.contains(t2));
        Map<T, T> map = new HashMap<>();
        map.put(t1, t2);
        map.remove(t2);
        Assertions.assertNotEquals(t1, map.get(t2));
        Assertions.assertNotEquals(t1, new Object());
    }

    public static <T, U, R> void testGetterAndSetter(U u, T t, Function<T, R> getter, BiConsumer<T, U> setter) {
        Assertions.assertNotEquals(u, getter.apply(t));
        setter.accept(t, u);
        Assertions.assertEquals(u, getter.apply(t));
    }
}
