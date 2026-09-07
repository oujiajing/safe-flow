-- Structure Pingan business RBAC permissions by navigation module.

INSERT INTO sys_menu (
  parent_id, menu_code, title, route_path, component, icon, permission_code, sort_order, visible, status
)
SELECT parent.id, modules.module_code, modules.module_title, modules.route_path, 'PERMISSION_GROUP',
       NULL, modules.entry_code, modules.sort_order, 0, 'ACTIVE'
FROM sys_menu parent
CROSS JOIN (
  SELECT 'PINGAN_MONITOR_CENTER_MODULE' AS module_code, '监控中心' AS module_title,
         '/__permissions/pingan/monitor-center' AS route_path, 'PINGAN_MONITOR_CENTER_ENTRY' AS entry_code, 210 AS sort_order
  UNION ALL SELECT 'PINGAN_THREE_CHECK_MODULE', '一班三查',
         '/__permissions/pingan/three-check', 'PINGAN_THREE_CHECK_ENTRY', 220
  UNION ALL SELECT 'PINGAN_HAZARD_MODULE', '隐患排查',
         '/__permissions/pingan/hazard', 'PINGAN_HAZARD_ENTRY', 230
  UNION ALL SELECT 'PINGAN_RISK_MODULE', '风险管控',
         '/__permissions/pingan/risk', 'PINGAN_RISK_ENTRY', 240
  UNION ALL SELECT 'PINGAN_SPECIAL_WORK_MODULE', '特殊作业',
         '/__permissions/pingan/special-work', 'PINGAN_SPECIAL_WORK_ENTRY', 250
  UNION ALL SELECT 'PINGAN_TRAINING_MODULE', '宣教培训',
         '/__permissions/pingan/training', 'PINGAN_TRAINING_ENTRY', 260
  UNION ALL SELECT 'PINGAN_POINTS_MODULE', '安全积分',
         '/__permissions/pingan/points', 'PINGAN_POINTS_ENTRY', 270
  UNION ALL SELECT 'PINGAN_LEDGER_MODULE', '安全台账',
         '/__permissions/pingan/ledger', 'PINGAN_LEDGER_ENTRY', 280
  UNION ALL SELECT 'PINGAN_DATABASE_MODULE', '数据库',
         '/__permissions/pingan/database', 'PINGAN_DATABASE_ENTRY', 290
) modules
WHERE parent.menu_code = 'PINGAN_PERMISSION_GROUP'
  AND NOT EXISTS (
    SELECT 1
    FROM sys_menu existing
    WHERE existing.menu_code = modules.module_code
       OR existing.permission_code = modules.entry_code
  );

UPDATE sys_menu
SET parent_id = (SELECT id FROM sys_menu WHERE menu_code = 'PINGAN_PERMISSION_GROUP'),
    title = CASE menu_code
      WHEN 'PINGAN_MONITOR_CENTER_MODULE' THEN '监控中心'
      WHEN 'PINGAN_THREE_CHECK_MODULE' THEN '一班三查'
      WHEN 'PINGAN_HAZARD_MODULE' THEN '隐患排查'
      WHEN 'PINGAN_RISK_MODULE' THEN '风险管控'
      WHEN 'PINGAN_SPECIAL_WORK_MODULE' THEN '特殊作业'
      WHEN 'PINGAN_TRAINING_MODULE' THEN '宣教培训'
      WHEN 'PINGAN_POINTS_MODULE' THEN '安全积分'
      WHEN 'PINGAN_LEDGER_MODULE' THEN '安全台账'
      WHEN 'PINGAN_DATABASE_MODULE' THEN '数据库'
      ELSE title
    END,
    component = 'PERMISSION_GROUP',
    icon = NULL,
    visible = 0,
    status = 'ACTIVE',
    deleted = 0,
    updated_at = CURRENT_TIMESTAMP
WHERE menu_code IN (
  'PINGAN_MONITOR_CENTER_MODULE',
  'PINGAN_THREE_CHECK_MODULE',
  'PINGAN_HAZARD_MODULE',
  'PINGAN_RISK_MODULE',
  'PINGAN_SPECIAL_WORK_MODULE',
  'PINGAN_TRAINING_MODULE',
  'PINGAN_POINTS_MODULE',
  'PINGAN_LEDGER_MODULE',
  'PINGAN_DATABASE_MODULE'
);

