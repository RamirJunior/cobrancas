package com.ramir.cobrancas.lock;

import com.ramir.cobrancas.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class LockExecutor {

    private final LockService lockService;

    public <T> T execute(String key, Supplier<T> action) {
        if (!lockService.lock(key, Duration.ofSeconds(5))) {
            throw new BusinessException("Geracao de cobranca em andamento.");
        }

        try {
            return action.get();
        } finally {
            lockService.unlock(key);
        }
    }
}
