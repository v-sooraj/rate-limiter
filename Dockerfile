# Stage 1: Build the application
FROM gradle:8.5-jdk17 AS build
WORKDIR /home/gradle/src
COPY --chown=gradle:gradle . .
RUN gradle jar --no-daemon

# Stage 2: Runtime image
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /home/gradle/src/build/libs/*.jar app.jar

# Optional: set SLF4J default log level to info
ENV JAVA_OPTS="-Dorg.slf4j.simpleLogger.defaultLogLevel=info"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
