FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

RUN apt-get update \
    && apt-get install -y maven \
    && rm -rf /var/lib/apt/lists/*

COPY pom.xml .
COPY src ./src

RUN mvn -B -DskipTests clean package

FROM eclipse-temurin:21-jre

RUN useradd -r -u 1001 appuser

WORKDIR /app

COPY --from=build /app/target/chat-application-0.0.1-SNAPSHOT.jar app.jar

RUN chown -R appuser:appuser /app
USER appuser

EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Dserver.port=${PORT:-8080} -jar /app/app.jar"]
