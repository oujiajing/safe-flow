CREATE TABLE sys_check_item_library (
  id BIGSERIAL PRIMARY KEY,
  risk_type VARCHAR(120) NOT NULL,
  check_item TEXT NOT NULL,
  applicable_stage VARCHAR(40) NOT NULL,
  default_check_result VARCHAR(80) NOT NULL DEFAULT '无隐患',
  default_rectification_description TEXT NULL,
  default_follow_up_plan TEXT NULL,
  require_image SMALLINT NOT NULL DEFAULT 0,
  require_video SMALLINT NOT NULL DEFAULT 0,
  sort_order INT NOT NULL DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_sys_check_item_library_stage_status
  ON sys_check_item_library (applicable_stage, status, deleted, sort_order, id);

CREATE TABLE sys_team_check_template (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(180) NOT NULL,
  company_org_id BIGINT NOT NULL,
  department_org_id BIGINT NULL,
  team_org_id BIGINT NULL,
  inspection_stage VARCHAR(40) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  version INT NOT NULL DEFAULT 0,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_sys_team_check_template_scope_stage
  ON sys_team_check_template (
    company_org_id,
    department_org_id,
    team_org_id,
    inspection_stage,
    deleted
  );

CREATE INDEX idx_sys_team_check_template_scope
  ON sys_team_check_template (company_org_id, department_org_id, team_org_id, status, deleted);

CREATE TABLE sys_team_check_template_item (
  id BIGSERIAL PRIMARY KEY,
  template_id BIGINT NOT NULL,
  library_item_id BIGINT NULL,
  risk_type VARCHAR(120) NOT NULL,
  check_item TEXT NOT NULL,
  default_check_result VARCHAR(80) NOT NULL DEFAULT '无隐患',
  default_rectification_description TEXT NULL,
  default_follow_up_plan TEXT NULL,
  require_image SMALLINT NOT NULL DEFAULT 0,
  require_video SMALLINT NOT NULL DEFAULT 0,
  sort_order INT NOT NULL DEFAULT 0,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_sys_team_check_template_item_template
  ON sys_team_check_template_item (template_id, deleted, sort_order, id);

INSERT INTO sys_menu (
  id, parent_id, menu_code, title, route_path, component, icon, permission_code, sort_order, visible, status
) VALUES
  (
    1009,
    1000,
    'SYSTEM_TEAM_CHECK_ITEM_TEMPLATE',
    '班组检查项模板',
    '/system/team-check-item-templates',
    '/system-management/team-check-item-templates/index.vue',
    'lucide:list-checks',
    'SYSTEM_TEAM_CHECK_ITEM_TEMPLATE_VIEW',
    9,
    1,
    'ACTIVE'
  );

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
CROSS JOIN sys_menu m
WHERE r.role_code = 'ADMIN'
  AND m.menu_code IN ('SYSTEM_CONTENT_PROFILE', 'SYSTEM_TEAM_CHECK_ITEM_TEMPLATE')
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_menu rm
    WHERE rm.role_id = r.id
      AND rm.menu_id = m.id
  );
