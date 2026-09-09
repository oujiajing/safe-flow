CREATE TABLE training_exam_task (
  id BIGSERIAL PRIMARY KEY,
  company_id BIGINT NOT NULL,
  department_id BIGINT NOT NULL,
  exam VARCHAR(255) NOT NULL,
  exam_date DATE NOT NULL,
  status VARCHAR(32) NOT NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted SMALLINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_training_exam_task_org_date
  ON training_exam_task (company_id, department_id, exam_date);
CREATE INDEX idx_training_exam_task_status
  ON training_exam_task (status);

CREATE TABLE training_exam_result (
  id BIGSERIAL PRIMARY KEY,
  task_id BIGINT NOT NULL,
  company_id BIGINT NOT NULL,
  department_id BIGINT NOT NULL,
  exam_person_user_id BIGINT NOT NULL,
  exam_person_name VARCHAR(120) NOT NULL,
  score NUMERIC(5, 1) NOT NULL,
  exam_date DATE NOT NULL,
  status VARCHAR(32) NOT NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted SMALLINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_training_exam_result_task
  ON training_exam_result (task_id, deleted);
CREATE INDEX idx_training_exam_result_org_date
  ON training_exam_result (company_id, department_id, exam_date);
CREATE INDEX idx_training_exam_result_status
  ON training_exam_result (status);
