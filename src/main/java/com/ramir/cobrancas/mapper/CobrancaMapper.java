package com.ramir.cobrancas.mapper;

import com.ramir.cobrancas.domain.Cobranca;
import com.ramir.cobrancas.domain.enums.CobrancaMetodoEnum;
import com.ramir.cobrancas.domain.enums.CobrancaStatusEnum;
import com.ramir.cobrancas.domain.enums.CobrancaTipoEnum;
import com.ramir.cobrancas.dto.CobrancaBasicoResponseDTO;
import com.ramir.cobrancas.dto.CobrancaCompletoResponseDTO;
import com.ramir.cobrancas.dto.CobrancaRequestDTO;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class CobrancaMapper {

    /**
     * Converte RequestDTO em Entity.
     * Usado apenas na criação da cobrança.
     */
    public Cobranca toEntity(CobrancaRequestDTO dto,
                             String idUsuario,
                             String nomeSolicitante) {

        return Cobranca.builder()
                .idUsuario(idUsuario)
                .nomeSolicitante(nomeSolicitante)

                .tipo(dto.getTipo() == null ?
                        CobrancaTipoEnum.RECARGA :
                        dto.getTipo())

                .metodo(dto.getMetodo() == null ?
                        CobrancaMetodoEnum.PIX :
                        dto.getMetodo())

                .status(CobrancaStatusEnum.SOLICITADA)

                .valorSolicitacao(dto.getValor())
                .dataCriacao(LocalDateTime.now())
                .build();
    }

    /**
     * Entity -> DTO da criação.
     * Retorna apenas os campos pedidos pelo endpoint POST.
     */
    public CobrancaBasicoResponseDTO toBasicoResponse(Cobranca cobranca) {

        return CobrancaBasicoResponseDTO.builder()
                .id(cobranca.getId())
                .txid(cobranca.getTxid())
                .copiaECola(cobranca.getCopiaECola())
                .dataExpiracao(cobranca.getDataExpiracao())
                .build();
    }

    /**
     * Entity -> DTO detalhado.
     * Usado no GET por ID.
     */
    public CobrancaCompletoResponseDTO toCompletoResponse(Cobranca cobranca) {

        return CobrancaCompletoResponseDTO.builder()
                .id(cobranca.getId())
                .idUsuario(cobranca.getIdUsuario())

                .tipo(cobranca.getTipo())
                .metodo(cobranca.getMetodo())
                .status(cobranca.getStatus())

                .txid(cobranca.getTxid())
                .transactionId(cobranca.getTransactionId())

                .valorSolicitado(cobranca.getValorSolicitacao())
                .valorPago(cobranca.getValorPago())

                .dataCriacao(cobranca.getDataCriacao())
                .dataExpiracao(cobranca.getDataExpiracao())
                .dataFinalizada(cobranca.getDataFinalizada())

                .build();
    }

    /**
     * Atualiza somente os dados vindos do webhook PIX.
     */
    public void atualizarPagamentoPix(Cobranca cobranca,
                                      LocalDateTime dataPagamento) {

        cobranca.setStatus(CobrancaStatusEnum.FINALIZADA);
        cobranca.setDataFinalizada(dataPagamento);
    }

    /**
     * Atualiza somente dados do checkout cartão.
     */
    public void atualizarCheckout(Cobranca cobranca,
                                  String acsUrl,
                                  String payload) {

        cobranca.setStatus(CobrancaStatusEnum.FINALIZADA);
        cobranca.setAcsUrl(acsUrl);
        cobranca.setThreeDsPayload(payload);
    }
}
