CREATE TABLE hazard_rectification_order (
  id BIGSERIAL PRIMARY KEY,
  order_no VARCHAR(40) NOT NULL,
  source_type VARCHAR(40) NOT NULL,
  source_module_key VARCHAR(80) NOT NULL,
  source_record_id BIGINT NOT NULL,
  source_record_no VARCHAR(80) NULL,
  root_dispatch_record_id BIGINT NULL,
  company_id BIGINT NOT NULL,
  department_id BIGINT NOT NULL,
  team_id BIGINT NOT NULL,
  business_date DATE NOT NULL,
  hazard_count INT NOT NULL DEFAULT 0,
  status VARCHAR(40) NOT NULL,
  rectification_department_id BIGINT NULL,
  rectification_responsible_user_id BIGINT NULL,
  rectification_requirement TEXT NULL,
  rectification_deadline TIMESTAMP NULL,
  issued_by BIGINT NULL,
  issued_at TIMESTAMP NULL,
  rectified_by BIGINT NULL,
  rectified_at TIMESTAMP NULL,
  rectification_description TEXT NULL,
  acceptance_user_id BIGINT NULL,
  acceptance_department_id BIGINT NULL,
  acceptance_at TIMESTAMP NULL,
  acceptance_result VARCHAR(40) NULL,
  acceptance_remark TEXT NULL,
  closed_at TIMESTAMP NULL,
  version INT NOT NULL DEFAULT 0,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_hazard_rectification_order_source
  ON hazard_rectification_order (source_type, source_module_key, source_record_id, deleted);

CREATE UNIQUE INDEX uk_hazard_rectification_order_no
  ON hazard_rectification_order (order_no, deleted);

CREATE INDEX idx_hazard_rectification_order_query
  ON hazard_rectification_order (
    source_type,
    source_module_key,
    root_dispatch_record_id,
    status,
    company_id,
    department_id,
    team_id,
    business_date,
    deleted
  );

CREATE TABLE hazard_rectification_order_item (
  id BIGSERIAL PRIMARY KEY,
  order_id BIGINT NOT NULL,
  source_line_id VARCHAR(120) NOT NULL,
  source_line_index INT NOT NULL,
  library_item_id BIGINT NULL,
  risk_type VARCHAR(120) NULL,
  check_item TEXT NOT NULL,
  hazard_description TEXT NULL,
  before_photo TEXT NULL,
  before_video TEXT NULL,
  default_follow_up_plan TEXT NULL,
  source_snapshot_json TEXT NULL,
  rectification_status VARCHAR(40) NOT NULL,
  closed_at TIMESTAMP NULL,
  sort_order INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_hazard_rectification_order_item_order
  ON hazard_rectification_order_item (order_id, deleted, sort_order, id);

CREATE UNIQUE INDEX uk_hazard_rectification_order_item_source
  ON hazard_rectification_order_item (order_id, source_line_id, deleted);

CREATE TABLE hazard_rectification_flow_log (
  id BIGSERIAL PRIMARY KEY,
  order_id BIGINT NOT NULL,
  from_status VARCHAR(40) NULL,
  to_status VARCHAR(40) NOT NULL,
  action VARCHAR(60) NOT NULL,
  action_label VARCHAR(80) NOT NULL,
  operator_id BIGINT NULL,
  remark TEXT NULL,
  payload_json TEXT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_hazard_rectification_flow_log_order
  ON hazard_rectification_flow_log (order_id, created_at, id);
