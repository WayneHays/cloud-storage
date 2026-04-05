FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY settings.gradle ./
COPY backend/build.gradle ./backend/
COPY backend/settings.gradle ./backend/
COPY backend/src ./backend/src
RUN chmod +x gradlew && sed -i 's/\r$//' gradlew && ./gradlew :backend:bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY backend/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]