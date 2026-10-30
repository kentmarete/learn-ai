FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN ./gradlew --no-daemon dependencies -q
COPY src ./src
RUN ./gradlew --no-daemon bootJar -q

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/agentic-ai-java-*.jar app.jar
COPY data ./data
EXPOSE 8000
CMD ["java", "-jar", "app.jar"]
