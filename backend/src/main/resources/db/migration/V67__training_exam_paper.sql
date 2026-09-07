CREATE TABLE training_exam_paper (
  id BIGSERIAL PRIMARY KEY,
  company_id BIGINT NOT NULL,
  department_id BIGINT NOT NULL,
  paper_name VARCHAR(255) NOT NULL,
  description VARCHAR(1000),
  questions_json TEXT NOT NULL,
  question_count INTEGER NOT NULL DEFAULT 0,
  total_score NUMERIC(8, 1) NOT NULL DEFAULT 0,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted SMALLINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_training_exam_paper_org
  ON training_exam_paper (company_id, department_id, deleted);

CREATE INDEX idx_training_exam_paper_updated
  ON training_exam_paper (updated_at, deleted);
