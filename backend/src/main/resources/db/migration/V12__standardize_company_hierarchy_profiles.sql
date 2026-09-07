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
  (1, '广晟控股集团', '广晟控股集团', '集团', '广晟控股集团', NULL, NULL, NULL),
  (2, '广晟矿业集团', '广晟矿业集团', '集团', '广晟控股集团', '广晟矿业集团', NULL, NULL),
  (3, '广晟幕墙', '广晟幕墙', '分公司', '广晟控股集团', '广晟矿业集团', '广晟幕墙', NULL),
  (4, '广晟源成', '广晟源成', '子公司', '广晟控股集团', '广晟矿业集团', '广晟幕墙', '广晟源成'),
  (8, '梅州嘉晟', '梅州嘉晟', '子公司', '广晟控股集团', '广晟矿业集团', '广晟矿投', '梅州嘉晟'),
  (11, '广晟矿投', '广晟矿投', '分公司', '广晟控股集团', '广晟矿业集团', '广晟矿投', NULL),
  (12, '河源古云', '河源古云', '子公司', '广晟控股集团', '广晟矿业集团', '广晟矿投', '河源古云'),
  (13, '博泰实业', '博泰实业', '子公司', '广晟控股集团', '广晟矿业集团', '广晟矿投', '博泰实业'),
  (14, '潮安立源', '潮安立源', '子公司', '广晟控股集团', '广晟矿业集团', '广晟矿投', '潮安立源'),
  (15, '广东省冶金工业总公司', '广东省冶金工业总公司', '子公司', '广晟控股集团', '广晟矿业集团', '广晟矿投', '广东省冶金工业总公司'),
  (16, '广晟禾尚田', '广晟禾尚田', '子公司', '广晟控股集团', '广晟矿业集团', '广晟矿投', '广晟禾尚田'),
  (17, '南储仓储', '南储仓储', '分公司', '广晟控股集团', '广晟矿业集团', '南储仓储', NULL),
  (18, '南储运输', '南储运输', '子公司', '广晟控股集团', '广晟矿业集团', '南储仓储', '南储运输'),
  (19, '佛山南储', '佛山南储', '子公司', '广晟控股集团', '广晟矿业集团', '南储仓储', '佛山南储'),
  (20, '常州南储', '常州南储', '子公司', '广晟控股集团', '广晟矿业集团', '南储仓储', '常州南储'),
  (21, '广晟冶金', '广晟冶金', '分公司', '广晟控股集团', '广晟矿业集团', '广晟冶金', NULL),
  (22, '经发公司', '经发公司', '子公司', '广晟控股集团', '广晟矿业集团', '广晟冶金', '经发公司'),
  (23, '瑶岭矿业', '瑶岭矿业', '分公司', '广晟控股集团', '广晟矿业集团', '瑶岭矿业', NULL),
  (24, '资源公司', '资源公司', '子公司', '广晟控股集团', '广晟矿业集团', '瑶岭矿业', '资源公司'),
  (25, '阳春金同', '阳春金同', '分公司', '广晟控股集团', '广晟矿业集团', '阳春金同', NULL),
  (26, '黄金集团', '黄金集团', '分公司', '广晟控股集团', '广晟矿业集团', '黄金集团', NULL),
  (28, '广晟新材', '广晟新材', '分公司', '广晟控股集团', '广晟矿业集团', '广晟新材', NULL),
  (29, '高力公司', '高力公司', '子公司', '广晟控股集团', '广晟矿业集团', '广晟新材', '高力公司'),
  (30, '广晟仓储', '广晟仓储', '分公司', '广晟控股集团', '广晟矿业集团', '广晟仓储', NULL),
  (31, '金粤幕墙', '金粤幕墙', '分公司', '广晟控股集团', '广晟矿业集团', '金粤幕墙', NULL),
  (32, '泛澳公司', '泛澳公司', '分公司', '广晟控股集团', '广晟矿业集团', '泛澳公司', NULL);

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
