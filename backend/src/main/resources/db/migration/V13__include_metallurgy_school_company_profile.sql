UPDATE sys_org
SET org_type = 'COMPANY',
    updated_at = CURRENT_TIMESTAMP
WHERE id = 27
  AND org_type = 'SCHOOL'
  AND deleted = 0;

INSERT INTO sys_company_profile
  (org_id, short_name, description, company_type, level1_name, level2_name, level3_name, level4_name, deleted)
SELECT 27, 'Demo Training School', 'Demo Training School', '分公司', 'Demo控股集团', 'Demo Safety Holdings', 'Demo Training School', NULL, 0
WHERE EXISTS (
    SELECT 1
    FROM sys_org
    WHERE id = 27
      AND deleted = 0
  )
  AND NOT EXISTS (
    SELECT 1
    FROM sys_company_profile
    WHERE org_id = 27
  );

UPDATE sys_company_profile
SET short_name = 'Demo Training School',
    description = 'Demo Training School',
    company_type = '分公司',
    level1_name = 'Demo控股集团',
    level2_name = 'Demo Safety Holdings',
    level3_name = 'Demo Training School',
    level4_name = NULL,
    updated_at = CURRENT_TIMESTAMP,
    deleted = 0
WHERE org_id = 27;
