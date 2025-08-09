# Dark Launch (Shadow Traffic) — Traefik + Docker

This lab demonstrates Dark Launch (aka Shadow/Mirrored traffic) using Traefik as the reverse proxy.
Users continue to receive responses from v1 (primary), while the same requests are copied to v2 (shadow)
for testing, performance, and error monitoring. Shadow responses are not returned to users.

> Use dark launch when you want to exercise a new version in production-like conditions without impacting users.

---

## Why Dark Launch?

- No user impact: Users keep seeing stable v1 responses.
- Real production traffic: v2 gets identical requests for realistic load/perf/error signals.
- Safer than canary: Even if v2 is wrong or slow, users don’t notice.
- Great for profiling, schema read checks, feature parity, and logging/metrics validation.

Caveats

- No side effects: Ensure mirrored calls to v2 do not create/modify state (send as read-only or suppress writes).
- Auth/PII: Be mindful when duplicating headers/bodies. Mask sensitive fields as required by policy/GDPR.
- Idempotency: If you must hit write endpoints, make v2 operate in a sandbox mode or short-circuit mutations.

---

## Project Layout

```
dark-launch-lab/
├─ docker-compose.yml
├─ traefik/
│  ├─ traefik.yml        # static (entrypoints, providers, dashboard)
│  └─ dynamic.yml        # router + mirroring middleware
├─ backend-v1/           # /api/version -> "v1"
│  ├─ Dockerfile
│  └─ src/...
└─ backend-v2/           # /api/version -> "v2" (shadow)
   ├─ Dockerfile
   └─ src/...
```

Traefik listens on:

- :80  -> user traffic (web entrypoint)
- :8080 -> Traefik dashboard (/dashboard/)

---

## Traefik configuration

### traefik/traefik.yml (static)

```yaml
entryPoints:
  web:
    address: ":80"
  traefik:
    address: ":8080"

api:
  dashboard: true

providers:
  docker:
    exposedByDefault: false
  file:
    filename: /etc/traefik/dynamic.yml
```

### traefik/dynamic.yml (dynamic)

```yaml
http:
  routers:
    dark-router:
      rule: "Host(`localhost`)"
      entryPoints: ["web"]
      service: v1-with-shadow
      middlewares: ["shadow-mirror"]

  middlewares:
    shadow-mirror:
      mirroring:
        service: v1-primary
        mirrors:
          - name: v2-shadow
            percent: 100

  services:
    v1-with-shadow:
      loadBalancer:
        servers:
          - url: "http://backend-v1:8080"

    v1-primary:
      loadBalancer:
        servers:
          - url: "http://backend-v1:8080"

    v2-shadow:
      loadBalancer:
        servers:
          - url: "http://backend-v2:8080"
```

The mirroring middleware duplicates traffic to v2-shadow while responses are always served from v1-primary.

---

## docker-compose.yml

```yaml
services:
  traefik:
    image: traefik:v3.0
    command: ["--configFile=/etc/traefik/traefik.yml"]
    ports:
      - "80:80"
      - "8080:8080"
    volumes:
      - ./traefik/traefik.yml:/etc/traefik/traefik.yml:ro
      - ./traefik/dynamic.yml:/etc/traefik/dynamic.yml:ro
      - /var/run/docker.sock:/var/run/docker.sock
    depends_on:
      - backend-v1
      - backend-v2

  backend-v1:
    build: ./backend-v1
    expose:
      - "8080"
    labels:
      - "traefik.enable=true"

  backend-v2:
    build: ./backend-v2
    expose:
      - "8080"
    labels:
      - "traefik.enable=true"
```

Spring Boot containers must listen on 0.0.0.0:8080 inside the container.

---

## Run it

```bash
docker compose up --build
open http://localhost:8080/dashboard/
```

Hit your API repeatedly:

```bash
curl -H "Host: localhost" http://localhost/api/version
# -> always returns from v1
```

Check v2 logs to verify mirrored traffic is arriving (even though you never see its response).

---

## Testing patterns

- Correctness parity: send synthetic traffic; compare v1 vs v2 responses in logs/metrics.
- Latency/CPU: v2 may be slower; fine-tune GC, threads, caches.
- Headers/PII: verify mirroring preserves needed auth headers; redact sensitive fields if required.
- Writes: for POST/PUT/DELETE, prefer sandbox-mode in v2 or ensure idempotency/no-ops.

---

## Observability tips

- Enable request/response logging in v2 (shadow), structured logs (traceId, userId if safe).
- Export metrics (Micrometer/Prometheus): http_server_requests_seconds, error counts, p95.
- Correlate by request ID: include a header (for example, X-Request-ID) set by Traefik or upstream.

Example to inject a request ID header in Traefik:

```yaml
http:
  middlewares:
    reqid:
      headers:
        customRequestHeaders:
          X-Request-ID: "{reqid}"
  routers:
    dark-router:
      middlewares: ["reqid", "shadow-mirror"]
```

---

## Production considerations

- Data writes: use feature flags or sandbox mode to avoid double writes.
- Backpressure: mirroring doubles backend load, ensure headroom.
- Sampling: use percent < 100 for partial mirroring.
- RBAC & audit: protect Traefik configs; track who changes mirroring.
- Security: sanitize payloads or selectively drop headers when mirroring if policy requires.

---

## Roll forward to canary/blue-green (next steps)

- After confidence in v2, increase canary weight to serve some user traffic.
- Or flip blue-green from v1 to v2.
- Keep dark launch as a regression tool for the next release.

---

## Troubleshooting

- Bad Gateway: check that backends bind to 0.0.0.0:8080 and are healthy.
- No traffic on v2: ensure the shadow-mirror middleware is attached to the router and service names resolve.
- CORS: if a browser is calling through Traefik, configure CORS headers on the backend or at Traefik middleware.
- TLS: terminate TLS at Traefik with Let’s Encrypt/ACME if exposing over the internet.