INSERT INTO sys_menu (
  parent_id, menu_code, title, route_path, component, icon, permission_code, sort_order, visible, status
)
SELECT module.id, item.permission_code, item.title, item.route_path, 'PERMISSION', NULL,
       item.permission_code, item.sort_order, 0, 'ACTIVE'
FROM (
  SELECT 'PINGAN_MONITOR_CENTER_MODULE' AS module_code, 'PINGAN_MONITOR_CENTER_VIEW' AS permission_code,
         '查看' AS title, '/__permissions/pingan/monitor-center-view' AS route_path, 211 AS sort_order
  UNION ALL SELECT 'PINGAN_MONITOR_CENTER_MODULE', 'PINGAN_MONITOR_CENTER_GLOBAL_VIEW',
         '全局查看', '/__permissions/pingan/monitor-center-global-view', 212

  UNION ALL SELECT 'PINGAN_THREE_CHECK_MODULE', 'PINGAN_THREE_CHECK_VIEW',
         '查看', '/__permissions/pingan/three-check-view', 221
  UNION ALL SELECT 'PINGAN_THREE_CHECK_MODULE', 'PINGAN_TEAM_DISPATCH_MANAGE',
         '班组派班管理', '/__permissions/pingan/team-dispatch-manage', 222
  UNION ALL SELECT 'PINGAN_THREE_CHECK_MODULE', 'PINGAN_THREE_CHECK_EXECUTE',
         '执行', '/__permissions/pingan/three-check-execute', 223
  UNION ALL SELECT 'PINGAN_THREE_CHECK_MODULE', 'PINGAN_THREE_CHECK_UPDATE',
         '更新', '/__permissions/pingan/three-check-update', 224
  UNION ALL SELECT 'PINGAN_THREE_CHECK_MODULE', 'PINGAN_THREE_CHECK_WITHDRAW',
         '撤回', '/__permissions/pingan/three-check-withdraw', 225
  UNION ALL SELECT 'PINGAN_THREE_CHECK_MODULE', 'PINGAN_THREE_CHECK_REMIND',
         '催办', '/__permissions/pingan/three-check-remind', 226
  UNION ALL SELECT 'PINGAN_THREE_CHECK_MODULE', 'PINGAN_CURTAIN_WALL_THREE_CHECK_VIEW',
         '幕墙查看', '/__permissions/pingan/curtain-wall-three-check-view', 227
  UNION ALL SELECT 'PINGAN_THREE_CHECK_MODULE', 'PINGAN_GROUP_THREE_CHECK_VIEW',
         '集团查看', '/__permissions/pingan/group-three-check-view', 228
  UNION ALL SELECT 'PINGAN_THREE_CHECK_MODULE', 'PINGAN_KEY_SITE_INSPECTION',
         '重点场所巡检', '/__permissions/pingan/key-site-inspection', 229

  UNION ALL SELECT 'PINGAN_HAZARD_MODULE', 'PINGAN_HAZARD_VIEW',
         '查看', '/__permissions/pingan/hazard-view', 231
  UNION ALL SELECT 'PINGAN_HAZARD_MODULE', 'PINGAN_HAZARD_REPORT',
         '上报', '/__permissions/pingan/hazard-report', 232
  UNION ALL SELECT 'PINGAN_HAZARD_MODULE', 'PINGAN_HAZARD_RECTIFICATION',
         '整改', '/__permissions/pingan/hazard-rectification', 233
  UNION ALL SELECT 'PINGAN_HAZARD_MODULE', 'PINGAN_HAZARD_ACCEPT',
         '验收', '/__permissions/pingan/hazard-accept', 234
  UNION ALL SELECT 'PINGAN_HAZARD_MODULE', 'PINGAN_HAZARD_CLOSE',
         '闭环', '/__permissions/pingan/hazard-close', 235

  UNION ALL SELECT 'PINGAN_RISK_MODULE', 'PINGAN_RISK_VIEW',
         '查看', '/__permissions/pingan/risk-view', 241
  UNION ALL SELECT 'PINGAN_RISK_MODULE', 'PINGAN_RISK_MANAGE',
         '管理', '/__permissions/pingan/risk-manage', 242
  UNION ALL SELECT 'PINGAN_RISK_MODULE', 'PINGAN_RISK_CONFIRM',
         '告知确认', '/__permissions/pingan/risk-confirm', 243

  UNION ALL SELECT 'PINGAN_SPECIAL_WORK_MODULE', 'PINGAN_SPECIAL_WORK_VIEW',
         '查看', '/__permissions/pingan/special-work-view', 251
  UNION ALL SELECT 'PINGAN_SPECIAL_WORK_MODULE', 'PINGAN_SPECIAL_WORK_APPLY',
         '申请', '/__permissions/pingan/special-work-apply', 252
  UNION ALL SELECT 'PINGAN_SPECIAL_WORK_MODULE', 'PINGAN_SPECIAL_WORK_APPROVE',
         '审批', '/__permissions/pingan/special-work-approve', 253
  UNION ALL SELECT 'PINGAN_SPECIAL_WORK_MODULE', 'PINGAN_SPECIAL_WORK_REVIEW',
         '审核', '/__permissions/pingan/special-work-review', 254

  UNION ALL SELECT 'PINGAN_TRAINING_MODULE', 'PINGAN_TRAINING_VIEW',
         '查看', '/__permissions/pingan/training-view', 261
  UNION ALL SELECT 'PINGAN_TRAINING_MODULE', 'PINGAN_TRAINING_STUDY_EXAM',
         '学习考试', '/__permissions/pingan/training-study-exam', 262
  UNION ALL SELECT 'PINGAN_TRAINING_MODULE', 'PINGAN_TRAINING_MANAGE',
         '管理', '/__permissions/pingan/training-manage', 263

  UNION ALL SELECT 'PINGAN_POINTS_MODULE', 'PINGAN_POINTS_VIEW',
         '查看', '/__permissions/pingan/points-view', 271
  UNION ALL SELECT 'PINGAN_POINTS_MODULE', 'PINGAN_POINTS_MANAGE',
         '管理', '/__permissions/pingan/points-manage', 272

  UNION ALL SELECT 'PINGAN_LEDGER_MODULE', 'PINGAN_LEDGER_VIEW',
         '查看', '/__permissions/pingan/ledger-view', 281
  UNION ALL SELECT 'PINGAN_LEDGER_MODULE', 'PINGAN_LEDGER_MANAGE',
         '管理', '/__permissions/pingan/ledger-manage', 282

  UNION ALL SELECT 'PINGAN_DATABASE_MODULE', 'PINGAN_DATABASE_VIEW',
         '查看', '/__permissions/pingan/database-view', 291
  UNION ALL SELECT 'PINGAN_DATABASE_MODULE', 'PINGAN_DATABASE_MANAGE',
         '管理', '/__permissions/pingan/database-manage', 292
) item
JOIN sys_menu module ON module.menu_code = item.module_code
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_menu existing
  WHERE existing.permission_code = item.permission_code
);

