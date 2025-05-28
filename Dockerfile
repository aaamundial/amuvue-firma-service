# Dockerfila
FROM eclipse-temurin:8-jdk-jammy



WORKDIR /app

# 1) Tu fat-jar de Spring Boot
COPY target/firma-service-0.1.0.jar app.jar

# 2) Todo el directorio libs (contiene FirmaElectronica.jar + lib/*.jar)
COPY libs ./libs

EXPOSE 8080

# Arranca Spring Boot en Java 8

ENTRYPOINT ["java","-Dloader.path=libs/,libs/lib/","-Dserver.port=${PORT:-8080}","-Dfile.encoding=UTF-8","-jar","app.jar"]

