package com.example.parquimetro.service;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.UUID;

@Service
public class MercadoPagoService {

    public String crearPreferenciaPago(String cocheraCodigo, BigDecimal tarifa, String referenciaExterna, String email) {
        
        // 🚀 MODO PRESENTACIÓN: Imprime en la consola del backend que se está simulando
        System.out.println("🟢 SIMULADOR ACTIVADO: Generando pase de pago para cochera " + cocheraCodigo);
        
        // Genera un ID instantáneo y falso para que el Frontend abra el modal simulado
        // sin intentar conectarse a los servidores reales de Mercado Pago
        return "simulador-mp-" + UUID.randomUUID().toString();
    }
}