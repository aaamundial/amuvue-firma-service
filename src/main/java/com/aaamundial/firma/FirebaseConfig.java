package com.aaamundial.firma;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Configuration;
import javax.annotation.PostConstruct;
import java.io.IOException;

@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void initialize() throws IOException {
        // Solo inicializa si no existe ya una app por defecto
        if (FirebaseApp.getApps().isEmpty()) {

            // Obtener el project ID de las variables de entorno de Cloud Run.
            // Esta es la forma más robusta de hacerlo en este entorno.
            String projectId = System.getenv("GOOGLE_CLOUD_PROJECT");

            // Si por alguna razón la variable no está, el SDK puede intentar adivinarlo,
            // pero es mejor ser explícito.
            if (projectId == null || projectId.isBlank()) {
                System.err.println("ADVERTENCIA: La variable de entorno GOOGLE_CLOUD_PROJECT no está configurada.");
                // Aún así, intentamos inicializar sin él, dejando que el SDK lo descubra.
                FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.getApplicationDefault())
                    .build();
                FirebaseApp.initializeApp(options);
                System.out.println("FirebaseApp ha sido inicializado con credenciales por defecto (Project ID inferido).");
            } else {
                // Esta es la ruta ideal: somos explícitos.
                FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.getApplicationDefault())
                    .setProjectId(projectId) // <-- ¡LA LÍNEA CLAVE Y MÁGICA!
                    .build();
                FirebaseApp.initializeApp(options);
                System.out.println("FirebaseApp ha sido inicializado explícitamente para el proyecto: " + projectId);
            }
        }
    }
}