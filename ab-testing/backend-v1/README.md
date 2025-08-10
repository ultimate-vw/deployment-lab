# Backend v1 (Blue)

Minimal Spring Boot API used in the Blue–Green Deployment lab.  
Exposes `/api/version` → `"Hi! I am blue!"`.

---

## **Endpoints**
- **GET** `/api/version` → `"Hi! I am blue!"`
- **GET** `/actuator/health` → Spring Boot health endpoint

---

## **Build & Run (local JVM)**

```bash
./mvnw spring-boot:run
# or
./mvnw -DskipTests package && java -jar target/*.jar
```

---

## **Docker**

```bash
docker build -t backend-v1:local .
docker run --rm -p 8080:8080 backend-v1:local
```

---

## **Config**

Environment variables:
- `SERVER_PORT` (default: `8080`)
- `SPRING_PROFILES_ACTIVE`
- `LOG_LEVEL` (if configured)
- CORS/JWT settings if applicable

---

## **Health & Observability**
- `/actuator/health`
- Logs via `stdout`
- Optional: `/actuator/metrics` if you enable them

---

## **Troubleshooting**
- “Bad Gateway” in Traefik?  
  Ensure the app listens on `0.0.0.0:8080` inside the container and is healthy.

```bash
docker exec -it <container> curl -s http://localhost:8080/api/version
```
