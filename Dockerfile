
# 1. Build Stage
FROM maven:3.8.5-openjdk-17 AS build
COPY . .
RUN mvn clean package -DskipTests

# 2. Run Stage
FROM openjdk:17-jdk-slim
COPY --from=build /target/*.jar app.jar
ENV JAVA_OPTS="-Xmx350m -Xms350m"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
