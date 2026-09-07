INSERT INTO sys_menu (
  parent_id, menu_code, title, route_path, component, icon, permission_code, sort_order, visible, status, deleted
)
SELECT parent.id, item.module_code, item.title, item.route_path, 'PERMISSION_GROUP', NULL,
       item.entry_code, item.sort_order, 0, 'ACTIVE', 0
FROM (
  SELECT 'PINGAN_TEAM_DISPATCH_MODULE' AS module_code,
         '班组派班' AS title,
         '/__permissions/pingan/three-check/team-dispatch' AS route_path,
         'PINGAN_TEAM_DISPATCH_ENTRY' AS entry_code,
         2210 AS sort_order
  UNION ALL SELECT 'PINGAN_ONE_SHIFT_THREE_CHECKS_MODULE',
         '三查',
         '/__permissions/pingan/three-check/one-shift-three-checks',
         'PINGAN_ONE_SHIFT_THREE_CHECKS_ENTRY',
         2220
  UNION ALL SELECT 'PINGAN_CURTAIN_WALL_TEAM_DISPATCH_MODULE',
         '幕墙班组派班',
         '/__permissions/pingan/three-check/curtain-wall-team-dispatch',
         'PINGAN_CURTAIN_WALL_TEAM_DISPATCH_ENTRY',
         2230
  UNION ALL SELECT 'PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_MODULE',
         '班前安全活动',
         '/__permissions/pingan/three-check/pre-shift-safety-activity',
         'PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_ENTRY',
         2240
  UNION ALL SELECT 'PINGAN_KEY_SITES_MODULE',
         '重点场所',
         '/__permissions/pingan/three-check/key-sites',
         'PINGAN_KEY_SITES_ENTRY',
         2250
) item
JOIN sys_menu parent ON parent.menu_code = 'PINGAN_THREE_CHECK_MODULE'
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_menu existing
  WHERE existing.menu_code = item.module_code
     OR existing.permission_code = item.entry_code
);

UPDATE sys_menu
SET parent_id = (SELECT id FROM sys_menu WHERE menu_code = 'PINGAN_THREE_CHECK_MODULE'),
    title = CASE menu_code
      WHEN 'PINGAN_TEAM_DISPATCH_MODULE' THEN '班组派班'
      WHEN 'PINGAN_ONE_SHIFT_THREE_CHECKS_MODULE' THEN '三查'
      WHEN 'PINGAN_CURTAIN_WALL_TEAM_DISPATCH_MODULE' THEN '幕墙班组派班'
      WHEN 'PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_MODULE' THEN '班前安全活动'
      WHEN 'PINGAN_KEY_SITES_MODULE' THEN '重点场所'
      ELSE title
    END,
    component = 'PERMISSION_GROUP',
    icon = NULL,
    visible = 0,
    status = 'ACTIVE',
    deleted = 0,
    sort_order = CASE menu_code
      WHEN 'PINGAN_TEAM_DISPATCH_MODULE' THEN 2210
      WHEN 'PINGAN_ONE_SHIFT_THREE_CHECKS_MODULE' THEN 2220
      WHEN 'PINGAN_CURTAIN_WALL_TEAM_DISPATCH_MODULE' THEN 2230
      WHEN 'PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_MODULE' THEN 2240
      WHEN 'PINGAN_KEY_SITES_MODULE' THEN 2250
      ELSE sort_order
    END,
    updated_at = CURRENT_TIMESTAMP
WHERE menu_code IN (
  'PINGAN_TEAM_DISPATCH_MODULE',
  'PINGAN_ONE_SHIFT_THREE_CHECKS_MODULE',
  'PINGAN_CURTAIN_WALL_TEAM_DISPATCH_MODULE',
  'PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_MODULE',
  'PINGAN_KEY_SITES_MODULE'
);

INSERT INTO sys_menu (
  parent_id, menu_code, title, route_path, component, icon, permission_code, sort_order, visible, status, deleted
)
SELECT module.id, item.permission_code, item.title, item.route_path, 'PERMISSION', NULL,
       item.permission_code, item.sort_order, 0, 'ACTIVE', 0
