package com.urbanspork.common.manage.shadowsocks;

import com.urbanspork.common.config.ServerConfig;
import com.urbanspork.common.config.ServerUserConfig;
import com.urbanspork.common.util.ByteString;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public record ServerUserManager(Map<BytesKey, ServerUser> users) {

    public static ServerUserManager from(ServerConfig config) {
        ServerUserManager manager = new ServerUserManager(new ConcurrentHashMap<>());
        List<ServerUserConfig> user = config.getUser();
        if (user != null) {
            user.stream().map(ServerUser::from).forEach(manager::addUser);
        }
        return manager;
    }

    public static ServerUserManager empty() {
        return new ServerUserManager(Collections.emptyMap());
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

        @Override
        public String toString() {
            return ByteString.valueOf(bytes);
        }
    }
}
