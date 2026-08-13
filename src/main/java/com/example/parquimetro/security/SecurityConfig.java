package com.example.parquimetro.security;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
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
            .cors(cors -> {}) // Delegado al CorsFilter de máxima prioridad
            .authorizeHttpRequests(auth -> auth
                // Permitir explícitamente todas las peticiones OPTIONS (Preflight de CORS)
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                
                // Rutas públicas de la API
                .requestMatchers("/api/usuarios/registro", "/api/usuarios/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/mercado-pago/webhook").permitAll()
                
                // Todo lo demás exige autenticación
                .anyRequest().authenticated()
            );
        return http.build();
    }

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilterRegistration() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        
        // Dominios permitidos (Producción y Local)
        config.setAllowedOrigins(List.of(
            "https://parkinh.blackkode.com.ar",
            "http://localhost:5173"
        ));
        
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        source.registerCorsConfiguration("/**", config);
        
        CorsFilter corsFilter = new CorsFilter(source);
        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(corsFilter);
        
        // 🚀 MÁXIMA PRIORIDAD: Se ejecuta antes que cualquier seguridad o filtro del sistema
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }
}