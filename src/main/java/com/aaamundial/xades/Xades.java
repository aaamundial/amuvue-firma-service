package com.aaamundial.xades;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Xades {

  /**
   * Envía al proceso externo tu herramienta de firma (por ejemplo otro JAR)
   * y devuelve el XML firmado como bytes.
   */
  public byte[] sign(String xmlContent, String p12Path, String password) throws Exception {
    Path tempXml = Files.createTempFile("in", ".xml");
    Files.write(tempXml, xmlContent.getBytes(StandardCharsets.UTF_8));

    Path tempOut = Files.createTempFile("out", ".xml");

    String java8 = findJava8();
    String jar = findJar("FirmaElectronica.jar");
    if (jar == null) {
      throw new IllegalStateException("No se encontró el JAR de firma");
    }

    List<String> cmd = new ArrayList<>();
    cmd.add(java8);
    cmd.add("-Dfile.encoding=UTF-8");
    cmd.add("-jar");
    cmd.add(jar);
    cmd.add(tempXml.toString());
    cmd.add(p12Path);
    cmd.add(password);
    cmd.add(tempOut.toString());

    ProcessBuilder pb = new ProcessBuilder(cmd).inheritIO();
    Process p = pb.start();
    if (p.waitFor() != 0) {
      throw new RuntimeException("Error al invocar la herramienta de firma");
    }

    byte[] signed = Files.readAllBytes(tempOut);
    Files.deleteIfExists(tempXml);
    Files.deleteIfExists(tempOut);
    return signed;
  }

  /**
   * Busca el JAR de firma en el working directory y subdirectorios.
   */
  private String findJar(String jarName) {
    File base = new File(".");
    File[] roots = base.listFiles(File::isDirectory);
    if (roots != null) {
      for (File root : roots) {
        File[] files = root.listFiles();
        if (files != null) {
          for (File file : files) {
            if (file.getName().equals(jarName)) {
              return file.getAbsolutePath();
            }
          }
        }
      }
    }
    return null;
  }

  /**
   * Devuelve la ruta al binario de Java 8 si existe en Debian/Ubuntu,
   * si no, devuelve "java" (el runtime por defecto, Java 17 para Spring Boot).
   */
  private String findJava8() {
    String java8Path = "/usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java";
    if (new File(java8Path).canExecute()) {
      return java8Path;
    }
    return "java";
  }
}
