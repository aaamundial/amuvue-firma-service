package com.aaamundial.xades;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Xades {
  /**  
   * Envía al proceso externo tu herramienta de firma (por ejemplo otro JAR)
   * y devuelve el XML firmado como bytes.
   */
  public byte[] sign(String xmlContent, String p12Path, String password) throws Exception {
    // crea archivos temporales
    Path tempXml = Files.createTempFile("in", ".xml");
    Files.writeString(tempXml, xmlContent, StandardCharsets.UTF_8);

    Path tempOut = Files.createTempFile("out", ".xml");

    // invoca tu JAR de firma
    List<String> cmd = new ArrayList<>();
    cmd.add("java");
    cmd.add("-Dfile.encoding=UTF-8");
    cmd.add("-jar");
    cmd.add("TuHerramientaFirma.jar");       // el JAR real que hace la firma
    cmd.add(tempXml.toString());
    cmd.add(p12Path);
    cmd.add(password);
    cmd.add(tempOut.toString());

    ProcessBuilder pb = new ProcessBuilder(cmd);
    pb.inheritIO();
    Process p = pb.start();
    if (p.waitFor() != 0) {
      throw new RuntimeException("Error al invocar la herramienta de firma");
    }

    byte[] signed = Files.readAllBytes(tempOut);
    Files.deleteIfExists(tempXml);
    Files.deleteIfExists(tempOut);
    return signed;
  }
}
