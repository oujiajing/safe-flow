INSERT INTO sys_company_profile
  (org_id, short_name, description, company_type, level1_name, level2_name, level3_name, level4_name, deleted)
SELECT 1, '广晟控股集团', '广晟控股集团', '集团', '广晟控股集团', NULL, NULL, NULL, 0
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_company_profile
  WHERE org_id = 1
)
UNION ALL
SELECT 2, '广晟矿业集团', '广晟矿业集团', '集团', '广晟控股集团', '广晟矿业集团', NULL, NULL, 0
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_company_profile
  WHERE org_id = 2
);

UPDATE sys_company_profile
SET address = (SELECT source.address FROM sys_company_profile source WHERE source.org_id = 4 AND source.deleted = 0),
    safety_manager_username =
      (SELECT source.safety_manager_username FROM sys_company_profile source WHERE source.org_id = 4 AND source.deleted = 0),
    reporter_l1_usernames =
      (SELECT source.reporter_l1_usernames FROM sys_company_profile source WHERE source.org_id = 4 AND source.deleted = 0),
    reporter_l2_usernames =
      (SELECT source.reporter_l2_usernames FROM sys_company_profile source WHERE source.org_id = 4 AND source.deleted = 0),
    reporter_l3_usernames =
      (SELECT source.reporter_l3_usernames FROM sys_company_profile source WHERE source.org_id = 4 AND source.deleted = 0),
    report_l1_time = (SELECT source.report_l1_time FROM sys_company_profile source WHERE source.org_id = 4 AND source.deleted = 0),
    report_l2_time = (SELECT source.report_l2_time FROM sys_company_profile source WHERE source.org_id = 4 AND source.deleted = 0),
    report_l3_time = (SELECT source.report_l3_time FROM sys_company_profile source WHERE source.org_id = 4 AND source.deleted = 0),
    attachment1_url = (SELECT source.attachment1_url FROM sys_company_profile source WHERE source.org_id = 4 AND source.deleted = 0),
    attachment2_url = (SELECT source.attachment2_url FROM sys_company_profile source WHERE source.org_id = 4 AND source.deleted = 0),
    company_intro = (SELECT source.company_intro FROM sys_company_profile source WHERE source.org_id = 4 AND source.deleted = 0),
    updated_at = CURRENT_TIMESTAMP,
    deleted = 0
WHERE org_id IN (1, 2)
  AND EXISTS (
    SELECT 1
    FROM sys_company_profile source
    WHERE source.org_id = 4
      AND source.deleted = 0
  );
