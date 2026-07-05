# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime environment
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

# Install Python 3, gcc/g++ compilers, and symlink python for code execution support
RUN apk add --no-cache python3 g++ && ln -sf python3 /usr/bin/python

COPY --from=build /app/target/*.jar app.jar
RUN mkdir -p temp_runs && chmod 777 temp_runs

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
