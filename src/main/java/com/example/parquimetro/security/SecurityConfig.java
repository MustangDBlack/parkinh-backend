package com.example.parquimetro.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults()) // Utiliza la configuración del CorsFilter de abajo
            .authorizeHttpRequests(auth -> auth
                // Permitir explícitamente todas las peticiones OPTIONS (Preflight de CORS)
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                
                // Rutas públicas
                .requestMatchers("/api/usuarios/registro", "/api/usuarios/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/mercado-pago/webhook").permitAll()
                
                // Resto protegido
                .anyRequest().authenticated()
            );
        return http.build();
    }

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Dominios permitidos en producción y desarrollo local
        configuration.setAllowedOrigins(List.of(
            "https://parkinh.blackkode.com.ar",
            "http://localhost:5173"
        ));
        
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        source.registerCorsConfiguration("/**", configuration);
        return new CorsFilter(source);
    }
}