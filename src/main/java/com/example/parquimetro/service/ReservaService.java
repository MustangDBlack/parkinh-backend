package com.example.parquimetro.service;

import com.example.parquimetro.model.Reserva;
import com.example.parquimetro.model.Cochera;
import com.example.parquimetro.repository.ReservaRepository;
import com.example.parquimetro.repository.CocheraRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

@Service
public class ReservaService {

    private final ReservaRepository reservaRepository;
    private final CocheraRepository cocheraRepository;

    private static final ZoneId ZONA_JUJUY = ZoneId.of("America/Argentina/Jujuy");
    private static final double PRECIO_HORA_BASE = 500.0;

    public ReservaService(ReservaRepository reservaRepository, CocheraRepository cocheraRepository) {
        this.reservaRepository = reservaRepository;
        this.cocheraRepository = cocheraRepository;
    }

    @Transactional
    public Reserva crearReserva(Reserva reserva) {
        reserva.setHoraEntrada(LocalDateTime.now(ZONA_JUJUY));
        reserva.setHoraFinEsperada(calcularHoraFinEsperada(reserva.getTipoPase()));
        reserva.setEstadoPago("PENDIENTE");
        return reservaRepository.save(reserva);
    }

    @Transactional
    public Reserva finalizarReserva(Long reservaId) {
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));

        reserva.setHoraSalida(LocalDateTime.now(ZONA_JUJUY));

        if (reserva.getHoraFinEsperada() != null) {
            // Calcula el total acumulado de las horas extra (Base + Multa exponencial por cada hora)
            double cargoExtra = calcularMultaPorExceso(reserva.getHoraFinEsperada(), reserva.getHoraSalida());
            
            if (cargoExtra > 0) {
                // Sumamos la base inicial + los cargos extra generados por el retraso
                double montoFinal = reserva.getMontoTotal() + cargoExtra;
                reserva.setMontoTotal(montoFinal);
                reserva.setEstadoPago("PENDIENTE"); // Bloqueamos al usuario por deuda
            } else {
                reserva.setEstadoPago("PAGADO"); // Todo en orden / Dentro de la tolerancia
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
    public Reserva extenderReserva(Long reservaId, int horasACargar) {
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new RuntimeException("Reserva activa no encontrada"));

        if (reserva.getHoraSalida() != null) {
            throw new RuntimeException("No se puede extender una reserva que ya ha finalizado.");
        }

        LocalDateTime ahora = LocalDateTime.now(ZONA_JUJUY);
        LocalDateTime actualFin = reserva.getHoraFinEsperada();

        LocalDateTime nuevaHoraFin = actualFin.isBefore(ahora) ? ahora.plusHours(horasACargar) : actualFin.plusHours(horasACargar);
        reserva.setHoraFinEsperada(nuevaHoraFin);

        double costoExtension = horasACargar * PRECIO_HORA_BASE;
        reserva.setMontoTotal(reserva.getMontoTotal() + costoExtension);
        reserva.setEstadoPago("PAGADO"); 

        return reservaRepository.save(reserva);
    }

    @Transactional
    public Reserva finalizarReservaPorCocheraCodigo(String codigo) {
        Optional<Reserva> reservaOpt = reservaRepository.findFirstByCocheraCodigoAndHoraSalidaIsNullOrderByHoraEntradaDesc(codigo);

        if (reservaOpt.isEmpty()) {
            cocheraRepository.findByCodigo(codigo).ifPresent(cocheraBugeada -> {
                if (cocheraBugeada.isOcupado()) {
                    cocheraBugeada.setOcupado(false);
                    cocheraRepository.save(cocheraBugeada); 
                }
            });
            return null;
        }

        return finalizarReserva(reservaOpt.get().getId());
    }

    public boolean tieneReservaActiva(String username) {
        return reservaRepository.existsByUsuarioUsernameAndHoraSalidaIsNull(username);
    }

    public boolean tieneDeudaPendiente(String username) {
        return reservaRepository.existsByUsuarioUsernameAndEstadoPago(username, "PENDIENTE");
    }

    /**
     * 🚀 CORREGIDO: Suma la Tarifa Base ($500) + la Multa Exponencial progresiva ($200, $400, $800...)
     * por cada hora extra transcurrida.
     */
    public double calcularMultaPorExceso(LocalDateTime horaFinEsperada, LocalDateTime horaSalidaReal) {
        if (horaSalidaReal.isBefore(horaFinEsperada) || horaSalidaReal.isEqual(horaFinEsperada)) {
            return 0.0;
        }

        Duration tiempoExtra = Duration.between(horaFinEsperada, horaSalidaReal);
        long minutosExtra = tiempoExtra.toMinutes();

        if (minutosExtra <= 15) return 0.0; // Tolerancia institucional de 15 minutos

        long horasExtra = (long) Math.ceil(minutosExtra / 60.0);
        double totalCargoExtra = 0.0;

        for (int i = 1; i <= horasExtra; i++) {
            // 1. Multa exponencial pura para la hora i: 200, 400, 800, 1600...
            double multaExponencial = 200.0 * Math.pow(2, i - 1);
            
            // 2. Cada hora extra suma la Tarifa Base de la hora ($500) + su respectiva multa
            double costoHoraExtra = PRECIO_HORA_BASE + multaExponencial;
            
            totalCargoExtra += costoHoraExtra;
        }

        return totalCargoExtra;
    }

    private LocalDateTime calcularHoraFinEsperada(String tipoPase) {
        LocalDateTime ahora = LocalDateTime.now(ZONA_JUJUY);
        if (tipoPase == null) return ahora.plusHours(1);

        switch (tipoPase.toUpperCase()) {
            case "1_HORA": return ahora.plusHours(1);
            case "2_HORAS": return ahora.plusHours(2);
            case "3_HORAS": return ahora.plusHours(3);
            case "4_HORAS": return ahora.plusHours(4);
            default: return ahora.plusHours(1);
        }
    }
}