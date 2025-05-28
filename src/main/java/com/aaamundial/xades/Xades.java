package com.aaamundial.xades;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

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

    // 4) Comando para lanzar Java 8
    String java8 = findJava8();  
    List<String> cmd = new ArrayList<>();
    cmd.add(java8);
    cmd.add("-Dfile.encoding=UTF-8");
    cmd.add("-cp");
    cmd.add(cp);
    // La clase principal dentro de ese JAR (fqn) – ajusta si usas otro paquete.
    cmd.add("firmaelectronica.FirmaElectronica");
    cmd.add(tempXml.toString());
    cmd.add(p12Path);
    cmd.add(password);
    cmd.add(tempOut.toString());

    ProcessBuilder pb = new ProcessBuilder(cmd).inheritIO();
    Process p = pb.start();
    if (p.waitFor() != 0) {
      throw new RuntimeException("Error al invocar la herramienta de firma");
    }

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

  private String findJava8() {
    String java8Path = "/usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java";
    if (new File(java8Path).canExecute()) {
      return java8Path;
    }
    return "java";
  }
}
