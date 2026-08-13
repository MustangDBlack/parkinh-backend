package com.example.parquimetro.service;

import com.example.parquimetro.model.Usuario;
import com.example.parquimetro.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UsuarioService {

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public Usuario registrarUsuario(Usuario usuario) {
        // 1. Hasheamos la contraseña ANTES de guardar
        String hash = passwordEncoder.encode(usuario.getPassword());
        usuario.setPassword(hash);

        // 2. Limpieza de patente
        if (usuario.getPatenteHabitual() != null) {
            usuario.setPatenteHabitual(usuario.getPatenteHabitual().replace(" ", "").trim().toUpperCase());
        }
        
        return repository.save(usuario);
    }

    public Usuario autenticar(String username, String password) {
        // 1. Buscamos al usuario por el nombre de usuario
        Usuario usuario = repository.findByUsername(username.trim())
                .orElseThrow(() -> new RuntimeException("Credenciales incorrectas"));

        // 2. Comparamos el password recibido con el HASH guardado
        if (passwordEncoder.matches(password.trim(), usuario.getPassword())) {
            return usuario;
        } else {
            throw new RuntimeException("Credenciales incorrectas");
        }
    }

    public Optional<Usuario> buscarPorPatente(String patente) {
        if (patente == null || patente.trim().isEmpty()) {
            return Optional.empty();
        }
        String patenteLimpia = patente.replace(" ", "").trim().toUpperCase();
        return repository.findByPatenteHabitualIgnoreCase(patenteLimpia);
    }
}
