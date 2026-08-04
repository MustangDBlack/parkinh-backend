package com.example.parquimetro.service;

import com.example.parquimetro.model.Cochera;
import com.example.parquimetro.repository.CocheraRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CocheraService {

    private final CocheraRepository cocheraRepository;

    public CocheraService(CocheraRepository cocheraRepository) {
        this.cocheraRepository = cocheraRepository;
    }

    // Traemos solo las cocheras que siguen activas (Baja Lógica)
    public List<Cochera> listarTodas() {
        return cocheraRepository.findByActivoTrue();
    }

    // 🚀 SOLUCIÓN AL ERROR 3: Lógica inteligente de creación / actualización
    public Cochera crearCochera(Cochera cochera) {
        // 1. Si la cochera ya tiene ID, es una actualización (alguien estacionó o se fue)
        if (cochera.getId() != null) {
            return cocheraRepository.save(cochera);
        }

        // 2. Si no tiene ID, es un intento de crear un NUEVO lugar desde el panel Admin
        Optional<Cochera> cocheraExistente = cocheraRepository.findByCodigo(cochera.getCodigo());

        if (cocheraExistente.isPresent()) {
            Cochera existente = cocheraExistente.get();
            
            // Si el lugar existe pero estaba oculto (Baja Lógica), lo REVIVIMOS
            if (!existente.isActivo()) {
                existente.setActivo(true);
                existente.setOcupado(false); // Por seguridad, nace vacío
                return cocheraRepository.save(existente);
            } else {
                // Si ya existe y está activo, detenemos la duplicación
                throw new RuntimeException("La cochera " + cochera.getCodigo() + " ya existe en la playa.");
            }
        }

        // 3. Si realmente no existe en la base de datos, lo creamos desde cero
        cochera.setActivo(true);
        cochera.setOcupado(false);
        return cocheraRepository.save(cochera);
    }

    public Cochera obtenerPorCodigo(String codigo) {
        return cocheraRepository.findByCodigo(codigo)
                .orElseThrow(() -> new RuntimeException("Cochera no encontrada con el código: " + codigo));
    }

    // --- LÓGICA DE BAJA LÓGICA REFORZADA ---
    public void eliminarCochera(String codigo) {
        Cochera cochera = obtenerPorCodigo(codigo);
        
        // En vez de borrarla físicamente, la ocultamos
        cochera.setActivo(false);
        // 🚀 BLINDAJE: Si el admin la elimina por error con un auto adentro, liberamos el lugar
        cochera.setOcupado(false); 
        
        cocheraRepository.save(cochera);
    }
}