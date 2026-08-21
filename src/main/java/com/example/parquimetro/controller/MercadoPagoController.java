package com.example.parquimetro.controller;

import com.example.parquimetro.model.Cochera;
import com.example.parquimetro.model.Reserva;
import com.example.parquimetro.model.Usuario;
import com.example.parquimetro.repository.UsuarioRepository;
import com.example.parquimetro.service.CocheraService;
import com.example.parquimetro.service.ReservaService;
import com.example.parquimetro.service.MercadoPagoService;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.resources.payment.Payment;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

@RestController
// 🚀 CAMBIO VITAL: Reemplazamos el "*" por los dominios permitidos y habilitamos credenciales
@CrossOrigin(origins = {"https://parkinh.blackkode.com.ar", "http://localhost:5173"}, allowCredentials = "true")
@RequestMapping("/api/mercado-pago")
public class MercadoPagoController {

    private final CocheraService cocheraService;
    private final ReservaService reservaService;
    private final MercadoPagoService mercadoPagoService;
    private final UsuarioRepository usuarioRepository; 

    public MercadoPagoController(CocheraService cocheraService, ReservaService reservaService, 
                                 MercadoPagoService mercadoPagoService, UsuarioRepository usuarioRepository) {
        this.cocheraService = cocheraService;
        this.reservaService = reservaService;
        this.mercadoPagoService = mercadoPagoService;
        this.usuarioRepository = usuarioRepository;
    }

    @PostMapping(value = "/reservar-digital/{codigo}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Transactional 
    public ResponseEntity<String> reservarDigital(
            @PathVariable String codigo, 
            @RequestParam String patente,
            @RequestParam String tipoPase,
            @RequestParam String turno,
            @RequestParam BigDecimal monto, 
            @RequestParam String email, 
            @RequestParam(required = false) String username) { 
        
        try {
            if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest().body("Error: El monto de la reserva debe ser mayor a 0.");
            }

            Cochera cochera = cocheraService.obtenerPorCodigo(codigo);
            if (cochera.isOcupado()) { 
                return ResponseEntity.badRequest().body("Error: El lugar ya fue ocupado por alguien más.");
            }

            // Referencia externa única vinculada al código de cochera y patente
            String referenciaExterna = codigo + "_" + patente.replace(" ", "").toUpperCase();
            
            // 1. Creamos la preferencia en Mercado Pago
            String idPreferencia = mercadoPagoService.crearPreferenciaPago(codigo, monto, referenciaExterna, email);
            
            // 2. Registramos la reserva como PENDIENTE (sin bloquear la cochera físicamente aún, o dejándola en espera)
            Reserva nuevaReserva = new Reserva();
            nuevaReserva.setCochera(cochera);
            nuevaReserva.setPatente(patente.replace(" ", "").toUpperCase());
            nuevaReserva.setTipoPase(tipoPase);
            nuevaReserva.setTurno(turno);
            nuevaReserva.setMontoTotal(monto.doubleValue()); 
            nuevaReserva.setMetodoPago("MERCADO_PAGO");
            nuevaReserva.setEstadoPago("PENDIENTE"); 
            
            if (username != null && !username.trim().isEmpty()) {
                Optional<Usuario> userOpt = usuarioRepository.findByUsername(username);
                userOpt.ifPresent(nuevaReserva::setUsuario); 
            }
            
            reservaService.crearReserva(nuevaReserva);
            
            // 🚀 CORREGIDO: Devolvemos JSON con preferenceId para que el frontend lo lea como data.preferenceId
            return ResponseEntity.ok("{\"preferenceId\":\"" + idPreferencia + "\"}");

        } catch (Exception e) {
            System.err.println("❌ ERROR AL CREAR PREFERENCIA: " + e.getMessage());
            return ResponseEntity.internalServerError().body("Error al procesar el pago: " + e.getMessage());
        }
    }

    /**
     * 🚀 WEBHOOK REAL: Recibe las notificaciones de Mercado Pago cuando el pago se aprueba.
     * 
     * Mercado Pago envía las notificaciones de DOS formas:
     * 1. Formato antiguo (query params): GET /webhook?topic=payment&id=123456
     * 2. Formato nuevo (JSON body): POST /webhook con body {"type":"payment","data":{"id":"123456"}}
     * 
     * Este método maneja AMBOS formatos para evitar errores 400.
     */
    @RequestMapping(value = "/webhook", method = {RequestMethod.GET, RequestMethod.POST})
    @Transactional
    public ResponseEntity<?> recibirNotificacionPago(
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String id,
            @RequestBody(required = false) Map<String, Object> payload) {
        try {
            System.out.println("🔔 WEBHOOK RECIBIDO DESDE MERCADO PAGO - topic: " + topic + ", id: " + id + ", payload: " + payload);

            Long paymentId = null;

            // Formato 1: Query params (?topic=payment&id=123456)
            if (id != null && !id.isEmpty()) {
                paymentId = Long.valueOf(id);
            }
            // Formato 2: JSON body ({"type":"payment","data":{"id":"123456"}})
            else if (payload != null && payload.containsKey("type")) {
                String type = (String) payload.get("type");
                if ("payment".equals(type)) {
                    Map<String, Object> data = (Map<String, Object>) payload.get("data");
                    if (data != null && data.containsKey("id")) {
                        paymentId = Long.valueOf(data.get("id").toString());
                    }
                }
            }

            if (paymentId != null) {
                // Consultamos los detalles del pago directamente a la API de Mercado Pago
                PaymentClient paymentClient = new PaymentClient();
                Payment payment = paymentClient.get(paymentId);

                if ("approved".equals(payment.getStatus())) {
                    String externalReference = payment.getExternalReference(); // Ej: "A1_AB123CD"
                    
                    if (externalReference != null && externalReference.contains("_")) {
                        String[] partes = externalReference.split("_");
                        String codigoCochera = partes[0];
                        String patente = partes[1];

                        // Ocupamos la cochera físicamente y actualizamos el estado del pago
                        Cochera cochera = cocheraService.obtenerPorCodigo(codigoCochera);
                        if (cochera != null) {
                            cochera.setOcupado(true);
                            cocheraService.crearCochera(cochera);
                        }

                        System.out.println("✅ PAGO APROBADO para cochera " + codigoCochera + " y patente " + patente);
                    }
                }
            }

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            System.err.println("❌ ERROR EN WEBHOOK: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}