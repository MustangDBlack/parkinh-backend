package com.example.parquimetro.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 1. Activa CORS buscando automáticamente el bean corsConfigurationSource
            .cors(Customizer.withDefaults())
            // 2. Desactiva CSRF (necesario para APIs REST)
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // 3. Permite todas las peticiones OPTIONS de forma global
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                
                // 4. Rutas públicas de usuarios y webhooks
                .requestMatchers("/api/usuarios/registro", "/api/usuarios/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/mercado-pago/webhook").permitAll()
                
                // 🚀 5. Permitir que cualquiera pueda ver el mapa de cocheras (Evita el error 403)
                .requestMatchers(HttpMethod.GET, "/api/cocheras").permitAll()
                
                // 6. Todo lo demás protegido
                .anyRequest().authenticated()
            );
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Orígenes exactos
        configuration.setAllowedOrigins(Arrays.asList(
            "https://parkinh.blackkode.com.ar",
            "http://localhost:5173"
        ));
        
        // Métodos explícitos
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"));
        
        // Cabeceras (Agregamos explícitamente las que React suele enviar)
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization", "Content-Type", "Accept", "X-Requested-With", "Origin"
        ));
        
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L); // Cachea la respuesta preflight 1 hora

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}