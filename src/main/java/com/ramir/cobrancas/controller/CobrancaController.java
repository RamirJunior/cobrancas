package com.ramir.cobrancas.controller;

import com.ramir.cobrancas.dto.*;
import com.ramir.cobrancas.service.CobrancaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/cobrancas")
public class CobrancaController {

    private final CobrancaService service;

    /**
     * POST /api/v1/cobrancas
     * Cria uma nova cobrança.
     */
    @PostMapping
    public ResponseEntity<CobrancaBasicoResponseDTO> criarCobranca(@Valid @RequestBody CobrancaRequestDTO request) {
        CobrancaBasicoResponseDTO response = service.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/v1/cobrancas/{id}
     * Busca uma cobrança pelo ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<CobrancaCompletoResponseDTO> buscarPorId(@PathVariable Long id) {
        CobrancaCompletoResponseDTO response = service.buscar(id);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/v1/cobrancas/webhook/pix
     * Recebe notificações de pagamento PIX.
     */
    @PostMapping("/webhook/pix")
    public ResponseEntity<Void> processarWebhookPix(@RequestBody(required = false) PixWebhookDTO webhookDTO) {
        service.webhook(webhookDTO);
        return ResponseEntity.ok().build();
    }

    /**
     * POST /api/v1/cobrancas/{transactionId}/validate
     * Valida o checkout do cartão (3DS).
     */
    @PostMapping("/{transactionId}/validate")
    public ResponseEntity<Void> validarCheckout(@PathVariable String transactionId,
                                                @Valid @RequestBody CheckoutValidationDTO request) {
        service.validarCheckout(transactionId, request);
        return ResponseEntity.ok().build();
    }
}
