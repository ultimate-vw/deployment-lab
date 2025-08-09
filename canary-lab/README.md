# Canary Deployment (Traefik v2 + Docker Compose)

Zero-downtime **canary releases** using Traefik’s **weighted services** (file provider). You’ll shift a small % of live traffic to a new version, watch metrics, then gradually ramp up until 100%—or roll back instantly.

---

## ✅ What this gives you
- **One command up**: `docker compose up -d`
- **Two app versions** behind Traefik: `backend-v1` (stable) and `backend-v2` (canary)
- **Weighted routing** controlled from a single file: `dynamic/canary.yml`
- **Safe ramp-up plan**: 1% → 5% → 10% → 25% → 50% → 100%
- **Instant rollback** by setting the weight of v2 to 0

> No app code required. We use the tiny `traefik/whoami` container to prove routing and weights.

---

## 🧱 Architecture

```
        ┌─────────────┐
        │   Client    │  curl / browser / load test
        └─────┬───────┘
              │
        ┌─────▼─────────────────────────────────────┐
        │                 Traefik                   │
        │    routers.app  →  service: apps-weighted │
        └─────┬─────────────────────────────────────┘
              │  (weighted split via file provider)
     ┌────────┴────────┐                     ┌──────────────┐
     │ backend-v1      │  weight = 90%       │ backend-v2   │  weight = 10%
     │ (stable)        │◀────────────────────▶│ (canary)    │
     └─────────────────┘                     └──────────────┘
```

---

## 📦 Prerequisites
- Docker & Docker Compose
- Ports free: **80** (HTTP), **8088** (Traefik dashboard — optional)

---

## 📁 Repository Layout

```
canary-traefik/
├─ .env
├─ docker-compose.yml
├─ traefik/
│  └─ traefik.yml
└─ dynamic/
   └─ canary.yml   # ← change weights here
```

Create the files exactly as shown below.

---

## 🔐 `.env`

```env
# You can change these if ports clash
TRAEFIK_HTTP_PORT=80
TRAEFIK_DASHBOARD_PORT=8088

# App ports (internal container ports for whoami)
APP_PORT=8080
```

---

## 🐳 `docker-compose.yml`

```yaml
services:
  traefik:
    image: traefik:v2.11
    container_name: traefik
    command:
      - "--api.dashboard=true"
      - "--entrypoints.web.address=:${TRAEFIK_HTTP_PORT}"
      - "--providers.docker=true"
      - "--providers.docker.exposedbydefault=false"
      - "--providers.file.directory=/etc/traefik/dynamic"
      - "--providers.file.watch=true"
    ports:
      - "${TRAEFIK_HTTP_PORT}:80"
      - "${TRAEFIK_DASHBOARD_PORT}:8080"
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock:ro
      - ./traefik/traefik.yml:/etc/traefik/traefik.yml:ro
      - ./dynamic:/etc/traefik/dynamic:ro
    labels:
      # Expose Traefik dashboard at /dashboard (optional)
      - "traefik.enable=true"
      - "traefik.http.routers.traefik.rule=PathPrefix(`/dashboard`) || PathPrefix(`/api`)"
      - "traefik.http.routers.traefik.service=api@internal"
      - "traefik.http.routers.traefik.entrypoints=web"

  backend-v1:
    image: traefik/whoami:v1.10
    container_name: backend-v1
    environment:
      - WHOAMI_NAME=backend-v1
    labels:
      - "traefik.enable=true"
      # We register a *named service* for v1 that file provider will reference
      - "traefik.http.services.app-v1.loadbalancer.server.port=${APP_PORT}"

  backend-v2:
    image: traefik/whoami:v1.10
    container_name: backend-v2
    environment:
      - WHOAMI_NAME=backend-v2
    labels:
      - "traefik.enable=true"
      # We register a *named service* for v2 that file provider will reference
      - "traefik.http.services.app-v2.loadbalancer.server.port=${APP_PORT}"
```

> **Note:** We don’t attach a router directly to v1 or v2. The **file provider** (`dynamic/canary.yml`) creates a **router** → `apps-weighted` service, which spreads traffic across the two Docker services by weights.

---

## ⚙️ `traefik/traefik.yml` (static config)

```yaml
api:
  dashboard: true

log:
  level: INFO

accessLog: {}

providers:
  docker:
    exposedByDefault: false
  file:
    directory: /etc/traefik/dynamic
    watch: true

entryPoints:
  web:
    address: ":80"
```

