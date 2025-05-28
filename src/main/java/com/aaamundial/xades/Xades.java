package com.aaamundial.xades;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Xades {

  public byte[] sign(String xmlContent, String p12Path, String password) throws Exception {
    Path tempXml = Files.createTempFile("in", ".xml");
    Files.write(tempXml, xmlContent.getBytes(StandardCharsets.UTF_8));
    Path tempOut = Files.createTempFile("out", ".xml");

    String java8 = findJava8();
    // Nombre real del JAR que copiaste en Dockerfile
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

  private String findJar(String jarName) {
    File base = new File(".");
    // 1) Busca en el directorio base
    File[] files = base.listFiles();
    if (files != null) {
      for (File f : files) {
        if (f.isFile() && f.getName().equals(jarName)) {
          return f.getAbsolutePath();
        }
      }
    }
    // 2) Luego busca en subdirectorios
    File[] dirs = base.listFiles(File::isDirectory);
    if (dirs != null) {
      for (File dir : dirs) {
        File[] nested = dir.listFiles();
        if (nested != null) {
          for (File f : nested) {
            if (f.isFile() && f.getName().equals(jarName)) {
              return f.getAbsolutePath();
            }
          }
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
