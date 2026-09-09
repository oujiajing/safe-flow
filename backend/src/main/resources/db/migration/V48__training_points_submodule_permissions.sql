INSERT INTO sys_menu (
  parent_id, menu_code, title, route_path, component, icon, permission_code, sort_order, visible, status, deleted
)
SELECT parent.id, item.module_code, item.title, item.route_path, 'PERMISSION_GROUP', NULL,
       item.entry_code, item.sort_order, 0, 'ACTIVE', 0
FROM (
  SELECT 'PINGAN_TRAINING_EXAM_TASKS_MODULE' AS module_code,
         '考试任务' AS title,
         '/__permissions/pingan/training/exam-tasks' AS route_path,
         'PINGAN_TRAINING_EXAM_TASKS_ENTRY' AS entry_code,
         2710 AS sort_order
  UNION ALL SELECT 'PINGAN_TRAINING_EXAM_RESULTS_MODULE',
         '考试成绩',
         '/__permissions/pingan/training/exam-results',
         'PINGAN_TRAINING_EXAM_RESULTS_ENTRY',
         2720
  UNION ALL SELECT 'PINGAN_TRAINING_SAFETY_LEARNING_MODULE',
         '安全学习',
         '/__permissions/pingan/training/safety-learning',
         'PINGAN_TRAINING_SAFETY_LEARNING_ENTRY',
         2730
) item
JOIN sys_menu parent ON parent.menu_code = 'PINGAN_TRAINING_MODULE'
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_menu existing
  WHERE existing.menu_code = item.module_code
     OR existing.permission_code = item.entry_code
);

INSERT INTO sys_menu (
  parent_id, menu_code, title, route_path, component, icon, permission_code, sort_order, visible, status, deleted
)
SELECT parent.id, item.module_code, item.title, item.route_path, 'PERMISSION_GROUP', NULL,
       item.entry_code, item.sort_order, 0, 'ACTIVE', 0
FROM (
  SELECT 'PINGAN_POINTS_FLOW_MODULE' AS module_code,
         '积分流水' AS title,
         '/__permissions/pingan/points/flow' AS route_path,
         'PINGAN_POINTS_FLOW_ENTRY' AS entry_code,
         2810 AS sort_order
  UNION ALL SELECT 'PINGAN_POINTS_RANKING_MODULE',
         '积分榜单',
         '/__permissions/pingan/points/ranking',
         'PINGAN_POINTS_RANKING_ENTRY',
         2820
) item
JOIN sys_menu parent ON parent.menu_code = 'PINGAN_POINTS_MODULE'
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_menu existing
  WHERE existing.menu_code = item.module_code
     OR existing.permission_code = item.entry_code
);

UPDATE sys_menu
SET parent_id = (SELECT id FROM sys_menu WHERE menu_code = 'PINGAN_TRAINING_MODULE'),
    title = CASE menu_code
      WHEN 'PINGAN_TRAINING_EXAM_TASKS_MODULE' THEN '考试任务'
      WHEN 'PINGAN_TRAINING_EXAM_RESULTS_MODULE' THEN '考试成绩'
      WHEN 'PINGAN_TRAINING_SAFETY_LEARNING_MODULE' THEN '安全学习'
      ELSE title
    END,
    component = 'PERMISSION_GROUP',
    icon = NULL,
    visible = 0,
    status = 'ACTIVE',
    deleted = 0,
    sort_order = CASE menu_code
      WHEN 'PINGAN_TRAINING_EXAM_TASKS_MODULE' THEN 2710
      WHEN 'PINGAN_TRAINING_EXAM_RESULTS_MODULE' THEN 2720
      WHEN 'PINGAN_TRAINING_SAFETY_LEARNING_MODULE' THEN 2730
      ELSE sort_order
    END,
    updated_at = CURRENT_TIMESTAMP
WHERE menu_code IN (
  'PINGAN_TRAINING_EXAM_TASKS_MODULE',
  'PINGAN_TRAINING_EXAM_RESULTS_MODULE',
  'PINGAN_TRAINING_SAFETY_LEARNING_MODULE'
);

