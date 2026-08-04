package com.example.parquimetro.controller;

import com.example.parquimetro.model.Reserva;
import com.example.parquimetro.repository.ReservaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/reservas")
@CrossOrigin(origins = "*")
public class ReservaController {

    private final ReservaRepository repository;

    public ReservaController(ReservaRepository repository) {
        this.repository = repository;
    }

    // Ruta para obtener TODO el historial de tickets (Para el Dashboard del Admin)
    @GetMapping
    public List<Reserva> obtenerHistorial() {
        // 🚀 CORRECCIÓN: Usamos el método optimizado para evitar saturar la base de datos
        return repository.findAllWithDetails(); 
    }

    // 🚀 NUEVO: Obtener el HISTORIAL de un usuario específico
    @GetMapping("/usuario/{username}")
    public List<Reserva> obtenerHistorialUsuario(@PathVariable String username) {
        return repository.findByUsuarioUsernameOrderByHoraEntradaDesc(username);
    }

    // 🚀 NUEVO: Endpoint para SALDAR LA DEUDA
    // React llamará a este endpoint una vez que MercadoPago apruebe el pago de la multa.
    @PutMapping("/{id}/pagar-deuda")
    public Reserva pagarDeuda(@PathVariable Long id) {
        Reserva reserva = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserva no encontrada"));

        if ("PAGADO".equals(reserva.getEstadoPago())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Este ticket ya se encuentra pagado.");
        }

        // Cambiamos el estado para liberar al usuario del bloqueo
        reserva.setEstadoPago("PAGADO");
        
        return repository.save(reserva);
    }
}