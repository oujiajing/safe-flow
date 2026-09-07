INSERT INTO sys_menu (
  parent_id, menu_code, title, route_path, component, icon, permission_code, sort_order, visible, status
)
SELECT parent.id, 'PINGAN_NOTIFICATION_WARNING_MODULE', '消息预警',
       '/__permissions/pingan/notification-warning', 'PERMISSION_GROUP', NULL,
       'PINGAN_NOTIFICATION_WARNING_ENTRY', 295, 0, 'ACTIVE'
FROM sys_menu parent
WHERE parent.menu_code = 'PINGAN_PERMISSION_GROUP'
  AND NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE permission_code = 'PINGAN_NOTIFICATION_WARNING_ENTRY'
  );

INSERT INTO sys_menu (
  parent_id, menu_code, title, route_path, component, icon, permission_code, sort_order, visible, status
)
SELECT parent.id, permission.permission_code, permission.title, permission.route_path,
       'PERMISSION', NULL, permission.permission_code, permission.sort_order, 0, 'ACTIVE'
FROM sys_menu parent
CROSS JOIN (
  SELECT 'PINGAN_NOTIFICATION_BUSINESS_WARNING' AS permission_code, '接收业务逾期预警' AS title,
         '/__permissions/pingan/notification-business-warning' AS route_path, 296 AS sort_order
  UNION ALL SELECT 'PINGAN_NOTIFICATION_ORG_SUMMARY', '接收下级组织汇总',
         '/__permissions/pingan/notification-org-summary', 297
) permission
WHERE parent.permission_code = 'PINGAN_NOTIFICATION_WARNING_ENTRY'
  AND NOT EXISTS (
    SELECT 1 FROM sys_menu existing WHERE existing.permission_code = permission.permission_code
  );

INSERT INTO sys_menu (
  parent_id, menu_code, title, route_path, component, icon, permission_code, sort_order, visible, status
)
SELECT parent.id, 'SYSTEM_NOTIFICATION_CENTER_MODULE', '消息中心',
       '/__permissions/system/notification-center', 'PERMISSION_GROUP', NULL,
       'SYSTEM_NOTIFICATION_CENTER_ENTRY', -70, 0, 'ACTIVE'
FROM sys_menu parent
WHERE parent.menu_code = 'SYSTEM_MANAGEMENT'
  AND NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE permission_code = 'SYSTEM_NOTIFICATION_CENTER_ENTRY'
  );

INSERT INTO sys_menu (
  parent_id, menu_code, title, route_path, component, icon, permission_code, sort_order, visible, status
)
SELECT parent.id, permission.permission_code, permission.title, permission.route_path,
       'PERMISSION', NULL, permission.permission_code, permission.sort_order, 0, 'ACTIVE'
FROM sys_menu parent
CROSS JOIN (
  SELECT 'PINGAN_NOTIFICATION_ANNOUNCEMENT_MANAGE' AS permission_code, '公告管理' AS title,
         '/__permissions/system/announcement-manage' AS route_path, -69 AS sort_order
  UNION ALL SELECT 'PINGAN_NOTIFICATION_ANNOUNCEMENT_PUBLISH', '公告发布',
         '/__permissions/system/announcement-publish', -68
  UNION ALL SELECT 'PINGAN_NOTIFICATION_SYSTEM_ALERT', '接收系统告警',
         '/__permissions/system/notification-alert', -67
) permission
WHERE parent.permission_code = 'SYSTEM_NOTIFICATION_CENTER_ENTRY'
  AND NOT EXISTS (
    SELECT 1 FROM sys_menu existing WHERE existing.permission_code = permission.permission_code
  );

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT role.id, menu.id
FROM sys_role role
CROSS JOIN sys_menu menu
WHERE role.role_code = 'ADMIN'
  AND menu.permission_code IN (
    'PINGAN_NOTIFICATION_WARNING_ENTRY',
    'PINGAN_NOTIFICATION_BUSINESS_WARNING',
    'PINGAN_NOTIFICATION_ORG_SUMMARY',
    'SYSTEM_NOTIFICATION_CENTER_ENTRY',
    'PINGAN_NOTIFICATION_ANNOUNCEMENT_MANAGE',
    'PINGAN_NOTIFICATION_ANNOUNCEMENT_PUBLISH',
    'PINGAN_NOTIFICATION_SYSTEM_ALERT'
  )
  AND NOT EXISTS (
    SELECT 1 FROM sys_role_menu mapping
    WHERE mapping.role_id = role.id AND mapping.menu_id = menu.id
  );

