package com.urbanspork.common.manage.shadowsocks;

import com.urbanspork.common.config.ServerUserConfig;
import com.urbanspork.test.TestUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Shadowsocks - Server User")
class ServerUserTestCase {
    @Test
    void testBeingKey() {
        ServerUserConfig userConfig = new ServerUserConfig("username", "4w0GKJ9U3Ox7CIXGU4A3LDQAqP6qrp/tUi/ilpOR9p4=");
        ServerUser user1 = ServerUser.from(userConfig);
        ServerUser user2 = ServerUser.from(userConfig);
        Assertions.assertEquals(user2.toString(), user1.toString());
        TestUtil.testEqualsAndHashcode(user2, user1);
    }
}
