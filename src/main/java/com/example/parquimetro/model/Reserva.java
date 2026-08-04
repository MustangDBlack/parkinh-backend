package com.example.parquimetro.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "reservas")
@Data
@NoArgsConstructor  // Añadimos constructores por buenas prácticas con JPA
@AllArgsConstructor
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String patente; // Ej: "AB123CD"

    @Column(nullable = false)
    private LocalDateTime horaEntrada; // La hora exacta a la que se ocupó el lugar

    private LocalDateTime horaSalida; // Queda vacío hasta que el auto se va

    // --- VITAL PARA EL MOTOR DE MULTAS Y LAS ALERTAS PUSH ---
    @Column(name = "hora_fin_esperada")
    private LocalDateTime horaFinEsperada; 

    private Double montoTotal; // El precio cerrado según el abono elegido

    // Opciones operativas: "1_HORA", "2_HORAS", "3_HORAS", "TURNO_COMPLETO"
    private String tipoPase;  
    
    private String turno;      // "MANANA", "TARDE", "NOCHE"
    private String metodoPago; // "EFECTIVO" o "TRANSFERENCIA"

    // 🚀 NUEVO: CONTROL DE DEUDAS Y MOROSIDAD
    @Column(name = "estado_pago")
    private String estadoPago = "PAGADO"; // Por defecto nace pagado, cambia a PENDIENTE si hay multa

    // ¡La Magia Relacional! Conectamos el ticket con la cochera física
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cochera_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"}) // <-- ESTO EVITA EL ERROR 500
    private Cochera cochera;

    // Conectamos el ticket con el usuario del sistema institucional
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id") // Lo dejamos sin 'nullable = false' por si estaciona alguien no registrado
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"}) // <-- ESTO EVITA EL ERROR 500
    private Usuario usuario;
}