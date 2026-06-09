.PHONY: up down restart logs infra-up build build-image test kafka-send send-test-notification

COMPOSE = docker compose
SERVICE = notification-service

up:
	$(COMPOSE) up -d

down:
	$(COMPOSE) down -v

restart:
	$(COMPOSE) restart $(SERVICE)

logs:
	$(COMPOSE) logs -f $(SERVICE)

# Start only infrastructure dependencies (Postgres + Kafka), not the service container.
infra-up:
	$(COMPOSE) up -d postgres kafka

build:
	./gradlew build --no-daemon

# Builds notification-service:local OCI image to the local Docker daemon via Jib.
# Requires CODEARTIFACT_AUTH_TOKEN to be set.
build-image:
	./gradlew jibDockerBuild \
		-Djib.to.image=notification-service:local \
		--no-daemon

test:
	./gradlew test --no-daemon

# Publish a minimal domain-events Kafka message for local testing.
kafka-send:
	$(COMPOSE) exec kafka kafka-console-producer \
		--bootstrap-server localhost:9092 \
		--topic domain-events <<< '{"eventType":"TEST_EVENT","tenantId":"00000000-0000-0000-0000-000000000001","payload":"{}"}'

send-test-notification:
	curl -s -X POST http://localhost:8083/notifications \
		-H "Content-Type: application/json" \
		-d '{"tenantId":"00000000-0000-0000-0000-000000000001","userId":"00000000-0000-0000-0000-000000000002","channel":"IN_APP","notificationType":"TEST","payload":"{\"msg\":\"hello\"}"}' \
		| jq .
