# 1. Build Stage
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY . .
RUN if [ -d "quizgenix" ]; then cp -r quizgenix/* .; fi
RUN mvn clean package -DskipTests

# 2. Run Stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENV JAVA_OPTS="-Xmx350m -Xms350m"
EXPOSE 8080

# --- CRITICAL FIX IS THIS LINE BELOW ---
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.net.preferIPv4Stack=true -jar app.jar"]
