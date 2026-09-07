ALTER TABLE training_exam_question_bank
  ADD COLUMN options_json TEXT;

ALTER TABLE training_exam_question_bank
  ADD COLUMN answers_json TEXT;

ALTER TABLE training_exam_question_bank
  ADD COLUMN answer_explanation VARCHAR(2000);

ALTER TABLE training_exam_question_bank
  ADD COLUMN case_material TEXT;

ALTER TABLE training_exam_question_bank
  ADD COLUMN children_json TEXT;
