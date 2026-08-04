package com.example.parquimetro.controller;

import com.example.parquimetro.model.Usuario;
import com.example.parquimetro.service.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "*")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @PostMapping("/registro")
    public Usuario registrar(@RequestBody Usuario usuario) {
        return service.registrarUsuario(usuario);
    }

    // --- RUTA PARA INICIAR SESIÓN ---
    @PostMapping("/login")
    public Usuario login(@RequestBody Usuario usuario) {
        return service.autenticar(
            usuario.getUsername(), 
            usuario.getPassword()
        );
    }

    // --- RUTA: FICHA INSTITUCIONAL (Para que el Guardia busque por patente) ---
    @GetMapping("/patente/{patente}")
    public ResponseEntity<Usuario> buscarPorPatente(@PathVariable String patente) {
        return service.buscarPorPatente(patente)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // --- NUEVA RUTA: ACTUALIZAR TOKEN DE NOTIFICACIONES (FCM) ---
    // Recibe el token generado en React y lo vincula de forma segura al usuario
    @PutMapping("/{username}/fcm-token")
    public ResponseEntity<Void> actualizarFcmToken(
            @PathVariable String username, 
            @RequestBody Map<String, String> body) {
        
        String token = body.get("token");
        if (token == null) {
            return ResponseEntity.badRequest().build();
        }
        
        service.actualizarFcmToken(username, token);
        return ResponseEntity.ok().build();
    }

    // --- NUEVA RUTA DE PRUEBA: DISPARAR NOTIFICACIÓN DESDE EL BACKEND ---
    // La hacemos GET solo por comodidad para probarla rápido desde la barra de direcciones del navegador
    @GetMapping("/{username}/disparar-alerta")
    public ResponseEntity<String> dispararAlertaPrueba(@PathVariable String username) {
        service.enviarNotificacionPrueba(username);
        return ResponseEntity.ok("Disparo ordenado al usuario: " + username);
    }
}