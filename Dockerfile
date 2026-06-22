# Multistage build producing one image with all 3 Spring Boot services.
#
# Why one container?  Render's free tier gives one Web Service.  Three JVMs
# co-tenant under one process group, two of them on localhost-only ports
# (discussions-service:4001, genai-service:4002) and the third (graph) bound
# to $PORT for Render's edge router.  Inter-service calls become loopback —
# no service discovery, no internal DNS, no certs.
#
# Image size: ~210 MB on temurin-21-jre-alpine.
# RAM at idle (all 3 booted, no traffic): ~340 MB.  Fits Render's 512 MB cap.

# ---- Stage 1: build all 3 Spring Boot jars in parallel under one Maven run ----
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /workspace

# Cache the Maven deps layer separately so source changes don't redownload.
COPY discussions-service/pom.xml discussions-service/pom.xml
COPY discussions-graph/pom.xml   discussions-graph/pom.xml
COPY genai-service/pom.xml       genai-service/pom.xml
RUN cd discussions-service && mvn -B -ntp -q dependency:go-offline -DskipTests \
 && cd ../discussions-graph   && mvn -B -ntp -q dependency:go-offline -DskipTests \
 && cd ../genai-service       && mvn -B -ntp -q dependency:go-offline -DskipTests

# Now copy sources and build.  Tests are skipped here — CI runs them in a
# separate job.  -DfinalName forces a stable artifact name we can copy.
COPY discussions-service/src discussions-service/src
COPY discussions-graph/src   discussions-graph/src
COPY genai-service/src       genai-service/src

RUN cd discussions-service && mvn -B -ntp -q package -DskipTests -Dfinal.name=service \
 && cd ../discussions-graph   && mvn -B -ntp -q package -DskipTests -Dfinal.name=graph   \
 && cd ../genai-service       && mvn -B -ntp -q package -DskipTests -Dfinal.name=genai

# Each Spring Boot fat jar has a manifest pointing at its launcher; we don't
# bother layering because all 3 jars fit in a single image layer comfortably.

# ---- Stage 2: tiny runtime ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# curl is needed by start.sh's readiness probe loop.
RUN apk add --no-cache curl bash tini \
 && addgroup -S app && adduser -S -G app app

COPY --from=builder /workspace/discussions-service/target/*.jar /app/service.jar
COPY --from=builder /workspace/discussions-graph/target/*.jar   /app/graph.jar
COPY --from=builder /workspace/genai-service/target/*.jar       /app/genai.jar
COPY start.sh /app/start.sh
RUN chmod +x /app/start.sh && chown -R app:app /app

USER app

# Render injects $PORT for the public-facing port (graph).  The other two
# services run on fixed localhost ports inside the container.
EXPOSE 4003

# tini reaps zombies and forwards SIGTERM cleanly so all 3 JVMs shut down
# together when Render scales the instance down for sleep.
ENTRYPOINT ["/sbin/tini", "--"]
CMD ["/app/start.sh"]
