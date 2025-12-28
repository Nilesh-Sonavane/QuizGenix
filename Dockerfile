# 1. Build Stage
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY . .

# --- FIX: Handle Nested Folder Structure ---
# If the 'quizgenix' folder exists inside the container, move its contents out to /app
RUN if [ -d "quizgenix" ]; then cp -r quizgenix/* .; fi
# -------------------------------------------

# Now Maven is guaranteed to find pom.xml
RUN mvn clean package -DskipTests

# 2. Run Stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

ENV JAVA_OPTS="-Xmx350m -Xms350m"
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
