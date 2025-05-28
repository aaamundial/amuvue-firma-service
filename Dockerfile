# Dockerfila
FROM eclipse-temurin:17-jdk-jammy

# Instala sólo el runtime de Java 8 para la firma
RUN apt-get update && \
    apt-get install -y openjdk-8-jre-headless && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

# 1) Tu fat-jar de Spring Boot
COPY target/firma-service-0.1.0.jar app.jar

# 2) Todo el directorio libs (contiene FirmaElectronica.jar + lib/*.jar)
COPY libs ./libs

EXPOSE 8080

# Arranca Spring Boot en Java 17 escuchando en $PORT
ENTRYPOINT ["bash","-c","java -Dloader.path=libs/,libs/lib/ -Dserver.port=${PORT:-8080} -Dfile.encoding=UTF-8 -jar app.jar"]
