# Dockerfile
 FROM eclipse-temurin:17-jdk-jammy
 RUN apt-get update && \
     apt-get install -y openjdk-8-jre-headless && \
     rm -rf /var/lib/apt/lists/*

 WORKDIR /app

 # Copia tu fat-jar de Spring Boot
 COPY target/firma-service-0.1.0.jar app.jar

 # Copia también el JAR que hace la firma
 COPY libs/FirmaElectronica.jar FirmaElectronica.jar

 EXPOSE 8080

 ENTRYPOINT ["bash","-c","java -Dserver.port=${PORT:-8080} -Dfile.encoding=UTF-8 -jar app.jar"]

