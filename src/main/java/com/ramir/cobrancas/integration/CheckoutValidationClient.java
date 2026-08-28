package com.ramir.cobrancas.integration;

import com.ramir.cobrancas.dto.CheckoutValidationDTO;
import org.springframework.stereotype.Component;

@Component
public class CheckoutValidationClient {

    public String validar(String transactionId, CheckoutValidationDTO dto) {
        return "AUTORIZADO";
    }
}
