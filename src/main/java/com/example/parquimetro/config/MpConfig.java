package com.example.parquimetro.config;

import com.mercadopago.MercadoPagoConfig;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MpConfig {

    @Value("${mercadopago.access.token}")
    private String accessToken;

    @PostConstruct
    public void inicializarMercadoPago() {
        try {
            MercadoPagoConfig.setAccessToken(accessToken);
            System.out.println("✅ MÓDULO FINANCIERO: Mercado Pago inicializado correctamente.");
        } catch (Exception e) {
            System.err.println("❌ ERROR FATAL: No se pudo cargar el token de Mercado Pago.");
        }
    }
}