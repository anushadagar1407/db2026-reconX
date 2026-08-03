# ============================================================================
# TICKET-ADV146 — Spring Boot Dockerfile (multi-stage Maven -> JRE 25 slim)
#
# Shared JDK layers feed one-shot tests and the production JAR build.
# The final image copies only the JAR onto a slim JRE image.
# ============================================================================

FROM eclipse-temurin:25-jdk AS build-base
WORKDIR /workspace/backend
COPY backend/pom.xml backend/mvnw ./
COPY backend/.mvn .mvn
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
COPY recon-audit-starter/pom.xml /workspace/recon-audit-starter/pom.xml
COPY recon-audit-starter/src /workspace/recon-audit-starter/src
RUN ./mvnw -q -B -f ../recon-audit-starter/pom.xml install
RUN ./mvnw -q -B dependency:go-offline || true
COPY backend/src src

FROM build-base AS test
CMD ["./mvnw", "--batch-mode", "--no-transfer-progress", "verify"]

FROM build-base AS build
RUN ./mvnw -q -B -DskipTests package && \
    mv target/reconx-*.jar app.jar

FROM eclipse-temurin:25-jre-alpine AS production
RUN addgroup -S reconx && adduser -S reconx -G reconx
USER reconx
WORKDIR /app
COPY --from=build /workspace/backend/app.jar app.jar
EXPOSE 8080
ENV JAVA_OPTS=""
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]
