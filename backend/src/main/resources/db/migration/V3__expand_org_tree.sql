INSERT INTO sys_org (id, parent_id, org_type, org_code, org_name, org_path, sort_order, status) VALUES
  (11, 2, 'COMPANY', 'GSKT', '广晟矿投', '/1/2/11/', 2, 'ACTIVE'),
  (12, 11, 'COMPANY', 'HYGY', '河源古云', '/1/2/11/12/', 2, 'ACTIVE'),
  (13, 11, 'COMPANY', 'BTSY', '博泰实业', '/1/2/11/13/', 3, 'ACTIVE'),
  (14, 11, 'COMPANY', 'CALY', '潮安立源', '/1/2/11/14/', 4, 'ACTIVE'),
  (15, 11, 'COMPANY', 'GDYJGY', '广东省冶金工业总公司', '/1/2/11/15/', 5, 'ACTIVE'),
  (16, 11, 'COMPANY', 'GSHST', '广晟禾尚田', '/1/2/11/16/', 6, 'ACTIVE'),
  (17, 2, 'COMPANY', 'NCCY', '南储仓储', '/1/2/17/', 3, 'ACTIVE'),
  (18, 17, 'COMPANY', 'NCYS', '南储运输', '/1/2/17/18/', 1, 'ACTIVE'),
  (19, 17, 'COMPANY', 'FSNC', '佛山南储', '/1/2/17/19/', 2, 'ACTIVE'),
  (20, 17, 'COMPANY', 'CZNC', '常州南储', '/1/2/17/20/', 3, 'ACTIVE'),
  (21, 2, 'COMPANY', 'GSYJ', '广晟冶金', '/1/2/21/', 4, 'ACTIVE'),
  (22, 21, 'COMPANY', 'JFGS', '经发公司', '/1/2/21/22/', 1, 'ACTIVE'),
  (23, 2, 'COMPANY', 'YLORE', '瑶岭矿业', '/1/2/23/', 5, 'ACTIVE'),
  (24, 23, 'COMPANY', 'ZYGS', '资源公司', '/1/2/23/24/', 1, 'ACTIVE'),
  (25, 2, 'COMPANY', 'YCJT', '阳春金同', '/1/2/25/', 6, 'ACTIVE'),
  (26, 2, 'COMPANY', 'HJGROUP', '黄金集团', '/1/2/26/', 7, 'ACTIVE'),
  (27, 2, 'SCHOOL', 'YJJX', '冶金技校', '/1/2/27/', 8, 'ACTIVE'),
  (28, 2, 'COMPANY', 'GSXC', '广晟新材', '/1/2/28/', 9, 'ACTIVE'),
  (29, 28, 'COMPANY', 'GLGS', '高力公司', '/1/2/28/29/', 1, 'ACTIVE'),
  (30, 2, 'COMPANY', 'GSCC', '广晟仓储', '/1/2/30/', 10, 'ACTIVE'),
  (31, 2, 'COMPANY', 'JYMQ', '金粤幕墙', '/1/2/31/', 11, 'ACTIVE'),
  (32, 2, 'COMPANY', 'FAGS', '泛澳公司', '/1/2/32/', 12, 'ACTIVE');

UPDATE sys_org SET parent_id = 11, org_path = '/1/2/11/8/', sort_order = 1 WHERE id = 8;
UPDATE sys_org SET org_path = '/1/2/11/8/9/' WHERE id = 9;
UPDATE sys_org SET org_path = '/1/2/11/8/9/10/' WHERE id = 10;
