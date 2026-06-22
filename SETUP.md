# AnonCircles — Local setup

One-shot guide for running all 4 services on a fresh machine. Tested on macOS (Apple Silicon + Intel) and Linux. Windows works too with WSL2.

> **TL;DR — already have JDK 21, Node 22, Postgres or Docker?**
> Skip to **[3. Configure secrets](#3-configure-secrets-once)** and **[4. Boot everything](#4-boot-everything)**.

---

## 1. Install prerequisites

You need **three tools**: JDK 21, Node.js 22+, and PostgreSQL 16 (either via Docker or natively).

### macOS (Homebrew)

```bash
# Homebrew itself
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# JDK 21 (Eclipse Temurin)
brew install --cask temurin@21

# Node.js 22 (versioned formula — pinned for stability)
brew install node@22

# Link it; --overwrite handles the case where a generic `node` is already
# installed and owns /opt/homebrew/lib/node_modules/npm/* files.
brew link --overwrite --force node@22

# node@22 is keg-only, so add it to PATH explicitly:
echo 'export PATH="/opt/homebrew/opt/node@22/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc
node --version   # v22.x.x

# Postgres 16 — pick ONE of the two options below
brew install postgresql@16          # native (recommended — no Docker needed)
# OR
brew install --cask docker          # Docker Desktop, if you'd rather containerize
```

After installing JDK, **add it to your shell**:

```bash
cat >> ~/.zshrc <<'EOF'

# AnonCircles JDK 21
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
export PATH="$JAVA_HOME/bin:$PATH"
EOF
source ~/.zshrc
java -version   # should print "openjdk version 21.x.x"
```

### Linux (apt/dnf)

```bash
# Ubuntu/Debian
sudo apt update
sudo apt install -y openjdk-21-jdk nodejs npm postgresql-16

# Fedora/RHEL
sudo dnf install -y java-21-openjdk nodejs npm postgresql-server
```

### Windows
Use **WSL2 + Ubuntu** and follow the Linux instructions inside WSL.

---

## 2. Start Postgres

### Option A — native Postgres (macOS Homebrew)

```bash
brew services start postgresql@16

# Create the database (run once)
createdb anoncircles
```

The brew install creates a Postgres role matching your macOS username, with no password. That matches the `.env.example` defaults for native installs.

### Option B — Postgres via Docker

```bash
cd discussions-service
docker compose up -d        # boots postgres on localhost:5432 with user/pw postgres/postgres
```

`docker compose ps` should show `anoncircles-postgres` as healthy.

### Option C — Postgres on Linux

```bash
sudo systemctl start postgresql
sudo -u postgres createdb anoncircles
sudo -u postgres psql -c "CREATE USER postgres WITH PASSWORD 'postgres' SUPERUSER;"  # if missing
```

> **Flyway runs migrations automatically** on the service's first boot — you don't need to load any SQL by hand.

---

## 3. Configure secrets (once)

The 4 repos communicate via a **shared secret**. Generate it once and paste into both files that use it.

```bash
cd /path/to/Discussions   # this monorepo root — wherever you cloned it

# Generate fresh dev secrets
INTERNAL_TOKEN=$(openssl rand -base64 32)
JWT_SECRET=$(openssl rand -base64 32)

# Copy .env.example → .env in all 4 repos
for d in discussions-service discussions-graph genai-service discussions-ui; do
  cp $d/.env.example $d/.env
done

# Patch the shared INTERNAL_SERVICE_TOKEN into BOTH graph and genai
sed -i.bak "s|INTERNAL_SERVICE_TOKEN=.*|INTERNAL_SERVICE_TOKEN=$INTERNAL_TOKEN|" \
  discussions-graph/.env genai-service/.env

# Patch the JWT signing key into discussions-service
sed -i.bak "s|ENGAGE_AUTH_SECRET=.*|ENGAGE_AUTH_SECRET=$JWT_SECRET|" \
  discussions-service/.env

# If you used native brew postgres (no password, your macOS username is the role):
sed -i.bak "s|SPRING_DATASOURCE_USERNAME=.*|SPRING_DATASOURCE_USERNAME=$(whoami)|;
            s|SPRING_DATASOURCE_PASSWORD=.*|SPRING_DATASOURCE_PASSWORD=|" \
  discussions-service/.env

# Clean up the sed backups
rm -f discussions-service/.env.bak discussions-graph/.env.bak genai-service/.env.bak

echo "✅ Secrets configured."
```

> **Gemini API key (optional)** — set `GEMINI_API_KEY` in `genai-service/.env` to a real key from [aistudio.google.com/apikey](https://aistudio.google.com/apikey) to use real Gemini. Leave blank to use the bundled `MockGenaiProvider` (returns templated text, no network).

---

## 4. Boot everything

Open **4 terminal tabs**, one per service. Each `.env` is loaded automatically by Spring Boot (via `spring-boot-maven-plugin` reading `${VAR}` placeholders) — you don't need a separate dotenv tool.

```bash
# Terminal 1 — discussions-service (REST API on :4001)
cd discussions-service
set -a; source .env; set +a       # export .env into the shell
./mvnw spring-boot:run

# Terminal 2 — genai-service (SSE on :4002)
cd genai-service
set -a; source .env; set +a
./mvnw spring-boot:run

# Terminal 3 — discussions-graph (GraphQL BFF on :4003)
cd discussions-graph
set -a; source .env; set +a
./mvnw spring-boot:run

# Terminal 4 — discussions-ui (Vite dev on :5173)
cd discussions-ui
npm install
npm run dev
```

When all four are up, open **http://localhost:5173** in your browser.

---

## 5. Verify the stack

```bash
# Health checks
curl -s http://localhost:4001/actuator/health | jq .status   # "UP"
curl -s http://localhost:4002/actuator/health | jq .status   # "UP"
curl -s http://localhost:4003/actuator/health | jq .status   # "UP"

# End-to-end smoke: mint a session, list circles
TOKEN=$(curl -s -X POST http://localhost:4001/auth/session | jq -r .engageAuth)
curl -s http://localhost:4001/circles -H "Authorization: Bearer $TOKEN" | jq '.data | length'
```

---

## Troubleshooting

| Symptom | Fix |
|---|---|
| `docker: command not found` | Install Docker Desktop (`brew install --cask docker`) OR use native Postgres (Option A above). |
| `Connection refused: localhost:5432` | Postgres isn't running. `brew services list` should show `postgresql@16 started`. |
| Service boots but returns 500 on every request | `ENGAGE_AUTH_SECRET` is missing or shorter than 32 bytes. Regenerate with `openssl rand -base64 32`. |
| Graph returns 401/UNAUTHENTICATED on `generateDescription` | Token mismatch — `INTERNAL_SERVICE_TOKEN` must be **identical** in `discussions-graph/.env` and `genai-service/.env`. |
| UI shows "Waking the demo..." forever locally | The splash only appears if the backend takes >1s to respond. Local servers always respond fast — if you see this, the BFF is down. |
| `java -version` shows the wrong JDK | `export JAVA_HOME=$(/usr/libexec/java_home -v 21)` then restart your terminal. |
| Port already in use | Another instance is running. `lsof -ti:4001,4002,4003,5173 \| xargs kill`. |

---

## Production deployment

For deploying to the free tier (Vercel + Render + Neon), see **[DEPLOY.md](./DEPLOY.md)**.
