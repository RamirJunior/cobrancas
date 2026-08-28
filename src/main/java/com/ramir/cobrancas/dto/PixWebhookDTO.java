package com.ramir.cobrancas.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Data
public class PixWebhookDTO {

    private List<PixDTO> pix;

    @Data
    public static class PixDTO {
        private String txid;
        private OffsetDateTime horario;
        private BigDecimal valor;
    }
}
