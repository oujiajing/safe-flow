ALTER TABLE training_exam_task
  ALTER COLUMN department_id DROP NOT NULL;

ALTER TABLE training_exam_task
  ADD COLUMN team_id BIGINT NULL;

CREATE INDEX idx_training_exam_task_scope
  ON training_exam_task (company_id, department_id, team_id, exam_date, deleted);

ALTER TABLE training_exam_question_bank
  ALTER COLUMN department_id DROP NOT NULL;

ALTER TABLE training_exam_question_bank
  ADD COLUMN team_id BIGINT NULL;

CREATE INDEX idx_training_exam_question_bank_scope
  ON training_exam_question_bank (company_id, department_id, team_id, deleted);
