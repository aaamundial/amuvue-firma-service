//C:\amuvue-firma-service\src\main\java\com\aaamundial\firma\SigningController.java
package com.aaamundial.firma;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class SigningController {

  @PostMapping(path = "/sign", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<byte[]> sign(
      @RequestPart("xml") MultipartFile xmlFile,
      @RequestPart("p12") MultipartFile p12File,
      @RequestParam("password") String pwd
  ) throws Exception {

    byte[] xml = xmlFile.getBytes();
    byte[] p12 = p12File.getBytes();

    // guarda el P12 en un temp file para la librería Xades
    java.nio.file.Path tempP12 = java.nio.file.Files.createTempFile("firma", ".p12");
    java.nio.file.Files.write(tempP12, p12);

    // Invoca tu clase Xades del JAR
    com.aaamundial.xades.Xades xades = new com.aaamundial.xades.Xades();
    byte[] signed = xades.sign(new String(xml, "UTF-8"),
                               tempP12.toString(),
                               pwd);

    // limpia
    java.nio.file.Files.delete(tempP12);

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_VALUE)
        .body(signed);
  }
}
