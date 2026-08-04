package com.example.parquimetro.service;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferencePayerRequest; // 🚀 NUEVA IMPORTACIÓN (El Pagador)
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.preference.Preference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.Collections;

@Service
public class MercadoPagoService {

    @Value("${mercadopago.access.token}")
    private String accessToken;

    // Inyectamos la URL dinámica para soportar Ngrok o Localhost
    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @PostConstruct
    public void init() {
        try {
            MercadoPagoConfig.setAccessToken(accessToken);
            System.out.println("✅ SERVICIO MP: Listo. Frontend enrutado a: " + frontendUrl);
        } catch (Exception e) {
            System.err.println("❌ Error al cargar token MP: " + e.getMessage());
        }
    }

    // 🚀 CORRECCIÓN: Agregamos "String email" como parámetro obligatorio
    public String crearPreferenciaPago(String cocheraCodigo, BigDecimal tarifa, String referenciaExterna, String email) {
        try {
            PreferenceItemRequest itemRequest = PreferenceItemRequest.builder()
                    .title("Reserva Estacionamiento IESNH - Lugar: " + cocheraCodigo)
                    .quantity(1)
                    .unitPrice(tarifa) 
                    .currencyId("ARS")
                    .build();

            // 🚀 NUEVO: Construimos el objeto del pagador con el email simulado desde React
            PreferencePayerRequest payerRequest = PreferencePayerRequest.builder()
                    .email(email)
                    .build();

            // Usamos la variable dinámica
            PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                    .success(frontendUrl + "/pago-exitoso")
                    .pending(frontendUrl + "/pago-pendiente")
                    .failure(frontendUrl + "/pago-fallido")
                    .build();

            PreferenceRequest requestBuilder = PreferenceRequest.builder()
                    .items(Collections.singletonList(itemRequest))
                    .payer(payerRequest) // 🚀 VINCULAMOS EL PAGADOR AQUÍ PARA EVITAR EL RECHAZO
                    .autoReturn("approved")
                    .externalReference(referenciaExterna)
                    .backUrls(backUrls)
                    .build();

            PreferenceClient client = new PreferenceClient();
            Preference preference = client.create(requestBuilder);

            // 🚀 CAMBIO VITAL PARA REACT (OPCIÓN A): Devolvemos el ID, no el InitPoint
            return preference.getId();

        } catch (MPApiException apiException) {
            // Diagnóstico avanzado que captura el estado exacto de MP
            int statusCode = apiException.getApiResponse().getStatusCode();
            String content = apiException.getApiResponse().getContent();
            
            System.err.println("🚨 ERROR CRÍTICO MP - STATUS: " + statusCode);
            System.err.println("🚨 ERROR CRÍTICO MP - CONTENIDO: " + content);
            
            throw new RuntimeException("Error en MP (Status " + statusCode + "): " + content);
        } catch (MPException e) {
            System.err.println("🚨 ERROR INTERNO SDK: " + e.getMessage());
            throw new RuntimeException("Error interno de Mercado Pago.");
        } catch (Exception e) {
            System.err.println("🚨 ERROR GENERAL: " + e.getMessage());
            throw new RuntimeException("Error inesperado al crear preferencia.");
        }
    }
}