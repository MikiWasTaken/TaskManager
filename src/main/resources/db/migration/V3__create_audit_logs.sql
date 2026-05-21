CREATE TABLE audit_logs (
    id          BIGSERIAL PRIMARY KEY,
    action      VARCHAR(100)  NOT NULL,
    performed_by VARCHAR(100),
    entity_type VARCHAR(100),
    entity_id   BIGINT,
    details     TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);