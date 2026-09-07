ALTER TABLE training_exam_result
  ADD COLUMN submission_request_id VARCHAR(120);

ALTER TABLE training_exam_result
  ADD COLUMN submitted_at TIMESTAMP;

CREATE UNIQUE INDEX uk_training_exam_result_submission_request
  ON training_exam_result (submission_request_id);
