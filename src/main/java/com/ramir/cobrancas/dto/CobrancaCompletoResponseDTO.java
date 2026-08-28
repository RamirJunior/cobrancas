package com.ramir.cobrancas.dto;

import com.ramir.cobrancas.domain.enums.CobrancaMetodoEnum;
import com.ramir.cobrancas.domain.enums.CobrancaStatusEnum;
import com.ramir.cobrancas.domain.enums.CobrancaTipoEnum;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class CobrancaCompletoResponseDTO {

    private Long id;
    private String idUsuario;
    private String txid;
    private String transactionId;
    private BigDecimal valorSolicitado;
    private BigDecimal valorPago;
    private CobrancaMetodoEnum metodo;
    private CobrancaTipoEnum tipo;
    private CobrancaStatusEnum status;

    private LocalDateTime dataCriacao;
    private LocalDateTime dataExpiracao;
    private LocalDateTime dataFinalizada;
}
