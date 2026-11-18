# Etapa 1: construir con Maven y Java 21
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn -q clean package -DskipTests

# Etapa 2: ejecutar en Tomcat 10.1 con JDK 21
FROM tomcat:10.1-jdk21
COPY --from=build /app/target/cafeexpress.war /usr/local/tomcat/webapps/ROOT.war
EXPOSE 8080
