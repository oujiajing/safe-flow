CREATE TEMPORARY TABLE tmp_company_hierarchy_profiles (
  org_id BIGINT NOT NULL,
  short_name VARCHAR(120) NOT NULL,
  description TEXT NOT NULL,
  company_type VARCHAR(32) NOT NULL,
  level1_name VARCHAR(120) NOT NULL,
  level2_name VARCHAR(120) NULL,
  level3_name VARCHAR(120) NULL,
  level4_name VARCHAR(120) NULL
);

INSERT INTO tmp_company_hierarchy_profiles
  (org_id, short_name, description, company_type, level1_name, level2_name, level3_name, level4_name)
VALUES
  (1, 'Demo控股集团', 'Demo控股集团', '集团', 'Demo控股集团', NULL, NULL, NULL),
  (2, 'Demo Safety Holdings', 'Demo Safety Holdings', '集团', 'Demo控股集团', 'Demo Safety Holdings', NULL, NULL),
  (3, 'Demo Works Company', 'Demo Works Company', '分公司', 'Demo控股集团', 'Demo Safety Holdings', 'Demo Works Company', NULL),
  (4, 'Demo Works Company', 'Demo Works Company', '子公司', 'Demo控股集团', 'Demo Safety Holdings', 'Demo Works Company', 'Demo Works Company'),
  (8, 'Demo East Site', 'Demo East Site', '子公司', 'Demo控股集团', 'Demo Safety Holdings', 'Demo矿投', 'Demo East Site'),
  (11, 'Demo矿投', 'Demo矿投', '分公司', 'Demo控股集团', 'Demo Safety Holdings', 'Demo矿投', NULL),
  (12, 'Demo North Site', 'Demo North Site', '子公司', 'Demo控股集团', 'Demo Safety Holdings', 'Demo矿投', 'Demo North Site'),
  (13, 'Demo Industrial', 'Demo Industrial', '子公司', 'Demo控股集团', 'Demo Safety Holdings', 'Demo矿投', 'Demo Industrial'),
  (14, 'Demo Coastal Site', 'Demo Coastal Site', '子公司', 'Demo控股集团', 'Demo Safety Holdings', 'Demo矿投', 'Demo Coastal Site'),
  (15, 'Demo Metallurgy Company', 'Demo Metallurgy Company', '子公司', 'Demo控股集团', 'Demo Safety Holdings', 'Demo矿投', 'Demo Metallurgy Company'),
  (16, 'Demo Harvest Site', 'Demo Harvest Site', '子公司', 'Demo控股集团', 'Demo Safety Holdings', 'Demo矿投', 'Demo Harvest Site'),
  (17, 'Demo Storage', 'Demo Storage', '分公司', 'Demo控股集团', 'Demo Safety Holdings', 'Demo Storage', NULL),
  (18, 'Demo Transport', 'Demo Transport', '子公司', 'Demo控股集团', 'Demo Safety Holdings', 'Demo Storage', 'Demo Transport'),
  (19, 'Demo South Depot', 'Demo South Depot', '子公司', 'Demo控股集团', 'Demo Safety Holdings', 'Demo Storage', 'Demo South Depot'),
  (20, 'Demo East Depot', 'Demo East Depot', '子公司', 'Demo控股集团', 'Demo Safety Holdings', 'Demo Storage', 'Demo East Depot'),
  (21, 'Demo Metallurgy', 'Demo Metallurgy', '分公司', 'Demo控股集团', 'Demo Safety Holdings', 'Demo Metallurgy', NULL),
  (22, 'Demo Development', 'Demo Development', '子公司', 'Demo控股集团', 'Demo Safety Holdings', 'Demo Metallurgy', 'Demo Development'),
  (23, 'Demo Site', 'Demo Site', '分公司', 'Demo控股集团', 'Demo Safety Holdings', 'Demo Site', NULL),
  (24, 'Demo Company', 'Demo Company', '子公司', 'Demo控股集团', 'Demo Safety Holdings', 'Demo Site', 'Demo Company'),
  (25, 'Demo Spring Site', 'Demo Spring Site', '分公司', 'Demo控股集团', 'Demo Safety Holdings', 'Demo Spring Site', NULL),
  (26, 'Demo Gold Group', 'Demo Gold Group', '分公司', 'Demo控股集团', 'Demo Safety Holdings', 'Demo Gold Group', NULL),
  (28, 'Demo Materials', 'Demo Materials', '分公司', 'Demo控股集团', 'Demo Safety Holdings', 'Demo Materials', NULL),
  (29, '高力公司', '高力公司', '子公司', 'Demo控股集团', 'Demo Safety Holdings', 'Demo Materials', '高力公司'),
  (30, 'Demo Warehouse', 'Demo Warehouse', '分公司', 'Demo控股集团', 'Demo Safety Holdings', 'Demo Warehouse', NULL),
  (31, 'Demo Works Subsidiary', 'Demo Works Subsidiary', '分公司', 'Demo控股集团', 'Demo Safety Holdings', 'Demo Works Subsidiary', NULL),
  (32, 'Demo Pacific Company', 'Demo Pacific Company', '分公司', 'Demo控股集团', 'Demo Safety Holdings', 'Demo Pacific Company', NULL);

INSERT INTO sys_company_profile (org_id, short_name, description, company_type, level1_name, level2_name, level3_name, level4_name, deleted)
SELECT h.org_id, h.short_name, h.description, h.company_type, h.level1_name, h.level2_name, h.level3_name, h.level4_name, 0
FROM tmp_company_hierarchy_profiles h
WHERE NOT EXISTS (
  SELECT 1
  FROM sys_company_profile p
  WHERE p.org_id = h.org_id
);

UPDATE sys_company_profile
SET short_name = (SELECT h.short_name FROM tmp_company_hierarchy_profiles h WHERE h.org_id = sys_company_profile.org_id),
    description = (SELECT h.description FROM tmp_company_hierarchy_profiles h WHERE h.org_id = sys_company_profile.org_id),
    company_type = (SELECT h.company_type FROM tmp_company_hierarchy_profiles h WHERE h.org_id = sys_company_profile.org_id),
    level1_name = (SELECT h.level1_name FROM tmp_company_hierarchy_profiles h WHERE h.org_id = sys_company_profile.org_id),
    level2_name = (SELECT h.level2_name FROM tmp_company_hierarchy_profiles h WHERE h.org_id = sys_company_profile.org_id),
    level3_name = (SELECT h.level3_name FROM tmp_company_hierarchy_profiles h WHERE h.org_id = sys_company_profile.org_id),
    level4_name = (SELECT h.level4_name FROM tmp_company_hierarchy_profiles h WHERE h.org_id = sys_company_profile.org_id),
    updated_at = CURRENT_TIMESTAMP,
    deleted = 0
WHERE org_id IN (SELECT org_id FROM tmp_company_hierarchy_profiles);

DROP TABLE tmp_company_hierarchy_profiles;
