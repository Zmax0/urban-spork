package com.urbanspork.common.manage.shadowsocks;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum ServerUserManager {

    DEFAULT(new ConcurrentHashMap<>()),
    EMPTY(Collections.emptyMap());

    private final Map<BytesKey, ServerUser> users;

    ServerUserManager(Map<BytesKey, ServerUser> users) {
        this.users = users;
    }

    public void addUser(ServerUser user) {
        users.put(new BytesKey(user.identityHash()), user);
    }

    public ServerUser getUserByHash(byte[] userHash) {
        return users.get(new BytesKey(userHash));
    }

    public void removeUserByHash(byte[] userHash) {
        users.remove(new BytesKey(userHash));
    }

    public int userCount() {
        return users.size();
    }

    public Iterator<ServerUser> userIterator() {
        return users.values().iterator();
    }

    public void clear() {
        users.clear();
    }

    record BytesKey(byte[] bytes) {
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            BytesKey bytesKey = (BytesKey) o;
            return Arrays.equals(bytes, bytesKey.bytes);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(bytes);
        }
    }
}
