# 1. Build Stage
FROM maven:3.8.5-openjdk-17 AS build
# Set the working directory so we know exactly where we are
WORKDIR /app
COPY . .
# Run the build inside /app
RUN mvn clean package -DskipTests

# 2. Run Stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# Copy the built jar from the /app/target folder in the previous stage
COPY --from=build /app/target/*.jar app.jar
ENV JAVA_OPTS="-Xmx350m -Xms350m"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
