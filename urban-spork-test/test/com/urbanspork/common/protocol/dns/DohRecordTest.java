package com.urbanspork.common.protocol.dns;


import com.urbanspork.test.TestUtil;
import org.junit.jupiter.api.Test;

class DohRecordTest {
    @Test
    void testGetterAndSetter() {
        DohRecord record = new DohRecord();
        TestUtil.testGetterAndSetter("name", record, DohRecord::getName, DohRecord::setName);
        TestUtil.testGetterAndSetter(1, record, DohRecord::getTtl, DohRecord::setTtl);
    }
}
