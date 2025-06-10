//C:\amuvue-firma-service\src\main\java\com\aaamundial\firma\SigningController.java
package com.aaamundial.firma;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.aaamundial.firma.XadesSignerService;
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

    XadesSignerService signer = new XadesSignerService();
    byte[] signed = signer.sign(xml, p12, pwd);

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_VALUE)
        .body(signed);
  }
}
