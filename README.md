# notification-service

Multi-channel notification delivery for the Repositree platform. Accepts notification requests via REST API and Kafka domain events, persists them to Postgres, and dispatches across **email, push, SMS, and in-app** channels.

## Architecture

```
                  ┌──────────────────────────────────────┐
                  │         notification-service          │
                  │                                       │
  REST POST ─────►│  NotificationController               │
                  │       │                               │
  Kafka events ──►│  NotificationEventConsumer            │
                  │       │                               │
                  │  NotificationService                  │
                  │       │                               │
                  │  Postgres (JPA + Flyway)              │
                  │  Outbox → Debezium CDC → Kafka        │
                  └──────────────────────────────────────┘
```

- **Spring Boot 3.3** / Java 21 / Kotlin buildSrc conventions
- **Kafka** consumer on `domain-events` topic; producer via transactional outbox
- **PostgreSQL 16** with Flyway migrations
- **Jib** builds OCI images — no local Docker daemon needed for CI
- Deployed as an **ECS Fargate** service (see [infra/terraform](infra/terraform/))

## Local Development

### Prerequisites

- Docker + Docker Compose
- JDK 21 (or `sdk use java 21-tem`)
- `make` (optional but recommended)
- AWS CodeArtifact token (for building from source — see below)

### Run everything with Docker

```bash
# 1. Build the local image (requires CodeArtifact token)
export CODEARTIFACT_AUTH_TOKEN=$(aws codeartifact get-authorization-token \
  --domain repositree --domain-owner 955413563895 \
  --query authorizationToken --output text --region ap-south-1)

make build-image    # builds notification-service:local via Gradle + Jib

# 2. Start all services (Postgres, Kafka, the service)
make up

# 3. Tail logs
make logs

# 4. Hit the API
make send-test-notification
```

The service is available at **http://localhost:8083**.

### Makefile targets

| Target | Description |
|---|---|
| `make up` | Start Postgres + Kafka + service |
| `make down` | Stop and remove containers |
| `make restart` | Restart only the service container |
| `make logs` | Tail service logs |
| `make infra-up` | Start only Postgres + Kafka (deps only) |
| `make build` | Gradle build + tests |
| `make build-image` | Build `notification-service:local` Docker image |
| `make test` | Run tests only |
| `make kafka-send` | Publish a test domain event to Kafka |
| `make send-test-notification` | POST a notification via REST |

### Without the Docker image (run from IDE/Gradle)

```bash
make infra-up          # starts Postgres + Kafka only
./gradlew bootRun      # connects to localhost:5434 + localhost:9093
```

## API

Base URL: `http://localhost:8083`

### Enqueue a notification

```http
POST /notifications
Content-Type: application/json

{
  "tenantId": "00000000-0000-0000-0000-000000000001",
  "userId":   "00000000-0000-0000-0000-000000000002",
  "channel":  "IN_APP",
  "notificationType": "DOCUMENT_READY",
  "payload": "{\"message\": \"Your document is ready\"}"
}
```

Channels: `EMAIL` | `PUSH` | `SMS` | `IN_APP`

### List notifications for a user

```http
GET /notifications?tenantId=<uuid>&userId=<uuid>
```

### Mark as read

```http
PUT /notifications/{id}/read
```

### Health

```http
GET /actuator/health
GET /actuator/health/liveness
GET /actuator/health/readiness
GET /actuator/prometheus
```

## Configuration

All config is driven by environment variables with sane local defaults.

| Variable | Default | Description |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5434/notification_service` | JDBC URL |
| `DB_USER` | `notif_svc` | DB username |
| `DB_PASSWORD` | `secret` | DB password |
| `KAFKA_BROKERS` | `localhost:9092` | Kafka bootstrap servers |
| `KAFKA_TOPIC_DOMAIN_EVENTS` | `domain-events` | Inbound event topic |
| `SERVER_PORT` | `8080` | HTTP port |

## CI/CD

GitHub Actions workflow: [`.github/workflows/ci.yml`](.github/workflows/ci.yml)

**On every push/PR to `main`:**
1. Fetches a CodeArtifact auth token via AWS OIDC (`repositree-codeartifact-consume` role)
2. Runs `./gradlew build` (compile + test)
3. Uploads test reports on failure

**On merge to `main`:**
4. Builds and pushes Docker image to GHCR via `./gradlew jib`

### Fixing CI (OIDC trust policy)

The `repositree-codeartifact-consume` IAM role trust policy must include this repo. Add this to its trust policy conditions:

```json
{
  "StringLike": {
    "token.actions.githubusercontent.com:sub": "repo:RepositreeVentures/notification-service:*"
  }
}
```

Run this once to patch it (requires `iam:UpdateAssumeRolePolicy`):

```bash
./scripts/patch-oidc-trust.sh
```

See [`scripts/patch-oidc-trust.sh`](scripts/patch-oidc-trust.sh) for the full command.

## Infrastructure (AWS)

Terraform lives in [`infra/terraform/`](infra/terraform/). See its [README](infra/terraform/README.md) for full deploy instructions.

**Resources provisioned:**
- ECS Fargate cluster + service + task definition
- RDS PostgreSQL 16 (db.t4g.small, Multi-AZ off for non-prod)
- MSK Kafka cluster (kafka.t3.small, single-AZ for non-prod)
- Application Load Balancer (internal)
- VPC + subnets + security groups (or attaches to existing)
- IAM task execution role + task role
- SSM Parameter Store secrets

**Quick deploy (after CI pushes the image):**

```bash
cd infra/terraform
cp terraform.tfvars.example terraform.tfvars  # fill in values
terraform init
terraform plan
terraform apply
```

## Database Migrations

Managed by Flyway. Migration files in `src/main/resources/db/migration/`.

| Version | Description |
|---|---|
| V1 | `notification` table — core domain |
| V2 | `outbox_event` table — transactional outbox for CDC |

## Dependencies

Internal platform libs (from CodeArtifact `platform-libs` v0.1.3):
- `repositree-common` — shared domain types
- `repositree-tenant` — tenant context propagation
- `repositree-outbox` — transactional outbox support
- `repositree-audit` — audit logging
- `repositree-observability` — Micrometer + tracing config
- `repositree-test` — Testcontainers test base classes
