ALTER TABLE sys_user_profile
  ADD COLUMN submit_date DATE NULL;

ALTER TABLE sys_user_profile
  ADD COLUMN applicant_name VARCHAR(120) NULL;

ALTER TABLE sys_user_profile
  ADD COLUMN remark VARCHAR(500) NULL;

ALTER TABLE sys_user_profile
  ADD COLUMN certificate_valid_until DATE NULL;

ALTER TABLE sys_user_profile
  ADD COLUMN join_date DATE NULL;
