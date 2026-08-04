package com.example.parquimetro.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "usuarios")
@Data
@NoArgsConstructor // VITAL: JPA lo necesita para crear el objeto desde la base de datos
@AllArgsConstructor // Recomendado: Te permite crear Usuarios completos en una sola línea en tus tests o seeders
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    // 🚀 NUEVO: Obligatorio para generar la preferencia en Mercado Pago
    @Column(unique = true, nullable = false)
    private String email; 

    private String rol; // ADMIN, GUARDIA o USER

    // --- CAMPOS INSTITUCIONALES ---
    private String tipoPerfil; // ALUMNO, DOCENTE o PARTICULAR
    private String carrera; 
    private String turnoCursado; 
    private String curso; 
    private String comision; 
    private String patenteHabitual; 
    
    // Contacto directo para alertas en el estacionamiento
    private String whatsapp; 

    // --- CAMPOS PARA PARTICULARES (NUEVO) ---
    private String nombreCompleto;
    private String dni;

    // --- SISTEMA DE NOTIFICACIONES PUSH ---
    // Almacena la "matrícula" única del dispositivo para Firebase Cloud Messaging
    @Column(name = "fcm_token", length = 500)
    private String fcmToken;
}