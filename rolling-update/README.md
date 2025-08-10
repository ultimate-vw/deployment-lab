Rolling updates: 

This can be implemented by two ways: 

Production grade: 

-> We use kubernetes setup 
-> We define in yaml how many desired instances we need 
-> Kubernetes will depploy new pods and delete the old ones


# Rolling Update - Kubernetes (Traefik Ingress) + Docker Sim

This lab demonstrates Rolling Update - gradually replacing old Pods with new ones - primarily using Kubernetes
(Deployment strategy: type RollingUpdate), with an optional Docker Compose simulation.

Rolling updates are the default in Kubernetes: you set a new image tag; the Deployment rolls Pods gradually, honoring maxUnavailable and maxSurge.

---

## Why Rolling Update?

- Gradual, safe rollout: Limit blast radius with small increments.
- Built-in health checks: Readiness + liveness gating.
- Auto rollback (with tools like Argo Rollouts or kubectl rollout undo).

Trade-offs

- Some users will hit both versions during rollout.
- Rollback is not instant (unlike blue-green), but still fast.
- Requires careful DB migration strategy (backward compat during rollout).

---

## Kubernetes setup

### 1) Namespace (optional)

```yaml
# k8s/00-namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: rolling-lab
```
Apply:
```bash
kubectl apply -f k8s/00-namespace.yaml
```

### 2) Service + Deployment (v1 -> v2 via image tag)

```yaml
# k8s/10-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: backend
  namespace: rolling-lab
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxUnavailable: 1
      maxSurge: 1
  selector:
    matchLabels:
      app: backend
  template:
    metadata:
      labels:
        app: backend
    spec:
      containers:
        - name: app
          image: your-dockerhub-username/backend:v1
          imagePullPolicy: IfNotPresent
          ports:
            - containerPort: 8080
          readinessProbe:
            httpGet:
              path: /actuator/health
              port: 8080
            initialDelaySeconds: 5
            periodSeconds: 5
          livenessProbe:
            httpGet:
              path: /actuator/health
              port: 8080
            initialDelaySeconds: 10
            periodSeconds: 10
---
apiVersion: v1
kind: Service
metadata:
  name: backend-svc
  namespace: rolling-lab
spec:
  selector:
    app: backend
  ports:
    - port: 80
      targetPort: 8080
      protocol: TCP
      name: http
```

### 3) Ingress (Traefik)

```yaml
# k8s/20-ingress.yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: backend-ingress
  namespace: rolling-lab
  annotations:
    kubernetes.io/ingress.class: traefik
spec:
  rules:
    - host: rolling.localtest.me
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: backend-svc
                port:
                  number: 80
```

localtest.me resolves to 127.0.0.1, convenient for local clusters (kind/minikube).

### 4) Apply and test

```bash
kubectl apply -f k8s/10-deployment.yaml
kubectl apply -f k8s/20-ingress.yaml

# wait for pods
kubectl -n rolling-lab rollout status deploy/backend

# test
curl -H "Host: rolling.localtest.me" http://127.0.0.1/
# or explicitly:
curl -H "Host: rolling.localtest.me" http://127.0.0.1/api/version
```

### 5) Perform a rolling update (v1 -> v2)

Push your-dockerhub-username/backend:v2, then:

```bash
kubectl -n rolling-lab set image deploy/backend app=your-dockerhub-username/backend:v2
kubectl -n rolling-lab rollout status deploy/backend
```

Watch Pods change:

```bash
kubectl -n rolling-lab get pods -w -l app=backend
```

Roll back if needed:

```bash
kubectl -n rolling-lab rollout undo deploy/backend
```

---

## Docker Compose simulation (optional)

This simulates a rolling update locally by starting extra v2 instances and then removing v1 containers.
It is not the same as K8s, but helpful for demos without a cluster.

```yaml
# docker-compose.yml
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

  backend-v1:
    image: your-dockerhub-username/backend:v1
    expose: ["8080"]
    labels:
      - "traefik.enable=true"
      - "traefik.http.routers.backend.rule=Host(`localhost`)"
      - "traefik.http.routers.backend.entrypoints=web"
      - "traefik.http.services.backend.loadbalancer.server.port=8080"

  backend-v2:
    image: your-dockerhub-username/backend:v2
    expose: ["8080"]
    labels:
      - "traefik.enable=true"
      - "traefik.http.routers.backend.rule=Host(`localhost`)"
      - "traefik.http.routers.backend.entrypoints=web"
      - "traefik.http.services.backend.loadbalancer.server.port=8080"
```

Steps:

```bash
# start v1
docker compose up -d traefik backend-v1

# begin "roll" by adding v2 (both receive traffic)
docker compose up -d backend-v2

# optional: scale v2 higher to bias traffic
docker compose up -d --scale backend-v2=2

# remove v1 when satisfied
docker compose rm -sf backend-v1
```

---

## Observability and production tips

- Probes: keep readiness strict so only healthy Pods receive traffic.
- Surge/unavailable: tune maxSurge/maxUnavailable per SLOs and capacity.
- DB migrations: use expand -> deploy -> contract; never break old code mid-rollout.
- Session/state: prefer stateless; otherwise use sticky sessions/Redis.
- Automation: integrate into CI/CD (Helm + GitOps). Consider progressive delivery (Argo Rollouts) for canary/blue-green on top.

---

## Troubleshooting

- Stuck rollout: kubectl describe deploy/backend and kubectl describe pod/<name> to see failing probes.
- 404 via Ingress: verify hostname, ingress class, and Traefik running.
- Bad Gateway: ensure app binds 0.0.0.0:8080 in containers and readiness passes.
- Undo failed? Use kubectl rollout history to inspect revisions.

