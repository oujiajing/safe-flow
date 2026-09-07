INSERT INTO sys_org (id, parent_id, org_type, org_code, org_name, org_path, sort_order, status) VALUES
  (11, 2, 'COMPANY', 'GSKT', 'Demo矿投', '/1/2/11/', 2, 'ACTIVE'),
  (12, 11, 'COMPANY', 'HYGY', 'Demo North Site', '/1/2/11/12/', 2, 'ACTIVE'),
  (13, 11, 'COMPANY', 'BTSY', 'Demo Industrial', '/1/2/11/13/', 3, 'ACTIVE'),
  (14, 11, 'COMPANY', 'CALY', 'Demo Coastal Site', '/1/2/11/14/', 4, 'ACTIVE'),
  (15, 11, 'COMPANY', 'GDYJGY', 'Demo Metallurgy Company', '/1/2/11/15/', 5, 'ACTIVE'),
  (16, 11, 'COMPANY', 'GSHST', 'Demo Harvest Site', '/1/2/11/16/', 6, 'ACTIVE'),
  (17, 2, 'COMPANY', 'NCCY', 'Demo Storage', '/1/2/17/', 3, 'ACTIVE'),
  (18, 17, 'COMPANY', 'NCYS', 'Demo Transport', '/1/2/17/18/', 1, 'ACTIVE'),
  (19, 17, 'COMPANY', 'FSNC', 'Demo South Depot', '/1/2/17/19/', 2, 'ACTIVE'),
  (20, 17, 'COMPANY', 'CZNC', 'Demo East Depot', '/1/2/17/20/', 3, 'ACTIVE'),
  (21, 2, 'COMPANY', 'GSYJ', 'Demo Metallurgy', '/1/2/21/', 4, 'ACTIVE'),
  (22, 21, 'COMPANY', 'JFGS', 'Demo Development', '/1/2/21/22/', 1, 'ACTIVE'),
  (23, 2, 'COMPANY', 'YLORE', 'Demo Site', '/1/2/23/', 5, 'ACTIVE'),
  (24, 23, 'COMPANY', 'ZYGS', 'Demo Company', '/1/2/23/24/', 1, 'ACTIVE'),
  (25, 2, 'COMPANY', 'YCJT', 'Demo Spring Site', '/1/2/25/', 6, 'ACTIVE'),
  (26, 2, 'COMPANY', 'HJGROUP', 'Demo Gold Group', '/1/2/26/', 7, 'ACTIVE'),
  (27, 2, 'SCHOOL', 'YJJX', 'Demo Training School', '/1/2/27/', 8, 'ACTIVE'),
  (28, 2, 'COMPANY', 'GSXC', 'Demo Materials', '/1/2/28/', 9, 'ACTIVE'),
  (29, 28, 'COMPANY', 'GLGS', '高力公司', '/1/2/28/29/', 1, 'ACTIVE'),
  (30, 2, 'COMPANY', 'GSCC', 'Demo Warehouse', '/1/2/30/', 10, 'ACTIVE'),
  (31, 2, 'COMPANY', 'JYMQ', 'Demo Works Subsidiary', '/1/2/31/', 11, 'ACTIVE'),
  (32, 2, 'COMPANY', 'FAGS', 'Demo Pacific Company', '/1/2/32/', 12, 'ACTIVE');

UPDATE sys_org SET parent_id = 11, org_path = '/1/2/11/8/', sort_order = 1 WHERE id = 8;
UPDATE sys_org SET org_path = '/1/2/11/8/9/' WHERE id = 9;
UPDATE sys_org SET org_path = '/1/2/11/8/9/10/' WHERE id = 10;
