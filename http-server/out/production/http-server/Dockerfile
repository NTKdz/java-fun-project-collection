# Use an official OpenJDK runtime as the base image
FROM openjdk:17-jdk-slim

# Set the working directory inside the container
WORKDIR /app

# Copy the Java source code to the container
COPY SimpleHttpServer.java /app

# Compile the Java source code
RUN javac SimpleHttpServer.java

# Expose the port the server will run on
EXPOSE 8080

# Command to run the application
CMD ["java", "SimpleHttpServer"]