UPDATE sys_menu
SET parent_id = (SELECT id FROM sys_menu WHERE menu_code = 'PINGAN_POINTS_MODULE'),
    title = CASE menu_code
      WHEN 'PINGAN_POINTS_FLOW_MODULE' THEN '积分流水'
      WHEN 'PINGAN_POINTS_RANKING_MODULE' THEN '积分榜单'
      ELSE title
    END,
    component = 'PERMISSION_GROUP',
    icon = NULL,
    visible = 0,
    status = 'ACTIVE',
    deleted = 0,
    sort_order = CASE menu_code
      WHEN 'PINGAN_POINTS_FLOW_MODULE' THEN 2810
      WHEN 'PINGAN_POINTS_RANKING_MODULE' THEN 2820
      ELSE sort_order
    END,
    updated_at = CURRENT_TIMESTAMP
WHERE menu_code IN (
  'PINGAN_POINTS_FLOW_MODULE',
  'PINGAN_POINTS_RANKING_MODULE'
);

INSERT INTO sys_menu (
  parent_id, menu_code, title, route_path, component, icon, permission_code, sort_order, visible, status, deleted
)
SELECT module.id, item.permission_code, item.title, item.route_path, 'PERMISSION', NULL,
       item.permission_code, item.sort_order, 0, 'ACTIVE', 0
