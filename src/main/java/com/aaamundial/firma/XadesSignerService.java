// Archivo: src/main/java/com/aaamundial/firma/XadesSignerService.java
package com.aaamundial.firma;

import xades4j.production.*;
import xades4j.providers.impl.DirectKeyingDataProvider;
import xades4j.properties.DataObjectFormatProperty;
import xades4j.algorithms.EnvelopedSignatureTransform;
// ¡IMPORTAMOS LA CLASE QUE VAMOS A USAR AHORA!
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

public class XadesSignerService {

    public byte[] sign(byte[] xmlBytes, byte[] p12Bytes, String pwd) throws Exception {

        /* ---------- 1) Certificado y clave del PKCS#12 ---------- */
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(new java.io.ByteArrayInputStream(p12Bytes), pwd.toCharArray());
        String alias = ks.aliases().nextElement();
        X509Certificate cert = (X509Certificate) ks.getCertificate(alias);
        PrivateKey      key  = (PrivateKey) ks.getKey(alias, pwd.toCharArray());

        /* ---------- 2) Perfil XAdES ---------- */
        DirectKeyingDataProvider kdp = new DirectKeyingDataProvider(cert, key);
        XadesSigner signer = new XadesBesSigningProfile(kdp).newSigner();

        /* ---------- 3) DOM del XML ---------- */
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true); 
        Document doc = dbf.newDocumentBuilder()
                .parse(new java.io.ByteArrayInputStream(xmlBytes));

        /* ---------- 4) Objeto firmado (enveloped) ---------- */
        // AQUÍ ESTÁ LA CORRECCIÓN: La variable 'obj' ahora es del tipo correcto 'DataObjectDesc'
        DataObjectDesc obj = new DataObjectReference("")
                .withTransform(new EnvelopedSignatureTransform())
                .withDataObjectFormat(new DataObjectFormatProperty("text/xml"));
        
        // El constructor de SignedDataObjects puede aceptar un DataObjectDesc
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