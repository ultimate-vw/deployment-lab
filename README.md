# Deployment Strategies Lab

This repository contains multiple implementations of modern **deployment strategies** for backend applications,
using **Docker, Traefik, and Kubernetes**.  
The aim is to provide hands-on examples for each strategy and demonstrate their application in real-world systems.


# Deployment Strategies Lab — Consolidated Guide

This repository contains hands-on labs for multiple deployment strategies implemented using **Docker**, **Traefik**, and in some cases **Kubernetes**.

Each strategy includes:
- Full working code (Spring Boot backend services, Traefik reverse proxy configuration, and Docker Compose setup)
- A detailed README explaining the strategy, steps to run, production considerations, and example configurations.
- Diagrams showing the traffic flow and cutover process.

---

## Strategies

### 1. [Blue–Green Deployment](blue-green-lab/README.md)
Run two versions (Blue & Green) side-by-side. Flip traffic instantly between them via Traefik config.  
✅ Benefits: Zero-downtime, instant rollback, no mixed-version state.


```mermaid

flowchart LR
%% Blue–Green Deployment
    subgraph BlueEnv["Blue Environment (v1) - Current Production"]
        B1[Running v1 Application]
        DB1[(Database)]
    end

    subgraph GreenEnv["Green Environment (v2) - New Release"]
        G1[Running v2 Application]
        DB2[(Database)]
    end

    User[User Requests] --> LB[Load Balancer]

%% Step 1: Blue live
    LB --> B1
    B1 --> DB1

%% Step 2: Deploy Green in parallel
    Deploy[Deploy New Version to Green] --> G1
    G1 --> DB2

%% Step 3: Switch traffic
    Switch[Switch Load Balancer] --> LB
    LB --> G1

%% Step 4: Blue idle, fallback
    NoteBlue[Blue kept for rollback] --> B1

%% Styling
    classDef blue fill:#3498db,stroke:#1f618d,stroke-width:2px,color:#fff;
    classDef green fill:#2ecc71,stroke:#1e8449,stroke-width:2px,color:#fff;
    classDef infra fill:#95a5a6,stroke:#7f8c8d,stroke-width:2px,color:#fff;

    class B1,DB1,NoteBlue blue;
    class G1,DB2 green;
    class LB,Deploy,Switch,User infra;


```

---

### 2. [Canary Deployment](canary-lab/README.md)
Gradually shift traffic from the old version to the new version in small increments (e.g., 10%, 30%, 50%, etc.) before full rollout.  
✅ Benefits: Gradual exposure, better risk management.


```mermaid

flowchart LR
    
  %% --- Canary ---
  subgraph CN["Canary"]
    direction LR
    U2["Users"] --> CANA{"% Canary"}
    CANA -- "Small %" --> CV["Canary Version 🐤"]
    CANA -- "Rest" --> SV["Stable Version"]
    CV --> MON["Monitor"]
    MON --> DEC{"Healthy?"}
    DEC -- "Yes" --> ROLL["Rollout More"]
    DEC -- "No" --> RB2["Rollback"]
  end
```

---

### 3. [Dark Launch (Shadow Traffic)](dark-launch/README.md)
Mirror live traffic to a new version without impacting users. The shadow version runs silently for validation.  
✅ Benefits: Real traffic testing without user risk.

```mermaid
flowchart LR
  %% --- Dark Launch ---
  subgraph DL["Dark Launch"]
    direction LR
    U3["Users"] --> FE["Feature Disabled"]
    INT["Internal Users"] --> FEI["Feature Enabled"]
    FEI --> MON2["Monitor"]
    MON2 --> ADJ["Adjust"]
  end
  
```
---

### 4. [Rolling Update](rolling-update/README.md)
Replace instances of the old version with the new version gradually, without requiring double capacity.  
✅ Benefits: Balanced resource usage, no downtime.

```mermaid

flowchart LR
  %% --- Rolling Update ---
  subgraph RU["Rolling Update"]
    direction LR
    N1["Node 1"] --> UP1["Update"]
    N2["Node 2"] --> UP2["Update"]
    N3["Node 3"] --> UP3["Update"]
    UP1 --> MON3["Monitor"]
    UP2 --> MON3
    UP3 --> MON3
  end
```


---

### 5. [Feature Toggle Deployment](feature-toggle-lab/README.md)
Deploy features hidden behind toggles; enable or disable them dynamically without redeploying.  
✅ Benefits: Decouple deployment from release, safer experiments.

```mermaid
flowchart LR
  %% --- Feature Toggle ---
  subgraph FT["Feature Toggle"]
    direction LR
    CODE["Code with Toggles"] --> CFG{"Toggle On?"}
    CFG -- "Yes" --> ENF["Enable Feature"]
    CFG -- "No" --> DISF["Disable Feature"]
  end
```

