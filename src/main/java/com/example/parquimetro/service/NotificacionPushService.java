package com.example.parquimetro.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.WebpushConfig; // IMPORTANTE: Importación añadida
import com.google.firebase.messaging.WebpushNotification; // IMPORTANTE: Importación añadida
import org.springframework.stereotype.Service;

@Service
public class NotificacionPushService {

    /**
     * Envia una notificación push directamente a un dispositivo específico.
     * * @param token   El Token FCM único del dispositivo (Navegador/Celular)
     * @param titulo  El título principal de la alerta
     * @param cuerpo  El mensaje detallado
     */
    public void enviarAlerta(String token, String titulo, String cuerpo) {
        if (token == null || token.isEmpty()) {
            System.out.println("⚠️ No se puede enviar la notificación: El usuario no tiene un Token registrado.");
            return;
        }

        try {
            // 1. Armamos el diseño visual básico de la notificación
            Notification notification = Notification.builder()
                    .setTitle(titulo)
                    .setBody(cuerpo)
                    .build();

            // 2. CONFIGURACIÓN WEB: Forzamos la persistencia en el navegador del usuario
            WebpushConfig webpushConfig = WebpushConfig.builder()
                    .setNotification(WebpushNotification.builder()
                            .setRequireInteraction(true) // 🔥 Evita que la alerta desaparezca sola tras unos segundos
                            .build())
                    .build();

            // 3. Empaquetamos la notificación incluyendo la persistencia y la dirección de destino (Token)
            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(notification)
                    .setWebpushConfig(webpushConfig) // 🔥 Enlazamos el comportamiento persistente
                    .build();

            // 4. ¡Fuego! Enviamos el mensaje a través de los servidores de Google
            String response = FirebaseMessaging.getInstance().send(message);
            System.out.println("✅ Notificación Push persistente enviada con éxito. ID de respuesta: " + response);

        } catch (Exception e) {
            System.err.println("❌ Error crítico al enviar la notificación Push: " + e.getMessage());
        }
    }
}