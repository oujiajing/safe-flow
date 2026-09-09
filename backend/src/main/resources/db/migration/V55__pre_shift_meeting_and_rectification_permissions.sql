INSERT INTO sys_menu (
  parent_id, menu_code, title, route_path, component, icon, permission_code, sort_order, visible, status, deleted
)
SELECT parent.id, 'PINGAN_PRE_SHIFT_MEETING_MODULE', '班前会',
       '/__permissions/pingan/three-check/pre-shift-meeting', 'PERMISSION_GROUP', NULL,
       'PINGAN_PRE_SHIFT_MEETING_ENTRY', 2215, 0, 'ACTIVE', 0
FROM sys_menu parent
WHERE parent.menu_code = 'PINGAN_THREE_CHECK_MODULE'
  AND NOT EXISTS (
    SELECT 1 FROM sys_menu existing
    WHERE existing.menu_code = 'PINGAN_PRE_SHIFT_MEETING_MODULE'
       OR existing.permission_code = 'PINGAN_PRE_SHIFT_MEETING_ENTRY'
  );

INSERT INTO sys_menu (
  parent_id, menu_code, title, route_path, component, icon, permission_code, sort_order, visible, status, deleted
)
SELECT module.id, item.permission_code, item.title, item.route_path, 'PERMISSION', NULL,
       item.permission_code, item.sort_order, 0, 'ACTIVE', 0
FROM (
  SELECT 'PINGAN_PRE_SHIFT_MEETING_VIEW' AS permission_code, '查看' AS title,
         '/__permissions/pingan/three-check/pre-shift-meeting/view' AS route_path, 22151 AS sort_order
  UNION ALL SELECT 'PINGAN_PRE_SHIFT_MEETING_CREATE', '新增',
         '/__permissions/pingan/three-check/pre-shift-meeting/create', 22152
  UNION ALL SELECT 'PINGAN_PRE_SHIFT_MEETING_UPDATE', '编辑',
         '/__permissions/pingan/three-check/pre-shift-meeting/update', 22153
  UNION ALL SELECT 'PINGAN_PRE_SHIFT_MEETING_SUBMIT', '提交',
         '/__permissions/pingan/three-check/pre-shift-meeting/submit', 22154
  UNION ALL SELECT 'PINGAN_PRE_SHIFT_MEETING_VOID', '撤回',
         '/__permissions/pingan/three-check/pre-shift-meeting/void', 22155
  UNION ALL SELECT 'PINGAN_PRE_SHIFT_MEETING_DELETE', '删除',
         '/__permissions/pingan/three-check/pre-shift-meeting/delete', 22156
  UNION ALL SELECT 'PINGAN_PRE_SHIFT_MEETING_REMIND', '催办',
         '/__permissions/pingan/three-check/pre-shift-meeting/remind', 22157
) item
JOIN sys_menu module ON module.menu_code = 'PINGAN_PRE_SHIFT_MEETING_MODULE'
WHERE NOT EXISTS (
  SELECT 1 FROM sys_menu existing
  WHERE existing.menu_code = item.permission_code
     OR existing.permission_code = item.permission_code
);

INSERT INTO sys_menu (
  parent_id, menu_code, title, route_path, component, icon, permission_code, sort_order, visible, status, deleted
)
SELECT module.id, 'PINGAN_ONE_SHIFT_THREE_CHECKS_RECTIFICATION_ORDER_CREATE', '开整改单',
       '/__permissions/pingan/three-check/one-shift-three-checks/rectification-order-create',
       'PERMISSION', NULL, 'PINGAN_ONE_SHIFT_THREE_CHECKS_RECTIFICATION_ORDER_CREATE',
       2229, 0, 'ACTIVE', 0
FROM sys_menu module
WHERE module.menu_code = 'PINGAN_ONE_SHIFT_THREE_CHECKS_MODULE'
  AND NOT EXISTS (
    SELECT 1 FROM sys_menu existing
    WHERE existing.menu_code = 'PINGAN_ONE_SHIFT_THREE_CHECKS_RECTIFICATION_ORDER_CREATE'
       OR existing.permission_code = 'PINGAN_ONE_SHIFT_THREE_CHECKS_RECTIFICATION_ORDER_CREATE'
  );

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, new_menu.id
FROM sys_role_menu rm
JOIN sys_menu old_menu ON old_menu.id = rm.menu_id
JOIN (
  SELECT 'PINGAN_ONE_SHIFT_THREE_CHECKS_ENTRY' AS old_code, 'PINGAN_PRE_SHIFT_MEETING_ENTRY' AS new_code
  UNION ALL SELECT 'PINGAN_ONE_SHIFT_THREE_CHECKS_VIEW', 'PINGAN_PRE_SHIFT_MEETING_VIEW'
  UNION ALL SELECT 'PINGAN_ONE_SHIFT_THREE_CHECKS_CREATE', 'PINGAN_PRE_SHIFT_MEETING_CREATE'
  UNION ALL SELECT 'PINGAN_ONE_SHIFT_THREE_CHECKS_UPDATE', 'PINGAN_PRE_SHIFT_MEETING_UPDATE'
  UNION ALL SELECT 'PINGAN_ONE_SHIFT_THREE_CHECKS_SUBMIT', 'PINGAN_PRE_SHIFT_MEETING_SUBMIT'
  UNION ALL SELECT 'PINGAN_ONE_SHIFT_THREE_CHECKS_VOID', 'PINGAN_PRE_SHIFT_MEETING_VOID'
  UNION ALL SELECT 'PINGAN_ONE_SHIFT_THREE_CHECKS_DELETE', 'PINGAN_PRE_SHIFT_MEETING_DELETE'
  UNION ALL SELECT 'PINGAN_ONE_SHIFT_THREE_CHECKS_REMIND', 'PINGAN_PRE_SHIFT_MEETING_REMIND'
) mapping ON mapping.old_code = old_menu.permission_code
JOIN sys_menu new_menu ON new_menu.permission_code = mapping.new_code
WHERE NOT EXISTS (
  SELECT 1 FROM sys_role_menu existing
  WHERE existing.role_id = rm.role_id AND existing.menu_id = new_menu.id
);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, entry_menu.id
FROM sys_role_menu rm
JOIN sys_menu child ON child.id = rm.menu_id
JOIN sys_menu entry_menu ON entry_menu.permission_code = 'PINGAN_PRE_SHIFT_MEETING_ENTRY'
WHERE child.permission_code LIKE 'PINGAN_PRE_SHIFT_MEETING_%'
  AND child.permission_code <> 'PINGAN_PRE_SHIFT_MEETING_ENTRY'
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu existing
    WHERE existing.role_id = rm.role_id AND existing.menu_id = entry_menu.id
  );

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT role.id, menu.id
FROM sys_role role
JOIN sys_menu menu
  ON menu.permission_code = 'PINGAN_ONE_SHIFT_THREE_CHECKS_RECTIFICATION_ORDER_CREATE'
WHERE role.role_code = 'TEAM_LEADER'
  AND role.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu existing
    WHERE existing.role_id = role.id AND existing.menu_id = menu.id
  );