FROM (
  SELECT 'PINGAN_TRAINING_EXAM_TASKS_MODULE' AS module_code,
         'PINGAN_TRAINING_EXAM_TASKS_VIEW' AS permission_code,
         '查看' AS title,
         '/__permissions/pingan/training/exam-tasks/view' AS route_path,
         2711 AS sort_order
  UNION ALL SELECT 'PINGAN_TRAINING_EXAM_TASKS_MODULE',
         'PINGAN_TRAINING_EXAM_TASKS_CREATE',
         '新增',
         '/__permissions/pingan/training/exam-tasks/create',
         2712
  UNION ALL SELECT 'PINGAN_TRAINING_EXAM_TASKS_MODULE',
         'PINGAN_TRAINING_EXAM_TASKS_DELETE',
         '删除',
         '/__permissions/pingan/training/exam-tasks/delete',
         2713
  UNION ALL SELECT 'PINGAN_TRAINING_EXAM_TASKS_MODULE',
         'PINGAN_TRAINING_EXAM_TASKS_DOWNLOAD',
         '下载',
         '/__permissions/pingan/training/exam-tasks/download',
         2714

  UNION ALL SELECT 'PINGAN_TRAINING_EXAM_RESULTS_MODULE',
         'PINGAN_TRAINING_EXAM_RESULTS_VIEW',
         '查看',
         '/__permissions/pingan/training/exam-results/view',
         2721
  UNION ALL SELECT 'PINGAN_TRAINING_EXAM_RESULTS_MODULE',
         'PINGAN_TRAINING_EXAM_RESULTS_CREATE',
         '新增',
         '/__permissions/pingan/training/exam-results/create',
         2722
  UNION ALL SELECT 'PINGAN_TRAINING_EXAM_RESULTS_MODULE',
         'PINGAN_TRAINING_EXAM_RESULTS_DELETE',
         '删除',
         '/__permissions/pingan/training/exam-results/delete',
         2723

  UNION ALL SELECT 'PINGAN_TRAINING_SAFETY_LEARNING_MODULE',
         'PINGAN_TRAINING_SAFETY_LEARNING_VIEW',
         '查看',
         '/__permissions/pingan/training/safety-learning/view',
         2731
  UNION ALL SELECT 'PINGAN_TRAINING_SAFETY_LEARNING_MODULE',
         'PINGAN_TRAINING_SAFETY_LEARNING_CREATE',
         '新增',
         '/__permissions/pingan/training/safety-learning/create',
         2732
  UNION ALL SELECT 'PINGAN_TRAINING_SAFETY_LEARNING_MODULE',
         'PINGAN_TRAINING_SAFETY_LEARNING_DELETE',
         '删除',
         '/__permissions/pingan/training/safety-learning/delete',
         2733
  UNION ALL SELECT 'PINGAN_TRAINING_SAFETY_LEARNING_MODULE',
         'PINGAN_TRAINING_SAFETY_LEARNING_VOID',
         '作废',
         '/__permissions/pingan/training/safety-learning/void',
         2734
  UNION ALL SELECT 'PINGAN_TRAINING_SAFETY_LEARNING_MODULE',
         'PINGAN_TRAINING_SAFETY_LEARNING_DOWNLOAD',
         '下载',
         '/__permissions/pingan/training/safety-learning/download',
         2735

  UNION ALL SELECT 'PINGAN_POINTS_FLOW_MODULE',
         'PINGAN_POINTS_FLOW_VIEW',
         '查看',
         '/__permissions/pingan/points/flow/view',
         2811
  UNION ALL SELECT 'PINGAN_POINTS_FLOW_MODULE',
         'PINGAN_POINTS_FLOW_CREATE',
         '新增',
         '/__permissions/pingan/points/flow/create',
         2812
  UNION ALL SELECT 'PINGAN_POINTS_FLOW_MODULE',
         'PINGAN_POINTS_FLOW_DELETE',
         '删除',
         '/__permissions/pingan/points/flow/delete',
         2813

  UNION ALL SELECT 'PINGAN_POINTS_RANKING_MODULE',
         'PINGAN_POINTS_RANKING_VIEW',
         '查看',
         '/__permissions/pingan/points/ranking/view',
         2821
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
  SELECT 'PINGAN_TRAINING_VIEW' AS old_code, 'PINGAN_TRAINING_EXAM_TASKS_VIEW' AS new_code
  UNION ALL SELECT 'PINGAN_TRAINING_VIEW', 'PINGAN_TRAINING_EXAM_TASKS_DOWNLOAD'
  UNION ALL SELECT 'PINGAN_TRAINING_VIEW', 'PINGAN_TRAINING_EXAM_RESULTS_VIEW'
  UNION ALL SELECT 'PINGAN_TRAINING_VIEW', 'PINGAN_TRAINING_SAFETY_LEARNING_VIEW'
  UNION ALL SELECT 'PINGAN_TRAINING_VIEW', 'PINGAN_TRAINING_SAFETY_LEARNING_DOWNLOAD'
  UNION ALL SELECT 'PINGAN_TRAINING_STUDY_EXAM', 'PINGAN_TRAINING_EXAM_TASKS_VIEW'
  UNION ALL SELECT 'PINGAN_TRAINING_STUDY_EXAM', 'PINGAN_TRAINING_EXAM_RESULTS_VIEW'
  UNION ALL SELECT 'PINGAN_TRAINING_STUDY_EXAM', 'PINGAN_TRAINING_SAFETY_LEARNING_VIEW'
  UNION ALL SELECT 'PINGAN_TRAINING_MANAGE', 'PINGAN_TRAINING_EXAM_TASKS_VIEW'
  UNION ALL SELECT 'PINGAN_TRAINING_MANAGE', 'PINGAN_TRAINING_EXAM_TASKS_CREATE'
  UNION ALL SELECT 'PINGAN_TRAINING_MANAGE', 'PINGAN_TRAINING_EXAM_TASKS_DELETE'
  UNION ALL SELECT 'PINGAN_TRAINING_MANAGE', 'PINGAN_TRAINING_EXAM_TASKS_DOWNLOAD'
  UNION ALL SELECT 'PINGAN_TRAINING_MANAGE', 'PINGAN_TRAINING_EXAM_RESULTS_VIEW'
  UNION ALL SELECT 'PINGAN_TRAINING_MANAGE', 'PINGAN_TRAINING_EXAM_RESULTS_CREATE'
  UNION ALL SELECT 'PINGAN_TRAINING_MANAGE', 'PINGAN_TRAINING_EXAM_RESULTS_DELETE'
  UNION ALL SELECT 'PINGAN_TRAINING_MANAGE', 'PINGAN_TRAINING_SAFETY_LEARNING_VIEW'
  UNION ALL SELECT 'PINGAN_TRAINING_MANAGE', 'PINGAN_TRAINING_SAFETY_LEARNING_CREATE'
  UNION ALL SELECT 'PINGAN_TRAINING_MANAGE', 'PINGAN_TRAINING_SAFETY_LEARNING_DELETE'
  UNION ALL SELECT 'PINGAN_TRAINING_MANAGE', 'PINGAN_TRAINING_SAFETY_LEARNING_VOID'
  UNION ALL SELECT 'PINGAN_TRAINING_MANAGE', 'PINGAN_TRAINING_SAFETY_LEARNING_DOWNLOAD'
  UNION ALL SELECT 'PINGAN_POINTS_VIEW', 'PINGAN_POINTS_FLOW_VIEW'
  UNION ALL SELECT 'PINGAN_POINTS_VIEW', 'PINGAN_POINTS_RANKING_VIEW'
  UNION ALL SELECT 'PINGAN_POINTS_MANAGE', 'PINGAN_POINTS_FLOW_VIEW'
  UNION ALL SELECT 'PINGAN_POINTS_MANAGE', 'PINGAN_POINTS_FLOW_CREATE'
  UNION ALL SELECT 'PINGAN_POINTS_MANAGE', 'PINGAN_POINTS_FLOW_DELETE'
  UNION ALL SELECT 'PINGAN_POINTS_MANAGE', 'PINGAN_POINTS_RANKING_VIEW'
) mapping ON mapping.old_code = old_menu.permission_code
JOIN sys_menu new_menu ON new_menu.permission_code = mapping.new_code
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_role_menu existing
  WHERE existing.role_id = rm.role_id
    AND existing.menu_id = new_menu.id
);

