package com.ramir.cobrancas.dto;

import lombok.Data;

@Data
public class CheckoutValidationDTO {

    private String cavv;
    private String xid;
    private String eci;
}
