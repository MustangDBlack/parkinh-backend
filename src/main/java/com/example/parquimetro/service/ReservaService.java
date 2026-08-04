package com.example.parquimetro.service;

import com.example.parquimetro.model.Reserva;
import com.example.parquimetro.model.Cochera;
import com.example.parquimetro.repository.ReservaRepository;
import com.example.parquimetro.repository.CocheraRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final CocheraRepository cocheraRepository;

    public ReservaService(ReservaRepository reservaRepository, CocheraRepository cocheraRepository) {
        this.reservaRepository = reservaRepository;
        this.cocheraRepository = cocheraRepository;
    }

    @Transactional
    public Reserva crearReserva(Reserva reserva) {
        reserva.setHoraEntrada(LocalDateTime.now());
        reserva.setHoraFinEsperada(calcularHoraFinEsperada(reserva.getTipoPase()));
        return reservaRepository.save(reserva);
    }

    @Transactional
    public Reserva finalizarReserva(Long reservaId) {
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));

        reserva.setHoraSalida(LocalDateTime.now());

        if (reserva.getHoraFinEsperada() != null) {
            double multaAplicada = calcularMultaPorExceso(reserva.getHoraFinEsperada(), reserva.getHoraSalida());
            
            // 🚀 GESTIÓN DE DEUDAS: Si hay multa, nace la deuda
            if (multaAplicada > 0) {
                double montoFinal = reserva.getMontoTotal() + multaAplicada;
                reserva.setMontoTotal(montoFinal);
                reserva.setEstadoPago("PENDIENTE"); // Bloqueamos al usuario
            } else {
                reserva.setEstadoPago("PAGADO"); // Todo en orden
            }
        } else {
            reserva.setEstadoPago("PAGADO");
        }

        if (reserva.getCochera() != null) {
            Cochera cochera = reserva.getCochera();
            cochera.setOcupado(false);
            cocheraRepository.save(cochera);
        }

        return reservaRepository.save(reserva);
    }

    @Transactional
    public Reserva finalizarReservaPorCocheraCodigo(String codigo) {
        Optional<Reserva> reservaOpt = reservaRepository.findFirstByCocheraCodigoAndHoraSalidaIsNullOrderByHoraEntradaDesc(codigo);

        if (reservaOpt.isEmpty()) {
            // 🚀 CORRECCIÓN: Usamos findByCodigo para buscar por el String "A1", no por ID Long
            cocheraRepository.findByCodigo(codigo).ifPresent(cocheraBugeada -> {
                if (cocheraBugeada.isOcupado()) {
                    cocheraBugeada.setOcupado(false);
                    cocheraRepository.save(cocheraBugeada); 
                }
            });

            // 🚀 SOLUCIÓN AL ERROR 500: 
            // Retornamos null para reparar el error en silencio sin hacer colapsar el servidor.
            return null;
        }

        return finalizarReserva(reservaOpt.get().getId());
    }

    // 🚀 BLINDAJE: Método para verificar si el usuario ya tiene un lugar ocupado
    public boolean tieneReservaActiva(String username) {
        return reservaRepository.existsByUsuarioUsernameAndHoraSalidaIsNull(username);
    }

    // 🚀 NUEVO CANDADO ANTI-MOROSOS: Método para detectar si debe dinero
    public boolean tieneDeudaPendiente(String username) {
        return reservaRepository.existsByUsuarioUsernameAndEstadoPago(username, "PENDIENTE");
    }

    public double calcularMultaPorExceso(LocalDateTime horaFinEsperada, LocalDateTime horaSalidaReal) {
        if (horaSalidaReal.isBefore(horaFinEsperada) || horaSalidaReal.isEqual(horaFinEsperada)) {
            return 0.0;
        }

        Duration tiempoExtra = Duration.between(horaFinEsperada, horaSalidaReal);
        long minutosExtra = tiempoExtra.toMinutes();

        if (minutosExtra <= 15) return 0.0;

        int horasExtra = (int) Math.ceil(minutosExtra / 60.0);
        double totalMulta = 0;
        double precioHoraBase = 500.0;

        for (int i = 1; i <= horasExtra; i++) {
            totalMulta += (precioHoraBase + (200.0 * i));
        }

        return totalMulta;
    }

    private LocalDateTime calcularHoraFinEsperada(String tipoPase) {
        LocalDateTime ahora = LocalDateTime.now();
        if (tipoPase == null) return ahora.plusHours(1);

        switch (tipoPase.toUpperCase()) {
            case "1_HORA": return ahora.plusHours(1);
            case "2_HORAS": return ahora.plusHours(2);
            case "3_HORAS": return ahora.plusHours(3);
            case "TURNO_COMPLETO": 
            case "1_TURNO": return ahora.plusHours(4);
            // 🚀 LIMPIEZA: Extirpados Semana, Quincena y Mes
            default: return ahora.plusHours(1);
        }
    }
}