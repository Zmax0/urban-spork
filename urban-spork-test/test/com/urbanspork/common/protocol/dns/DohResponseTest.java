package com.urbanspork.common.protocol.dns;


import com.urbanspork.test.TestUtil;
import org.junit.jupiter.api.Test;

import java.util.List;

class DohResponseTest {
    @Test
    void testGetterAndSetter() {
        List<DohRecord> list = List.of(new DohRecord());
        DohResponse response = new DohResponse();
        TestUtil.testGetterAndSetter(1, response, DohResponse::getStatus, DohResponse::setStatus);
        TestUtil.testGetterAndSetter("tc", response, DohResponse::getTC, DohResponse::setTC);
        TestUtil.testGetterAndSetter("rd", response, DohResponse::getRD, DohResponse::setRD);
        TestUtil.testGetterAndSetter("ra", response, DohResponse::getRA, DohResponse::setRA);
        TestUtil.testGetterAndSetter("ad", response, DohResponse::getAD, DohResponse::setAD);
        TestUtil.testGetterAndSetter("cd", response, DohResponse::getCD, DohResponse::setCD);
        TestUtil.testGetterAndSetter(list, response, DohResponse::getAnswer, DohResponse::setAnswer);
    }
}
