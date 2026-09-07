INSERT INTO sys_menu (
  parent_id, menu_code, title, route_path, component, icon, permission_code, sort_order, visible, status
)
SELECT hazard.id, item.menu_code, item.title, item.route_path, 'PERMISSION_GROUP', NULL,
       item.permission_code, item.sort_order, 0, 'ACTIVE'
FROM sys_menu hazard
CROSS JOIN (
  SELECT 'PINGAN_HAZARD_SAFETY_CHECK_MODULE' AS menu_code,
         '安全检查' AS title,
         '/__permissions/pingan/hazard/safety-check' AS route_path,
         'PINGAN_HAZARD_SAFETY_CHECK_ENTRY' AS permission_code,
         231 AS sort_order
  UNION ALL SELECT 'PINGAN_HAZARD_RECTIFICATION_MODULE',
         '隐患整改',
         '/__permissions/pingan/hazard/rectification',
         'PINGAN_HAZARD_RECTIFICATION_ENTRY',
         232
  UNION ALL SELECT 'PINGAN_HAZARD_QUICK_SHOT_MODULE',
         '随手拍',
         '/__permissions/pingan/hazard/quick-shot',
         'PINGAN_HAZARD_QUICK_SHOT_ENTRY',
         233
  UNION ALL SELECT 'PINGAN_HAZARD_CURTAIN_WALL_PENALTY_MODULE',
         '幕墙处罚管理',
         '/__permissions/pingan/hazard/curtain-wall-penalty',
         'PINGAN_HAZARD_CURTAIN_WALL_PENALTY_ENTRY',
         234
  UNION ALL SELECT 'PINGAN_HAZARD_CURTAIN_WALL_ROUTINE_CHECK_MODULE',
         '幕墙日周月检',
         '/__permissions/pingan/hazard/curtain-wall-routine-check',
         'PINGAN_HAZARD_CURTAIN_WALL_ROUTINE_CHECK_ENTRY',
         235
) item
WHERE hazard.menu_code = 'PINGAN_HAZARD_MODULE'
  AND NOT EXISTS (
    SELECT 1
    FROM sys_menu existing
    WHERE existing.menu_code = item.menu_code
       OR existing.permission_code = item.permission_code
  );

UPDATE sys_menu
SET parent_id = (SELECT id FROM sys_menu WHERE menu_code = 'PINGAN_HAZARD_MODULE'),
    title = CASE menu_code
      WHEN 'PINGAN_HAZARD_SAFETY_CHECK_MODULE' THEN '安全检查'
      WHEN 'PINGAN_HAZARD_RECTIFICATION_MODULE' THEN '隐患整改'
      WHEN 'PINGAN_HAZARD_QUICK_SHOT_MODULE' THEN '随手拍'
      WHEN 'PINGAN_HAZARD_CURTAIN_WALL_PENALTY_MODULE' THEN '幕墙处罚管理'
      WHEN 'PINGAN_HAZARD_CURTAIN_WALL_ROUTINE_CHECK_MODULE' THEN '幕墙日周月检'
      ELSE title
    END,
    route_path = CASE menu_code
      WHEN 'PINGAN_HAZARD_SAFETY_CHECK_MODULE' THEN '/__permissions/pingan/hazard/safety-check'
      WHEN 'PINGAN_HAZARD_RECTIFICATION_MODULE' THEN '/__permissions/pingan/hazard/rectification'
      WHEN 'PINGAN_HAZARD_QUICK_SHOT_MODULE' THEN '/__permissions/pingan/hazard/quick-shot'
      WHEN 'PINGAN_HAZARD_CURTAIN_WALL_PENALTY_MODULE' THEN '/__permissions/pingan/hazard/curtain-wall-penalty'
      WHEN 'PINGAN_HAZARD_CURTAIN_WALL_ROUTINE_CHECK_MODULE' THEN '/__permissions/pingan/hazard/curtain-wall-routine-check'
      ELSE route_path
    END,
    component = 'PERMISSION_GROUP',
    icon = NULL,
    permission_code = CASE menu_code
      WHEN 'PINGAN_HAZARD_SAFETY_CHECK_MODULE' THEN 'PINGAN_HAZARD_SAFETY_CHECK_ENTRY'
      WHEN 'PINGAN_HAZARD_RECTIFICATION_MODULE' THEN 'PINGAN_HAZARD_RECTIFICATION_ENTRY'
      WHEN 'PINGAN_HAZARD_QUICK_SHOT_MODULE' THEN 'PINGAN_HAZARD_QUICK_SHOT_ENTRY'
      WHEN 'PINGAN_HAZARD_CURTAIN_WALL_PENALTY_MODULE' THEN 'PINGAN_HAZARD_CURTAIN_WALL_PENALTY_ENTRY'
      WHEN 'PINGAN_HAZARD_CURTAIN_WALL_ROUTINE_CHECK_MODULE' THEN 'PINGAN_HAZARD_CURTAIN_WALL_ROUTINE_CHECK_ENTRY'
      ELSE permission_code
    END,
    sort_order = CASE menu_code
      WHEN 'PINGAN_HAZARD_SAFETY_CHECK_MODULE' THEN 231
      WHEN 'PINGAN_HAZARD_RECTIFICATION_MODULE' THEN 232
      WHEN 'PINGAN_HAZARD_QUICK_SHOT_MODULE' THEN 233
      WHEN 'PINGAN_HAZARD_CURTAIN_WALL_PENALTY_MODULE' THEN 234
      WHEN 'PINGAN_HAZARD_CURTAIN_WALL_ROUTINE_CHECK_MODULE' THEN 235
      ELSE sort_order
    END,
    visible = 0,
    status = 'ACTIVE',
    deleted = 0,
    updated_at = CURRENT_TIMESTAMP
WHERE menu_code IN (
  'PINGAN_HAZARD_SAFETY_CHECK_MODULE',
  'PINGAN_HAZARD_RECTIFICATION_MODULE',
  'PINGAN_HAZARD_QUICK_SHOT_MODULE',
  'PINGAN_HAZARD_CURTAIN_WALL_PENALTY_MODULE',
  'PINGAN_HAZARD_CURTAIN_WALL_ROUTINE_CHECK_MODULE'
);

UPDATE sys_menu
SET sort_order = CASE permission_code
      WHEN 'PINGAN_HAZARD_VIEW' THEN 236
      WHEN 'PINGAN_HAZARD_REPORT' THEN 237
      WHEN 'PINGAN_HAZARD_RECTIFICATION' THEN 238
      WHEN 'PINGAN_HAZARD_ACCEPT' THEN 239
      WHEN 'PINGAN_HAZARD_CLOSE' THEN 240
      ELSE sort_order
    END,
    parent_id = (SELECT id FROM sys_menu WHERE menu_code = 'PINGAN_HAZARD_MODULE'),
    visible = 0,
    status = 'ACTIVE',
    deleted = 0,
    updated_at = CURRENT_TIMESTAMP
WHERE permission_code IN (
  'PINGAN_HAZARD_VIEW',
  'PINGAN_HAZARD_REPORT',
  'PINGAN_HAZARD_RECTIFICATION',
  'PINGAN_HAZARD_ACCEPT',
  'PINGAN_HAZARD_CLOSE'
);
