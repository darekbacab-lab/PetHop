FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN chmod +x mvnw
RUN ./mvnw -B dependency:go-offline
COPY src src
RUN ./mvnw -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
ENV PORT=8080
COPY --from=build /build/target/pethop-api-1.0.0-SNAPSHOT.jar app.jar
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system pethop \
    && useradd --system --gid pethop --home-dir /app --no-create-home pethop \
    && mkdir -p /var/lib/pethop/files \
    && chown -R pethop:pethop /app /var/lib/pethop
VOLUME ["/var/lib/pethop/files"]
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 CMD curl --fail --silent "http://127.0.0.1:${PORT:-8080}/actuator/health" || exit 1
USER pethop
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
