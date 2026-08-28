package com.ramir.cobrancas.lock;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LockService {

    private final Map<String, Long> locks = new ConcurrentHashMap<>();

    public boolean lock(String key, Duration ttl) {
        long now = System.currentTimeMillis();
        Long expiration = locks.get(key);

        if (expiration != null && expiration > now) {
            return false;
        }

        locks.put(key, now + ttl.toMillis());
        return true;
    }

    public void unlock(String key) {
        locks.remove(key);
    }
}
