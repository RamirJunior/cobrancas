package com.ramir.cobrancas.service.strategy;

import com.ramir.cobrancas.domain.Cobranca;
import com.ramir.cobrancas.domain.enums.CobrancaMetodoEnum;

public interface CobrancaCriacaoStrategy {

    CobrancaMetodoEnum metodo();

    void criar(Cobranca cobranca);
}
