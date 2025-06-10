// Archivo: src/main/java/com/aaamundial/firma/XadesSignerService.java
package com.aaamundial.firma;

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
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Enumeration; // Necesaria para iterar sobre los alias

public class XadesSignerService {

    public byte[] sign(byte[] xmlBytes, byte[] p12Bytes, String pwd) throws Exception {

        /* ---------- 1) Certificado y clave del PKCS#12 (CON BÚSQUEDA DE ALIAS) ---------- */
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(new java.io.ByteArrayInputStream(p12Bytes), pwd.toCharArray());
        
        String signingAlias = null;
        Enumeration<String> aliases = ks.aliases();

        while (aliases.hasMoreElements()) {
            String currentAlias = aliases.nextElement();
            // Buscamos el alias que es para firmar ("signing key")
            if (currentAlias.toLowerCase().contains("signing key")) {
                signingAlias = currentAlias;
                break;
            }
        }

        if (signingAlias == null) {
            // Si no lo encontramos, como plan B, tomamos el primero que tenga clave privada
            aliases = ks.aliases();
            while (aliases.hasMoreElements()) {
                String currentAlias = aliases.nextElement();
                if (ks.isKeyEntry(currentAlias)) {
                    signingAlias = currentAlias;
                    break;
                }
            }
        }
        
        if (signingAlias == null) {
            throw new KeyStoreException("No se encontró un alias válido para firmar en el archivo P12.");
        }

        X509Certificate cert = (X509Certificate) ks.getCertificate(signingAlias);
        PrivateKey key = (PrivateKey) ks.getKey(signingAlias, pwd.toCharArray());

        /* ---------- 2) Perfil XAdES ---------- */
        DirectKeyingDataProvider kdp = new DirectKeyingDataProvider(cert, key);
        XadesSigner signer = new XadesBesSigningProfile(kdp).newSigner();

        /* ---------- 3) DOM del XML ---------- */
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true); 
        Document doc = dbf.newDocumentBuilder()
                .parse(new java.io.ByteArrayInputStream(xmlBytes));

        /* ---------- 4) Objeto firmado (enveloped) ---------- */
        DataObjectDesc obj = new DataObjectReference("")
                .withTransform(new EnvelopedSignatureTransform())
                .withDataObjectFormat(new DataObjectFormatProperty("text/xml"));
        
        SignedDataObjects signedDataObjects = new SignedDataObjects(obj);
        
        signer.sign(signedDataObjects, doc.getDocumentElement());

        /* ---------- 5) DOM → bytes ---------- */
        return toBytes(doc);
    }

    /* Utilidad: convierte DOM a UTF-8 */
    private static byte[] toBytes(Document doc) throws TransformerException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Transformer tf = TransformerFactory.newInstance().newTransformer();
        tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        tf.transform(new DOMSource(doc), new StreamResult(out));
        return out.toByteArray();
    }
}