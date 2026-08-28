package com.ramir.cobrancas.service.strategy;

import com.ramir.cobrancas.domain.Cobranca;
import com.ramir.cobrancas.domain.enums.CobrancaMetodoEnum;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CartaoCriacaoStrategy implements CobrancaCriacaoStrategy {
    @Override
    public CobrancaMetodoEnum metodo() {
        return CobrancaMetodoEnum.CARTAO_CREDITO;
    }

    @Override
    public void criar(Cobranca cobranca) {
        cobranca.setTransactionId(UUID.randomUUID().toString());
        cobranca.setAcsUrl("https://teste-acs");
        cobranca.setThreeDsPayload("payload");
    }
}
