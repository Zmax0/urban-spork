package com.urbanspork.common.protocol.dns;


import com.urbanspork.test.TestUtil;
import org.junit.jupiter.api.Test;

class DoHRecordTest {
    @Test
    void testGetterAndSetter() {
        DoHRecord record = new DoHRecord();
        TestUtil.testGetterAndSetter("name", record, DoHRecord::getName, DoHRecord::setName);
        TestUtil.testGetterAndSetter(1, record, DoHRecord::getTtl, DoHRecord::setTtl);
    }
}