---

## 🎚️ `dynamic/canary.yml` (dynamic config — edit weights here)

```yaml
http:
  routers:
    app:
      rule: "PathPrefix(`/`)"
      entryPoints:
        - web
      service: apps-weighted

  services:
    # Concrete LB services that point to your docker services by URL
    app-v1:
      loadBalancer:
        servers:
          - url: "http://backend-v1:8080"

    app-v2:
      loadBalancer:
        servers:
          - url: "http://backend-v2:8080"

    # The canary split
    apps-weighted:
      weighted:
        services:
          - name: app-v1
            weight: 90
          - name: app-v2
            weight: 10
```

**How it works:** Traefik reads `apps-weighted`, which is a **composed service** that splits traffic between `app-v1` and `app-v2` by weight. Change the numbers to adjust the canary share.

---

## 🚀 Run

```bash
docker compose up -d
```

Traefik dashboard (optional): `http://localhost:${TRAEFIK_DASHBOARD_PORT}/dashboard/`  
App endpoint: `http://localhost:${TRAEFIK_HTTP_PORT}/`

---

## 🔍 Verify the split

Hit the endpoint repeatedly and watch the container names alternate according to weight:

```bash
# macOS/Linux
for i in {1..30}; do curl -s http://localhost/ | grep -E 'Hostname|X-Forwarded-Host|X-Forwarded-For|X-Real-Ip|Server'; echo ""; done
```

You should see responses coming from **backend-v1** ~90% of the time and **backend-v2** ~10% of the time.

---

## 📈 Ramp Plan (edit `dynamic/canary.yml`)

Recommended increments (each step after monitoring for errors/latency):
- 1% → 5% → 10% → 25% → 50% → 100%

Example edits:

```yaml
# Step 1 (1% canary)
services:
  apps-weighted:
    weighted:
      services:
        - name: app-v1
          weight: 99
        - name: app-v2
          weight: 1
```

```yaml
# Step 2 (5% canary)
services:
  apps-weighted:
    weighted:
      services:
        - name: app-v1
          weight: 95
        - name: app-v2
          weight: 5
```

…and so on.

> **Instant rollback:** set `app-v2` weight to **0** (or remove it).

---

## 🧪 Health checks (optional but recommended)

You can add health checks to the `loadBalancer` services:

```yaml
services:
  app-v1:
    loadBalancer:
      servers:
        - url: "http://backend-v1:8080"
      healthCheck:
        path: /
        interval: "5s"
        timeout: "2s"
```

Repeat for `app-v2`.

---

## 🧰 Swapping in real backends

Replace `traefik/whoami` services with your real app containers. Keep the **service names** `backend-v1` and `backend-v2`, or adjust the `servers.url` targets inside `dynamic/canary.yml` accordingly.

Example (Spring Boot JAR as v2):

```yaml
backend-v2:
  image: eclipse-temurin:17-jre
  container_name: backend-v2
  working_dir: /app
  volumes:
    - ./v2/app.jar:/app/app.jar:ro
  command: ["java", "-jar", "/app/app.jar"]
  labels:
    - "traefik.enable=true"
    - "traefik.http.services.app-v2.loadbalancer.server.port=8080"
```

---

## 🔒 Production notes
- Terminate TLS at Traefik with a `websecure` entrypoint and certs/ACME.
- Put Traefik behind a Cloud load balancer (ALB/NLB/GCLB) if needed.
- Add metrics/observability (Prometheus/Grafana) and error-rate SLOs to automate promotion/rollback.
- For sticky sessions, use Traefik sticky cookies on each LB service (not on the `weighted` service).

---

## 🧹 Clean up

```bash
docker compose down -v
```

---

## 🆘 Troubleshooting

- “All traffic goes to v1”: check that the `apps-weighted` service exists in `dynamic/canary.yml` and the router `app` points to it.
- “404 Not Found”: ensure the `routers.app` rule is `PathPrefix(`/`)` and Traefik got the file provider config (see logs).
- “Weights don’t change”: you edited the file—Traefik hot-reloads it. If not, restart: `docker compose restart traefik`.
- Port conflicts: change `TRAEFIK_HTTP_PORT`/`TRAEFIK_DASHBOARD_PORT` in `.env`.

---

## 📜 License
MIT (or do-whatever-you-want).

Happy canarying.
