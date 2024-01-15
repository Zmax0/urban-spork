package com.urbanspork.common.manage.shadowsocks;

import com.urbanspork.common.config.ServerUserConfig;
import com.urbanspork.test.TestUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@DisplayName("Shadowsocks - Server User Manager")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ServerUserManagerTestCase {
    @Test
    void testAddAndGet() {
        ServerUserConfig userConfig = new ServerUserConfig("username", "4w0GKJ9U3Ox7CIXGU4A3LDQAqP6qrp/tUi/ilpOR9p4=");
        ServerUser user0 = ServerUser.from(userConfig);
        ServerUser user1 = ServerUser.from(userConfig);
        ServerUserManager manager = ServerUserManager.DEFAULT;
        manager.clear();
        Assertions.assertEquals(0, manager.userCount());
        manager.addUser(user0);
        manager.addUser(user1);
        Assertions.assertEquals(1, manager.userCount());
        ServerUser user2 = manager.getUserByHash(user0.identityHash());
        Assertions.assertEquals(user0, manager.userIterator().next());
        Assertions.assertArrayEquals(user0.identityHash(), user2.identityHash());
        manager.clear();
        Assertions.assertEquals(0, manager.userCount());
    }

    @Test
    void testBytesKey() {
        ServerUserManager.BytesKey key1 = new ServerUserManager.BytesKey(new byte[]{1, 2, 3});
        ServerUserManager.BytesKey key2 = new ServerUserManager.BytesKey(new byte[]{1, 2, 3});
        TestUtil.testEqualsAndHashcode(key1, key2);
    }
}
