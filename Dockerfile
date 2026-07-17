# Stage 1: Build Rust word generator
FROM rust:1.96-slim AS rust-build
WORKDIR /build
COPY wordgen/ ./
RUN cargo build --release

# Stage 2: Build Java service
FROM maven:3.9-eclipse-temurin-21 AS java-build
WORKDIR /build
COPY service/ ./
RUN ./mvnw package -DskipTests -q

# Stage 3: Build frontend
FROM node:22-slim AS frontend-build
WORKDIR /build
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --quiet
COPY frontend/ ./
RUN npm run build

# Stage 4: Runtime
FROM eclipse-temurin:21-jre-alpine
LABEL org.opencontainers.image.source="https://github.com/Adi-UA/Whisper"
LABEL org.opencontainers.image.description="Shared daily safe-word service"

WORKDIR /app

# Copy Rust native library
COPY --from=rust-build /build/target/release/libwhisper_wordgen.so /app/lib/

# Copy Java JAR
COPY --from=java-build /build/target/whisper-service-0.0.1-SNAPSHOT.jar /app/whisper.jar

# Copy frontend static files into the JAR's classpath
COPY --from=frontend-build /build/../service/src/main/resources/static/ /app/static/

# SQLite database stored in a volume mount
VOLUME /app/data

ENV WHISPER_DB_URL=jdbc:sqlite:/app/data/whisper.db
ENV JAVA_OPTS=""

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -Djava.library.path=/app/lib ${JAVA_OPTS} -jar /app/whisper.jar"]
