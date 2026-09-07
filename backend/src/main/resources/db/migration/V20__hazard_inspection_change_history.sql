CREATE TABLE biz_change_history (
  id BIGSERIAL PRIMARY KEY,
  biz_type VARCHAR(64) NOT NULL,
  biz_id BIGINT NOT NULL,
  record_no VARCHAR(80) NOT NULL,
  module_key VARCHAR(80) NOT NULL,
  version INT NOT NULL,
  action VARCHAR(64) NOT NULL,
  field_key VARCHAR(120) NOT NULL,
  field_label VARCHAR(120) NOT NULL,
  before_value TEXT NULL,
  after_value TEXT NULL,
  value_type VARCHAR(32) NULL,
  operator_id BIGINT NULL,
  remark VARCHAR(255) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_biz_change_history_target ON biz_change_history (biz_type, biz_id, created_at);
CREATE INDEX idx_biz_change_history_record ON biz_change_history (module_key, biz_id, version);
