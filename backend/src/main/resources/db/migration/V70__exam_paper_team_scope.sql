ALTER TABLE training_exam_paper
  ADD COLUMN team_id BIGINT NULL;

CREATE INDEX idx_training_exam_paper_scope
  ON training_exam_paper (company_id, department_id, team_id, deleted);
