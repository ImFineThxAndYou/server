# Stage 1: Build with Gradle Wrapper
FROM openjdk:17-jdk-slim AS build
WORKDIR /app

# Copy gradle wrapper and build files
COPY gradlew .
COPY gradle ./gradle
COPY build.gradle .
COPY settings.gradle .

# Make gradlew executable
RUN chmod +x ./gradlew

# Copy source code
COPY src ./src

# Build the application, skipping tests
RUN ./gradlew build --no-daemon -x test

# Stage 2: Create the final image
FROM openjdk:17-jdk-slim
WORKDIR /app

# install curl for health check
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*
# Copy the built jar from the build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Expose port and run the application
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar","--server.address=0.0.0.0"]
