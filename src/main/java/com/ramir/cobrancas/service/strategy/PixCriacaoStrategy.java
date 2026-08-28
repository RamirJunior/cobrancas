package com.ramir.cobrancas.service.strategy;

import com.ramir.cobrancas.domain.Cobranca;
import com.ramir.cobrancas.domain.enums.CobrancaMetodoEnum;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PixCriacaoStrategy implements CobrancaCriacaoStrategy {
    @Override
    public CobrancaMetodoEnum metodo() {
        return CobrancaMetodoEnum.PIX;
    }

    @Override
    public void criar(Cobranca cobranca) {
        cobranca.setTxid(UUID.randomUUID().toString());
        cobranca.setCopiaECola("pix-teste-123");
        cobranca.setDataExpiracao(LocalDateTime.now().plusHours(2));
    }
}
