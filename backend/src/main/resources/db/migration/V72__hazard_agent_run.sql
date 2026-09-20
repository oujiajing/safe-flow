CREATE TABLE hazard_agent_run (
  id BIGSERIAL PRIMARY KEY,
  run_id VARCHAR(64) NOT NULL,
  source_module_key VARCHAR(80) NOT NULL,
  source_record_id BIGINT NOT NULL,
  source_record_version INT NOT NULL,
  actor_user_id BIGINT NOT NULL,
  company_id BIGINT NOT NULL,
  department_id BIGINT NULL,
  team_id BIGINT NULL,
  input_text TEXT NULL,
  input_hash VARCHAR(64) NOT NULL,
  attachment_hash_set VARCHAR(128) NOT NULL,
  model VARCHAR(160) NULL,
  prompt_version VARCHAR(80) NULL,
  workflow_version VARCHAR(80) NOT NULL,
  analysis_id VARCHAR(128) NULL,
  assessment_id VARCHAR(128) NULL,
  status VARCHAR(48) NOT NULL,
  knowledge_status VARCHAR(64) NULL,
  trace_id VARCHAR(128) NOT NULL,
  model_output_json TEXT NULL,
  assessment_json TEXT NULL,
  error_code VARCHAR(80) NULL,
  error_message TEXT NULL,
  started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  completed_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted SMALLINT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_hazard_agent_run_id ON hazard_agent_run(run_id);
CREATE INDEX idx_hazard_agent_run_record ON hazard_agent_run(source_module_key, source_record_id, created_at);
CREATE INDEX idx_hazard_agent_run_idempotency ON hazard_agent_run(source_record_id, source_record_version, input_hash, attachment_hash_set);

CREATE TABLE hazard_agent_run_attachment (
  id BIGSERIAL PRIMARY KEY,
  run_id VARCHAR(64) NOT NULL,
  attachment_id BIGINT NOT NULL,
  attachment_sha256 VARCHAR(64) NOT NULL,
  mime_type VARCHAR(160) NULL,
  file_size BIGINT NULL,
  image_index INT NOT NULL,
  analyzed_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX uk_hazard_agent_run_attachment ON hazard_agent_run_attachment(run_id, attachment_id);

CREATE TABLE hazard_agent_decision (
  id BIGSERIAL PRIMARY KEY,
  run_id VARCHAR(64) NOT NULL,
  candidate_id VARCHAR(128) NOT NULL,
  model_output_json TEXT NULL,
  decision VARCHAR(24) NOT NULL,
  edited_hazard_type VARCHAR(255) NULL,
  edited_description TEXT NULL,
  edited_risk_level VARCHAR(48) NULL,
  edited_measures_json TEXT NULL,
  reviewer_note TEXT NULL,
  decided_by BIGINT NOT NULL,
  decided_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  decision_version INT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX uk_hazard_agent_decision_version ON hazard_agent_decision(run_id, candidate_id, decision_version);
CREATE INDEX idx_hazard_agent_decision_run ON hazard_agent_decision(run_id, candidate_id, decision_version);
