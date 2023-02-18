FROM openjdk:17-jdk-slim as builder
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY src src
RUN chmod +x ./gradlew
RUN ./gradlew bootJar


FROM openjdk:17-jdk-slim
COPY --from=builder build/libs/*.jar app.jar
HEALTHCHECK --interval=10s --timeout=3s CMD curl -L -s GET 'http://localhost:50002/auth/ping' || PONG
EXPOSE 50002
ENTRYPOINT ["java","-Dspring.profiles.active=product","-jar","/app.jar"]
