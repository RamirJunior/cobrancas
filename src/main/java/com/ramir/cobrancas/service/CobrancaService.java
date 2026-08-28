package com.ramir.cobrancas.service;

import com.ramir.cobrancas.config.UserContext;
import com.ramir.cobrancas.domain.Cobranca;
import com.ramir.cobrancas.domain.enums.CobrancaMetodoEnum;
import com.ramir.cobrancas.domain.enums.CobrancaTipoEnum;
import com.ramir.cobrancas.dto.*;
import com.ramir.cobrancas.exceptions.BusinessException;
import com.ramir.cobrancas.integration.CheckoutValidationClient;
import com.ramir.cobrancas.lock.LockExecutor;
import com.ramir.cobrancas.mapper.CobrancaMapper;
import com.ramir.cobrancas.repository.CobrancaRepository;
import com.ramir.cobrancas.service.strategy.CobrancaCriacaoStrategyRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static com.ramir.cobrancas.domain.enums.CobrancaStatusEnum.FINALIZADA;
import static com.ramir.cobrancas.domain.enums.CobrancaStatusEnum.SOLICITADA;

@Service
@RequiredArgsConstructor
public class CobrancaService {

    private final CobrancaRepository repository;
    private final CobrancaMapper mapper;
    private final UserContext userContext;
    private final LockExecutor lockExecutor;
    private final CobrancaCriacaoStrategyRegistry registry;
    private final CheckoutValidationClient checkoutClient;

    public CobrancaBasicoResponseDTO criar(
            CobrancaRequestDTO dto) {

        return lockExecutor.execute(
                "cobrancas:" + userContext.getIdUsuario(),

                () -> {
                    CobrancaMetodoEnum metodo = Optional.ofNullable(dto.getMetodo()).orElse(CobrancaMetodoEnum.PIX);

                    CobrancaTipoEnum tipo = Optional.ofNullable(dto.getTipo()).orElse(CobrancaTipoEnum.RECARGA);

                    Cobranca cobranca = Cobranca.builder()
                            .idUsuario(userContext.getIdUsuario())
                            .nomeSolicitante(userContext.getGivenName() + " " + userContext.getFamilyName())
                            .status(SOLICITADA)
                            .metodo(metodo)
                            .tipo(tipo)
                            .valorSolicitacao(dto.getValor())
                            .dataCriacao(LocalDateTime.now())
                            .build();

                    registry.get(metodo).criar(cobranca);
                    repository.save(cobranca);

                    return CobrancaBasicoResponseDTO.builder()
                            .id(cobranca.getId())
                            .txid(cobranca.getTxid())
                            .copiaECola(cobranca.getCopiaECola())
                            .dataExpiracao(cobranca.getDataExpiracao())
                            .build();
                }
        );
    }

    public CobrancaCompletoResponseDTO buscar(Long id) {
        Cobranca cobranca = repository.findById(id).orElseThrow(() -> new BusinessException("Cobrança não encontrada."));
        return mapper.toCompletoResponse(cobranca);
    }

    public void webhook(PixWebhookDTO dto) {

        if (dto == null || dto.getPix() == null) return;

        dto.getPix().forEach(item -> {

            if (item.getTxid() == null) return;

            repository.findTopByTxidOrderByIdDesc(item.getTxid())
                    .ifPresent(cobranca -> {
                        if (cobranca.getStatus() == FINALIZADA)
                            return;

                        cobranca.setStatus(FINALIZADA);
                        cobranca.setValorPago(item.getValor());
                        cobranca.setDataFinalizada(
                                LocalDateTime.now(ZoneId.of("America/Sao_Paulo")));

                        repository.save(cobranca);
                    });
        });
    }

    public void validarCheckout(String transactionId, CheckoutValidationDTO dto){
        Cobranca cobranca = repository.findByTransactionId(transactionId)
                .orElseThrow(() -> new BusinessException("Cobrançca não encontrada"));
        checkoutClient.validar(transactionId, dto);
        cobranca.setStatus(FINALIZADA);
        repository.save(cobranca);
    }
}
