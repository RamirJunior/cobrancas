package com.ramir.cobrancas.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CobrancaBasicoResponseDTO {

    private Long id;
    private String txid;
    private String copiaECola;
    private LocalDateTime dataExpiracao;
}
