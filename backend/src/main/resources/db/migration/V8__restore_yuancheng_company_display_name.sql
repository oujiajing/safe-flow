UPDATE sys_org
SET org_name = 'Demo Works Company',
    updated_at = CURRENT_TIMESTAMP
WHERE id = 4
  AND org_type = 'COMPANY'
  AND deleted = 0;

UPDATE sys_company_profile
SET short_name = 'Demo Works Company',
    description = 'Demo Works Company',
    level4_name = 'Demo Works Company',
    updated_at = CURRENT_TIMESTAMP
WHERE org_id = 4
  AND deleted = 0;

