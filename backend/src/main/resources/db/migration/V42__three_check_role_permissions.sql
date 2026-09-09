UPDATE sys_role
SET role_name = '幕墙领导',
    data_scope = 'ORG_AND_CHILDREN'
WHERE role_code = 'CURTAIN_WALL_LEADER';

INSERT INTO sys_role (role_code, role_name, data_scope)
SELECT 'CURTAIN_WALL_LEADER', '幕墙领导', 'ORG_AND_CHILDREN'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_role WHERE role_code = 'CURTAIN_WALL_LEADER'
);

UPDATE sys_role
SET role_name = '集团领导',
    data_scope = 'ALL'
WHERE role_code = 'GROUP_LEADER';

INSERT INTO sys_role (role_code, role_name, data_scope)
SELECT 'GROUP_LEADER', '集团领导', 'ALL'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_role WHERE role_code = 'GROUP_LEADER'
);

UPDATE sys_menu
SET parent_id = (SELECT id FROM sys_menu WHERE menu_code = 'PINGAN_PERMISSION_GROUP'),
    title = '幕墙一班三查查看',
    route_path = '/__permissions/pingan/curtain-wall-three-check-view',
    component = 'PERMISSION',
    icon = NULL,
    permission_code = 'PINGAN_CURTAIN_WALL_THREE_CHECK_VIEW',
    sort_order = 216,
    visible = 0,
    status = 'ACTIVE'
WHERE menu_code = 'PINGAN_CURTAIN_WALL_THREE_CHECK_VIEW'
  AND NOT EXISTS (
    SELECT 1
    FROM sys_menu existing
    WHERE existing.permission_code = 'PINGAN_CURTAIN_WALL_THREE_CHECK_VIEW'
      AND existing.menu_code <> 'PINGAN_CURTAIN_WALL_THREE_CHECK_VIEW'
  );

INSERT INTO sys_menu (
  parent_id, menu_code, title, route_path, component, icon, permission_code, sort_order, visible, status
)
SELECT
  parent.id,
  'PINGAN_CURTAIN_WALL_THREE_CHECK_VIEW',
  '幕墙一班三查查看',
  '/__permissions/pingan/curtain-wall-three-check-view',
  'PERMISSION',
  NULL,
  'PINGAN_CURTAIN_WALL_THREE_CHECK_VIEW',
  216,
  0,
  'ACTIVE'
FROM sys_menu parent
WHERE parent.menu_code = 'PINGAN_PERMISSION_GROUP'
  AND NOT EXISTS (
    SELECT 1
    FROM sys_menu
    WHERE menu_code = 'PINGAN_CURTAIN_WALL_THREE_CHECK_VIEW'
       OR permission_code = 'PINGAN_CURTAIN_WALL_THREE_CHECK_VIEW'
  );

UPDATE sys_menu
SET parent_id = (SELECT id FROM sys_menu WHERE menu_code = 'PINGAN_PERMISSION_GROUP'),
    title = '幕墙一班三查查看',
    route_path = '/__permissions/pingan/curtain-wall-three-check-view',
    component = 'PERMISSION',
    icon = NULL,
    sort_order = 216,
    visible = 0,
    status = 'ACTIVE'
WHERE permission_code = 'PINGAN_CURTAIN_WALL_THREE_CHECK_VIEW';

UPDATE sys_menu
SET parent_id = (SELECT id FROM sys_menu WHERE menu_code = 'PINGAN_PERMISSION_GROUP'),
    title = '集团一班三查查看',
    route_path = '/__permissions/pingan/group-three-check-view',
    component = 'PERMISSION',
    icon = NULL,
    permission_code = 'PINGAN_GROUP_THREE_CHECK_VIEW',
    sort_order = 217,
    visible = 0,
    status = 'ACTIVE'
WHERE menu_code = 'PINGAN_GROUP_THREE_CHECK_VIEW'
  AND NOT EXISTS (
    SELECT 1
    FROM sys_menu existing
    WHERE existing.permission_code = 'PINGAN_GROUP_THREE_CHECK_VIEW'
      AND existing.menu_code <> 'PINGAN_GROUP_THREE_CHECK_VIEW'
  );

INSERT INTO sys_menu (
  parent_id, menu_code, title, route_path, component, icon, permission_code, sort_order, visible, status
)
SELECT
  parent.id,
  'PINGAN_GROUP_THREE_CHECK_VIEW',
  '集团一班三查查看',
  '/__permissions/pingan/group-three-check-view',
  'PERMISSION',
  NULL,
  'PINGAN_GROUP_THREE_CHECK_VIEW',
  217,
  0,
  'ACTIVE'
FROM sys_menu parent
WHERE parent.menu_code = 'PINGAN_PERMISSION_GROUP'
  AND NOT EXISTS (
    SELECT 1
    FROM sys_menu
    WHERE menu_code = 'PINGAN_GROUP_THREE_CHECK_VIEW'
       OR permission_code = 'PINGAN_GROUP_THREE_CHECK_VIEW'
  );

UPDATE sys_menu
SET parent_id = (SELECT id FROM sys_menu WHERE menu_code = 'PINGAN_PERMISSION_GROUP'),
    title = '集团一班三查查看',
    route_path = '/__permissions/pingan/group-three-check-view',
    component = 'PERMISSION',
    icon = NULL,
    sort_order = 217,
    visible = 0,
    status = 'ACTIVE'
WHERE permission_code = 'PINGAN_GROUP_THREE_CHECK_VIEW';

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
CROSS JOIN sys_menu m
WHERE r.role_code IN ('DEPARTMENT_MANAGER', 'WORKSHOP_DIRECTOR')
  AND m.permission_code IN ('PINGAN_TEAM_DISPATCH_MANAGE', 'PINGAN_THREE_CHECK_VIEW')
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
WHERE r.role_code = 'TEAM_LEADER'
  AND m.permission_code IN ('PINGAN_THREE_CHECK_EXECUTE', 'PINGAN_THREE_CHECK_WITHDRAW')
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
WHERE r.role_code = 'CURTAIN_WALL_LEADER'
  AND m.permission_code IN ('PINGAN_THREE_CHECK_VIEW', 'PINGAN_CURTAIN_WALL_THREE_CHECK_VIEW')
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
WHERE r.role_code = 'GROUP_LEADER'
  AND m.permission_code IN ('PINGAN_THREE_CHECK_VIEW', 'PINGAN_GROUP_THREE_CHECK_VIEW')
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_menu rm
    WHERE rm.role_id = r.id
      AND rm.menu_id = m.id
  );
