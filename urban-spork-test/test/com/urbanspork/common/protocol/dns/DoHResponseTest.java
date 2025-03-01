package com.urbanspork.common.protocol.dns;


import com.urbanspork.test.TestUtil;
import org.junit.jupiter.api.Test;

import java.util.List;

class DoHResponseTest {
    @Test
    void testGetterAndSetter() {
        List<DoHRecord> list = List.of(new DoHRecord());
        DoHResponse response = new DoHResponse();
        TestUtil.testGetterAndSetter(1, response, DoHResponse::getStatus, DoHResponse::setStatus);
        TestUtil.testGetterAndSetter("tc", response, DoHResponse::getTC, DoHResponse::setTC);
        TestUtil.testGetterAndSetter("rd", response, DoHResponse::getRD, DoHResponse::setRD);
        TestUtil.testGetterAndSetter("ra", response, DoHResponse::getRA, DoHResponse::setRA);
        TestUtil.testGetterAndSetter("ad", response, DoHResponse::getAD, DoHResponse::setAD);
        TestUtil.testGetterAndSetter("cd", response, DoHResponse::getCD, DoHResponse::setCD);
        TestUtil.testGetterAndSetter(list, response, DoHResponse::getQuestion, DoHResponse::setQuestion);
        TestUtil.testGetterAndSetter(list, response, DoHResponse::getAnswer, DoHResponse::setAnswer);
    }
}
