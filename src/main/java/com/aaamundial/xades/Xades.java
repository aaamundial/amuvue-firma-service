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
    // escribe el XML de entrada en un fichero temporal
    Path tempXml = Files.createTempFile("in", ".xml");
    Files.write(tempXml, xmlContent.getBytes(StandardCharsets.UTF_8));

    Path tempOut = Files.createTempFile("out", ".xml");

    // construye el comando usando Java 8 para la firma
    String java8 = findJava8();
    String jar = findJar("TuHerramientaFirma.jar");  // ajusta el nombre real
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
   * Busca el JAR de firma en el working directory o subdirectorios.
   */
  private String findJar(String jarName) {
    for (var root : new File(".").listFiles(File::isDirectory)) {
      for (var file : root.listFiles()) {
        if (file.getName().equals(jarName)) {
          return file.getAbsolutePath();
        }
      }
    }
    // o bien lo buscas en otra carpeta fija...
    return null;
  }

  /**
   * Devuelve la ruta al binario de Java 8 si existe en Debian/Ubuntu,
   * si no, devuelve "java" (que será Java 17 para Spring Boot).
   */
  private String findJava8() {
    String java8Path = "/usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java";
    if (new File(java8Path).canExecute()) {
      return java8Path;
    }
    // fallback al java por defecto (17+)
    return "java";
  }
}
