DROP INDEX IF EXISTS uk_sys_org_content_profile_org_deleted;
CREATE UNIQUE INDEX uk_sys_org_content_profile_org_active
  ON sys_org_content_profile (org_id)
  WHERE deleted = 0;

DROP INDEX IF EXISTS uk_sys_team_check_template_scope_stage;
CREATE UNIQUE INDEX uk_sys_team_check_template_scope_stage_active
  ON sys_team_check_template (company_org_id, department_org_id, team_org_id, inspection_stage)
  NULLS NOT DISTINCT
  WHERE deleted = 0;

DROP INDEX IF EXISTS uk_training_safety_learning_code;
CREATE UNIQUE INDEX uk_training_safety_learning_code_active
  ON training_safety_learning_content (code)
  WHERE deleted = 0;

DROP INDEX IF EXISTS uk_hazard_rectification_order_source;
CREATE UNIQUE INDEX uk_hazard_rectification_order_source_active
  ON hazard_rectification_order (source_type, source_module_key, source_record_id)
  WHERE deleted = 0;

DROP INDEX IF EXISTS uk_hazard_rectification_order_no;
CREATE UNIQUE INDEX uk_hazard_rectification_order_no_active
  ON hazard_rectification_order (order_no)
  WHERE deleted = 0;

DROP INDEX IF EXISTS uk_hazard_rectification_order_item_source;
CREATE UNIQUE INDEX uk_hazard_rectification_order_item_source_active
  ON hazard_rectification_order_item (order_id, source_line_id)
  WHERE deleted = 0;
