UPDATE sys_org o
SET deleted = 1,
    updated_at = CURRENT_TIMESTAMP
WHERE o.deleted = 0
  AND o.id > 9007199254740991
  AND o.org_type IN ('GROUP', 'COMPANY')
  AND EXISTS (
    SELECT 1
    FROM sys_company_profile p
    WHERE p.org_id = o.id
      AND p.deleted = 0
      AND (p.company_type IS NULL OR p.company_type NOT IN ('集团', '分公司', '子公司'))
  )
  AND NOT EXISTS (
    SELECT 1 FROM sys_org child WHERE child.parent_id = o.id AND child.deleted = 0
  )
  AND NOT EXISTS (
    SELECT 1 FROM sys_department_profile d WHERE d.company_org_id = o.id AND d.deleted = 0
  )
  AND NOT EXISTS (
    SELECT 1 FROM sys_team_profile t WHERE t.company_org_id = o.id AND t.deleted = 0
  )
  AND NOT EXISTS (
    SELECT 1 FROM sys_user_profile u WHERE u.company_org_id = o.id AND u.deleted = 0
  )
  AND NOT EXISTS (
    SELECT 1 FROM sys_user u WHERE u.org_id = o.id AND u.deleted = 0
  )
  AND NOT EXISTS (
    SELECT 1 FROM shift_task s WHERE s.company_id = o.id AND s.deleted = 0
  )
  AND NOT EXISTS (
    SELECT 1 FROM pre_shift_meeting m WHERE m.company_id = o.id AND m.deleted = 0
  )
  AND NOT EXISTS (
    SELECT 1 FROM three_check_record r WHERE r.company_id = o.id AND r.deleted = 0
  );

UPDATE sys_company_profile p
SET deleted = 1,
    updated_at = CURRENT_TIMESTAMP
WHERE p.deleted = 0
  AND (p.company_type IS NULL OR p.company_type NOT IN ('集团', '分公司', '子公司'))
  AND EXISTS (
    SELECT 1
    FROM sys_org o
    WHERE o.id = p.org_id
      AND o.deleted = 1
      AND o.id > 9007199254740991
  );
