CREATE TABLE risk_four_color_map (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(180) NOT NULL,
  company_id BIGINT NOT NULL,
  background_attachment_id BIGINT NULL,
  remark TEXT NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted SMALLINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_risk_four_color_map_company
  ON risk_four_color_map (company_id, deleted, updated_at DESC, id DESC);
CREATE INDEX idx_risk_four_color_map_name
  ON risk_four_color_map (name, deleted);
