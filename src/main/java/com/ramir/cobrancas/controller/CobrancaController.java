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

    @PostMapping
    public ResponseEntity<CobrancaBasicoResponseDTO> criarCobranca(@Valid @RequestBody CobrancaRequestDTO request) {
        CobrancaBasicoResponseDTO response = service.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CobrancaCompletoResponseDTO> buscarPorId(@PathVariable Long id) {
        CobrancaCompletoResponseDTO response = service.buscar(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/webhook/pix")
    public ResponseEntity<Void> processarWebhookPix(@RequestBody(required = false) PixWebhookDTO webhookDTO) {
        service.webhook(webhookDTO);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{transactionId}/validate")
    public ResponseEntity<Void> validarCheckout(@PathVariable String transactionId,
                                                @Valid @RequestBody CheckoutValidationDTO request) {
        service.validarCheckout(transactionId, request);
        return ResponseEntity.ok().build();
    }
}
