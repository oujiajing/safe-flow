CREATE INDEX idx_three_check_record_module_deleted_date
  ON three_check_record (module_key, deleted, business_date DESC, id DESC);

CREATE INDEX idx_three_check_record_module_deleted_status_date
  ON three_check_record (module_key, deleted, status, business_date DESC, id DESC);

CREATE INDEX idx_three_check_record_module_deleted_company_date
  ON three_check_record (module_key, deleted, company_id, business_date DESC, id DESC);

CREATE INDEX idx_three_check_record_module_deleted_department_date
  ON three_check_record (module_key, deleted, department_id, business_date DESC, id DESC);

CREATE INDEX idx_three_check_record_module_deleted_team_date
  ON three_check_record (module_key, deleted, team_id, business_date DESC, id DESC);

CREATE INDEX idx_three_check_record_module_deleted_owner_date
  ON three_check_record (module_key, deleted, owner_user_id, business_date DESC, id DESC);
