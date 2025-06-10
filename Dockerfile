# Dockerfila
FROM eclipse-temurin:17-jdk-jammy   

WORKDIR /app

# 1) Tu fat-jar de Spring Boot
COPY target/firma-service-0.1.0.jar app.jar



EXPOSE 8080

# Arranca Spring Boot en Java 17 escuchando en $PORT
ENTRYPOINT ["java","-Dserver.port=${PORT:-8080}","-Dfile.encoding=UTF-8","-jar","app.jar"]
