package com.ramir.cobrancas.integration;

import com.ramir.cobrancas.domain.enums.CobrancaStatusEnum;
import org.springframework.stereotype.Component;

@Component
public class StatusConsultaExternaClient {

    public CobrancaStatusEnum consultar(String txid) {
        if (txid == null || txid.isBlank()) {
            return CobrancaStatusEnum.SOLICITADA;
        }

        return CobrancaStatusEnum.FINALIZADA;
    }
}
