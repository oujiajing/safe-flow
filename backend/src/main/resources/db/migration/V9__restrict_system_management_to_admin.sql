DELETE FROM sys_role_menu
WHERE menu_id IN (
    SELECT id
    FROM sys_menu
    WHERE menu_code LIKE 'SYSTEM_%'
  )
  AND role_id NOT IN (
    SELECT id
    FROM sys_role
    WHERE role_code = 'ADMIN'
      AND deleted = 0
  );

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
CROSS JOIN sys_menu m
WHERE r.role_code = 'ADMIN'
  AND r.deleted = 0
  AND m.menu_code LIKE 'SYSTEM_%'
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_menu rm
    WHERE rm.role_id = r.id
      AND rm.menu_id = m.id
  );
