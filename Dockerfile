# Usa JDK 17 para Spring Boot
FROM eclipse-temurin:17-jdk-jammy

# Instala solo el runtime de Java 8 (bastará para firmar)
RUN apt-get update && \
    apt-get install -y openjdk-8-jre-headless && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copia tu fat-jar de Spring Boot 3.x
COPY target/firma-service-0.1.0.jar app.jar

# Puerto de Cloud Run (documentativo)
EXPOSE 8080

# Arranca Spring Boot con Java 17 (escucha en $PORT),
# y la clase Xades invocará más abajo explicitamente el binario de Java 8
ENTRYPOINT ["bash","-c","java -Dserver.port=${PORT:-8080} -Dfile.encoding=UTF-8 -jar app.jar"]

