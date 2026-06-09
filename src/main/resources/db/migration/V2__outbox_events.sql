-- Transactional outbox table for Debezium CDC.
-- Schema matches platform-libs 0.1.3 OutboxEvent entity exactly.
CREATE TABLE outbox_event (
    outbox_id       UUID PRIMARY KEY,
    tenant_id       UUID NOT NULL,
    aggregate_type  VARCHAR(64) NOT NULL,
    aggregate_id    UUID NOT NULL,
    event_type      VARCHAR(64) NOT NULL,
    payload         JSONB NOT NULL,
    actor_kind      VARCHAR(16) NOT NULL CHECK (actor_kind IN ('HUMAN','AGENT','SYSTEM')),
    event_class     VARCHAR(64) NOT NULL,
    agent_run_id    UUID,
    prompt_version  VARCHAR(128),
    ts              TIMESTAMPTZ NOT NULL DEFAULT now(),
    published       BOOLEAN NOT NULL DEFAULT false,
    seq             BIGSERIAL
);

CREATE INDEX idx_outbox_seq ON outbox_event (seq);
CREATE INDEX idx_outbox_aggregate ON outbox_event (aggregate_type, aggregate_id);
