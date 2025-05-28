//C:\amuvue-firma-service\src\main\java\com\aaamundial\xades\Xades.java
package com.aaamundial.xades;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.Method;

public class Xades {

  public byte[] sign(String xmlContent, String p12Path, String password) throws Exception {
    // 1) Temp files
    Path tempXml = Files.createTempFile("in", ".xml");
    Files.write(tempXml, xmlContent.getBytes(StandardCharsets.UTF_8));
    Path tempOut = Files.createTempFile("out", ".xml");

    // 2) Encuentra el JAR principal
    String jar = findJar("FirmaElectronica.jar");
    if (jar == null) {
      throw new IllegalStateException("No se encontró el JAR de firma");
    }

    // 3) Construye el classpath: JAR principal + todas las deps en lib/
    File jarFile = new File(jar);
    String parent = jarFile.getParent(); // debería ser ".../app/libs"
    // En Java >=6 el wildcard en classpath funciona: dir/* incluye todos los jars
    String cp = jar + File.pathSeparator + parent + "/lib/*";

    // 4) Invocar directamente el main de FirmaElectronica en esta JVM
    Class<?> cli = Class.forName("firmaelectronica.FirmaElectronica");
    Method main = cli.getMethod("main", String[].class);
    String[] args = new String[]{
      tempXml.toString(),
      p12Path,
      password,
      tempOut.toString()
    };
    main.invoke(null, (Object) args);

    // 5) Leer resultado
    byte[] signed = Files.readAllBytes(tempOut);
    Files.deleteIfExists(tempXml);
    Files.deleteIfExists(tempOut);
    return signed;
  }

  private String findJar(String jarName) {
    // Busca en ./libs (directorio copiado)
    File libs = new File("libs");
    File[] files = libs.listFiles();
    if (files != null) {
      for (File f : files) {
        if (f.isFile() && f.getName().equals(jarName)) {
          return f.getAbsolutePath();
        }
      }
    }
    return null;
  }


}
