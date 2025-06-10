package com.aaamundial.firma;

import xades4j.production.*;
import xades4j.providers.impl.DirectKeyingDataProvider;
import xades4j.utils.*;

import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import java.security.*;
import java.security.cert.X509Certificate;

public class XadesSignerService {

    public byte[] sign(byte[] xmlBytes, byte[] p12Bytes, String pwd) throws Exception {

        // 1) PKCS#12 en memoria
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(new java.io.ByteArrayInputStream(p12Bytes), pwd.toCharArray());
        String alias = ks.aliases().nextElement();
        PrivateKey pk = (PrivateKey) ks.getKey(alias, pwd.toCharArray());
        X509Certificate cert = (X509Certificate) ks.getCertificate(alias);

        // 2) Perfil XAdES-BES SHA-256
        DirectKeyingDataProvider kdp = new DirectKeyingDataProvider(pk, cert);
        XadesSigner signer = new XadesBesSigningProfile(kdp)
                .withSignatureMethod(SignatureMethod.RSA_SHA256)
                .withDigestMethod(DigestMethod.SHA256)
                .withCanonicalizationMethod(CanonicalizationMethod.C14N_OMIT_COMMENTS)
                .newSigner();

        // 3) Parse XML
        Document doc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(new java.io.ByteArrayInputStream(xmlBytes));

        // 4) Enveloped sobre el nodo raíz
        signer.sign(doc, new Enveloped(doc.getDocumentElement()));

        return DOMHelper.domToByteArray(doc, true);
    }
}
