UPDATE sys_company_profile
SET company_type = '子公司',
    level1_name = 'Demo控股集团',
    level2_name = 'Demo Safety Holdings',
    level3_name = 'Demo Works Company',
    level4_name = 'Demo Works Company',
    short_name = 'Demo Works Company',
    description = 'Demo Works Company',
    updated_at = CURRENT_TIMESTAMP
WHERE org_id = 4
  AND deleted = 0;


