# Use a base image with Java Runtime
# Using eclipse-temurin for a lightweight, official JRE
FROM eclipse-temurin:17-jre-jammy

# Set a working directory
WORKDIR /app

# Argument to accept the JAR file name
ARG JAR_FILE=target/*.jar

# Copy the fat JAR file from the target directory into the container
COPY ${JAR_FILE} app.jar

# Expose the port that the application will run on
# This is informational, the port is set in application.properties
# EXPOSE 8080

# Command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
