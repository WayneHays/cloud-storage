FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY gradlew gradlew.bat ./
COPY gradle ./gradle
COPY settings.gradle ./
COPY backend/build.gradle ./backend/

RUN sed -i 's/\r$//' gradlew && chmod +x gradlew
RUN ./gradlew :backend:dependencies --no-daemon

COPY backend/src ./backend/src
RUN ./gradlew :backend:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN apk add --no-cache curl
RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=builder --chown=spring:spring /app/backend/build/libs/backend-*.jar /app/app.jar
USER spring:spring
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]