package com.example.parquimetro.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    // 1. Configuramos la máquina encriptadora de contraseñas
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); 
    }

    // 2. Configuramos las puertas (Filtros)
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable()) // Desactivamos CSRF para que React pueda enviar POSTs
            .cors(cors -> cors.disable()) // Temporalmente desactivamos restricciones severas de origen
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/usuarios/registro").permitAll() // Puerta abierta para crear el primer Admin
                .anyRequest().permitAll() // Dejamos el resto abierto temporalmente para no romper tu mapa visual
            );
        return http.build();
    }
}