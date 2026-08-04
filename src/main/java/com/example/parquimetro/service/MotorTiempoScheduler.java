package com.example.parquimetro.service;

import com.example.parquimetro.model.Reserva;
import com.example.parquimetro.repository.ReservaRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class MotorTiempoScheduler {

    private final ReservaRepository reservaRepository;
    private final NotificacionPushService pushService;
    
    // Memoria caché mejorada. Guardará llaves como: "reservaId-15min", "reservaId-expirado", "reservaId-hora-1"
    private final Set<String> alertasEnviadas = new HashSet<>();

    // CONFIGURACIÓN: Define cuánto cuesta la hora extra (multa) en tu sistema
    private final double TARIFA_HORA_EXTRA = 200.0; // Cambia este valor por tu tarifa real

    public MotorTiempoScheduler(ReservaRepository reservaRepository, NotificacionPushService pushService) {
        this.reservaRepository = reservaRepository;
        this.pushService = pushService;
    }

    // Se ejecuta automáticamente cada 60 segundos
    @Scheduled(fixedRate = 60000)
    public void revisarTiemposEstacionamiento() {
        List<Reserva> activas = reservaRepository.findAllByHoraSalidaIsNull();
        LocalDateTime ahora = LocalDateTime.now();

        for (Reserva reserva : activas) {
            if (reserva.getHoraFinEsperada() != null && reserva.getUsuario() != null) {
                
                long minutosDiferencia = Duration.between(ahora, reserva.getHoraFinEsperada()).toMinutes();
                String token = reserva.getUsuario().getFcmToken();
                
                if (token == null || token.isEmpty()) continue;

                long id = reserva.getId();

                // --- CASO 1: FALTAN 15 MINUTOS O MENOS (ANTES DE VENCER) ---
                if (minutosDiferencia <= 15 && minutosDiferencia > 0) {
                    String llaveAlerta = id + "-15min";
                    if (!alertasEnviadas.contains(llaveAlerta)) {
                        pushService.enviarAlerta(
                            token,
                            "🚨 Alerta de Vencimiento",
                            "Tu tiempo en la cochera " + reserva.getCochera().getCodigo() + " vence en " + minutosDiferencia + " minutos. Evita multas."
                        );
                        alertasEnviadas.add(llaveAlerta);
                    }
                }
                
                // --- CASO 2: EL TIEMPO ACABA DE EXPIRAR (MINUTO 0) ---
                else if (minutosDiferencia <= 0) {
                    long minutosExcedidos = Math.abs(minutosDiferencia);
                    long horasExcedidadas = minutosExcedidos / 60;

                    // Alerta inmediata de expiración
                    String llaveExpirado = id + "-expirado";
                    if (!alertasEnviadas.contains(llaveExpirado)) {
                        pushService.enviarAlerta(
                            token,
                            "⚠️ ¡Tiempo Expirado!",
                            "Tu tiempo asignado ha terminado. Comenzará a correr el recargo por hora extra."
                        );
                        alertasEnviadas.add(llaveExpirado);
                    }

                    // --- CASO 3: PASÓ UNA HORA EXTRA (CADA HORA QUE PASA) ---
                    if (horasExcedidadas > 0) {
                        String llaveHoraExtra = id + "-hora-" + horasExcedidadas;
                        
                        if (!alertasEnviadas.contains(llaveHoraExtra)) {
                            // Calculamos la multa acumulada de forma dinámica
                            double multaAcumulada = horasExcedidadas * TARIFA_HORA_EXTRA;
                            
                            pushService.enviarAlerta(
                                token,
                                "🚨 Alerta de Multa Acumulada",
                                "Llevas " + horasExcedidadas + " hora(s) extra en la cochera " 
                                + reserva.getCochera().getCodigo() + ". Multa actual: $" + multaAcumulada
                            );
                            
                            alertasEnviadas.add(llaveHoraExtra);
                        }
                    }
                }
            }
        }
    }
    
    @Scheduled(fixedRate = 3600000)
    public void limpiarMemoriaAlertas() {
        alertasEnviadas.clear();
        System.out.println("🧹 Motor del Tiempo: Memoria de alertas limpiada.");
    }
}