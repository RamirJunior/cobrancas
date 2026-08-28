package com.ramir.cobrancas.service;

import com.ramir.cobrancas.config.UserContext;
import com.ramir.cobrancas.domain.Cobranca;
import com.ramir.cobrancas.domain.enums.CobrancaMetodoEnum;
import com.ramir.cobrancas.domain.enums.CobrancaStatusEnum;
import com.ramir.cobrancas.domain.enums.CobrancaTipoEnum;
import com.ramir.cobrancas.dto.CobrancaBasicoResponseDTO;
import com.ramir.cobrancas.dto.CobrancaRequestDTO;
import com.ramir.cobrancas.dto.PixWebhookDTO;
import com.ramir.cobrancas.exceptions.BusinessException;
import com.ramir.cobrancas.integration.CheckoutValidationClient;
import com.ramir.cobrancas.integration.StatusConsultaExternaClient;
import com.ramir.cobrancas.lock.LockExecutor;
import com.ramir.cobrancas.mapper.CobrancaMapper;
import com.ramir.cobrancas.repository.CobrancaRepository;
import com.ramir.cobrancas.service.strategy.CobrancaCriacaoStrategy;
import com.ramir.cobrancas.service.strategy.CobrancaCriacaoStrategyRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CobrancaServiceTest {

    @Mock
    private CobrancaRepository repository;

    @Mock
    private CobrancaMapper mapper;

    @Mock
    private UserContext userContext;

    @Mock
    private LockExecutor lockExecutor;

    @Mock
    private CobrancaCriacaoStrategyRegistry registry;

    @Mock
    private CheckoutValidationClient checkoutClient;

    @Mock
    private StatusConsultaExternaClient statusConsultaExternaClient;

    @InjectMocks
    private CobrancaService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(userContext.getIdUsuario()).thenReturn("user-1");
        when(userContext.getGivenName()).thenReturn("Junior");
        when(userContext.getFamilyName()).thenReturn("Ribeiro");
    }

    @Test
    void criarCobrancaSucessoPix() {
        CobrancaRequestDTO request = new CobrancaRequestDTO();
        request.setValor(new BigDecimal("25.50"));
        request.setMetodo(CobrancaMetodoEnum.PIX);
        request.setTipo(CobrancaTipoEnum.RECARGA);

        Cobranca cobranca = Cobranca.builder()
                .id(1L)
                .idUsuario("user-1")
                .nomeSolicitante("Junior Ribeiro")
                .status(CobrancaStatusEnum.SOLICITADA)
                .metodo(CobrancaMetodoEnum.PIX)
                .tipo(CobrancaTipoEnum.RECARGA)
                .valorSolicitacao(request.getValor())
                .dataCriacao(LocalDateTime.now())
                .build();

        CobrancaCriacaoStrategy strategy = mock(CobrancaCriacaoStrategy.class);

        when(lockExecutor.execute(any(), any())).thenAnswer(inv -> {
            return ((java.util.function.Supplier<CobrancaBasicoResponseDTO>) inv.getArgument(1)).get();
        });

        when(registry.get(CobrancaMetodoEnum.PIX)).thenReturn(strategy);
        when(mapper.toBasicoResponse(any())).thenReturn(CobrancaBasicoResponseDTO.builder()
                .id(1L)
                .txid("abc123")
                .copiaECola("pix")
                .dataExpiracao(LocalDateTime.now().plusHours(2))
                .build());

        CobrancaBasicoResponseDTO response = service.criar(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
    }

    @Test
    void criarCobrancaComLockIndisponivel() {
        CobrancaRequestDTO request = new CobrancaRequestDTO();
        request.setValor(new BigDecimal("10.00"));

        when(lockExecutor.execute(any(), any()))
                .thenThrow(new BusinessException("Geracao de cobranca em andamento."));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.criar(request));
        assertEquals("Geracao de cobranca em andamento.", ex.getMessage());
    }

    @Test
    void criarCobrancaComExcecaoInesperadaDeveMapearErroNegocio() {
        CobrancaRequestDTO request = new CobrancaRequestDTO();
        request.setValor(new BigDecimal("10.00"));

        when(lockExecutor.execute(any(), any()))
                .thenThrow(new RuntimeException("erro"));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.criar(request));
        assertEquals("Erro ao criar cobranca.", ex.getMessage());
    }

    @Test
    void processarNotificacaoWebhookPixFinalizandoCobrancaPendente() {
        PixWebhookDTO dto = new PixWebhookDTO();
        PixWebhookDTO.PixDTO item = new PixWebhookDTO.PixDTO();
        item.setTxid("abc123");
        item.setValor(new BigDecimal("25.50"));
        dto.setPix(Collections.singletonList(item));

        Cobranca cobranca = Cobranca.builder()
                .id(10L)
                .txid("abc123")
                .status(CobrancaStatusEnum.SOLICITADA)
                .valorSolicitacao(new BigDecimal("25.50"))
                .metodo(CobrancaMetodoEnum.PIX)
                .build();

        when(repository.findTopByTxidOrderByIdDesc("abc123")).thenReturn(Optional.of(cobranca));

        service.webhook(dto);

        verify(repository).save(any(Cobranca.class));
    }

    @Test
    void processarNotificacaoWebhookPixIgnorandoJaFinalizada() {
        PixWebhookDTO dto = new PixWebhookDTO();
        PixWebhookDTO.PixDTO item = new PixWebhookDTO.PixDTO();
        item.setTxid("abc123");
        item.setValor(new BigDecimal("25.50"));
        dto.setPix(Collections.singletonList(item));

        Cobranca cobranca = Cobranca.builder()
                .id(10L)
                .txid("abc123")
                .status(CobrancaStatusEnum.FINALIZADA)
                .valorSolicitacao(new BigDecimal("25.50"))
                .metodo(CobrancaMetodoEnum.PIX)
                .build();

        when(repository.findTopByTxidOrderByIdDesc("abc123")).thenReturn(Optional.of(cobranca));

        service.webhook(dto);

        verify(repository, never()).save(any(Cobranca.class));
    }

    @Test
    void validarCheckoutAtualizandoCobrancaExistente() {
        Cobranca cobranca = Cobranca.builder()
                .id(7L)
                .transactionId("txn-1")
                .status(CobrancaStatusEnum.SOLICITADA)
                .build();

        when(repository.findByTransactionId("txn-1")).thenReturn(Optional.of(cobranca));
        when(checkoutClient.validar(eq("txn-1"), any())).thenReturn("AUTORIZADO");

        service.validarCheckout("txn-1", new com.ramir.cobrancas.dto.CheckoutValidationDTO());

        assertEquals(CobrancaStatusEnum.FINALIZADA, cobranca.getStatus());
        verify(repository).save(cobranca);
    }

}