UPDATE sys_menu
SET parent_id = (SELECT id FROM sys_menu WHERE menu_code = 'PINGAN_MONITOR_CENTER_MODULE'),
    title = CASE permission_code
      WHEN 'PINGAN_MONITOR_CENTER_VIEW' THEN '查看'
      WHEN 'PINGAN_MONITOR_CENTER_GLOBAL_VIEW' THEN '全局查看'
      ELSE title
    END,
    component = 'PERMISSION',
    icon = NULL,
    sort_order = CASE permission_code
      WHEN 'PINGAN_MONITOR_CENTER_VIEW' THEN 211
      WHEN 'PINGAN_MONITOR_CENTER_GLOBAL_VIEW' THEN 212
      ELSE sort_order
    END,
    visible = 0,
    status = 'ACTIVE',
    deleted = 0,
    updated_at = CURRENT_TIMESTAMP
WHERE permission_code IN ('PINGAN_MONITOR_CENTER_VIEW', 'PINGAN_MONITOR_CENTER_GLOBAL_VIEW');

UPDATE sys_menu
SET parent_id = (SELECT id FROM sys_menu WHERE menu_code = 'PINGAN_THREE_CHECK_MODULE'),
    title = CASE permission_code
      WHEN 'PINGAN_THREE_CHECK_VIEW' THEN '查看'
      WHEN 'PINGAN_TEAM_DISPATCH_MANAGE' THEN '班组派班管理'
      WHEN 'PINGAN_THREE_CHECK_EXECUTE' THEN '执行'
      WHEN 'PINGAN_THREE_CHECK_UPDATE' THEN '更新'
      WHEN 'PINGAN_THREE_CHECK_WITHDRAW' THEN '撤回'
      WHEN 'PINGAN_THREE_CHECK_REMIND' THEN '催办'
      WHEN 'PINGAN_CURTAIN_WALL_THREE_CHECK_VIEW' THEN '幕墙查看'
      WHEN 'PINGAN_GROUP_THREE_CHECK_VIEW' THEN '集团查看'
      WHEN 'PINGAN_KEY_SITE_INSPECTION' THEN '重点场所巡检'
      ELSE title
    END,
    component = 'PERMISSION',
    icon = NULL,
    sort_order = CASE permission_code
      WHEN 'PINGAN_THREE_CHECK_VIEW' THEN 221
      WHEN 'PINGAN_TEAM_DISPATCH_MANAGE' THEN 222
      WHEN 'PINGAN_THREE_CHECK_EXECUTE' THEN 223
      WHEN 'PINGAN_THREE_CHECK_UPDATE' THEN 224
      WHEN 'PINGAN_THREE_CHECK_WITHDRAW' THEN 225
      WHEN 'PINGAN_THREE_CHECK_REMIND' THEN 226
      WHEN 'PINGAN_CURTAIN_WALL_THREE_CHECK_VIEW' THEN 227
      WHEN 'PINGAN_GROUP_THREE_CHECK_VIEW' THEN 228
      WHEN 'PINGAN_KEY_SITE_INSPECTION' THEN 229
      ELSE sort_order
    END,
    visible = 0,
    status = 'ACTIVE',
    deleted = 0,
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

