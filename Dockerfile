# Use the official OpenJDK 17 image as the base image
FROM openjdk:17-jdk-slim-buster

# Set the working directory inside the container
WORKDIR /app

# Copy the Spring Boot jar file into the container
COPY target/dicom-receiver-1.0.0.jar /app/dicom-receiver.jar

# Expose the port your Spring Boot application runs on
EXPOSE 8080

# Command to run the Spring Boot application
ENTRYPOINT ["java","--add-opens=java.base/java.time=ALL-UNNAMED", "-jar", "/app/dicom-receiver.jar" ]