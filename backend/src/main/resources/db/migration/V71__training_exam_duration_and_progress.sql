ALTER TABLE training_exam_task
  ADD COLUMN duration_minutes INTEGER NOT NULL DEFAULT 30;

ALTER TABLE training_exam_result
  ADD COLUMN started_at TIMESTAMP;

ALTER TABLE training_exam_result
  ADD COLUMN current_question_index INTEGER NOT NULL DEFAULT 0;

ALTER TABLE training_exam_result
  ADD COLUMN remaining_seconds INTEGER;
