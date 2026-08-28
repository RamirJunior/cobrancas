package com.ramir.cobrancas.controller;

import com.ramir.cobrancas.domain.Cobranca;
import com.ramir.cobrancas.domain.enums.CobrancaMetodoEnum;
import com.ramir.cobrancas.domain.enums.CobrancaStatusEnum;
import com.ramir.cobrancas.domain.enums.CobrancaTipoEnum;
import com.ramir.cobrancas.dto.CobrancaRequestDTO;
import com.ramir.cobrancas.repository.CobrancaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CobrancaControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CobrancaRepository repository;

    @Test
    void fluxoCriarEConsultarCobranca() throws Exception {
        CobrancaRequestDTO request = new CobrancaRequestDTO();
        request.setValor(new BigDecimal("320.00"));
        request.setMetodo(CobrancaMetodoEnum.PIX);
        request.setTipo(CobrancaTipoEnum.RECARGA);

        String response = mockMvc.perform(post("/api/v1/cobrancas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/api/v1/cobrancas/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
    }

    @Test
    void fluxoWebhookPixFinalizaCobranca() throws Exception {
        Cobranca cobranca = Cobranca.builder()
                .idUsuario("user-1")
                .nomeSolicitante("Junior Ribeiro")
                .tipo(CobrancaTipoEnum.RECARGA)
                .metodo(CobrancaMetodoEnum.PIX)
                .status(CobrancaStatusEnum.SOLICITADA)
                .valorSolicitacao(new BigDecimal("320.00"))
                .txid("abc123")
                .build();

        repository.save(cobranca);

        String payload = """
                {
                  "pix": [
                    {
                      "txid": "tax-123",
                      "horario": "2026-08-28T13:02:30Z",
                      "valor": 450.00
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/cobrancas/webhook/pix")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        Cobranca atual = repository.findTopByTxidOrderByIdDesc("tax-123").orElseThrow();
        assert atual.getStatus() == CobrancaStatusEnum.FINALIZADA;
    }
}