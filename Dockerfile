# syntax=docker/dockerfile:1
FROM gradle:8.5-jdk17 AS builder
WORKDIR /app

COPY gradle/ ./gradle/
COPY build.gradle.kts ./build.gradle.kts
COPY settings.gradle.kts ./settings.gradle.kts
COPY src/main/resources ./src/main/resources

RUN --mount=type=cache,target=/home/gradle/.gradle \
    gradle dependencies --no-daemon

COPY src ./src

RUN --mount=type=cache,target=/home/gradle/.gradle \
    gradle build -x test --no-daemon

RUN cp build/libs/*.jar application.jar && \
    java -Djarmode=tools -jar application.jar extract --layers --destination extracted

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring

COPY --from=builder /app/extracted/dependencies/ ./
COPY --from=builder /app/extracted/spring-boot-loader/ ./
COPY --from=builder /app/extracted/snapshot-dependencies/ ./
COPY --from=builder /app/extracted/application/ ./

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar application.jar"]