UPDATE sys_menu
SET parent_id = (SELECT id FROM sys_menu WHERE menu_code = 'PINGAN_HAZARD_MODULE'),
    title = CASE permission_code
      WHEN 'PINGAN_HAZARD_VIEW' THEN '查看'
      WHEN 'PINGAN_HAZARD_REPORT' THEN '上报'
      WHEN 'PINGAN_HAZARD_RECTIFICATION' THEN '整改'
      WHEN 'PINGAN_HAZARD_ACCEPT' THEN '验收'
      WHEN 'PINGAN_HAZARD_CLOSE' THEN '闭环'
      ELSE title
    END,
    component = 'PERMISSION',
    icon = NULL,
    sort_order = CASE permission_code
      WHEN 'PINGAN_HAZARD_VIEW' THEN 231
      WHEN 'PINGAN_HAZARD_REPORT' THEN 232
      WHEN 'PINGAN_HAZARD_RECTIFICATION' THEN 233
      WHEN 'PINGAN_HAZARD_ACCEPT' THEN 234
      WHEN 'PINGAN_HAZARD_CLOSE' THEN 235
      ELSE sort_order
    END,
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

UPDATE sys_menu
SET parent_id = (SELECT id FROM sys_menu WHERE menu_code = 'PINGAN_RISK_MODULE'),
    title = CASE permission_code
      WHEN 'PINGAN_RISK_VIEW' THEN '查看'
      WHEN 'PINGAN_RISK_MANAGE' THEN '管理'
      WHEN 'PINGAN_RISK_CONFIRM' THEN '告知确认'
      ELSE title
    END,
    component = 'PERMISSION',
    icon = NULL,
    sort_order = CASE permission_code
      WHEN 'PINGAN_RISK_VIEW' THEN 241
      WHEN 'PINGAN_RISK_MANAGE' THEN 242
      WHEN 'PINGAN_RISK_CONFIRM' THEN 243
      ELSE sort_order
    END,
    visible = 0,
    status = 'ACTIVE',
    deleted = 0,
    updated_at = CURRENT_TIMESTAMP
WHERE permission_code IN ('PINGAN_RISK_VIEW', 'PINGAN_RISK_MANAGE', 'PINGAN_RISK_CONFIRM');

