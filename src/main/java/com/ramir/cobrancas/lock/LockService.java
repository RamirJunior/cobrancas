package com.ramir.cobrancas.lock;

import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LockService {

    private final Set<String> locks = ConcurrentHashMap.newKeySet();

    public boolean lock(String key) {
        return locks.add(key);
    }

    public void unlock(String key) {
        locks.remove(key);
    }
}
