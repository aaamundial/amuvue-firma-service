package com.aaamundial.firma;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.firebase.FirebaseApp;
import org.springframework.stereotype.Service;

import java.util.Map;

import java.util.concurrent.ConcurrentHashMap;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

// Los records no cambian
record CertificateData(byte[] p12Bytes, String password) {}
record CertificateIdentifier(String uid, String empresaId) {}

@Service
public class CertificateProvider {

    private final Storage storage;
    private final Firestore db;
    private final LoadingCache<CertificateIdentifier, CertificateData> certificateCache;
    private final LoadingCache<CertificateIdentifier, Long> timestampCache;
    private final Map<CertificateIdentifier, Long> localTimestampCache = new ConcurrentHashMap<>();


    // <<< CAMBIO: El constructor ahora RECIBE el bean de FirebaseApp.
    // Esto crea una dependencia explícita y soluciona el problema de orden de arranque.
    public CertificateProvider(FirebaseApp firebaseApp) {
        this.storage = StorageOptions.newBuilder().build().getService();

        // <<< CAMBIO: Usamos el objeto 'firebaseApp' inyectado, no la llamada estática.
        String projectId = firebaseApp.getOptions().getProjectId();
        
        if (projectId == null || projectId.isBlank()) {
            throw new IllegalStateException("No se pudo determinar el ID del proyecto de Google Cloud desde FirebaseApp.");
        }

        FirestoreOptions firestoreOptions = FirestoreOptions.newBuilder()
                .setProjectId(projectId)
                .setDatabaseId("amuvue")
                .build();
        this.db = firestoreOptions.getService();

        this.certificateCache = CacheBuilder.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(24, TimeUnit.HOURS)
                .build(new CacheLoader<>() {
                    @Override
                    public CertificateData load(CertificateIdentifier id) throws Exception {
                        return fetchCertificateFromCloud(id.uid(), id.empresaId());
                    }
                });
        this.timestampCache = CacheBuilder.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(30, TimeUnit.SECONDS)  // Se verifica cada 30 segundos
                .build(new CacheLoader<>() {
                    @Override
                    public Long load(CertificateIdentifier id) throws Exception {
                        return fetchPasswordTimestamp(id.uid(), id.empresaId());
                    }
                });
    }

    // El resto de la clase no cambia...
    public CertificateData getCertificate(String uid, String empresaId) throws ExecutionException {
        CertificateIdentifier id = new CertificateIdentifier(uid, empresaId);
        
        // Verificar si la contraseña ha cambiado
        Long currentTimestamp = timestampCache.get(id);
        Long cachedTimestamp = getCachedTimestamp(id);
        
        if (cachedTimestamp == null || !currentTimestamp.equals(cachedTimestamp)) {
            System.out.println("Contraseña actualizada detectada, invalidando caché para: " + empresaId);
            certificateCache.invalidate(id);
            setCachedTimestamp(id, currentTimestamp);
        }
        
        return certificateCache.get(id);
    }
    
    private Long fetchPasswordTimestamp(String uid, String empresaId) throws Exception {
        DocumentReference docRef = db.collection("users").document(uid)
                                     .collection("empresas").document(empresaId);
        
        ApiFuture<DocumentSnapshot> future = docRef.get();
        DocumentSnapshot document = future.get();
        
        if (!document.exists()) {
            return 0L;
        }
        
        // Obtener el timestamp de cuando se actualizó la contraseña
        Object timestampObj = document.get("passwordUpdatedAt");
        if (timestampObj instanceof Number) {
            return ((Number) timestampObj).longValue();
        }
        
        // Si no existe el campo, usar la fecha de modificación del documento
        if (document.getUpdateTime() != null) {
            return document.getUpdateTime().getSeconds();
        }
        
        return 0L;
    }
    
    private Long getCachedTimestamp(CertificateIdentifier id) {
        return localTimestampCache.get(id);
    }
    
    private void setCachedTimestamp(CertificateIdentifier id, Long timestamp) {
        localTimestampCache.put(id, timestamp);
    }

    private CertificateData fetchCertificateFromCloud(String uid, String empresaId) throws Exception {
        System.out.println("CACHE MISS: Obteniendo certificado para uid: " + uid + ", empresaId: " + empresaId);

        DocumentReference docRef = db.collection("users").document(uid)
                                     .collection("empresas").document(empresaId);

        ApiFuture<DocumentSnapshot> future = docRef.get();
        DocumentSnapshot document = future.get();

        if (!document.exists()) {
            throw new RuntimeException("No se encontró la configuración para la empresa: " + empresaId);
        }

        String p12GcsPath = document.getString("p12_gcs_path");
        String password = document.getString("password");

        if (p12GcsPath == null || p12GcsPath.isBlank() || password == null || password.isBlank()) {
            throw new RuntimeException("p12_gcs_path o password no encontrados en Firestore para la empresa: " + empresaId);
        }


        // Definimos el nombre del bucket de forma fija.
        final String bucketName = "amuoctubre_cloudbuild";
        
        // La ruta del objeto es la cadena completa que viene de Firestore.
        final String objectName = p12GcsPath; 

        BlobId blobId = BlobId.of(bucketName, objectName);
        byte[] p12Bytes = storage.readAllBytes(blobId);

        return new CertificateData(p12Bytes, password);
    }
}