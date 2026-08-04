package com.example.parquimetro.repository;

import com.example.parquimetro.model.Cochera;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CocheraRepository extends JpaRepository<Cochera, Long> {
    
    // Spring crea la consulta SQL automáticamente
    Optional<Cochera> findByCodigo(String codigo);

    // Filtra automáticamente ocultando las que fueron "eliminadas" (Baja Lógica)
    List<Cochera> findByActivoTrue(); 
}