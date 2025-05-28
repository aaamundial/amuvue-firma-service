# Usa un JDK ligero
FROM eclipse-temurin:8-jdk-jammy

WORKDIR /app

# Copia el fat-jar
COPY target/firma-service-0.1.0.jar app.jar

# Puerto expuesto (no estrictamente necesario, pero sirve de documentación)
EXPOSE 8080

# Arranca el servicio en el puerto que Cloud Run inyecta en $PORT
ENTRYPOINT ["bash","-c","java -Dserver.port=${PORT:-8080} -Dfile.encoding=UTF-8 -jar app.jar"]
