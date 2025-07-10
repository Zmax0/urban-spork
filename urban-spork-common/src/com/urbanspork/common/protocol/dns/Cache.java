package com.urbanspork.common.protocol.dns;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public class Cache {
    private static final Expiry<String, Value> EXPIRY = new Expiry<>() {
        @Override
        public long expireAfterCreate(String key, Value value, long createAtNanos) {
            return value.ttl(Instant.EPOCH.plusNanos(createAtNanos)).toNanos();
        }

        @Override
        public long expireAfterUpdate(String key, Value value, long updateAtNanos, long ttlNanos) {
            return value.ttl(Instant.EPOCH.plusNanos(updateAtNanos)).toNanos();
        }

        @Override
        public long expireAfterRead(String key, Value value, long readAtNanos, long ttlNanos) {
            return ttlNanos;
        }
    };

    private final com.github.benmanes.caffeine.cache.Cache<String, Value> cache;

    public Cache(int size) {
        this.cache = Caffeine.newBuilder().maximumSize(size).expireAfter(EXPIRY).build();
    }

    public void put(String key, String value, long ttl, Instant now) {
        cache.put(key, new Value(value, now.plusSeconds(ttl)));
    }

    public Optional<String> get(String key, Instant now) {
        return Optional.ofNullable(cache.getIfPresent(key)).filter(v -> v.isCurrent(now)).map(v -> v.withUpdated(now).ip);
    }

    private record Value(String ip, Instant validUntil) {
        boolean isCurrent(Instant now) {
            return now.isBefore(validUntil);
        }

        Duration ttl(Instant now) {
            return Duration.between(now, validUntil);
        }

        Value withUpdated(Instant now) {
            return new Value(ip, now.plus(ttl(now)));
        }
    }
}