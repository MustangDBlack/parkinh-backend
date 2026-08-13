package com.example.parquimetro.controller;

import com.example.parquimetro.model.Reserva;
import com.example.parquimetro.repository.ReservaRepository;
import com.example.parquimetro.service.ReservaService;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.preference.*;
import com.mercadopago.resources.preference.Preference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    private final ReservaRepository repository;
    private final ReservaService service;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    // 🚀 Inyectamos correctamente el token desde el entorno
    public ReservaController(ReservaRepository repository, 
                             ReservaService service, 
                             @Value("${mercadopago.access.token}") String mpAccessToken) {
        this.repository = repository;
        this.service = service;
        MercadoPagoConfig.setAccessToken(mpAccessToken);
    }

    @GetMapping
    public List<Reserva> obtenerHistorial() {
        return repository.findAllWithDetails(); 
    }

    @GetMapping("/usuario/{username}")
    public List<Reserva> obtenerHistorialUsuario(@PathVariable String username) {
        return repository.findByUsuarioUsernameOrderByHoraEntradaDesc(username);
    }

    @PutMapping("/{id}/pagar-deuda")
    public String pagarDeudaConMulta(@PathVariable Long id) {
        Reserva reserva = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserva no encontrada"));

        if ("PAGADO".equals(reserva.getEstadoPago())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Este ticket ya se encuentra pagado.");
        }

        try {
            LocalDateTime ahora = LocalDateTime.now();
            LocalDateTime horaFin = reserva.getHoraFinEsperada();
            
            double montoMulta = 0.0;
            if (horaFin != null && ahora.isAfter(horaFin)) {
                long minutosExceso = Duration.between(horaFin, ahora).toMinutes();
                long horasExcedidas = (long) Math.ceil(minutosExceso / 60.0);
                if (horasExcedidas > 0) {
                    montoMulta = 200.0 * Math.pow(2, horasExcedidas - 1);
                }
            }

            double montoBase = reserva.getMontoTotal() != null ? reserva.getMontoTotal().doubleValue() : 500.0;

            List<PreferenceItemRequest> items = new ArrayList<>();

            items.add(PreferenceItemRequest.builder()
                    .id("BASE-" + id)
                    .title("Estacionamiento / Tarifa Base Cochera " + (reserva.getCochera() != null ? reserva.getCochera().getCodigo() : ""))
                    .quantity(1)
                    .unitPrice(new BigDecimal(montoBase))
                    .currencyId("ARS")
                    .build());

            if (montoMulta > 0) {
                items.add(PreferenceItemRequest.builder()
                        .id("MULTA-" + id)
                        .title("Multa Progresiva por Exceso de Tiempo")
                        .quantity(1)
                        .unitPrice(new BigDecimal(montoMulta))
                        .currencyId("ARS")
                        .build());
            }

            PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                    .success(frontendUrl + "/pago-exitoso?reservaId=" + id)
                    .pending(frontendUrl + "/pago-pendiente")
                    .failure(frontendUrl + "/pago-fallido")
                    .build();

            PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                    .items(items)
                    .backUrls(backUrls)
                    .autoReturn("approved")
                    .build();

            PreferenceClient client = new PreferenceClient();
            Preference preference = client.create(preferenceRequest);

            return "{\"preferenceId\":\"" + preference.getId() + "\"}";

        } catch (Exception e) {
            System.err.println("❌ ERROR MP PAGAR DEUDA: " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al procesar pago en Mercado Pago: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/solicitar-extension")
    public String solicitarExtension(@PathVariable Long id, @RequestParam int horas) {
        try {
            double precioPorHora = 500.0;
            double montoAPagar = horas * precioPorHora;

            PreferenceItemRequest itemRequest = PreferenceItemRequest.builder()
                    .id("EXT-" + id + "-" + horas)
                    .title("Prórroga Estacionamiento PARKINH - +" + horas + "hs")
                    .quantity(1)
                    .unitPrice(new BigDecimal(montoAPagar))
                    .currencyId("ARS")
                    .build();

            List<PreferenceItemRequest> items = new ArrayList<>();
            items.add(itemRequest);

            PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                    .success(frontendUrl + "/pago-exitoso?reservaId=" + id + "&horas=" + horas)
                    .pending(frontendUrl + "/pago-pendiente")
                    .failure(frontendUrl + "/pago-fallido")
                    .build();

            PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                    .items(items)
                    .backUrls(backUrls)
                    .autoReturn("approved")
                    .build();

            PreferenceClient client = new PreferenceClient();
            Preference preference = client.create(preferenceRequest);

            return "{\"preferenceId\":\"" + preference.getId() + "\"}";
        } catch (Exception e) {
            System.err.println("❌ ERROR MP SOLICITAR EXTENSION: " + e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error con Mercado Pago: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/confirmar-extension")
    public Reserva confirmarExtension(@PathVariable Long id, @RequestParam int horas) {
        try {
            return service.extenderReserva(id, horas);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}