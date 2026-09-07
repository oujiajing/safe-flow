ALTER TABLE training_exam_question
  ADD COLUMN options_json TEXT;

ALTER TABLE training_exam_question
  ADD COLUMN answers_json TEXT;

ALTER TABLE training_exam_question
  ADD COLUMN answer_explanation VARCHAR(2000);

ALTER TABLE training_exam_question
  ADD COLUMN case_material TEXT;

ALTER TABLE training_exam_question
  ADD COLUMN children_json TEXT;

ALTER TABLE training_exam_result_question
  ADD COLUMN options_json TEXT;

ALTER TABLE training_exam_result_question
  ADD COLUMN answers_json TEXT;

ALTER TABLE training_exam_result_question
  ADD COLUMN answer_explanation VARCHAR(2000);

ALTER TABLE training_exam_result_question
  ADD COLUMN case_material TEXT;

ALTER TABLE training_exam_result_question
  ADD COLUMN children_json TEXT;
