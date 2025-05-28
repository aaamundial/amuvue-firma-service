//C:\amuvue-firma-service\src\main\java\com\aaamundial\xades\Xades.java
package com.aaamundial.xades;


import java.nio.charset.StandardCharsets;
import firmaelectronica.FirmaElectronica;

public class Xades {

  public byte[] sign(String xmlContent, String p12Path, String password) throws Exception {
    // Llamada directa al API Java8 de FirmaElectronica (evita ProcessBuilder y temp files)
    byte[] xmlBytes = xmlContent.getBytes(StandardCharsets.UTF_8);
    byte[] p12Bytes = java.nio.file.Files.readAllBytes(java.nio.file.Path.of(p12Path));
    return FirmaElectronica.sign(xmlBytes, p12Bytes, password);
  }


}
