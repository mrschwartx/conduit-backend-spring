FROM eclipse-temurin:25-jdk AS builder
WORKDIR /build
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src src
RUN ./mvnw -B -q -DskipTests package \
    && cp target/conduit-backend-*.jar application.jar

FROM eclipse-temurin:25-jre-alpine AS runner
WORKDIR /app
ENV PORT=8080 \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
RUN addgroup -S app && adduser -S app -G app
COPY --from=builder --chown=app:app /build/application.jar ./application.jar
USER app
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD wget -qO- http://127.0.0.1:${PORT}/health || exit 1
ENTRYPOINT ["java", "-jar", "application.jar"]
