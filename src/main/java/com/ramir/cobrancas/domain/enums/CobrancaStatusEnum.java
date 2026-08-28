package com.ramir.cobrancas.domain.enums;

public enum CobrancaStatusEnum {

    SOLICITADA(2),
    EXPIRADA(3),
    ERRO_APROVACAO_PEDIDO(4),
    FINALIZADA(5),
    EM_REPROCESSAMENTO(6),
    ERRO_ANALISE_PENDENTE(9);

    private final Integer code;

    CobrancaStatusEnum(Integer code) {
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }
}
