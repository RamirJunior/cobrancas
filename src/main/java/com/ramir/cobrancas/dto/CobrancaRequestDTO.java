package com.ramir.cobrancas.dto;

import com.ramir.cobrancas.domain.enums.CobrancaMetodoEnum;
import com.ramir.cobrancas.domain.enums.CobrancaTipoEnum;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CobrancaRequestDTO {

    @NotNull
    private BigDecimal valor;
    private CobrancaTipoEnum tipo;
    private CobrancaMetodoEnum metodo;
}
