// Archivo: src/main/java/com/aaamundial/firma/XadesSignerService.java
package com.aaamundial.firma;

// IMPORTS NECESARIOS PARA LOGS Y EL RESTO DEL CÓDIGO
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xades4j.production.*;
import xades4j.providers.impl.DirectKeyingDataProvider;
import xades4j.properties.DataObjectFormatProperty;
import xades4j.algorithms.EnvelopedSignatureTransform;
import xades4j.properties.DataObjectDesc;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

public class XadesSignerService {

    // 1. AÑADIR UN LOGGER ESTÁTICO
    private static final Logger log = LoggerFactory.getLogger(XadesSignerService.class);

    public byte[] sign(byte[] xmlBytes, CertificateData certData) throws Exception {

        // 2. LOGS AL INICIO DEL MÉTODO PARA VERIFICAR LOS DATOS RECIBIDOS
        log.info("+++++++++ INICIANDO PROCESO DE FIRMA +++++++++");
        if (certData == null) {
            log.error("¡ERROR CRÍTICO! El objeto CertificateData es nulo.");
            throw new IllegalArgumentException("CertificateData no puede ser nulo.");
        }
        
        String pwd = certData.password();
        byte[] p12Bytes = certData.p12Bytes();

        if (pwd == null || pwd.isEmpty()) {
            log.error("¡ERROR CRÍTICO! La contraseña recibida es nula o vacía.");
        } else {
            // Por seguridad, no logueamos la clave, pero sí su longitud.
            // Si la longitud es diferente a la que esperas, ya tienes una pista.
            log.info("Contraseña recibida. Longitud: {}", pwd.length());
        }

        if (p12Bytes == null || p12Bytes.length == 0) {
            log.error("¡ERROR CRÍTICO! El archivo P12 (bytes) es nulo o está vacío.");
            throw new IllegalArgumentException("Los bytes del certificado P12 no pueden ser nulos o vacíos.");
        } else {
            log.info("Bytes del P12 recibidos. Tamaño: {} bytes.", p12Bytes.length);
        }


        try {
            /* ---------- 1) Certificado y clave del PKCS#12 ---------- */
            KeyStore ks = KeyStore.getInstance("PKCS12");
            
            log.info("Cargando KeyStore... Este es el punto donde ocurre el error si la clave es incorrecta.");
            // La siguiente línea es la que lanza la excepción
            ks.load(new ByteArrayInputStream(p12Bytes), pwd.toCharArray());
            log.info("¡KeyStore cargado exitosamente! La contraseña era correcta.");

            
            String signingAlias = null;
            Enumeration<String> aliases = ks.aliases();
            while (aliases.hasMoreElements()) {
                String currentAlias = aliases.nextElement();
                // Se busca un alias que contenga "signing key", si no, se usará el primero que se encuentre.
                if (ks.isKeyEntry(currentAlias)) {
                    if (signingAlias == null) {
                        signingAlias = currentAlias; // Guardamos el primer alias válido
                    }
                    if (currentAlias.toLowerCase().contains("firma") || currentAlias.toLowerCase().contains("signing")) {
                        signingAlias = currentAlias; // Preferimos uno con un nombre explícito
                        break;
                    }
                }
            }
            
            if (signingAlias == null) {
                log.error("No se encontró ningún alias de clave privada en el archivo P12.");
                throw new KeyStoreException("No se encontró un alias de clave privada en el archivo P12.");
            }
            log.info("Usando el alias de firma: '{}'", signingAlias);


            X509Certificate cert = (X509Certificate) ks.getCertificate(signingAlias);
            PrivateKey key = (PrivateKey) ks.getKey(signingAlias, pwd.toCharArray());

            /* ---------- 2) Perfil XAdES ---------- */
            DirectKeyingDataProvider kdp = new DirectKeyingDataProvider(cert, key);
            
            XadesBesSigningProfile profile = new XadesBesSigningProfile(kdp);
            XadesSigner signer = profile.newSigner();

            /* ---------- 3) DOM del XML ---------- */
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true); 
            Document doc = dbf.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xmlBytes));
            
            Element rootElem = doc.getDocumentElement();
            rootElem.setIdAttribute("id", true);

            /* ---------- 4) Objeto firmado (enveloped) ---------- */
            DataObjectDesc obj = new DataObjectReference("#comprobante")
                    .withTransform(new EnvelopedSignatureTransform())
                    .withDataObjectFormat(new DataObjectFormatProperty("text/xml", "UTF-8"));
            
            SignedDataObjects signedDataObjects = new SignedDataObjects(obj);
            
            signer.sign(signedDataObjects, rootElem);
            log.info("Documento firmado exitosamente en memoria.");

            /* ---------- 5) DOM → bytes ---------- */
            return toBytes(doc);

        // 3. UN BLOQUE CATCH ESPECÍFICO PARA LOGUEAR EL ERROR EXACTO
        } catch (Exception e) {
            log.error("+++++++++ ERROR DURANTE EL PROCESO DE FIRMA +++++++++");
            log.error("Clase de la excepción: {}", e.getClass().getName());
            log.error("Mensaje de la excepción: {}", e.getMessage());
            // Este log es clave, te mostrará la traza del error originado aquí.
            log.error("Stack Trace:", e); 
            
            // Volvemos a lanzar la excepción para que el controlador la maneje como antes
            throw e; 
        }
    }

    private static byte[] toBytes(Document doc) throws TransformerException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Transformer tf = TransformerFactory.newInstance().newTransformer();
        tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        tf.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        tf.transform(new DOMSource(doc), new StreamResult(out));
        return out.toByteArray();
    }
}