UPDATE sys_org
SET org_name = '广晟源成',
    updated_at = CURRENT_TIMESTAMP
WHERE id = 4
  AND org_type = 'COMPANY'
  AND deleted = 0;

UPDATE sys_company_profile
SET short_name = '广晟源成',
    description = '广晟源成',
    level4_name = '广晟源成',
    updated_at = CURRENT_TIMESTAMP
WHERE org_id = 4
  AND deleted = 0;