---

### 6. [A/B Testing Deployment](ab-testing/README.md)
Route users to different versions based on rules (e.g., 50% to A, 50% to B) to test features and measure results.  
✅ Benefits: Data-driven decision-making.

```mermaid
flowchart LR
%% --- A/B Testing ---
    subgraph AB["A/B Testing"]
        direction LR
        U1["Users 👥"] --> ST1{"Split Traffic"}
        ST1 -- "50%" --> VA1["Version A 🚀"]
        ST1 -- "50%" --> VB1["Version B ⚡"]
        VA1 --> MA1["Metrics A 📊"]
        VB1 --> MB1["Metrics B 📈"]
        MA1 --> CR1{"Compare"}
        MB1 --> CR1
        CR1 -- "Winner" --> DW1["Deploy 🏆"]
        CR1 -- "No Winner" --> RB1["Rollback 🔄"]
    end

```

---

### 7. [Shadow Indexing Deployment](shadow-indexing-lab/README.md)
Run a new indexing or processing pipeline alongside the old one, compare outputs without affecting production results.  
✅ Benefits: Validate changes in background.

```mermaid
flowchart LR
%% --- Shadow Indexing ---
    subgraph SI["Shadow Indexing"]
        direction LR
        W["Write"] --> IDX["Update Index"]
        W --> LOC["Local Store"]
        W --> REM["Remote Store"]
        RQ["Read"] --> IDX
        IDX -- "Local" --> LOC --> RESP["Return"]
        IDX -- "Remote" --> REM --> RESP
    end
```

---

## Common Tools & Components

- **[Traefik](https://doc.traefik.io/traefik/)** — Reverse proxy, routing, load balancing.
- **Spring Boot** — Backend REST services.
- **Docker Compose** — Multi-service local environments.
- **Kubernetes** — For some strategies (e.g., rolling updates in K8s).

---

## How to Navigate

- Each lab is in its own folder (e.g., `blue-green-lab/`, `canary-lab/`).
- Each lab contains its own `README.md` and runnable code.
- `Docs/` folder contains global documentation and diagrams (`deployment_strategy_diagrams/`).

---

## Running a Lab

```bash
cd blue-green-lab   # or any other strategy folder
docker compose up --build
```

Follow the lab-specific README for verification steps and strategy explanation.

---

## Diagrams

Visual diagrams for all strategies are available in:  
[deployment_strategy_diagrams/](deployment_strategy_diagrams/)

---

## License

This project is licensed under the MIT License.

---

## Derived (Not Implemented) Strategies

The following strategies are **not explicitly implemented** in code here but can be **derived** from the implemented ones:

| Strategy | Derived From | Notes |
|----------|--------------|-------|
| **Recreate** | Blue-Green | Shut down old version before starting the new one. |
| **A/B/N Testing** | AB Testing | Extend AB Testing to multiple variants beyond 2 versions. |
| **Canary + Feature Toggle** | Canary, Feature Toggle | Canary rollout combined with per-user feature flags. |
| **Shadow Launch with Gradual Shift** | Dark Launch, Canary | Shadow traffic followed by gradual live rollout. |

---

## Kubernetes Reference

- **Rolling Update** is implemented in Kubernetes as a reference for any other Kubernetes-based strategy.
- You need **Minikube** or a Kubernetes cluster to run Kubernetes examples locally.
- Setup for local testing:
```bash
minikube start
kubectl apply -f rolling-update/deployment.yaml
```

---

## Repository Structure

```
deployment-lab/
├── ab-testing/             # AB Testing deployment example
├── backend-template-base/  # Common backend code for all strategies
├── blue-green-lab/         # Blue-Green Deployment
├── canary-lab/             # Canary Release
├── dark-launch/            # Dark Launch (Shadow Traffic)
├── feature-toggle-lab/     # Feature Toggle
├── rolling-update/         # Rolling Update (Kubernetes)
├── shadow-indexing-lab/    # Shadow Indexing
```

---

## How to Run (Docker Examples)

Example for Blue-Green:
```bash
cd blue-green-lab
docker compose up -d --build
```

Example for Feature Toggle:
```bash
cd feature-toggle-lab
docker compose up -d --build
```

---

## How to Run (Kubernetes Example - Rolling Update)

```bash
minikube start
kubectl apply -f rolling-update/deployment.yaml
kubectl apply -f rolling-update/service.yaml
```

---

## Diagrams

All diagrams for each strategy are stored in `docs/diagrams`.

---
