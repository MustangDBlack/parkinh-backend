package com.example.parquimetro.config;

import com.example.parquimetro.model.Usuario;
import com.example.parquimetro.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(UsuarioRepository repository, PasswordEncoder passwordEncoder) {
        return args -> {
            // 1. Crear ADMIN si no existe
            if (repository.findByUsername("admin").isEmpty()) {
                Usuario admin = new Usuario();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("admin123")); 
                admin.setEmail("admin@sistema.com"); // <- CORRECCIÓN: Email obligatorio
                admin.setRol("ADMIN");
                admin.setTipoPerfil("ADMIN");
                repository.save(admin);
                System.out.println("✅ Usuario ADMIN creado exitosamente.");
            }

            // 2. Crear GUARDIA si no existe
            if (repository.findByUsername("guardia").isEmpty()) {
                Usuario guardia = new Usuario();
                guardia.setUsername("guardia");
                guardia.setPassword(passwordEncoder.encode("guardia123")); 
                guardia.setEmail("guardia@sistema.com"); // <- CORRECCIÓN: Email obligatorio
                guardia.setRol("GUARDIA");
                guardia.setTipoPerfil("GUARDIA");
                repository.save(guardia);
                System.out.println("✅ Usuario GUARDIA creado exitosamente.");
            }
        };
    }
}