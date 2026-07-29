# ============================================================================
# TICKET-ADV146 — Spring Boot Dockerfile (multi-stage Maven -> JRE 25 slim)
#
# Stage 1: build the JAR with Temurin JDK 25 + Maven.
# Stage 2: copy ONLY the JAR onto a slim JRE image.
# ============================================================================

FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace/backend
COPY backend/pom.xml backend/mvnw ./
COPY backend/.mvn .mvn
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
COPY recon-audit-starter/pom.xml /workspace/recon-audit-starter/pom.xml
COPY recon-audit-starter/src /workspace/recon-audit-starter/src
RUN ./mvnw -q -B -f ../recon-audit-starter/pom.xml install
RUN ./mvnw -q -B dependency:go-offline || true
COPY backend/src src
RUN ./mvnw -q -B -DskipTests package && \
    mv target/reconx-*.jar app.jar

FROM eclipse-temurin:25-jre-alpine
RUN addgroup -S reconx && adduser -S reconx -G reconx
USER reconx
WORKDIR /app
COPY --from=build /workspace/app.jar app.jar
EXPOSE 8080
ENV JAVA_OPTS=""
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]
