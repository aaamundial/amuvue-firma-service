package com.aaamundial.firma;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Bean; // <<< CAMBIO: Nuevo import
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.IOException;

@Configuration
public class FirebaseConfig {

    // <<< CAMBIO: El método PostConstruct ahora es privado, es un detalle de implementación.
    @PostConstruct
    private void initialize() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            String projectId = System.getenv("GOOGLE_CLOUD_PROJECT");

            if (projectId == null || projectId.isBlank()) {
                System.err.println("ADVERTENCIA: La variable de entorno GOOGLE_CLOUD_PROJECT no está configurada.");
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.getApplicationDefault())
                        .build();
                FirebaseApp.initializeApp(options);
                System.out.println("FirebaseApp ha sido inicializado con credenciales por defecto (Project ID inferido).");
            } else {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.getApplicationDefault())
                        .setProjectId(projectId)
                        .build();
                FirebaseApp.initializeApp(options);
                System.out.println("FirebaseApp ha sido inicializado explícitamente para el proyecto: " + projectId);
            }
        }
    }

    // <<< CAMBIO: Añadimos este método.
    // Esto crea un "Bean" que otros componentes de Spring pueden inyectar.
    // Spring garantiza que @PostConstruct se ejecuta antes de que este @Bean esté disponible.
    @Bean
    public FirebaseApp firebaseApp() {
        return FirebaseApp.getInstance();
    }
}