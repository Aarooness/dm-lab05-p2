FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl ffmpeg fontconfig ttf-dejavu \
    && addgroup -S app \
    && adduser -S -G app -h /app app \
    && mkdir -p /app/uploads \
    && chown -R app:app /app
WORKDIR /app
COPY --from=build --chown=app:app /workspace/target/marketing-facil-1.0.0.jar /app/app.jar
ENV JAVA_TOOL_OPTIONS="-Djava.awt.headless=true"
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
