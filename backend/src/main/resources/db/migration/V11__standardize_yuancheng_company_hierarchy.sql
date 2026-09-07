UPDATE sys_company_profile
SET company_type = '子公司',
    level1_name = '广晟控股集团',
    level2_name = '广晟矿业集团',
    level3_name = '广晟幕墙',
    level4_name = '广晟源成',
    short_name = '广晟源成',
    description = '广晟源成',
    updated_at = CURRENT_TIMESTAMP
WHERE org_id = 4
  AND deleted = 0;

