FROM sbtscala/scala-sbt:eclipse-temurin-jammy-17.0.9_9_1.9.7_2.13.12

WORKDIR /app

# Create Spark events directory
RUN mkdir -p /tmp/spark-events

# Copy only build files first to cache dependencies
COPY build.sbt .
COPY project/build.properties project/

# Download dependencies
RUN sbt update

# Copy source code
COPY . .

# Compile
RUN sbt compile

# Default command (can be overridden)
CMD ["sbt", "run"]
