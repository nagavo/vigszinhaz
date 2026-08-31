#
# Build stage
#
FROM maven:3.9.12-eclipse-temurin-21-alpine AS build
COPY src /home/vigszinhaz/src
COPY pom.xml /home/vigszinhaz
RUN mvn -f /home/vigszinhaz/pom.xml clean package

#
# Package stage
#
FROM eclipse-temurin:25-jdk-ubi10-minimal
COPY --from=build /home/vigszinhaz/target/vigszinhaz-1.0.jar /usr/local/lib/vigszinhaz-1.0.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/usr/local/lib/vigszinhaz-1.0.jar"]
