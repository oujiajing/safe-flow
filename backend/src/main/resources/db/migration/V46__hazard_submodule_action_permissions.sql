INSERT INTO sys_menu (
  parent_id, menu_code, title, route_path, component, icon, permission_code, sort_order, visible, status, deleted
)
SELECT hazard.id, 'PINGAN_HAZARD_LEGACY_COMPAT_MODULE', '隐患排查兼容权限',
       '/__permissions/pingan/hazard/legacy', 'PERMISSION_GROUP', NULL,
       'PINGAN_HAZARD_LEGACY_COMPAT_ENTRY', 299, 0, 'ACTIVE', 1
FROM sys_menu hazard
WHERE hazard.menu_code = 'PINGAN_HAZARD_MODULE'
  AND NOT EXISTS (
    SELECT 1
    FROM sys_menu existing
    WHERE existing.menu_code = 'PINGAN_HAZARD_LEGACY_COMPAT_MODULE'
       OR existing.permission_code = 'PINGAN_HAZARD_LEGACY_COMPAT_ENTRY'
  );

INSERT INTO sys_menu (
  parent_id, menu_code, title, route_path, component, icon, permission_code, sort_order, visible, status
)
SELECT module.id, item.permission_code, item.title, item.route_path, 'PERMISSION', NULL,
       item.permission_code, item.sort_order, 0, 'ACTIVE'
