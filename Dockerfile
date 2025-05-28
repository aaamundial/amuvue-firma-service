# Usa un JDK ligero
FROM eclipse-temurin:8-jdk-jammy

WORKDIR /app

# Copia el fat-jar
COPY target/firma-service-0.1.0.jar app.jar

# Puerto expuesto
EXPOSE 8080

# Arranca el servicio
ENTRYPOINT ["java","-Dfile.encoding=UTF-8","-jar","app.jar"]