FROM (
  SELECT 'PINGAN_TEAM_DISPATCH_MODULE' AS module_code,
         'PINGAN_TEAM_DISPATCH_VIEW' AS permission_code,
         '查看' AS title,
         '/__permissions/pingan/three-check/team-dispatch/view' AS route_path,
         2211 AS sort_order
  UNION ALL SELECT 'PINGAN_TEAM_DISPATCH_MODULE',
         'PINGAN_TEAM_DISPATCH_CREATE',
         '新增',
         '/__permissions/pingan/three-check/team-dispatch/create',
         2212
  UNION ALL SELECT 'PINGAN_TEAM_DISPATCH_MODULE',
         'PINGAN_TEAM_DISPATCH_UPDATE',
         '编辑',
         '/__permissions/pingan/three-check/team-dispatch/update',
         2213
  UNION ALL SELECT 'PINGAN_TEAM_DISPATCH_MODULE',
         'PINGAN_TEAM_DISPATCH_SUBMIT',
         '提交/生效',
         '/__permissions/pingan/three-check/team-dispatch/submit',
         2214
  UNION ALL SELECT 'PINGAN_TEAM_DISPATCH_MODULE',
         'PINGAN_TEAM_DISPATCH_VOID',
         '作废',
         '/__permissions/pingan/three-check/team-dispatch/void',
         2215
  UNION ALL SELECT 'PINGAN_TEAM_DISPATCH_MODULE',
         'PINGAN_TEAM_DISPATCH_DELETE',
         '删除',
         '/__permissions/pingan/three-check/team-dispatch/delete',
         2216
  UNION ALL SELECT 'PINGAN_TEAM_DISPATCH_MODULE',
         'PINGAN_TEAM_DISPATCH_REMIND',
         '催办',
         '/__permissions/pingan/three-check/team-dispatch/remind',
         2217
  UNION ALL SELECT 'PINGAN_TEAM_DISPATCH_MODULE',
         'PINGAN_TEAM_DISPATCH_EXPORT',
         '导出',
         '/__permissions/pingan/three-check/team-dispatch/export',
         2218

  UNION ALL SELECT 'PINGAN_ONE_SHIFT_THREE_CHECKS_MODULE',
         'PINGAN_ONE_SHIFT_THREE_CHECKS_VIEW',
         '查看',
         '/__permissions/pingan/three-check/one-shift-three-checks/view',
         2221
  UNION ALL SELECT 'PINGAN_ONE_SHIFT_THREE_CHECKS_MODULE',
         'PINGAN_ONE_SHIFT_THREE_CHECKS_CREATE',
         '新增',
         '/__permissions/pingan/three-check/one-shift-three-checks/create',
         2222
  UNION ALL SELECT 'PINGAN_ONE_SHIFT_THREE_CHECKS_MODULE',
         'PINGAN_ONE_SHIFT_THREE_CHECKS_UPDATE',
         '编辑',
         '/__permissions/pingan/three-check/one-shift-three-checks/update',
         2223
  UNION ALL SELECT 'PINGAN_ONE_SHIFT_THREE_CHECKS_MODULE',
         'PINGAN_ONE_SHIFT_THREE_CHECKS_SUBMIT',
         '提交',
         '/__permissions/pingan/three-check/one-shift-three-checks/submit',
         2224
  UNION ALL SELECT 'PINGAN_ONE_SHIFT_THREE_CHECKS_MODULE',
         'PINGAN_ONE_SHIFT_THREE_CHECKS_VOID',
         '作废',
         '/__permissions/pingan/three-check/one-shift-three-checks/void',
         2225
  UNION ALL SELECT 'PINGAN_ONE_SHIFT_THREE_CHECKS_MODULE',
         'PINGAN_ONE_SHIFT_THREE_CHECKS_DELETE',
         '删除',
         '/__permissions/pingan/three-check/one-shift-three-checks/delete',
         2226
  UNION ALL SELECT 'PINGAN_ONE_SHIFT_THREE_CHECKS_MODULE',
         'PINGAN_ONE_SHIFT_THREE_CHECKS_REMIND',
         '催办',
         '/__permissions/pingan/three-check/one-shift-three-checks/remind',
         2227
  UNION ALL SELECT 'PINGAN_ONE_SHIFT_THREE_CHECKS_MODULE',
         'PINGAN_ONE_SHIFT_THREE_CHECKS_EXPORT',
         '导出',
         '/__permissions/pingan/three-check/one-shift-three-checks/export',
         2228

  UNION ALL SELECT 'PINGAN_CURTAIN_WALL_TEAM_DISPATCH_MODULE',
         'PINGAN_CURTAIN_WALL_TEAM_DISPATCH_VIEW',
         '查看',
         '/__permissions/pingan/three-check/curtain-wall-team-dispatch/view',
         2231
  UNION ALL SELECT 'PINGAN_CURTAIN_WALL_TEAM_DISPATCH_MODULE',
         'PINGAN_CURTAIN_WALL_TEAM_DISPATCH_CREATE',
         '新增',
         '/__permissions/pingan/three-check/curtain-wall-team-dispatch/create',
         2232
  UNION ALL SELECT 'PINGAN_CURTAIN_WALL_TEAM_DISPATCH_MODULE',
         'PINGAN_CURTAIN_WALL_TEAM_DISPATCH_UPDATE',
         '编辑',
         '/__permissions/pingan/three-check/curtain-wall-team-dispatch/update',
         2233
  UNION ALL SELECT 'PINGAN_CURTAIN_WALL_TEAM_DISPATCH_MODULE',
         'PINGAN_CURTAIN_WALL_TEAM_DISPATCH_SUBMIT',
         '提交/生效',
         '/__permissions/pingan/three-check/curtain-wall-team-dispatch/submit',
         2234
  UNION ALL SELECT 'PINGAN_CURTAIN_WALL_TEAM_DISPATCH_MODULE',
         'PINGAN_CURTAIN_WALL_TEAM_DISPATCH_VOID',
         '作废',
         '/__permissions/pingan/three-check/curtain-wall-team-dispatch/void',
         2235
  UNION ALL SELECT 'PINGAN_CURTAIN_WALL_TEAM_DISPATCH_MODULE',
         'PINGAN_CURTAIN_WALL_TEAM_DISPATCH_DELETE',
         '删除',
         '/__permissions/pingan/three-check/curtain-wall-team-dispatch/delete',
         2236
  UNION ALL SELECT 'PINGAN_CURTAIN_WALL_TEAM_DISPATCH_MODULE',
         'PINGAN_CURTAIN_WALL_TEAM_DISPATCH_REMIND',
         '催办',
         '/__permissions/pingan/three-check/curtain-wall-team-dispatch/remind',
         2237
  UNION ALL SELECT 'PINGAN_CURTAIN_WALL_TEAM_DISPATCH_MODULE',
         'PINGAN_CURTAIN_WALL_TEAM_DISPATCH_EXPORT',
         '导出',
         '/__permissions/pingan/three-check/curtain-wall-team-dispatch/export',
         2238

  UNION ALL SELECT 'PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_MODULE',
         'PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_VIEW',
         '查看',
         '/__permissions/pingan/three-check/pre-shift-safety-activity/view',
         2241
  UNION ALL SELECT 'PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_MODULE',
         'PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_CREATE',
         '新增',
         '/__permissions/pingan/three-check/pre-shift-safety-activity/create',
         2242
  UNION ALL SELECT 'PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_MODULE',
         'PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_UPDATE',
         '编辑',
         '/__permissions/pingan/three-check/pre-shift-safety-activity/update',
         2243
  UNION ALL SELECT 'PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_MODULE',
         'PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_SUBMIT',
         '提交',
         '/__permissions/pingan/three-check/pre-shift-safety-activity/submit',
         2244
  UNION ALL SELECT 'PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_MODULE',
         'PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_VOID',
         '作废',
         '/__permissions/pingan/three-check/pre-shift-safety-activity/void',
         2245
  UNION ALL SELECT 'PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_MODULE',
         'PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_DELETE',
         '删除',
         '/__permissions/pingan/three-check/pre-shift-safety-activity/delete',
         2246
  UNION ALL SELECT 'PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_MODULE',
         'PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_REMIND',
         '催办',
         '/__permissions/pingan/three-check/pre-shift-safety-activity/remind',
         2247
  UNION ALL SELECT 'PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_MODULE',
         'PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_EXPORT',
         '导出',
         '/__permissions/pingan/three-check/pre-shift-safety-activity/export',
         2248

  UNION ALL SELECT 'PINGAN_KEY_SITES_MODULE',
         'PINGAN_KEY_SITES_VIEW',
         '查看',
         '/__permissions/pingan/three-check/key-sites/view',
         2251
  UNION ALL SELECT 'PINGAN_KEY_SITES_MODULE',
         'PINGAN_KEY_SITES_CREATE',
         '新增',
         '/__permissions/pingan/three-check/key-sites/create',
         2252
  UNION ALL SELECT 'PINGAN_KEY_SITES_MODULE',
         'PINGAN_KEY_SITES_UPDATE',
         '编辑',
         '/__permissions/pingan/three-check/key-sites/update',
         2253
  UNION ALL SELECT 'PINGAN_KEY_SITES_MODULE',
         'PINGAN_KEY_SITES_SUBMIT',
         '提交巡检',
         '/__permissions/pingan/three-check/key-sites/submit',
         2254
  UNION ALL SELECT 'PINGAN_KEY_SITES_MODULE',
         'PINGAN_KEY_SITES_VOID',
         '作废',
         '/__permissions/pingan/three-check/key-sites/void',
         2255
  UNION ALL SELECT 'PINGAN_KEY_SITES_MODULE',
         'PINGAN_KEY_SITES_DELETE',
         '删除',
         '/__permissions/pingan/three-check/key-sites/delete',
         2256
  UNION ALL SELECT 'PINGAN_KEY_SITES_MODULE',
         'PINGAN_KEY_SITES_REMIND',
         '催办',
         '/__permissions/pingan/three-check/key-sites/remind',
         2257
  UNION ALL SELECT 'PINGAN_KEY_SITES_MODULE',
         'PINGAN_KEY_SITES_EXPORT',
         '导出',
         '/__permissions/pingan/three-check/key-sites/export',
         2258
) item
JOIN sys_menu module ON module.menu_code = item.module_code
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_menu existing
  WHERE existing.menu_code = item.permission_code
     OR existing.permission_code = item.permission_code
);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT role.id, menu.id
FROM (
  SELECT 'COMPANY_LEADER' AS role_code, 'PINGAN_TEAM_DISPATCH_VIEW' AS permission_code
  UNION ALL SELECT 'COMPANY_LEADER', 'PINGAN_ONE_SHIFT_THREE_CHECKS_VIEW'
  UNION ALL SELECT 'COMPANY_LEADER', 'PINGAN_CURTAIN_WALL_TEAM_DISPATCH_VIEW'
  UNION ALL SELECT 'COMPANY_LEADER', 'PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_VIEW'
  UNION ALL SELECT 'COMPANY_LEADER', 'PINGAN_KEY_SITES_VIEW'
  UNION ALL SELECT 'ENTERPRISE_LEADER', 'PINGAN_TEAM_DISPATCH_VIEW'
  UNION ALL SELECT 'ENTERPRISE_LEADER', 'PINGAN_ONE_SHIFT_THREE_CHECKS_VIEW'
  UNION ALL SELECT 'ENTERPRISE_LEADER', 'PINGAN_CURTAIN_WALL_TEAM_DISPATCH_VIEW'
  UNION ALL SELECT 'ENTERPRISE_LEADER', 'PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_VIEW'
  UNION ALL SELECT 'ENTERPRISE_LEADER', 'PINGAN_KEY_SITES_VIEW'
  UNION ALL SELECT 'SAFETY_OFFICER', 'PINGAN_TEAM_DISPATCH_VIEW'
  UNION ALL SELECT 'SAFETY_OFFICER', 'PINGAN_TEAM_DISPATCH_REMIND'
  UNION ALL SELECT 'SAFETY_OFFICER', 'PINGAN_ONE_SHIFT_THREE_CHECKS_VIEW'
  UNION ALL SELECT 'SAFETY_OFFICER', 'PINGAN_ONE_SHIFT_THREE_CHECKS_REMIND'
  UNION ALL SELECT 'SAFETY_OFFICER', 'PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_VIEW'
  UNION ALL SELECT 'SAFETY_OFFICER', 'PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_REMIND'
  UNION ALL SELECT 'SAFETY_OFFICER', 'PINGAN_KEY_SITES_VIEW'
  UNION ALL SELECT 'SAFETY_OFFICER', 'PINGAN_KEY_SITES_REMIND'
  UNION ALL SELECT 'DEPARTMENT_MANAGER', 'PINGAN_TEAM_DISPATCH_VIEW'
  UNION ALL SELECT 'DEPARTMENT_MANAGER', 'PINGAN_TEAM_DISPATCH_CREATE'
  UNION ALL SELECT 'DEPARTMENT_MANAGER', 'PINGAN_TEAM_DISPATCH_UPDATE'
  UNION ALL SELECT 'DEPARTMENT_MANAGER', 'PINGAN_TEAM_DISPATCH_SUBMIT'
  UNION ALL SELECT 'DEPARTMENT_MANAGER', 'PINGAN_TEAM_DISPATCH_VOID'
  UNION ALL SELECT 'DEPARTMENT_MANAGER', 'PINGAN_TEAM_DISPATCH_DELETE'
  UNION ALL SELECT 'DEPARTMENT_MANAGER', 'PINGAN_ONE_SHIFT_THREE_CHECKS_VIEW'
  UNION ALL SELECT 'DEPARTMENT_MANAGER', 'PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_VIEW'
  UNION ALL SELECT 'DEPARTMENT_MANAGER', 'PINGAN_KEY_SITES_VIEW'
  UNION ALL SELECT 'DEPARTMENT_MANAGER', 'PINGAN_TEAM_DISPATCH_REMIND'
  UNION ALL SELECT 'DEPARTMENT_MANAGER', 'PINGAN_ONE_SHIFT_THREE_CHECKS_REMIND'
  UNION ALL SELECT 'DEPARTMENT_MANAGER', 'PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_REMIND'
  UNION ALL SELECT 'DEPARTMENT_MANAGER', 'PINGAN_KEY_SITES_REMIND'
  UNION ALL SELECT 'WORKSHOP_DIRECTOR', 'PINGAN_TEAM_DISPATCH_VIEW'
  UNION ALL SELECT 'WORKSHOP_DIRECTOR', 'PINGAN_TEAM_DISPATCH_CREATE'
  UNION ALL SELECT 'WORKSHOP_DIRECTOR', 'PINGAN_TEAM_DISPATCH_UPDATE'
  UNION ALL SELECT 'WORKSHOP_DIRECTOR', 'PINGAN_TEAM_DISPATCH_SUBMIT'
  UNION ALL SELECT 'WORKSHOP_DIRECTOR', 'PINGAN_TEAM_DISPATCH_VOID'
  UNION ALL SELECT 'WORKSHOP_DIRECTOR', 'PINGAN_TEAM_DISPATCH_DELETE'
  UNION ALL SELECT 'WORKSHOP_DIRECTOR', 'PINGAN_TEAM_DISPATCH_REMIND'
  UNION ALL SELECT 'WORKSHOP_DIRECTOR', 'PINGAN_ONE_SHIFT_THREE_CHECKS_VIEW'
  UNION ALL SELECT 'WORKSHOP_DIRECTOR', 'PINGAN_ONE_SHIFT_THREE_CHECKS_VOID'
  UNION ALL SELECT 'WORKSHOP_DIRECTOR', 'PINGAN_ONE_SHIFT_THREE_CHECKS_REMIND'
  UNION ALL SELECT 'WORKSHOP_DIRECTOR', 'PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_VIEW'
  UNION ALL SELECT 'WORKSHOP_DIRECTOR', 'PINGAN_KEY_SITES_VIEW'
  UNION ALL SELECT 'TEAM_LEADER', 'PINGAN_ONE_SHIFT_THREE_CHECKS_VIEW'
  UNION ALL SELECT 'TEAM_LEADER', 'PINGAN_ONE_SHIFT_THREE_CHECKS_CREATE'
  UNION ALL SELECT 'TEAM_LEADER', 'PINGAN_ONE_SHIFT_THREE_CHECKS_UPDATE'
  UNION ALL SELECT 'TEAM_LEADER', 'PINGAN_ONE_SHIFT_THREE_CHECKS_SUBMIT'
  UNION ALL SELECT 'TEAM_LEADER', 'PINGAN_ONE_SHIFT_THREE_CHECKS_VOID'
  UNION ALL SELECT 'TEAM_LEADER', 'PINGAN_ONE_SHIFT_THREE_CHECKS_REMIND'
  UNION ALL SELECT 'TEAM_LEADER', 'PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_VIEW'
  UNION ALL SELECT 'TEAM_LEADER', 'PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_CREATE'
  UNION ALL SELECT 'TEAM_LEADER', 'PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_UPDATE'
  UNION ALL SELECT 'TEAM_LEADER', 'PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_SUBMIT'
  UNION ALL SELECT 'TEAM_LEADER', 'PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_VOID'
  UNION ALL SELECT 'TEAM_LEADER', 'PINGAN_KEY_SITES_VIEW'
  UNION ALL SELECT 'TEAM_LEADER', 'PINGAN_KEY_SITES_CREATE'
  UNION ALL SELECT 'TEAM_LEADER', 'PINGAN_KEY_SITES_UPDATE'
  UNION ALL SELECT 'TEAM_LEADER', 'PINGAN_KEY_SITES_SUBMIT'
  UNION ALL SELECT 'TEAM_LEADER', 'PINGAN_KEY_SITES_VOID'
  UNION ALL SELECT 'TEAM_MEMBER', 'PINGAN_TEAM_DISPATCH_VIEW'
  UNION ALL SELECT 'TEAM_MEMBER', 'PINGAN_ONE_SHIFT_THREE_CHECKS_VIEW'
  UNION ALL SELECT 'TEAM_MEMBER', 'PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_VIEW'
  UNION ALL SELECT 'TEAM_MEMBER', 'PINGAN_KEY_SITES_VIEW'
  UNION ALL SELECT 'CURTAIN_WALL_LEADER', 'PINGAN_CURTAIN_WALL_TEAM_DISPATCH_VIEW'
  UNION ALL SELECT 'CURTAIN_WALL_LEADER', 'PINGAN_ONE_SHIFT_THREE_CHECKS_VIEW'
  UNION ALL SELECT 'GROUP_LEADER', 'PINGAN_TEAM_DISPATCH_VIEW'
  UNION ALL SELECT 'GROUP_LEADER', 'PINGAN_ONE_SHIFT_THREE_CHECKS_VIEW'
  UNION ALL SELECT 'GROUP_LEADER', 'PINGAN_CURTAIN_WALL_TEAM_DISPATCH_VIEW'
  UNION ALL SELECT 'GROUP_LEADER', 'PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_VIEW'
  UNION ALL SELECT 'GROUP_LEADER', 'PINGAN_KEY_SITES_VIEW'
) grants
JOIN sys_role role ON role.role_code = grants.role_code AND role.deleted = 0
JOIN sys_menu menu ON menu.permission_code = grants.permission_code AND menu.deleted = 0
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_role_menu existing
  WHERE existing.role_id = role.id
    AND existing.menu_id = menu.id
);

UPDATE sys_menu
SET visible = 0,
    status = 'INACTIVE',
    deleted = 1,
    updated_at = CURRENT_TIMESTAMP
WHERE permission_code IN (
  'PINGAN_THREE_CHECK_VIEW',
  'PINGAN_TEAM_DISPATCH_MANAGE',
  'PINGAN_THREE_CHECK_EXECUTE',
  'PINGAN_THREE_CHECK_UPDATE',
  'PINGAN_THREE_CHECK_WITHDRAW',
  'PINGAN_THREE_CHECK_REMIND',
  'PINGAN_CURTAIN_WALL_THREE_CHECK_VIEW',
  'PINGAN_GROUP_THREE_CHECK_VIEW',
  'PINGAN_KEY_SITE_INSPECTION'
);