UPDATE sys_menu
SET parent_id = (SELECT id FROM sys_menu WHERE menu_code = 'PINGAN_SPECIAL_WORK_MODULE'),
    title = CASE permission_code
      WHEN 'PINGAN_SPECIAL_WORK_VIEW' THEN '查看'
      WHEN 'PINGAN_SPECIAL_WORK_APPLY' THEN '申请'
      WHEN 'PINGAN_SPECIAL_WORK_APPROVE' THEN '审批'
      WHEN 'PINGAN_SPECIAL_WORK_REVIEW' THEN '审核'
      ELSE title
    END,
    component = 'PERMISSION',
    icon = NULL,
    sort_order = CASE permission_code
      WHEN 'PINGAN_SPECIAL_WORK_VIEW' THEN 251
      WHEN 'PINGAN_SPECIAL_WORK_APPLY' THEN 252
      WHEN 'PINGAN_SPECIAL_WORK_APPROVE' THEN 253
      WHEN 'PINGAN_SPECIAL_WORK_REVIEW' THEN 254
      ELSE sort_order
    END,
    visible = 0,
    status = 'ACTIVE',
    deleted = 0,
    updated_at = CURRENT_TIMESTAMP
WHERE permission_code IN (
  'PINGAN_SPECIAL_WORK_VIEW',
  'PINGAN_SPECIAL_WORK_APPLY',
  'PINGAN_SPECIAL_WORK_APPROVE',
  'PINGAN_SPECIAL_WORK_REVIEW'
);

UPDATE sys_menu
SET parent_id = (SELECT id FROM sys_menu WHERE menu_code = 'PINGAN_TRAINING_MODULE'),
    title = CASE permission_code
      WHEN 'PINGAN_TRAINING_VIEW' THEN '查看'
      WHEN 'PINGAN_TRAINING_STUDY_EXAM' THEN '学习考试'
      WHEN 'PINGAN_TRAINING_MANAGE' THEN '管理'
      ELSE title
    END,
    component = 'PERMISSION',
    icon = NULL,
    sort_order = CASE permission_code
      WHEN 'PINGAN_TRAINING_VIEW' THEN 261
      WHEN 'PINGAN_TRAINING_STUDY_EXAM' THEN 262
      WHEN 'PINGAN_TRAINING_MANAGE' THEN 263
      ELSE sort_order
    END,
    visible = 0,
    status = 'ACTIVE',
    deleted = 0,
    updated_at = CURRENT_TIMESTAMP
WHERE permission_code IN ('PINGAN_TRAINING_VIEW', 'PINGAN_TRAINING_STUDY_EXAM', 'PINGAN_TRAINING_MANAGE');

UPDATE sys_menu
SET parent_id = (SELECT id FROM sys_menu WHERE menu_code = 'PINGAN_POINTS_MODULE'),
    title = CASE permission_code
      WHEN 'PINGAN_POINTS_VIEW' THEN '查看'
      WHEN 'PINGAN_POINTS_MANAGE' THEN '管理'
      ELSE title
    END,
    component = 'PERMISSION',
    icon = NULL,
    sort_order = CASE permission_code
      WHEN 'PINGAN_POINTS_VIEW' THEN 271
      WHEN 'PINGAN_POINTS_MANAGE' THEN 272
      ELSE sort_order
    END,
    visible = 0,
    status = 'ACTIVE',
    deleted = 0,
    updated_at = CURRENT_TIMESTAMP
WHERE permission_code IN ('PINGAN_POINTS_VIEW', 'PINGAN_POINTS_MANAGE');

UPDATE sys_menu
SET parent_id = (SELECT id FROM sys_menu WHERE menu_code = 'PINGAN_LEDGER_MODULE'),
    title = CASE permission_code
      WHEN 'PINGAN_LEDGER_VIEW' THEN '查看'
      WHEN 'PINGAN_LEDGER_MANAGE' THEN '管理'
      ELSE title
    END,
    component = 'PERMISSION',
    icon = NULL,
    sort_order = CASE permission_code
      WHEN 'PINGAN_LEDGER_VIEW' THEN 281
      WHEN 'PINGAN_LEDGER_MANAGE' THEN 282
      ELSE sort_order
    END,
    visible = 0,
    status = 'ACTIVE',
    deleted = 0,
    updated_at = CURRENT_TIMESTAMP
WHERE permission_code IN ('PINGAN_LEDGER_VIEW', 'PINGAN_LEDGER_MANAGE');

