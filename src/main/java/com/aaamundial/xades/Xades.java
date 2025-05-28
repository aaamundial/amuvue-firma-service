//C:\amuvue-firma-service\src\main\java\com\aaamundial\xades\Xades.java
package com.aaamundial.xades;

import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.ByteArrayOutputStream;

import java.nio.charset.StandardCharsets;
import firmaelectronica.FirmaElectronica;

public class Xades {


  public byte[] sign(String xmlContent, String p12Path, String password) throws Exception {
    // 1) Temp files
    Path tempXml = Files.createTempFile("in", ".xml");
    Files.write(tempXml, xmlContent.getBytes(StandardCharsets.UTF_8));
    Path tempOut = Files.createTempFile("out", ".xml");

    // 2) Invoca en-proceso el main de FirmaElectronica (sin nueva JVM)
    //    Conserva temp-files para I/O de la librería
    // Guarda flujos originales
    java.io.InputStream oldIn = System.in;
    PrintStream oldOut = System.out;
    try {
      PrintStream ps = new PrintStream(new ByteArrayOutputStream(), true, "UTF-8");
      System.setOut(ps);
      Class<?> cls = Class.forName("firmaelectronica.FirmaElectronica");
      Method main = cls.getMethod("main", String[].class);
      String[] args = new String[]{
        tempXml.toString(),
        p12Path,
        password,
        tempOut.toString()
      };
      main.invoke(null, (Object) args);
    } finally {
      // Restaura flujos
      System.setOut(oldOut);
      System.setIn(oldIn);
    }

    // 3) Leer resultado desde tempOut
    byte[] signed = Files.readAllBytes(tempOut);
    Files.deleteIfExists(tempXml);
    Files.deleteIfExists(tempOut);
    return signed;
  }


}
