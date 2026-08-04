package com.example.parquimetro.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "cocheras")
@Data 
public class Cochera {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String codigo; // Ejemplo: "A1", "G1" (Grandes)
    
    @Column(nullable = false)
    private String tipo; // "ESTANDAR" o "GRANDE"
    
    private boolean ocupado = false; // Por defecto arranca libre
    
    private Double tarifaActual; // Para manejar el costo de la reserva

    // NUEVO: Campo para la Baja Lógica (verdadero por defecto)
    @Column(columnDefinition = "boolean default true")
    private boolean activo = true; 
}