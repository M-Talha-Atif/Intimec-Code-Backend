# Start from the shared base builder (contains deps + shared modules)
FROM intimec-base-builder:latest AS builder

# Copy tools/jars if they exist
RUN mkdir -p /app/tools/jars/ 
COPY tools/ tools/

# Copy only this service's code
COPY services/connector-runtime-service/ services/connector-runtime-service/

# Build this specific service
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -pl services/connector-runtime-service/src -am clean package -DskipTests

# Runtime Image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN apk add --no-cache curl
RUN addgroup -S appuser && adduser -S appuser -G appuser

# Copy the built JAR
COPY --from=builder /app/services/connector-runtime-service/src/target/*.jar app.jar

# Copy connector JARs
RUN mkdir -p /app/jars/
COPY --from=builder /app/tools/jars/ /app/jars/

RUN chown -R appuser:appuser /app

USER appuser
EXPOSE 8093
EXPOSE 9094

ENTRYPOINT ["java", "-jar", "app.jar"]