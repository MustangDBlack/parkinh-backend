package com.example.parquimetro.controller;

import com.example.parquimetro.model.Usuario;
import com.example.parquimetro.service.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}