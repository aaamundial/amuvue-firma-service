package com.aaamundial.firma;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions; // <<< CAMBIO: Nuevo import
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.firebase.FirebaseApp; // <<< CAMBIO: Nuevo import
// import com.google.firebase.cloud.FirestoreClient; // <<< CAMBIO: Eliminamos el import antiguo
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

// Objeto para guardar los datos del certificado
record CertificateData(byte[] p12Bytes, String password) {}
// Objeto para usar como clave en la caché
record CertificateIdentifier(String uid, String empresaId) {}

@Service
public class CertificateProvider {

    private final Storage storage;
    private final Firestore db; // <<< CAMBIO: El objeto Firestore ahora es un campo de la clase
    private final LoadingCache<CertificateIdentifier, CertificateData> certificateCache;

    // <<< CAMBIO: Hemos añadido un constructor para inicializar los servicios correctamente
    public CertificateProvider() {
        // Inicializamos Storage
        this.storage = StorageOptions.newBuilder().build().getService();

        // Obtenemos el ID del proyecto que FirebaseConfig ya inicializó
        String projectId = FirebaseApp.getInstance().getOptions().getProjectId();
        if (projectId == null || projectId.isBlank()) {
             // Fallback por si FirebaseApp no lo tiene, aunque no debería pasar.
            projectId = System.getenv("GOOGLE_CLOUD_PROJECT");
        }
        if (projectId == null || projectId.isBlank()) {
            throw new IllegalStateException("No se pudo determinar el ID del proyecto de Google Cloud.");
        }

        // Creamos una instancia de Firestore apuntando a la base de datos "amuvue"
        FirestoreOptions firestoreOptions = FirestoreOptions.newBuilder()
                .setProjectId(projectId)
                .setDatabaseId("amuvue") // ¡AQUÍ ESTÁ LA MAGIA!
                .build();
        this.db = firestoreOptions.getService();

        // Inicializamos la caché. Se debe hacer en el constructor para que
        // el CacheLoader pueda acceder al campo 'this.db' que acabamos de crear.
        this.certificateCache = CacheBuilder.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build(new CacheLoader<>() {
                @Override
                public CertificateData load(CertificateIdentifier id) throws Exception {
                    // Ahora esta llamada usa la instancia 'db' correcta
                    return fetchCertificateFromCloud(id.uid(), id.empresaId());
                }
            });
    }

    public CertificateData getCertificate(String uid, String empresaId) throws ExecutionException {
        return certificateCache.get(new CertificateIdentifier(uid, empresaId));
    }
    
    private CertificateData fetchCertificateFromCloud(String uid, String empresaId) throws Exception {
        System.out.println("CACHE MISS: Obteniendo certificado para uid: " + uid + ", empresaId: " + empresaId);

        // <<< CAMBIO: Ya no creamos la instancia de 'db' aquí, usamos la del campo de la clase.
        // Firestore db = FirestoreClient.getFirestore(); // ESTA LÍNEA SE ELIMINA

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

        String[] parts = p12GcsPath.split("/", 2);
        if (parts.length < 2) {
             throw new RuntimeException("Formato de p12_gcs_path inválido: " + p12GcsPath);
        }
        String bucketName = parts[0];
        String objectName = parts[1];

        BlobId blobId = BlobId.of(bucketName, objectName);
        byte[] p12Bytes = storage.readAllBytes(blobId);

        return new CertificateData(p12Bytes, password);
    }
}