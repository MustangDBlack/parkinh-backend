package com.example.parquimetro.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct; // Usa javax.annotation.PostConstruct si tienes Spring Boot 2.x
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void initFirebase() {
        try {
            // Evitamos que se inicie múltiples veces
            if (FirebaseApp.getApps().isEmpty()) {
                // Buscamos el archivo JSON en la carpeta resources
                InputStream serviceAccount = getClass().getClassLoader().getResourceAsStream("firebase-admin.json");

                if (serviceAccount == null) {
                    System.err.println("❌ ERROR FATAL: No se encontró el archivo firebase-admin.json en resources.");
                    return;
                }

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();

                FirebaseApp.initializeApp(options);
                System.out.println("✅ FIREBASE ADMIN INICIALIZADO EXITOSAMENTE PARA IES NUEVO HORIZONTE");
            }
        } catch (Exception e) {
            System.err.println("❌ Error al inicializar Firebase:");
            e.printStackTrace();
        }
    }
}