UPDATE sys_menu
SET parent_id = (SELECT id FROM sys_menu WHERE menu_code = 'PINGAN_DATABASE_MODULE'),
    title = CASE permission_code
      WHEN 'PINGAN_DATABASE_VIEW' THEN '查看'
      WHEN 'PINGAN_DATABASE_MANAGE' THEN '管理'
      ELSE title
    END,
    component = 'PERMISSION',
    icon = NULL,
    sort_order = CASE permission_code
      WHEN 'PINGAN_DATABASE_VIEW' THEN 291
      WHEN 'PINGAN_DATABASE_MANAGE' THEN 292
      ELSE sort_order
    END,
    visible = 0,
    status = 'ACTIVE',
    deleted = 0,
    updated_at = CURRENT_TIMESTAMP
WHERE permission_code IN ('PINGAN_DATABASE_VIEW', 'PINGAN_DATABASE_MANAGE');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
CROSS JOIN sys_menu m
WHERE r.role_code = 'ADMIN'
  AND m.permission_code LIKE 'PINGAN_%'
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
WHERE r.role_code IN ('COMPANY_LEADER', 'ENTERPRISE_LEADER')
  AND m.permission_code IN (
    'PINGAN_MONITOR_CENTER_ENTRY',
    'PINGAN_MONITOR_CENTER_VIEW',
    'PINGAN_THREE_CHECK_ENTRY',
    'PINGAN_THREE_CHECK_VIEW',
    'PINGAN_HAZARD_ENTRY',
    'PINGAN_HAZARD_VIEW',
    'PINGAN_RISK_ENTRY',
    'PINGAN_RISK_VIEW',
    'PINGAN_SPECIAL_WORK_ENTRY',
    'PINGAN_SPECIAL_WORK_VIEW',
    'PINGAN_TRAINING_ENTRY',
    'PINGAN_TRAINING_VIEW',
    'PINGAN_POINTS_ENTRY',
    'PINGAN_POINTS_VIEW',
    'PINGAN_LEDGER_ENTRY',
    'PINGAN_LEDGER_VIEW'
  )
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
WHERE r.role_code = 'DEPARTMENT_MANAGER'
  AND m.permission_code IN (
    'PINGAN_MONITOR_CENTER_ENTRY',
    'PINGAN_MONITOR_CENTER_VIEW',
    'PINGAN_THREE_CHECK_ENTRY',
    'PINGAN_THREE_CHECK_VIEW',
    'PINGAN_TEAM_DISPATCH_MANAGE',
    'PINGAN_THREE_CHECK_REMIND',
    'PINGAN_HAZARD_ENTRY',
    'PINGAN_HAZARD_VIEW',
    'PINGAN_RISK_ENTRY',
    'PINGAN_RISK_VIEW',
    'PINGAN_SPECIAL_WORK_ENTRY',
    'PINGAN_SPECIAL_WORK_VIEW',
    'PINGAN_TRAINING_ENTRY',
    'PINGAN_TRAINING_VIEW',
    'PINGAN_POINTS_ENTRY',
    'PINGAN_POINTS_VIEW',
    'PINGAN_LEDGER_ENTRY',
    'PINGAN_LEDGER_VIEW',
    'PINGAN_DATABASE_ENTRY',
    'PINGAN_DATABASE_VIEW'
  )
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
WHERE r.role_code = 'WORKSHOP_DIRECTOR'
  AND m.permission_code IN (
    'PINGAN_MONITOR_CENTER_ENTRY',
    'PINGAN_MONITOR_CENTER_VIEW',
    'PINGAN_THREE_CHECK_ENTRY',
    'PINGAN_THREE_CHECK_VIEW',
    'PINGAN_TEAM_DISPATCH_MANAGE',
    'PINGAN_THREE_CHECK_REMIND',
    'PINGAN_THREE_CHECK_WITHDRAW',
    'PINGAN_HAZARD_ENTRY',
    'PINGAN_HAZARD_VIEW',
    'PINGAN_HAZARD_RECTIFICATION',
    'PINGAN_RISK_ENTRY',
    'PINGAN_RISK_VIEW'
  )
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
WHERE r.role_code = 'SAFETY_OFFICER'
  AND m.permission_code IN (
    'PINGAN_MONITOR_CENTER_ENTRY',
    'PINGAN_MONITOR_CENTER_VIEW',
    'PINGAN_THREE_CHECK_ENTRY',
    'PINGAN_THREE_CHECK_VIEW',
    'PINGAN_THREE_CHECK_REMIND',
    'PINGAN_HAZARD_ENTRY',
    'PINGAN_HAZARD_VIEW',
    'PINGAN_HAZARD_RECTIFICATION',
    'PINGAN_HAZARD_ACCEPT',
    'PINGAN_HAZARD_CLOSE',
    'PINGAN_RISK_ENTRY',
    'PINGAN_RISK_VIEW',
    'PINGAN_SPECIAL_WORK_ENTRY',
    'PINGAN_SPECIAL_WORK_REVIEW',
    'PINGAN_TRAINING_ENTRY',
    'PINGAN_TRAINING_MANAGE'
  )
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
WHERE r.role_code = 'TEAM_LEADER'
  AND m.permission_code IN (
    'PINGAN_THREE_CHECK_ENTRY',
    'PINGAN_THREE_CHECK_VIEW',
    'PINGAN_THREE_CHECK_EXECUTE',
    'PINGAN_THREE_CHECK_UPDATE',
    'PINGAN_THREE_CHECK_WITHDRAW',
    'PINGAN_THREE_CHECK_REMIND',
    'PINGAN_HAZARD_ENTRY',
    'PINGAN_HAZARD_VIEW',
    'PINGAN_HAZARD_RECTIFICATION'
  )
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
WHERE r.role_code = 'TEAM_MEMBER'
  AND m.permission_code IN (
    'PINGAN_THREE_CHECK_ENTRY',
    'PINGAN_THREE_CHECK_VIEW',
    'PINGAN_HAZARD_ENTRY',
    'PINGAN_HAZARD_VIEW',
    'PINGAN_HAZARD_REPORT',
    'PINGAN_RISK_ENTRY',
    'PINGAN_RISK_VIEW',
    'PINGAN_RISK_CONFIRM',
    'PINGAN_TRAINING_ENTRY',
    'PINGAN_TRAINING_VIEW',
    'PINGAN_TRAINING_STUDY_EXAM'
  )
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
WHERE r.role_code = 'CURTAIN_WALL_LEADER'
  AND m.permission_code IN (
    'PINGAN_MONITOR_CENTER_ENTRY',
    'PINGAN_MONITOR_CENTER_VIEW',
    'PINGAN_THREE_CHECK_ENTRY',
    'PINGAN_THREE_CHECK_VIEW',
    'PINGAN_CURTAIN_WALL_THREE_CHECK_VIEW',
    'PINGAN_HAZARD_ENTRY',
    'PINGAN_HAZARD_VIEW',
    'PINGAN_RISK_ENTRY',
    'PINGAN_RISK_VIEW'
  )
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
WHERE r.role_code = 'GROUP_LEADER'
  AND m.permission_code IN (
    'PINGAN_MONITOR_CENTER_ENTRY',
    'PINGAN_MONITOR_CENTER_VIEW',
    'PINGAN_MONITOR_CENTER_GLOBAL_VIEW',
    'PINGAN_THREE_CHECK_ENTRY',
    'PINGAN_THREE_CHECK_VIEW',
    'PINGAN_GROUP_THREE_CHECK_VIEW',
    'PINGAN_HAZARD_ENTRY',
    'PINGAN_HAZARD_VIEW',
    'PINGAN_RISK_ENTRY',
    'PINGAN_RISK_VIEW',
    'PINGAN_SPECIAL_WORK_ENTRY',
    'PINGAN_SPECIAL_WORK_VIEW',
    'PINGAN_TRAINING_ENTRY',
    'PINGAN_TRAINING_VIEW',
    'PINGAN_POINTS_ENTRY',
    'PINGAN_POINTS_VIEW',
    'PINGAN_LEDGER_ENTRY',
    'PINGAN_LEDGER_VIEW',
    'PINGAN_DATABASE_ENTRY',
    'PINGAN_DATABASE_VIEW'
  )
  AND m.status = 'ACTIVE'
  AND m.deleted = 0
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_menu rm
    WHERE rm.role_id = r.id
      AND rm.menu_id = m.id
  );
