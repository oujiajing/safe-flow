-- Existing role grants may contain only a checked parent permission node.
-- Materialize active descendants so frontend access codes and backend action checks
-- both see the concrete executable permissions.

WITH RECURSIVE grant_tree(role_id, menu_id) AS (
  SELECT rm.role_id, rm.menu_id
  FROM sys_role_menu rm
  JOIN sys_menu menu ON menu.id = rm.menu_id
  LEFT JOIN sys_menu root ON root.menu_code = 'PINGAN_PERMISSION_GROUP'
  WHERE menu.deleted = 0
    AND menu.status = 'ACTIVE'
    AND menu.component = 'PERMISSION_GROUP'
    AND menu.parent_id IS NOT NULL
    AND (root.id IS NULL OR menu.parent_id <> root.id)
  UNION ALL
  SELECT grant_tree.role_id, child.id
  FROM grant_tree
  JOIN sys_menu child ON child.parent_id = grant_tree.menu_id
  WHERE child.deleted = 0
    AND child.status = 'ACTIVE'
)
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT grant_tree.role_id, grant_tree.menu_id
FROM grant_tree
JOIN sys_menu menu ON menu.id = grant_tree.menu_id
WHERE menu.deleted = 0
  AND menu.status = 'ACTIVE'
  AND NOT EXISTS (
    SELECT 1
    FROM sys_role_menu existing
    WHERE existing.role_id = grant_tree.role_id
      AND existing.menu_id = grant_tree.menu_id
  );