FROM (
  SELECT 'PINGAN_HAZARD_SAFETY_CHECK_MODULE' AS module_code,
         'PINGAN_HAZARD_SAFETY_CHECK_VIEW' AS permission_code,
         '查看' AS title,
         '/__permissions/pingan/hazard/safety-check/view' AS route_path,
         2311 AS sort_order
  UNION ALL SELECT 'PINGAN_HAZARD_SAFETY_CHECK_MODULE',
         'PINGAN_HAZARD_SAFETY_CHECK_CREATE',
         '新增',
         '/__permissions/pingan/hazard/safety-check/create',
         2312
  UNION ALL SELECT 'PINGAN_HAZARD_SAFETY_CHECK_MODULE',
         'PINGAN_HAZARD_SAFETY_CHECK_DELETE',
         '删除',
         '/__permissions/pingan/hazard/safety-check/delete',
         2313
  UNION ALL SELECT 'PINGAN_HAZARD_SAFETY_CHECK_MODULE',
         'PINGAN_HAZARD_SAFETY_CHECK_VOID',
         '作废',
         '/__permissions/pingan/hazard/safety-check/void',
         2314
  UNION ALL SELECT 'PINGAN_HAZARD_SAFETY_CHECK_MODULE',
         'PINGAN_HAZARD_SAFETY_CHECK_EXPORT',
         '导出',
         '/__permissions/pingan/hazard/safety-check/export',
         2315

  UNION ALL SELECT 'PINGAN_HAZARD_RECTIFICATION_MODULE',
         'PINGAN_HAZARD_RECTIFICATION_VIEW',
         '查看',
         '/__permissions/pingan/hazard/rectification/view',
         2321
  UNION ALL SELECT 'PINGAN_HAZARD_RECTIFICATION_MODULE',
         'PINGAN_HAZARD_RECTIFICATION_CREATE',
         '新增',
         '/__permissions/pingan/hazard/rectification/create',
         2322
  UNION ALL SELECT 'PINGAN_HAZARD_RECTIFICATION_MODULE',
         'PINGAN_HAZARD_RECTIFICATION_REPORT',
         '上报',
         '/__permissions/pingan/hazard/rectification/report',
         2323
  UNION ALL SELECT 'PINGAN_HAZARD_RECTIFICATION_MODULE',
         'PINGAN_HAZARD_RECTIFICATION_RECTIFY',
         '整改',
         '/__permissions/pingan/hazard/rectification/rectify',
         2324
  UNION ALL SELECT 'PINGAN_HAZARD_RECTIFICATION_MODULE',
         'PINGAN_HAZARD_RECTIFICATION_ACCEPT',
         '验收',
         '/__permissions/pingan/hazard/rectification/accept',
         2325
  UNION ALL SELECT 'PINGAN_HAZARD_RECTIFICATION_MODULE',
         'PINGAN_HAZARD_RECTIFICATION_DELETE',
         '删除',
         '/__permissions/pingan/hazard/rectification/delete',
         2326
  UNION ALL SELECT 'PINGAN_HAZARD_RECTIFICATION_MODULE',
         'PINGAN_HAZARD_RECTIFICATION_VOID',
         '作废',
         '/__permissions/pingan/hazard/rectification/void',
         2327
  UNION ALL SELECT 'PINGAN_HAZARD_RECTIFICATION_MODULE',
         'PINGAN_HAZARD_RECTIFICATION_EXPORT',
         '导出',
         '/__permissions/pingan/hazard/rectification/export',
         2328

  UNION ALL SELECT 'PINGAN_HAZARD_QUICK_SHOT_MODULE',
         'PINGAN_HAZARD_QUICK_SHOT_VIEW',
         '查看',
         '/__permissions/pingan/hazard/quick-shot/view',
         2331
  UNION ALL SELECT 'PINGAN_HAZARD_QUICK_SHOT_MODULE',
         'PINGAN_HAZARD_QUICK_SHOT_REPORT',
         '上报',
         '/__permissions/pingan/hazard/quick-shot/report',
         2332
  UNION ALL SELECT 'PINGAN_HAZARD_QUICK_SHOT_MODULE',
         'PINGAN_HAZARD_QUICK_SHOT_REVIEW',
         '审核',
         '/__permissions/pingan/hazard/quick-shot/review',
         2333
  UNION ALL SELECT 'PINGAN_HAZARD_QUICK_SHOT_MODULE',
         'PINGAN_HAZARD_QUICK_SHOT_DELETE',
         '删除',
         '/__permissions/pingan/hazard/quick-shot/delete',
         2334
  UNION ALL SELECT 'PINGAN_HAZARD_QUICK_SHOT_MODULE',
         'PINGAN_HAZARD_QUICK_SHOT_VOID',
         '作废',
         '/__permissions/pingan/hazard/quick-shot/void',
         2335
  UNION ALL SELECT 'PINGAN_HAZARD_QUICK_SHOT_MODULE',
         'PINGAN_HAZARD_QUICK_SHOT_EXPORT',
         '导出',
         '/__permissions/pingan/hazard/quick-shot/export',
         2336

  UNION ALL SELECT 'PINGAN_HAZARD_CURTAIN_WALL_PENALTY_MODULE',
         'PINGAN_HAZARD_CURTAIN_WALL_PENALTY_VIEW',
         '查看',
         '/__permissions/pingan/hazard/curtain-wall-penalty/view',
         2341
  UNION ALL SELECT 'PINGAN_HAZARD_CURTAIN_WALL_PENALTY_MODULE',
         'PINGAN_HAZARD_CURTAIN_WALL_PENALTY_CREATE',
         '新增',
         '/__permissions/pingan/hazard/curtain-wall-penalty/create',
         2342
  UNION ALL SELECT 'PINGAN_HAZARD_CURTAIN_WALL_PENALTY_MODULE',
         'PINGAN_HAZARD_CURTAIN_WALL_PENALTY_DELETE',
         '删除',
         '/__permissions/pingan/hazard/curtain-wall-penalty/delete',
         2343
  UNION ALL SELECT 'PINGAN_HAZARD_CURTAIN_WALL_PENALTY_MODULE',
         'PINGAN_HAZARD_CURTAIN_WALL_PENALTY_VOID',
         '作废',
         '/__permissions/pingan/hazard/curtain-wall-penalty/void',
         2344
  UNION ALL SELECT 'PINGAN_HAZARD_CURTAIN_WALL_PENALTY_MODULE',
         'PINGAN_HAZARD_CURTAIN_WALL_PENALTY_EXPORT',
         '导出',
         '/__permissions/pingan/hazard/curtain-wall-penalty/export',
         2345

  UNION ALL SELECT 'PINGAN_HAZARD_CURTAIN_WALL_ROUTINE_CHECK_MODULE',
         'PINGAN_HAZARD_CURTAIN_WALL_ROUTINE_CHECK_VIEW',
         '查看',
         '/__permissions/pingan/hazard/curtain-wall-routine-check/view',
         2351
  UNION ALL SELECT 'PINGAN_HAZARD_CURTAIN_WALL_ROUTINE_CHECK_MODULE',
         'PINGAN_HAZARD_CURTAIN_WALL_ROUTINE_CHECK_CREATE',
         '新增',
         '/__permissions/pingan/hazard/curtain-wall-routine-check/create',
         2352
  UNION ALL SELECT 'PINGAN_HAZARD_CURTAIN_WALL_ROUTINE_CHECK_MODULE',
         'PINGAN_HAZARD_CURTAIN_WALL_ROUTINE_CHECK_DELETE',
         '删除',
         '/__permissions/pingan/hazard/curtain-wall-routine-check/delete',
         2353
  UNION ALL SELECT 'PINGAN_HAZARD_CURTAIN_WALL_ROUTINE_CHECK_MODULE',
         'PINGAN_HAZARD_CURTAIN_WALL_ROUTINE_CHECK_VOID',
         '作废',
         '/__permissions/pingan/hazard/curtain-wall-routine-check/void',
         2354
  UNION ALL SELECT 'PINGAN_HAZARD_CURTAIN_WALL_ROUTINE_CHECK_MODULE',
         'PINGAN_HAZARD_CURTAIN_WALL_ROUTINE_CHECK_EXPORT',
         '导出',
         '/__permissions/pingan/hazard/curtain-wall-routine-check/export',
         2355
) item
JOIN sys_menu module ON module.menu_code = item.module_code
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_menu existing
  WHERE existing.menu_code = item.permission_code
     OR existing.permission_code = item.permission_code
);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, new_menu.id
FROM sys_role_menu rm
JOIN sys_menu old_menu ON old_menu.id = rm.menu_id
JOIN (
  SELECT 'PINGAN_HAZARD_VIEW' AS old_code, 'PINGAN_HAZARD_SAFETY_CHECK_VIEW' AS new_code
  UNION ALL SELECT 'PINGAN_HAZARD_VIEW', 'PINGAN_HAZARD_RECTIFICATION_VIEW'
  UNION ALL SELECT 'PINGAN_HAZARD_VIEW', 'PINGAN_HAZARD_QUICK_SHOT_VIEW'
  UNION ALL SELECT 'PINGAN_HAZARD_VIEW', 'PINGAN_HAZARD_CURTAIN_WALL_PENALTY_VIEW'
  UNION ALL SELECT 'PINGAN_HAZARD_VIEW', 'PINGAN_HAZARD_CURTAIN_WALL_ROUTINE_CHECK_VIEW'
  UNION ALL SELECT 'PINGAN_HAZARD_REPORT', 'PINGAN_HAZARD_QUICK_SHOT_REPORT'
  UNION ALL SELECT 'PINGAN_HAZARD_REPORT', 'PINGAN_HAZARD_RECTIFICATION_CREATE'
  UNION ALL SELECT 'PINGAN_HAZARD_REPORT', 'PINGAN_HAZARD_RECTIFICATION_REPORT'
  UNION ALL SELECT 'PINGAN_HAZARD_RECTIFICATION', 'PINGAN_HAZARD_RECTIFICATION_RECTIFY'
  UNION ALL SELECT 'PINGAN_HAZARD_RECTIFICATION', 'PINGAN_HAZARD_RECTIFICATION_DELETE'
  UNION ALL SELECT 'PINGAN_HAZARD_ACCEPT', 'PINGAN_HAZARD_RECTIFICATION_ACCEPT'
  UNION ALL SELECT 'PINGAN_HAZARD_ACCEPT', 'PINGAN_HAZARD_QUICK_SHOT_REVIEW'
  UNION ALL SELECT 'PINGAN_HAZARD_CLOSE', 'PINGAN_HAZARD_RECTIFICATION_VOID'
  UNION ALL SELECT 'PINGAN_HAZARD_CLOSE', 'PINGAN_HAZARD_QUICK_SHOT_VOID'
) mapping ON mapping.old_code = old_menu.permission_code
JOIN sys_menu new_menu ON new_menu.permission_code = mapping.new_code
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_role_menu existing
  WHERE existing.role_id = rm.role_id
    AND existing.menu_id = new_menu.id
);

UPDATE sys_menu
SET parent_id = (SELECT id FROM sys_menu WHERE menu_code = 'PINGAN_HAZARD_LEGACY_COMPAT_MODULE'),
    sort_order = CASE permission_code
      WHEN 'PINGAN_HAZARD_VIEW' THEN 2991
      WHEN 'PINGAN_HAZARD_REPORT' THEN 2992
      WHEN 'PINGAN_HAZARD_RECTIFICATION' THEN 2993
      WHEN 'PINGAN_HAZARD_ACCEPT' THEN 2994
      WHEN 'PINGAN_HAZARD_CLOSE' THEN 2995
      ELSE sort_order
    END,
    updated_at = CURRENT_TIMESTAMP
WHERE permission_code IN (
  'PINGAN_HAZARD_VIEW',
  'PINGAN_HAZARD_REPORT',
  'PINGAN_HAZARD_RECTIFICATION',
  'PINGAN_HAZARD_ACCEPT',
  'PINGAN_HAZARD_CLOSE'
);
