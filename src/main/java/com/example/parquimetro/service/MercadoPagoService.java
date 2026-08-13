package com.example.parquimetro.service;

import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferencePayerRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.preference.Preference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;

@Service
public class MercadoPagoService {

    // Ya no inyectamos el Token aquí, de eso se encarga MpConfig.java
    
    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${app.backend.url:http://localhost:8080}")
    private String backendUrl;

    public String crearPreferenciaPago(String cocheraCodigo, BigDecimal tarifa, String referenciaExterna, String email) {
        try {
            PreferenceItemRequest itemRequest = PreferenceItemRequest.builder()
                    .title("Reserva Estacionamiento IESNH - Lugar: " + cocheraCodigo)
                    .quantity(1)
                    .unitPrice(tarifa) 
                    .currencyId("ARS")
                    .build();

            PreferencePayerRequest payerRequest = PreferencePayerRequest.builder()
                    .email(email)
                    .build();

            PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                    .success(frontendUrl + "/pago-exitoso")
                    .pending(frontendUrl + "/pago-pendiente")
                    .failure(frontendUrl + "/pago-fallido")
                    .build();

            PreferenceRequest requestBuilder = PreferenceRequest.builder()
                    .items(Collections.singletonList(itemRequest))
                    .payer(payerRequest) 
                    .autoReturn("approved")
                    .externalReference(referenciaExterna)
                    .backUrls(backUrls)
                    .notificationUrl(backendUrl + "/api/mercado-pago/webhook")
                    .build();

            PreferenceClient client = new PreferenceClient();
            Preference preference = client.create(requestBuilder);

            return preference.getId();

        } catch (MPApiException apiException) {
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