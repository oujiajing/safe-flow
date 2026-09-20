CREATE TABLE hazard_agent_rectification_apply (
  id BIGSERIAL PRIMARY KEY,
  run_id VARCHAR(64) NOT NULL,
  assessment_id VARCHAR(128) NOT NULL,
  source_module_key VARCHAR(80) NOT NULL,
  source_record_id BIGINT NOT NULL,
  source_record_version INT NOT NULL,
  decision_set_version INT NOT NULL,
  preview_digest VARCHAR(80) NOT NULL,
  idempotency_key VARCHAR(128) NOT NULL,
  status VARCHAR(48) NOT NULL,
  order_id BIGINT NULL,
  snapshot_json TEXT NOT NULL,
  actor_user_id BIGINT NOT NULL,
  error_code VARCHAR(80) NULL,
  error_message TEXT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  applied_at TIMESTAMP NULL,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX uk_hazard_agent_rectification_apply_run ON hazard_agent_rectification_apply(run_id);
CREATE UNIQUE INDEX uk_hazard_agent_rectification_apply_idempotency ON hazard_agent_rectification_apply(run_id, idempotency_key);
CREATE INDEX idx_hazard_agent_rectification_apply_source ON hazard_agent_rectification_apply(source_module_key, source_record_id, created_at);
