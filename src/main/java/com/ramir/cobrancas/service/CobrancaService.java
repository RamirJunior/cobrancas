package com.ramir.cobrancas.service;

import com.ramir.cobrancas.config.UserContext;
import com.ramir.cobrancas.domain.Cobranca;
import com.ramir.cobrancas.domain.enums.CobrancaMetodoEnum;
import com.ramir.cobrancas.domain.enums.CobrancaStatusEnum;
import com.ramir.cobrancas.domain.enums.CobrancaTipoEnum;
import com.ramir.cobrancas.dto.*;
import com.ramir.cobrancas.exceptions.BusinessException;
import com.ramir.cobrancas.integration.CheckoutValidationClient;
import com.ramir.cobrancas.integration.StatusConsultaExternaClient;
import com.ramir.cobrancas.lock.LockExecutor;
import com.ramir.cobrancas.mapper.CobrancaMapper;
import com.ramir.cobrancas.repository.CobrancaRepository;
import com.ramir.cobrancas.service.strategy.CobrancaCriacaoStrategy;
import com.ramir.cobrancas.service.strategy.CobrancaCriacaoStrategyRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CobrancaService {

    private final CobrancaRepository repository;
    private final CobrancaMapper mapper;
    private final UserContext userContext;
    private final LockExecutor lockExecutor;
    private final CobrancaCriacaoStrategyRegistry registry;
    private final CheckoutValidationClient checkoutClient;
    private final StatusConsultaExternaClient statusConsultaExternaClient;

    public CobrancaBasicoResponseDTO criar(CobrancaRequestDTO dto) {
        try {
            return lockExecutor.execute("cobrancas:" + userContext.getIdUsuario(), () -> {
                CobrancaMetodoEnum metodo = Optional.ofNullable(dto.getMetodo()).orElse(CobrancaMetodoEnum.PIX);
                CobrancaTipoEnum tipo = Optional.ofNullable(dto.getTipo()).orElse(CobrancaTipoEnum.RECARGA);

                Cobranca cobranca = Cobranca.builder()
                        .idUsuario(userContext.getIdUsuario())
                        .nomeSolicitante(userContext.getGivenName() + " " + userContext.getFamilyName())
                        .status(CobrancaStatusEnum.SOLICITADA)
                        .metodo(metodo)
                        .tipo(tipo)
                        .valorSolicitacao(dto.getValor())
                        .dataCriacao(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")))
                        .build();

                CobrancaCriacaoStrategy strategy = registry.get(metodo);
                if (strategy == null) {
                    throw new BusinessException("Erro ao criar cobranca.");
                }

                strategy.criar(cobranca);
                repository.save(cobranca);

                return mapper.toBasicoResponse(cobranca);
            });
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("Erro ao criar cobranca.");
        }
    }

    public CobrancaCompletoResponseDTO buscar(Long id) {
        Cobranca cobranca = repository.findById(id)
                .orElseThrow(() -> new BusinessException("Cobrança não encontrada."));

        if (cobranca.getMetodo() == CobrancaMetodoEnum.PIX &&
                isStatusExternoConsultavel(cobranca.getStatus())) {
            CobrancaStatusEnum statusExterno = statusConsultaExternaClient.consultar(cobranca.getTxid());

            if (statusExterno != cobranca.getStatus()) {
                Cobranca novaVersao = cloneCobranca(cobranca);
                novaVersao.setStatus(statusExterno);
                novaVersao.setCobrancaPai(cobranca);

                if (statusExterno == CobrancaStatusEnum.FINALIZADA) {
                    novaVersao.setDataFinalizada(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")));
                }

                repository.save(novaVersao);
                cobranca = novaVersao;
            }
        }

        return mapper.toCompletoResponse(cobranca);
    }

    public void webhook(PixWebhookDTO dto) {
        if (dto == null || dto.getPix() == null || dto.getPix().isEmpty()) {
            return;
        }

        dto.getPix().forEach(item -> {
            if (item == null || item.getTxid() == null || item.getTxid().isBlank()) {
                return;
            }

            repository.findTopByTxidOrderByIdDesc(item.getTxid())
                    .ifPresent(cobranca -> {
                        if (cobranca.getStatus() == CobrancaStatusEnum.FINALIZADA) {
                            return;
                        }

                        Cobranca novaVersao = cloneCobranca(cobranca);
                        novaVersao.setCobrancaPai(cobranca);
                        novaVersao.setStatus(CobrancaStatusEnum.FINALIZADA);
                        novaVersao.setValorPago(item.getValor());
                        novaVersao.setDataFinalizada(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")));
                        repository.save(novaVersao);
                    });
        });
    }

    public void validarCheckout(String transactionId, CheckoutValidationDTO dto) {
        Cobranca cobranca = repository.findByTransactionId(transactionId)
                .orElseThrow(() -> new BusinessException("Cobrança não encontrada."));

        String retorno = checkoutClient.validar(transactionId, dto);

        cobranca.setStatus(CobrancaStatusEnum.FINALIZADA);
        cobranca.setAcsUrl(retorno);
        cobranca.setThreeDsPayload(dto.getCavv() + ":" + dto.getXid() + ":" + dto.getEci());

        repository.save(cobranca);
    }

    private boolean isStatusExternoConsultavel(CobrancaStatusEnum status) {
        return status == CobrancaStatusEnum.SOLICITADA
                || status == CobrancaStatusEnum.EXPIRADA
                || status == CobrancaStatusEnum.ERRO_APROVACAO_PEDIDO
                || status == CobrancaStatusEnum.EM_REPROCESSAMENTO
                || status == CobrancaStatusEnum.ERRO_ANALISE_PENDENTE;
    }

    private Cobranca cloneCobranca(Cobranca source) {
        return Cobranca.builder()
                .idUsuario(source.getIdUsuario())
                .nomeSolicitante(source.getNomeSolicitante())
                .tipo(source.getTipo())
                .metodo(source.getMetodo())
                .status(source.getStatus())
                .valorSolicitacao(source.getValorSolicitacao())
                .valorPago(source.getValorPago())
                .txid(source.getTxid())
                .copiaECola(source.getCopiaECola())
                .transactionId(source.getTransactionId())
                .acsUrl(source.getAcsUrl())
                .threeDsPayload(source.getThreeDsPayload())
                .dataCriacao(source.getDataCriacao())
                .dataExpiracao(source.getDataExpiracao())
                .dataFinalizada(source.getDataFinalizada())
                .cobrancaPai(source.getCobrancaPai())
                .build();
    }
}
