CREATE TABLE notification (
    id                  UUID PRIMARY KEY,
    tenant_id           UUID NOT NULL,
    user_id             UUID NOT NULL,
    channel             VARCHAR(16) NOT NULL CHECK (channel IN ('EMAIL','PUSH','SMS','IN_APP')),
    notification_type   VARCHAR(128) NOT NULL,
    status              VARCHAR(16) NOT NULL CHECK (status IN ('PENDING','DISPATCHED','DELIVERED','FAILED','READ')),
    payload             JSONB NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    dispatched_at       TIMESTAMPTZ,
    failure_reason      TEXT
);

CREATE INDEX idx_notification_user ON notification (tenant_id, user_id, created_at DESC);
CREATE INDEX idx_notification_status ON notification (status) WHERE status IN ('PENDING','FAILED');
