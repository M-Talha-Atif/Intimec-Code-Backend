# Start from the shared base builder (contains deps + shared modules)
FROM intimec-base-builder:latest AS builder

# Copy only this service's code
COPY services/auth-service/ services/auth-service/

# Build this specific service
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -pl services/auth-service/src -am clean package -DskipTests

# Runtime Image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN apk add --no-cache curl
RUN addgroup -S appuser && adduser -S appuser -G appuser

COPY --from=builder /app/services/auth-service/src/target/*.jar app.jar
RUN chown -R appuser:appuser /app

USER appuser
EXPOSE 8090
EXPOSE 8002

ENTRYPOINT ["java", "-jar", "app.jar"]
