package com.ramir.cobrancas.domain.enums;

public enum CobrancaStatusEnum {

    SOLICITADA(),
    EXPIRADA(),
    ERRO_APROVACAO_PEDIDO(),
    FINALIZADA(),
    EM_PROCESSAMENTO(),
    ERRO_ANALISE_PENDENTE();

    private final Integer code = 0;
}
