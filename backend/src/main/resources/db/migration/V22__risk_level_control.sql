CREATE TABLE risk_control_library (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(180) NOT NULL,
  company_id BIGINT NOT NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted SMALLINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_risk_control_library_company
  ON risk_control_library (company_id, deleted, updated_at DESC, id DESC);
CREATE INDEX idx_risk_control_library_name
  ON risk_control_library (name, deleted);

CREATE TABLE risk_control_hazard (
  id BIGSERIAL PRIMARY KEY,
  library_id BIGINT NOT NULL,
  company_id BIGINT NOT NULL,
  risk_point VARCHAR(255) NOT NULL,
  danger_source VARCHAR(255) NOT NULL,
  risk_influence_factors TEXT NULL,
  accident_type VARCHAR(180) NULL,
  likelihood VARCHAR(80) NULL,
  exposure_frequency VARCHAR(80) NULL,
  consequence VARCHAR(80) NULL,
  risk_value VARCHAR(80) NULL,
  risk_level VARCHAR(120) NULL,
  engineering_measures TEXT NULL,
  management_measures TEXT NULL,
  emergency_measures TEXT NULL,
  superior_responsible_person VARCHAR(180) NULL,
  responsible_department VARCHAR(180) NULL,
  responsible_contact VARCHAR(180) NULL,
  possible_hazard TEXT NULL,
  rectification_measures TEXT NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted SMALLINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_risk_control_hazard_library
  ON risk_control_hazard (library_id, deleted, id);
CREATE INDEX idx_risk_control_hazard_company
  ON risk_control_hazard (company_id, deleted, id);
