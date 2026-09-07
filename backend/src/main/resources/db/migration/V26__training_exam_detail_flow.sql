ALTER TABLE training_exam_task
  ADD COLUMN code VARCHAR(64);

ALTER TABLE training_exam_task
  ADD COLUMN remark VARCHAR(500);

ALTER TABLE training_exam_result
  ADD COLUMN code VARCHAR(64);

ALTER TABLE training_exam_result
  ADD COLUMN remark VARCHAR(500);

CREATE TABLE training_exam_question (
  id BIGSERIAL PRIMARY KEY,
  task_id BIGINT NOT NULL,
  question_type VARCHAR(64) NOT NULL,
  question_text VARCHAR(1000) NOT NULL,
  selected_option VARCHAR(500),
  all_options VARCHAR(1000),
  answer VARCHAR(500),
  score NUMERIC(5, 1) NOT NULL DEFAULT 0,
  actual_score NUMERIC(5, 1) NOT NULL DEFAULT 0,
  sort_order INTEGER NOT NULL DEFAULT 0,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted SMALLINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_training_exam_question_task
  ON training_exam_question (task_id, deleted, sort_order);

CREATE TABLE training_exam_result_question (
  id BIGSERIAL PRIMARY KEY,
  result_id BIGINT NOT NULL,
  task_question_id BIGINT NULL,
  question_type VARCHAR(64) NOT NULL,
  question_text VARCHAR(1000) NOT NULL,
  selected_option VARCHAR(500),
  all_options VARCHAR(1000),
  answer VARCHAR(500),
  score NUMERIC(5, 1) NOT NULL DEFAULT 0,
  actual_score NUMERIC(5, 1) NOT NULL DEFAULT 0,
  sort_order INTEGER NOT NULL DEFAULT 0,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted SMALLINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_training_exam_result_question_result
  ON training_exam_result_question (result_id, deleted, sort_order);
