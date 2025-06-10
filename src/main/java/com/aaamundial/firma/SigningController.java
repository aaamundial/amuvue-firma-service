package com.aaamundial.firma;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// DTO (Data Transfer Object) para el request JSON
record SignRequest(String xmlContent, String uid, String empresaId) {}

@RestController
public class SigningController {

    private final CertificateProvider certificateProvider;

    // Inyecta el proveedor de certificados vía constructor
    public SigningController(CertificateProvider certificateProvider) {
        this.certificateProvider = certificateProvider;
    }
    
    @PostMapping(path = "/sign", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> sign(@RequestBody SignRequest request) {
        try {
            // Validar que los datos necesarios llegaron
            if (request.uid() == null || request.uid().isBlank() ||
                request.empresaId() == null || request.empresaId().isBlank()) {
                return ResponseEntity.badRequest().body("uid y empresaId son obligatorios".getBytes());
            }

            // Obtener los datos del certificado usando el proveedor
            CertificateData certData = certificateProvider.getCertificate(request.uid(), request.empresaId());

            // Firmar el XML
            XadesSignerService signer = new XadesSignerService();
            byte[] signed = signer.sign(request.xmlContent().getBytes("UTF-8"), certData);

            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(signed);

        } catch (Exception e) {
            e.printStackTrace(); // Loguear el error es vital para depuración
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(e.getMessage().getBytes());
        }
    }
}