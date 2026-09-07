CREATE TABLE training_exam_question_bank (
  id BIGSERIAL PRIMARY KEY,
  company_id BIGINT NOT NULL,
  department_id BIGINT NOT NULL,
  question_type VARCHAR(64) NOT NULL,
  question_text VARCHAR(1000) NOT NULL,
  selected_option VARCHAR(500),
  all_options VARCHAR(1000),
  answer VARCHAR(500),
  score NUMERIC(5, 1) NOT NULL DEFAULT 0,
  actual_score NUMERIC(5, 1) NOT NULL DEFAULT 0,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted SMALLINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_training_exam_question_bank_org
  ON training_exam_question_bank (company_id, department_id, deleted);

CREATE INDEX idx_training_exam_question_bank_type
  ON training_exam_question_bank (question_type, deleted);
