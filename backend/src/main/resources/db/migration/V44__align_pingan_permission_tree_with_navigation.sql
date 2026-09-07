-- Align the hidden Pingan permission tree with the visible business navigation.

UPDATE sys_menu
SET sort_order = -91,
    updated_at = CURRENT_TIMESTAMP
WHERE menu_code = 'PINGAN_PERMISSION_GROUP';

INSERT INTO sys_menu (
  parent_id, menu_code, title, route_path, component, icon, permission_code, sort_order, visible, status
)
SELECT parent.id, 'PINGAN_ORGANIZATION_MODULE', '组织架构',
       '/__permissions/pingan/organization', 'PERMISSION_GROUP', NULL,
       'PINGAN_ORGANIZATION_ENTRY', 220, 0, 'ACTIVE'
FROM sys_menu parent
WHERE parent.menu_code = 'PINGAN_PERMISSION_GROUP'
  AND NOT EXISTS (
    SELECT 1
    FROM sys_menu existing
    WHERE existing.menu_code = 'PINGAN_ORGANIZATION_MODULE'
       OR existing.permission_code = 'PINGAN_ORGANIZATION_ENTRY'
  );

UPDATE sys_menu
SET parent_id = (SELECT id FROM sys_menu WHERE menu_code = 'PINGAN_PERMISSION_GROUP'),
    title = '组织架构',
    route_path = '/__permissions/pingan/organization',
    component = 'PERMISSION_GROUP',
    icon = NULL,
    permission_code = 'PINGAN_ORGANIZATION_ENTRY',
    sort_order = 220,
    visible = 0,
    status = 'ACTIVE',
    deleted = 0,
    updated_at = CURRENT_TIMESTAMP
WHERE menu_code = 'PINGAN_ORGANIZATION_MODULE'
   OR permission_code = 'PINGAN_ORGANIZATION_ENTRY';

INSERT INTO sys_menu (
  parent_id, menu_code, title, route_path, component, icon, permission_code, sort_order, visible, status
)
SELECT module.id, 'PINGAN_ORGANIZATION_VIEW', '查看',
       '/__permissions/pingan/organization-view', 'PERMISSION', NULL,
       'PINGAN_ORGANIZATION_VIEW', 221, 0, 'ACTIVE'
FROM sys_menu module
WHERE module.menu_code = 'PINGAN_ORGANIZATION_MODULE'
  AND NOT EXISTS (
    SELECT 1
    FROM sys_menu existing
    WHERE existing.permission_code = 'PINGAN_ORGANIZATION_VIEW'
  );

UPDATE sys_menu
SET parent_id = (SELECT id FROM sys_menu WHERE menu_code = 'PINGAN_ORGANIZATION_MODULE'),
    title = '查看',
    route_path = '/__permissions/pingan/organization-view',
    component = 'PERMISSION',
    icon = NULL,
    sort_order = 221,
    visible = 0,
    status = 'ACTIVE',
    deleted = 0,
    updated_at = CURRENT_TIMESTAMP
WHERE permission_code = 'PINGAN_ORGANIZATION_VIEW';

UPDATE sys_menu
SET sort_order = CASE menu_code
      WHEN 'PINGAN_MONITOR_CENTER_MODULE' THEN 210
      WHEN 'PINGAN_ORGANIZATION_MODULE' THEN 220
      WHEN 'PINGAN_RISK_MODULE' THEN 230
      WHEN 'PINGAN_THREE_CHECK_MODULE' THEN 240
      WHEN 'PINGAN_HAZARD_MODULE' THEN 250
      WHEN 'PINGAN_SPECIAL_WORK_MODULE' THEN 260
      WHEN 'PINGAN_TRAINING_MODULE' THEN 270
      WHEN 'PINGAN_POINTS_MODULE' THEN 280
      WHEN 'PINGAN_LEDGER_MODULE' THEN 290
      WHEN 'PINGAN_DATABASE_MODULE' THEN 300
      ELSE sort_order
    END,
    updated_at = CURRENT_TIMESTAMP
WHERE menu_code IN (
  'PINGAN_MONITOR_CENTER_MODULE',
  'PINGAN_ORGANIZATION_MODULE',
  'PINGAN_RISK_MODULE',
  'PINGAN_THREE_CHECK_MODULE',
  'PINGAN_HAZARD_MODULE',
  'PINGAN_SPECIAL_WORK_MODULE',
  'PINGAN_TRAINING_MODULE',
  'PINGAN_POINTS_MODULE',
  'PINGAN_LEDGER_MODULE',
  'PINGAN_DATABASE_MODULE'
);

UPDATE sys_menu
SET status = 'INACTIVE',
    deleted = 1,
    updated_at = CURRENT_TIMESTAMP
WHERE permission_code IN ('PINGAN_REPORT_VIEW', 'PINGAN_GLOBAL_SAFETY_INSPECTION');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
CROSS JOIN sys_menu m
WHERE r.role_code = 'ADMIN'
  AND m.permission_code IN ('PINGAN_ORGANIZATION_ENTRY', 'PINGAN_ORGANIZATION_VIEW')
  AND m.status = 'ACTIVE'
  AND m.deleted = 0
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_menu rm
    WHERE rm.role_id = r.id
      AND rm.menu_id = m.id
  );

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
CROSS JOIN sys_menu m
WHERE r.role_code IN (
    'COMPANY_LEADER',
    'ENTERPRISE_LEADER',
    'DEPARTMENT_MANAGER',
    'WORKSHOP_DIRECTOR',
    'SAFETY_OFFICER',
    'GROUP_LEADER',
    'CURTAIN_WALL_LEADER'
  )
  AND m.permission_code IN ('PINGAN_ORGANIZATION_ENTRY', 'PINGAN_ORGANIZATION_VIEW')
  AND m.status = 'ACTIVE'
  AND m.deleted = 0
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_menu rm
    WHERE rm.role_id = r.id
      AND rm.menu_id = m.id
  );
