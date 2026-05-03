CREATE TABLE accounts (
  id         BIGSERIAL PRIMARY KEY,
  name       VARCHAR(255) NOT NULL,
  balance    NUMERIC(19, 2) NOT NULL DEFAULT 0
);

CREATE TABLE transfers (
  id               BIGSERIAL PRIMARY KEY,
  from_account_id  BIGINT NOT NULL REFERENCES accounts (id),
  to_account_id    BIGINT NOT NULL REFERENCES accounts (id),
  amount           NUMERIC(19, 2) NOT NULL,
  created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
  reversed         BOOLEAN NOT NULL DEFAULT FALSE,
  CONSTRAINT transfers_positive_amount CHECK (amount > 0),
  CONSTRAINT transfers_distinct_accounts CHECK (from_account_id <> to_account_id)
);

CREATE TABLE audit_log (
  id               BIGSERIAL PRIMARY KEY,
  operation_type   VARCHAR(64) NOT NULL,
  from_account_id  BIGINT,
  to_account_id    BIGINT,
  amount           NUMERIC(19, 2),
  outcome          VARCHAR(32) NOT NULL,
  detail           VARCHAR(1024),
  transfer_id      BIGINT REFERENCES transfers (id),
  created_at       TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_created_at ON audit_log (created_at DESC);
CREATE INDEX idx_transfers_created_at ON transfers (created_at DESC);
