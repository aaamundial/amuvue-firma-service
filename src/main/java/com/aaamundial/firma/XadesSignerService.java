// Archivo: src/main/java/com/aaamundial/firma/XadesSignerService.java
package com.aaamundial.firma;

import xades4j.production.*;
import xades4j.providers.impl.DirectKeyingDataProvider;
import xades4j.properties.DataObjectFormatProperty;
import xades4j.algorithms.EnvelopedSignatureTransform;
import xades4j.properties.DataObjectDesc;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

public class XadesSignerService {

    public byte[] sign(byte[] xmlBytes, byte[] p12Bytes, String pwd) throws Exception {

        /* ---------- 1) Certificado y clave del PKCS#12 ---------- */
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(new java.io.ByteArrayInputStream(p12Bytes), pwd.toCharArray());
        
        String signingAlias = null;
        Enumeration<String> aliases = ks.aliases();
        while (aliases.hasMoreElements()) {
            String currentAlias = aliases.nextElement();
            if (currentAlias.toLowerCase().contains("signing key")) {
                signingAlias = currentAlias;
                break;
            }
        }
        
        if (signingAlias == null) {
            throw new KeyStoreException("No se encontró un alias de firma ('signing key') en el archivo P12.");
        }

        X509Certificate cert = (X509Certificate) ks.getCertificate(signingAlias);
        PrivateKey key = (PrivateKey) ks.getKey(signingAlias, pwd.toCharArray());

        /* ---------- 2) Perfil XAdES ---------- */
        // Para el SRI, a menudo se requiere RSA-SHA1, aunque SHA256 es más moderno y seguro.
        // Si SHA256 falla, prueba cambiando a XadesBesSigningProfile.SHA1_RSA
        XadesBesSigningProfile profile = new XadesBesSigningProfile(kdp);
        XadesSigner signer = profile.newSigner();

        /* ---------- 3) DOM del XML ---------- */
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true); 
        Document doc = dbf.newDocumentBuilder()
                .parse(new java.io.ByteArrayInputStream(xmlBytes));
        
        // Es crucial que el elemento raíz a firmar tenga el atributo 'Id' (no 'id' en minúsculas)
        // en el DOM para que la referencia funcione.
        Element rootElem = doc.getDocumentElement();
        rootElem.setIdAttribute("id", true);

        /* ---------- 4) Objeto firmado (enveloped) ---------- */
        // La referencia DEBE apuntar al ID del nodo 'comprobante'
        DataObjectDesc obj = new DataObjectReference("#comprobante")
                .withTransform(new EnvelopedSignatureTransform())
                .withDataObjectFormat(new DataObjectFormatProperty("text/xml", "UTF-8")); // Especificar codificación
        
        SignedDataObjects signedDataObjects = new SignedDataObjects(obj);
        
        // La firma se inserta como último hijo del elemento raíz
        signer.sign(signedDataObjects, rootElem);

        /* ---------- 5) DOM → bytes ---------- */
        return toBytes(doc);
    }

    /* Utilidad: convierte DOM a UTF-8 */
    private static byte[] toBytes(Document doc) throws TransformerException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Transformer tf = TransformerFactory.newInstance().newTransformer();
        tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        // Opcional: para que el XML de salida no tenga la declaración <?xml ...?>
        // tf.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        tf.transform(new DOMSource(doc), new StreamResult(out));
        return out.toByteArray();
    }
}