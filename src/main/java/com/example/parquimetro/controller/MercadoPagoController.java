package com.example.parquimetro.controller;

import com.example.parquimetro.model.Cochera;
import com.example.parquimetro.model.Reserva;
import com.example.parquimetro.model.Usuario;
import com.example.parquimetro.repository.UsuarioRepository;
import com.example.parquimetro.service.CocheraService;
import com.example.parquimetro.service.ReservaService;
import com.example.parquimetro.service.MercadoPagoService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

@RestController
@CrossOrigin(origins = "*") 
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

    @PostMapping(value = "/reservar-digital/{codigo}", produces = MediaType.TEXT_PLAIN_VALUE)
    @Transactional 
    public ResponseEntity<String> reservarDigital(
            @PathVariable String codigo, 
            @RequestParam String patente,
            @RequestParam String tipoPase,
            @RequestParam String turno,
            @RequestParam BigDecimal monto, 
            @RequestParam String email, // 🚀 NUEVO: Recibimos el email desde React
            @RequestParam(required = false) String username) { 
        
        try {
            if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest().body("Error: El monto de la reserva debe ser mayor a 0.");
            }

            Cochera cochera = cocheraService.obtenerPorCodigo(codigo);
            if (cochera.isOcupado()) { 
                return ResponseEntity.badRequest().body("Error: El lugar ya fue ocupado por alguien más.");
            }

            String referenciaExterna = "cochera:" + codigo;
            
            // 🚀 PASAMOS EL EMAIL AL SERVICIO
            String idPreferencia = mercadoPagoService.crearPreferenciaPago(codigo, monto, referenciaExterna, email);
            
            Reserva nuevaReserva = new Reserva();
            nuevaReserva.setCochera(cochera);
            nuevaReserva.setPatente(patente.replace(" ", "").toUpperCase());
            nuevaReserva.setTipoPase(tipoPase);
            nuevaReserva.setTurno(turno);
            nuevaReserva.setMontoTotal(monto.doubleValue()); 
            nuevaReserva.setMetodoPago("MERCADO_PAGO"); 
            
            if (username != null && !username.trim().isEmpty()) {
                Optional<Usuario> userOpt = usuarioRepository.findByUsername(username);
                userOpt.ifPresent(nuevaReserva::setUsuario); 
            }
            
            reservaService.crearReserva(nuevaReserva);

            cochera.setOcupado(true);
            cocheraService.crearCochera(cochera); 
            
            // 🚀 Retornamos el ID de la preferencia para que React renderice el botón
            return ResponseEntity.ok(idPreferencia);

        } catch (Exception e) {
            System.err.println("❌ ERROR AL CREAR PREFERENCIA: " + e.getMessage());
            return ResponseEntity.internalServerError().body("Error al procesar el pago: " + e.getMessage());
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<?> recibirNotificacionPago(@RequestBody Map<String, Object> payload) {
        System.out.println("🔔 WEBHOOK RECIBIDO DESDE MERCADO PAGO: \n" + payload);
        return ResponseEntity.ok().build();
    }
}