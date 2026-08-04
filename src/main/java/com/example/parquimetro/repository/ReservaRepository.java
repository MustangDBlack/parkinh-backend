package com.example.parquimetro.repository;

import com.example.parquimetro.model.Reserva;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {
    
    // 1. Mantenemos este por si necesitas buscar por ID específico
    Reserva findByCocheraIdAndHoraSalidaIsNull(Long cocheraId);

    // 🚀 LA CORRECCIÓN MÁGICA:
    // Cambiamos el nombre para forzar que traiga solo la más reciente (First)
    // y la ordene por hora de entrada descendente. Esto elimina el error de duplicidad.
    @EntityGraph(attributePaths = {"usuario", "cochera"})
    Optional<Reserva> findFirstByCocheraCodigoAndHoraSalidaIsNullOrderByHoraEntradaDesc(String codigo);

    // 3. Para el Dashboard
    @Query("SELECT r FROM Reserva r LEFT JOIN FETCH r.usuario u LEFT JOIN FETCH r.cochera c")
    List<Reserva> findAllWithDetails();

    // 4. Para el Motor del Tiempo
    @EntityGraph(attributePaths = {"usuario", "cochera"})
    List<Reserva> findAllByHoraSalidaIsNull();

    // 🚀 CANDADO ANTI-DUPLICIDAD:
    // Verifica si un usuario ya tiene un ticket activo sin cerrar
    boolean existsByUsuarioUsernameAndHoraSalidaIsNull(String username);

    // --- NUEVAS HERRAMIENTAS PARA GESTIÓN DE DEUDAS ---

    // 🚀 BLINDAJE ANTI-MOROSOS: 
    // Verifica si el usuario tiene alguna reserva con un estado de pago específico (Ej: "PENDIENTE")
    boolean existsByUsuarioUsernameAndEstadoPago(String username, String estadoPago);

    // 🚀 HISTORIAL DE TICKETS: 
    // Trae todas las reservas de un usuario para armar su historial y saber qué debe.
    // Usamos EntityGraph para traer los datos de la cochera de forma eficiente.
    @EntityGraph(attributePaths = {"cochera"})
    List<Reserva> findByUsuarioUsernameOrderByHoraEntradaDesc(String username);
}