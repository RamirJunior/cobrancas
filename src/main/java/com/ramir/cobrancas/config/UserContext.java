package com.ramir.cobrancas.config;

import org.springframework.stereotype.Component;

@Component
public class UserContext {

    public String getIdUsuario() {
        return "user-1";
    }

    public String getGivenName() {
        return "Junior";
    }

    public String getFamilyName() {
        return "Ribeiro";
    }

    public String getCpf() {
        return "123.456.789-00";
    }
}
