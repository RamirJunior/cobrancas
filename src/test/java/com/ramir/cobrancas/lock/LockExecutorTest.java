package com.ramir.cobrancas.lock;

import com.ramir.cobrancas.exceptions.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LockExecutorTest {
    @Test
    void lockExecutorDeveLiberarNoFinallyMesmoComExcecao() {
        LockService lockService = mock(LockService.class);
        when(lockService.lock("k", Duration.ofSeconds(5))).thenReturn(true);

        LockExecutor executor = new LockExecutor(lockService);

        assertThrows(RuntimeException.class, () -> {
            executor.execute("k", () -> {
                throw new RuntimeException("fail");
            });
        });

        verify(lockService).unlock("k");
    }

    @Test
    void lockExecutorDeveLancarBusinessExceptionQuandoLockIndisponivel() {
        LockService lockService = mock(LockService.class);
        when(lockService.lock("k", Duration.ofSeconds(5))).thenReturn(false);

        LockExecutor executor = new LockExecutor(lockService);

        BusinessException ex = assertThrows(BusinessException.class, () -> {
            executor.execute("k", () -> "ok");
        });

        assertEquals("Geracao de cobranca em andamento.", ex.getMessage());
    }

}