INSERT INTO sys_menu (
  parent_id, menu_code, title, route_path, component, icon, permission_code, sort_order, visible, status, deleted
)
SELECT parent.id, 'PINGAN_TRAINING_LEGACY_COMPAT_MODULE', '宣教培训兼容权限',
       '/__permissions/pingan/training/legacy', 'PERMISSION_GROUP', NULL,
       'PINGAN_TRAINING_LEGACY_COMPAT_ENTRY', 2799, 0, 'ACTIVE', 1
FROM sys_menu parent
WHERE parent.menu_code = 'PINGAN_TRAINING_MODULE'
  AND NOT EXISTS (
    SELECT 1
    FROM sys_menu existing
    WHERE existing.menu_code = 'PINGAN_TRAINING_LEGACY_COMPAT_MODULE'
       OR existing.permission_code = 'PINGAN_TRAINING_LEGACY_COMPAT_ENTRY'
  );

INSERT INTO sys_menu (
  parent_id, menu_code, title, route_path, component, icon, permission_code, sort_order, visible, status, deleted
)
SELECT parent.id, 'PINGAN_POINTS_LEGACY_COMPAT_MODULE', '安全积分兼容权限',
       '/__permissions/pingan/points/legacy', 'PERMISSION_GROUP', NULL,
       'PINGAN_POINTS_LEGACY_COMPAT_ENTRY', 2899, 0, 'ACTIVE', 1
FROM sys_menu parent
WHERE parent.menu_code = 'PINGAN_POINTS_MODULE'
  AND NOT EXISTS (
    SELECT 1
    FROM sys_menu existing
    WHERE existing.menu_code = 'PINGAN_POINTS_LEGACY_COMPAT_MODULE'
       OR existing.permission_code = 'PINGAN_POINTS_LEGACY_COMPAT_ENTRY'
  );

UPDATE sys_menu
SET parent_id = (SELECT id FROM sys_menu WHERE menu_code = 'PINGAN_TRAINING_LEGACY_COMPAT_MODULE'),
    sort_order = CASE permission_code
      WHEN 'PINGAN_TRAINING_VIEW' THEN 27991
      WHEN 'PINGAN_TRAINING_STUDY_EXAM' THEN 27992
      WHEN 'PINGAN_TRAINING_MANAGE' THEN 27993
      ELSE sort_order
    END,
    updated_at = CURRENT_TIMESTAMP
WHERE permission_code IN (
  'PINGAN_TRAINING_VIEW',
  'PINGAN_TRAINING_STUDY_EXAM',
  'PINGAN_TRAINING_MANAGE'
);

UPDATE sys_menu
SET parent_id = (SELECT id FROM sys_menu WHERE menu_code = 'PINGAN_POINTS_LEGACY_COMPAT_MODULE'),
    sort_order = CASE permission_code
      WHEN 'PINGAN_POINTS_VIEW' THEN 28991
      WHEN 'PINGAN_POINTS_MANAGE' THEN 28992
      ELSE sort_order
    END,
    updated_at = CURRENT_TIMESTAMP
WHERE permission_code IN (
  'PINGAN_POINTS_VIEW',
  'PINGAN_POINTS_MANAGE'
);
