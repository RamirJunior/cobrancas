package com.ramir.cobrancas.domain;

import com.ramir.cobrancas.domain.enums.CobrancaMetodoEnum;
import com.ramir.cobrancas.domain.enums.CobrancaStatusEnum;
import com.ramir.cobrancas.domain.enums.CobrancaTipoEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cobrancas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cobranca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String idUsuario;
    private String nomeSolicitante;

    @Enumerated(EnumType.STRING)
    private CobrancaTipoEnum tipo;

    @Enumerated(EnumType.STRING)
    private CobrancaMetodoEnum metodo;

    @Enumerated(EnumType.STRING)
    private CobrancaStatusEnum status;

    private BigDecimal valorSolicitacao;
    private BigDecimal valorPago;

    private String txid;
    private String copiaECola;
    private String transactionId;
    private String acsUrl;
    private String threeDsPayload;

    private LocalDateTime dataCriacao;
    private LocalDateTime dataExpiracao;
    private LocalDateTime dataFinalizada;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cobranca_pai_id")
    private Cobranca cobrancaPai;
}
