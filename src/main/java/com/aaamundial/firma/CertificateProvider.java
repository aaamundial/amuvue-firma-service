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
                .expireAfterWrite(1, TimeUnit.HOURS)
                .build(new CacheLoader<>() {
                    @Override
                    public CertificateData load(CertificateIdentifier id) throws Exception {
                        return fetchCertificateFromCloud(id.uid(), id.empresaId());
                    }
                });
    }

    // El resto de la clase no cambia...
    public CertificateData getCertificate(String uid, String empresaId) throws ExecutionException {
        return certificateCache.get(new CertificateIdentifier(uid, empresaId));
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