#!/usr/bin/env bash
# Bootstraps all 3 Spring Boot JARs inside one container for the Render
# single-instance deployment.
#
# Layout:
#   discussions-service → localhost:4001 (background, internal)
#   genai-service       → localhost:4002 (background, internal)
#   discussions-graph   → 0.0.0.0:$PORT  (foreground, public-facing on Render)
#
# Lifecycle:
#   - Each child gets a SIGTERM handler so `docker stop` / Render's sleep
#     signal propagates cleanly.
#   - Graph runs in the foreground so its exit code becomes the container's
#     exit code (Render restarts the container on non-zero).
#   - Service + genai are required for graph to function, so we wait for
#     /actuator/health=200 on both before starting graph.
set -euo pipefail

# ---- JVM tuning for 3 JVMs in 512 MB ----
# MaxRAMPercentage=30 per JVM → 30%*512 = ~150 MB heap each → 450 MB heap
# across all three.  Plus ~50 MB metaspace each (capped) = ~600 MB committed
# best-case, but RSS settles around 340 MB at idle thanks to UseSerialGC and
# container memory awareness.
JAVA_FLAGS="-XX:MaxRAMPercentage=30 \
  -XX:+UseSerialGC \
  -XX:TieredStopAtLevel=1 \
  -Xss256k \
  -XX:MaxMetaspaceSize=96m \
  -Dspring.jmx.enabled=false \
  -Dspring.main.lazy-initialization=true"

SERVICE_PORT=4001
GENAI_PORT=4002
GRAPH_PORT="${PORT:-4003}"

log() { echo "[start.sh] $*"; }

# ---- Trap so all children die together ----
pids=()
shutdown() {
  log "SIGTERM received, stopping ${#pids[@]} child JVM(s)…"
  for pid in "${pids[@]}"; do
    kill -TERM "$pid" 2>/dev/null || true
  done
  wait
  exit 0
}
trap shutdown SIGTERM SIGINT

# ---- Start discussions-service (internal) ----
log "Starting discussions-service on :$SERVICE_PORT"
SPRING_PROFILES_ACTIVE=prod \
PORT=$SERVICE_PORT \
SERVER_ADDRESS=127.0.0.1 \
java $JAVA_FLAGS -jar /app/service.jar &
pids+=("$!")

# ---- Start genai-service (internal) ----
log "Starting genai-service on :$GENAI_PORT"
SPRING_PROFILES_ACTIVE=prod \
PORT=$GENAI_PORT \
SERVER_ADDRESS=127.0.0.1 \
java $JAVA_FLAGS -jar /app/genai.jar &
pids+=("$!")

# ---- Wait for both internal services to be ready before binding graph ----
# Graph 401s every request until its downstreams answer, so we'd rather not
# accept traffic at the Render edge until they're up.  60s budget per service
# covers Spring Boot cold-start with the lazy-init flag.
wait_ready() {
  local name=$1 port=$2 budget=${3:-60}
  log "Waiting for $name health (port $port, ${budget}s budget)…"
  for ((i=0; i<budget; i++)); do
    if curl -fsS "http://127.0.0.1:$port/actuator/health" >/dev/null 2>&1; then
      log "$name ready after ${i}s"
      return 0
    fi
    sleep 1
  done
  log "ERROR: $name did not become ready within ${budget}s"
  return 1
}

wait_ready discussions-service "$SERVICE_PORT" 60
wait_ready genai-service       "$GENAI_PORT"   60

# ---- Start discussions-graph (foreground, public) ----
log "Starting discussions-graph on :$GRAPH_PORT (foreground)"
exec env \
  SPRING_PROFILES_ACTIVE=prod \
  PORT=$GRAPH_PORT \
  DISCUSSIONS_SERVICE_URL="http://127.0.0.1:$SERVICE_PORT" \
  GENAI_SERVICE_URL="http://127.0.0.1:$GENAI_PORT" \
  java $JAVA_FLAGS -jar /app/graph.jar
