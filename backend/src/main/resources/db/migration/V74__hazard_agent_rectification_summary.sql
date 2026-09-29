CREATE TABLE hazard_agent_rectification_summary (
  id BIGSERIAL PRIMARY KEY,
  order_id BIGINT NOT NULL,
  order_version INT NOT NULL,
  summary_version INT NOT NULL,
  status VARCHAR(32) NOT NULL,
  source_digest VARCHAR(96) NOT NULL,
  generated_json TEXT NOT NULL,
  confirmed_json TEXT NULL,
  created_by BIGINT NOT NULL,
  confirmed_by BIGINT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  confirmed_at TIMESTAMP NULL,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX uk_hazard_agent_summary_version ON hazard_agent_rectification_summary(order_id, summary_version);
CREATE INDEX idx_hazard_agent_summary_order ON hazard_agent_rectification_summary(order_id, created_at);
