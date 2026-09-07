-- H2 is only the fast test profile and does not support PostgreSQL partial unique indexes.
-- Drop the historical constraints so repeated soft-delete tests do not preserve the old defect.
-- Production uniqueness is enforced and verified by the PostgreSQL-specific V60 migration.
DROP INDEX IF EXISTS uk_sys_org_content_profile_org_deleted;
CREATE INDEX uk_sys_org_content_profile_org_active
  ON sys_org_content_profile (org_id, deleted);

DROP INDEX IF EXISTS uk_sys_team_check_template_scope_stage;
CREATE INDEX uk_sys_team_check_template_scope_stage_active
  ON sys_team_check_template (
    company_org_id,
    department_org_id,
    team_org_id,
    inspection_stage,
    deleted
  );

DROP INDEX IF EXISTS uk_training_safety_learning_code;
CREATE INDEX uk_training_safety_learning_code_active
  ON training_safety_learning_content (code, deleted);

DROP INDEX IF EXISTS uk_hazard_rectification_order_source;
CREATE INDEX uk_hazard_rectification_order_source_active
  ON hazard_rectification_order (source_type, source_module_key, source_record_id, deleted);

DROP INDEX IF EXISTS uk_hazard_rectification_order_no;
CREATE INDEX uk_hazard_rectification_order_no_active
  ON hazard_rectification_order (order_no, deleted);

DROP INDEX IF EXISTS uk_hazard_rectification_order_item_source;
CREATE INDEX uk_hazard_rectification_order_item_source_active
  ON hazard_rectification_order_item (order_id, source_line_id, deleted);
