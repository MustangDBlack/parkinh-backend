package com.example.parquimetro.controller;

import com.example.parquimetro.model.Cochera;
import com.example.parquimetro.model.Reserva;
import com.example.parquimetro.model.Usuario;
import com.example.parquimetro.repository.UsuarioRepository;
import com.example.parquimetro.service.CocheraService;
import com.example.parquimetro.service.ReservaService;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@RestController
// 🚀 CAMBIO VITAL: Reemplazamos el "*" por los dominios permitidos y habilitamos credenciales
@CrossOrigin(origins = {"https://parkinh.blackkode.com.ar", "http://localhost:5173"}, allowCredentials = "true")
@RequestMapping("/api/cocheras")
public class CocheraController {

    private final CocheraService service;
    private final ReservaService reservaService;
    private final UsuarioRepository usuarioRepository; 

    public CocheraController(CocheraService service, ReservaService reservaService, UsuarioRepository usuarioRepository) {
        this.service = service;
        this.reservaService = reservaService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    public List<Cochera> obtenerCocheras() {
        return service.listarTodas();
    }

    @PostMapping
    public Cochera registrarCochera(@RequestBody Cochera cochera) {
        return service.crearCochera(cochera);
    }

    @PostMapping("/{codigo}/entrada")
    @Transactional 
    public Reserva registrarEntrada(
            @PathVariable String codigo, 
            @RequestParam String patente,
            @RequestParam String tipoPase,
            @RequestParam String turno,
            @RequestParam BigDecimal monto, 
            @RequestParam(required = false, defaultValue = "TRANSFERENCIA") String metodoPago,
            @RequestParam(required = false) String username) { 
        
        Cochera cochera = service.obtenerPorCodigo(codigo); 
        
        // 1. Verificamos que el lugar no esté ocupado físicamente
        if (cochera.isOcupado()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cochera " + codigo + " ya se encuentra ocupada.");
        }

        // 2. BLINDAJE BACKEND: Un usuario activo, un solo lugar activo + Candado Anti-Morosos
        if (username != null && !username.trim().isEmpty()) {
            Optional<Usuario> userOpt = usuarioRepository.findByUsername(username);
            if (userOpt.isPresent()) {
                Usuario usuario = userOpt.get();
                
                // Verificamos si el usuario ya tiene un ticket sin cerrar
                if (reservaService.tieneReservaActiva(usuario.getUsername())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El usuario ya tiene un lugar asignado. Debe liberar su espacio actual antes de ocupar otro.");
                }

                // CANDADO ANTI-DEUDAS: Si debe dinero, se rechaza el acceso
                if (reservaService.tieneDeudaPendiente(usuario.getUsername())) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ACCESO DENEGADO: Tienes una deuda pendiente por tiempo excedido. Debes abonarla para volver a utilizar el estacionamiento.");
                }
            }
        }

        cochera.setOcupado(true);
        service.crearCochera(cochera); 
        
        Reserva nuevaReserva = new Reserva();
        nuevaReserva.setCochera(cochera);
        nuevaReserva.setPatente(patente.replace(" ", "").toUpperCase()); 
        nuevaReserva.setTipoPase(tipoPase);
        nuevaReserva.setTurno(turno);
        nuevaReserva.setMontoTotal(monto.doubleValue()); 
        nuevaReserva.setMetodoPago(metodoPago); 
        
        if (username != null && !username.trim().isEmpty()) {
            Optional<Usuario> userOpt = usuarioRepository.findByUsername(username);
            if (userOpt.isPresent()) {
                nuevaReserva.setUsuario(userOpt.get()); 
            }
        }
        
        return reservaService.crearReserva(nuevaReserva);
    }

    @PostMapping("/{codigo}/salida")
    public Reserva registrarSalida(@PathVariable String codigo) {
        Reserva reserva = reservaService.finalizarReservaPorCocheraCodigo(codigo);
        
        if (reserva == null) {
            throw new ResponseStatusException(HttpStatus.OK, "El lugar " + codigo + " figuraba ocupado por error de sistema y ha sido liberado automáticamente.");
        }
        
        return reserva;
    }

    @DeleteMapping("/{codigo}")
    public void eliminarCochera(@PathVariable String codigo) {
        service.eliminarCochera(codigo);
    }
}