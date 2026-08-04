package com.example.parquimetro.service;

import com.example.parquimetro.model.Usuario;
import com.example.parquimetro.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder; // IMPORTANTE
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UsuarioService {

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder; // Inyectamos el encoder
    private final NotificacionPushService pushService; // AGREGADO: Servicio de Push

    // Actualizamos el constructor para incluir el servicio de notificaciones
    public UsuarioService(UsuarioRepository repository, PasswordEncoder passwordEncoder, NotificacionPushService pushService) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.pushService = pushService;
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
        // 1. Buscamos al usuario SOLO por el nombre de usuario
        // ASEGÚRATE que tu UsuarioRepository tenga el método: Optional<Usuario> findByUsername(String username);
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

    // --- NUEVO: ACTUALIZAR TOKEN DE FIREBASE ---
    public void actualizarFcmToken(String username, String token) {
        repository.findByUsername(username).ifPresent(usuario -> {
            usuario.setFcmToken(token);
            repository.save(usuario);
            System.out.println("🚀 Token FCM guardado con éxito para el usuario: " + username);
        });
    }

    // --- NUEVO: DISPARO DE PRUEBA DESDE JAVA ---
    public void enviarNotificacionPrueba(String username) {
        repository.findByUsername(username).ifPresent(usuario -> {
            if (usuario.getFcmToken() != null && !usuario.getFcmToken().isEmpty()) {
                pushService.enviarAlerta(
                    usuario.getFcmToken(), 
                    "🚀 ¡El Backend ha tomado el control!", 
                    "Hola " + username + ". Esta notificación fue disparada 100% desde tu servidor en Spring Boot."
                );
            } else {
                System.out.println("⚠️ El usuario " + username + " no tiene token FCM.");
            }
        });
    }
}