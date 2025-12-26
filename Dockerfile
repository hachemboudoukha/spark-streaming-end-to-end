# Build Stage
FROM sbtscala/scala-sbt:eclipse-temurin-jammy-17.0.9_9_1.9.7_2.13.12 AS build
WORKDIR /app
COPY . .
RUN sbt assembly

# Runtime Stage
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/scala-2.13/spark_streaming-assembly-0.1.0.jar app.jar
COPY config/ config/
COPY data/ data/

# Entrypoint will be overridden by docker-compose commands
CMD ["java", "-jar", "app.jar"]
