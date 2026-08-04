package com.example.parquimetro.repository;

import com.example.parquimetro.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    
    // Método para iniciar sesión (Busca estrictamente por el nombre de usuario)
    Optional<Usuario> findByUsername(String username);

    // CORRECCIÓN CLAVE: Agregamos IgnoreCase para que busque la patente sin importar mayúsculas/minúsculas
    Optional<Usuario> findByPatenteHabitualIgnoreCase(String patenteHabitual);
    
    // Método de autenticación para el login
    Optional<Usuario> findByUsernameAndPassword(String username, String password);
}