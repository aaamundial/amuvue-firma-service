package com.aaamundial.firma;

import xades4j.production.*;
import xades4j.providers.impl.DirectKeyingDataProvider;
import xades4j.utils.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.security.*;
import java.security.cert.X509Certificate;
import java.io.ByteArrayOutputStream;

public class XadesSignerService {

    public byte[] sign(byte[] xmlBytes, byte[] p12Bytes, String pwd) throws Exception {

        // 1) PKCS#12 en memoria
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(new java.io.ByteArrayInputStream(p12Bytes), pwd.toCharArray());
        String alias = ks.aliases().nextElement();
        X509Certificate cert = (X509Certificate) ks.getCertificate(alias);
        PrivateKey pk       = (PrivateKey) ks.getKey(alias, pwd.toCharArray());

        // 2) Perfil XAdES-BES SHA-256
        DirectKeyingDataProvider kdp = new DirectKeyingDataProvider(cert, pk);
        XadesSigner signer = new XadesBesSigningProfile(kdp)
                .withSignatureMethod(SignatureMethod.RSA_SHA256)
                .withDigestMethod(DigestMethod.SHA256)
                .withCanonicalizationMethod(CanonicalizationMethod.C14N_OMIT_COMMENTS)
                .newSigner();

        // 3) Parse XML
        Document doc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(new java.io.ByteArrayInputStream(xmlBytes));

        // Objeto firmado → “enveloped” sobre nodo raíz
        DataObjectDesc obj = new Enveloped(documentRoot(doc));
        signer.sign(new SignedDataObjects(obj), doc.getDocumentElement());

        return toBytes(doc);

    }

    /** nodo raíz */
    private static Element documentRoot(Document doc) {
        return doc.getDocumentElement();
    }

    /** Convierte el DOM a bytes UTF-8 */
    private static byte[] toBytes(Document doc) throws TransformerException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Transformer tf = TransformerFactory.newInstance().newTransformer();
        tf.transform(new DOMSource(doc), new StreamResult(out));
        return out.toByteArray();
    }
}
