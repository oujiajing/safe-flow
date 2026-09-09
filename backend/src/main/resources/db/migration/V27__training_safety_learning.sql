CREATE TABLE training_safety_learning_content (
  id BIGSERIAL PRIMARY KEY,
  company_id BIGINT NOT NULL,
  category VARCHAR(120) NOT NULL,
  title VARCHAR(255) NOT NULL,
  content TEXT,
  cover_image VARCHAR(500),
  video VARCHAR(500),
  learning_date DATE NOT NULL,
  duration_text VARCHAR(120),
  code VARCHAR(64) NOT NULL,
  attachment_id BIGINT NULL,
  attachment_text VARCHAR(500),
  html_extract TEXT,
  draft VARCHAR(32),
  status VARCHAR(32) NOT NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted SMALLINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_training_safety_learning_code
  ON training_safety_learning_content (code, deleted);

CREATE INDEX idx_training_safety_learning_company_date
  ON training_safety_learning_content (company_id, learning_date);

CREATE INDEX idx_training_safety_learning_category
  ON training_safety_learning_content (category);

CREATE INDEX idx_training_safety_learning_status
  ON training_safety_learning_content (status);
