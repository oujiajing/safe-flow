-- Master data for the isolated portfolio_demo database.
-- Business display values are Chinese; codes and usernames remain stable English IDs.

BEGIN;

DO $$
DECLARE
  company_id BIGINT;
  safety_dept_id BIGINT;
  ops_dept_id BIGINT;
  equipment_dept_id BIGINT;
  assembly_workshop_id BIGINT;
  maintenance_workshop_id BIGINT;
BEGIN
  INSERT INTO sys_org (parent_id, org_type, org_code, org_name, org_path, sort_order, status, deleted)
  VALUES (NULL, 'COMPANY', 'DEMO-COMPANY', '演示公司', '/pending/', 10, 'ACTIVE', 0)
  ON CONFLICT (org_code) DO UPDATE SET org_name = EXCLUDED.org_name, status = 'ACTIVE', deleted = 0, updated_at = CURRENT_TIMESTAMP
  RETURNING id INTO company_id;
  UPDATE sys_org SET org_path = '/' || company_id || '/' WHERE id = company_id;

  INSERT INTO sys_org (parent_id, org_type, org_code, org_name, org_path, sort_order, status, deleted)
  VALUES (company_id, 'DEPARTMENT', 'DEMO-SAFETY', '安全质量部', '/pending/', 10, 'ACTIVE', 0)
  ON CONFLICT (org_code) DO UPDATE SET org_name = EXCLUDED.org_name, parent_id = EXCLUDED.parent_id, status = 'ACTIVE', deleted = 0, updated_at = CURRENT_TIMESTAMP
  RETURNING id INTO safety_dept_id;
  INSERT INTO sys_org (parent_id, org_type, org_code, org_name, org_path, sort_order, status, deleted)
  VALUES (company_id, 'DEPARTMENT', 'DEMO-OPS', '装配运营部', '/pending/', 20, 'ACTIVE', 0)
  ON CONFLICT (org_code) DO UPDATE SET org_name = EXCLUDED.org_name, parent_id = EXCLUDED.parent_id, status = 'ACTIVE', deleted = 0, updated_at = CURRENT_TIMESTAMP
  RETURNING id INTO ops_dept_id;
  INSERT INTO sys_org (parent_id, org_type, org_code, org_name, org_path, sort_order, status, deleted)
  VALUES (company_id, 'DEPARTMENT', 'DEMO-EQUIPMENT', '设备保障部', '/pending/', 30, 'ACTIVE', 0)
  ON CONFLICT (org_code) DO UPDATE SET org_name = EXCLUDED.org_name, parent_id = EXCLUDED.parent_id, status = 'ACTIVE', deleted = 0, updated_at = CURRENT_TIMESTAMP
  RETURNING id INTO equipment_dept_id;

  INSERT INTO sys_org (parent_id, org_type, org_code, org_name, org_path, sort_order, status, deleted)
  VALUES (ops_dept_id, 'WORKSHOP', 'DEMO-ASSEMBLY-WORKSHOP', '总装一车间', '/pending/', 10, 'ACTIVE', 0)
  ON CONFLICT (org_code) DO UPDATE SET org_name = EXCLUDED.org_name, parent_id = EXCLUDED.parent_id, status = 'ACTIVE', deleted = 0, updated_at = CURRENT_TIMESTAMP
  RETURNING id INTO assembly_workshop_id;
  INSERT INTO sys_org (parent_id, org_type, org_code, org_name, org_path, sort_order, status, deleted)
  VALUES (equipment_dept_id, 'WORKSHOP', 'DEMO-MAINTENANCE-WORKSHOP', '检维修车间', '/pending/', 20, 'ACTIVE', 0)
  ON CONFLICT (org_code) DO UPDATE SET org_name = EXCLUDED.org_name, parent_id = EXCLUDED.parent_id, status = 'ACTIVE', deleted = 0, updated_at = CURRENT_TIMESTAMP
  RETURNING id INTO maintenance_workshop_id;

  INSERT INTO sys_org (parent_id, org_type, org_code, org_name, org_path, sort_order, status, deleted)
  SELECT assembly_workshop_id, 'TEAM', code, name, '/pending/', sort_order, 'ACTIVE', 0
  FROM (VALUES
    ('DEMO-MORNING-TEAM', '晨星装配班', 10),
    ('DEMO-GREEN-TEAM', '青禾保障班', 20)) AS teams(code, name, sort_order)
  ON CONFLICT (org_code) DO UPDATE SET org_name = EXCLUDED.org_name, parent_id = EXCLUDED.parent_id, status = 'ACTIVE', deleted = 0, updated_at = CURRENT_TIMESTAMP;

  INSERT INTO sys_org (parent_id, org_type, org_code, org_name, org_path, sort_order, status, deleted)
  SELECT parent_id, 'TEAM', code, name, '/pending/', sort_order, 'ACTIVE', 0
  FROM (VALUES
    (maintenance_workshop_id, 'DEMO-BLUE-TEAM', '蓝盾检修班', 10),
    (safety_dept_id, 'DEMO-SAFETY-TEAM', '安全管理组', 20),
    (equipment_dept_id, 'DEMO-EQUIPMENT-TEAM', '设备运行组', 30)) AS teams(parent_id, code, name, sort_order)
  ON CONFLICT (org_code) DO UPDATE SET org_name = EXCLUDED.org_name, parent_id = EXCLUDED.parent_id, status = 'ACTIVE', deleted = 0, updated_at = CURRENT_TIMESTAMP;

  UPDATE sys_org child SET org_path = parent.org_path || child.id || '/'
  FROM sys_org parent WHERE child.parent_id = parent.id AND child.org_code LIKE 'DEMO-%';
  UPDATE sys_org child SET org_path = parent.org_path || child.id || '/'
  FROM sys_org parent WHERE child.parent_id = parent.id AND child.org_code LIKE 'DEMO-%';
  UPDATE sys_org child SET org_path = parent.org_path || child.id || '/'
  FROM sys_org parent WHERE child.parent_id = parent.id AND child.org_code LIKE 'DEMO-%';
END $$;

INSERT INTO sys_company_profile (org_id, short_name, description, address, company_type, company_intro, deleted)
SELECT id, '演示公司', '用于演示安全生产数字化管控流程的虚构企业。', '演示地址', 'MANUFACTURING', '演示公司围绕装配、设备保障和现场安全管理开展数字化演示。', 0
FROM sys_org WHERE org_code = 'DEMO-COMPANY'
ON CONFLICT (org_id) DO UPDATE SET short_name = EXCLUDED.short_name, description = EXCLUDED.description, address = EXCLUDED.address, company_type = EXCLUDED.company_type, company_intro = EXCLUDED.company_intro, deleted = 0, updated_at = CURRENT_TIMESTAMP;

INSERT INTO sys_department_profile (org_id, company_org_id, department_type, child_sort_order, description, top_level_name, group_name, deleted)
SELECT d.id, c.id, 'BUSINESS', d.sort_order, d.org_name || '负责演示业务运行。', c.org_name, '演示公司', 0
FROM sys_org d CROSS JOIN sys_org c
WHERE d.org_code IN ('DEMO-SAFETY', 'DEMO-OPS', 'DEMO-EQUIPMENT') AND c.org_code = 'DEMO-COMPANY'
ON CONFLICT (org_id) DO UPDATE SET company_org_id = EXCLUDED.company_org_id, description = EXCLUDED.description, deleted = 0, updated_at = CURRENT_TIMESTAMP;

INSERT INTO sys_team_profile (org_id, company_org_id, workshop_org_id, group_name, level1_unit, workshop_name, work_type_code, work_type_name, active, deleted)
SELECT t.id, c.id, w.id, '演示公司', d.org_name, w.org_name, t.org_code, t.org_name, 1, 0
FROM sys_org t JOIN sys_org w ON w.id = t.parent_id JOIN sys_org d ON d.id = w.parent_id CROSS JOIN sys_org c
WHERE t.org_code IN ('DEMO-MORNING-TEAM', 'DEMO-GREEN-TEAM', 'DEMO-BLUE-TEAM', 'DEMO-SAFETY-TEAM', 'DEMO-EQUIPMENT-TEAM') AND c.org_code = 'DEMO-COMPANY'
ON CONFLICT (org_id) DO UPDATE SET company_org_id = EXCLUDED.company_org_id, workshop_org_id = EXCLUDED.workshop_org_id, group_name = EXCLUDED.group_name, workshop_name = EXCLUDED.workshop_name, work_type_name = EXCLUDED.work_type_name, deleted = 0, updated_at = CURRENT_TIMESTAMP;

WITH people(username, real_name, org_code, role_code, employee_code, position_name, team_code) AS (VALUES
  ('portfolio-admin', '演示管理员', 'DEMO-COMPANY', 'ADMIN', 'DEMO-E001', '系统管理员', NULL),
  ('demo-company-lead', '林远', 'DEMO-COMPANY', 'COMPANY_LEADER', 'DEMO-E002', '企业负责人', NULL),
  ('demo-safety', '周宁', 'DEMO-SAFETY-TEAM', 'SAFETY_OFFICER', 'DEMO-E003', '安全管理员', 'DEMO-SAFETY-TEAM'),
  ('demo-workshop-lead', '沈川', 'DEMO-ASSEMBLY-WORKSHOP', 'WORKSHOP_DIRECTOR', 'DEMO-E004', '车间负责人', 'DEMO-MORNING-TEAM'),
  ('demo-team-lead', '顾晨', 'DEMO-MORNING-TEAM', 'TEAM_LEADER', 'DEMO-E005', '班组负责人', 'DEMO-MORNING-TEAM'),
  ('demo-worker', '许禾', 'DEMO-GREEN-TEAM', 'TEAM_MEMBER', 'DEMO-E006', '作业人员', 'DEMO-GREEN-TEAM'),
  ('demo-rectifier', '唐越', 'DEMO-BLUE-TEAM', 'TEAM_MEMBER', 'DEMO-E007', '整改负责人', 'DEMO-BLUE-TEAM'),
  ('demo-acceptor', '苏晴', 'DEMO-SAFETY-TEAM', 'SAFETY_OFFICER', 'DEMO-E008', '验收人员', 'DEMO-SAFETY-TEAM'),
  ('demo-member-01', '叶青', 'DEMO-MORNING-TEAM', 'TEAM_MEMBER', 'DEMO-E009', '装配作业员', 'DEMO-MORNING-TEAM'),
  ('demo-member-02', '韩松', 'DEMO-MORNING-TEAM', 'TEAM_MEMBER', 'DEMO-E010', '装配作业员', 'DEMO-MORNING-TEAM'),
  ('demo-member-03', '白露', 'DEMO-GREEN-TEAM', 'TEAM_MEMBER', 'DEMO-E011', '保障作业员', 'DEMO-GREEN-TEAM'),
  ('demo-member-04', '赵川', 'DEMO-BLUE-TEAM', 'TEAM_MEMBER', 'DEMO-E012', '检修作业员', 'DEMO-BLUE-TEAM'),
  ('demo-member-05', '方圆', 'DEMO-EQUIPMENT-TEAM', 'TEAM_MEMBER', 'DEMO-E013', '设备运行员', 'DEMO-EQUIPMENT-TEAM'),
  ('demo-member-06', '宋安', 'DEMO-SAFETY-TEAM', 'TEAM_MEMBER', 'DEMO-E014', '安全协调员', 'DEMO-SAFETY-TEAM'))
INSERT INTO sys_user (username, password_hash, real_name, org_id, status, deleted)
SELECT p.username, a.password_hash, p.real_name, o.id, 'ACTIVE', 0
FROM people p JOIN sys_org o ON o.org_code = p.org_code CROSS JOIN LATERAL (SELECT password_hash FROM sys_user WHERE username = 'portfolio-admin' LIMIT 1) a
ON CONFLICT (username) DO UPDATE SET real_name = EXCLUDED.real_name, org_id = EXCLUDED.org_id, status = 'ACTIVE', deleted = 0, updated_at = CURRENT_TIMESTAMP;

WITH people(username, employee_code, company_code, department_code, team_code, role_code, position_name) AS (VALUES
  ('portfolio-admin', 'DEMO-E001', 'DEMO-COMPANY', NULL, NULL, 'ADMIN', '系统管理员'),
  ('demo-company-lead', 'DEMO-E002', 'DEMO-COMPANY', NULL, NULL, 'COMPANY_LEADER', '企业负责人'),
  ('demo-safety', 'DEMO-E003', 'DEMO-COMPANY', 'DEMO-SAFETY', 'DEMO-SAFETY-TEAM', 'SAFETY_OFFICER', '安全管理员'),
  ('demo-workshop-lead', 'DEMO-E004', 'DEMO-COMPANY', 'DEMO-OPS', 'DEMO-MORNING-TEAM', 'WORKSHOP_DIRECTOR', '车间负责人'),
  ('demo-team-lead', 'DEMO-E005', 'DEMO-COMPANY', 'DEMO-OPS', 'DEMO-MORNING-TEAM', 'TEAM_LEADER', '班组负责人'),
  ('demo-worker', 'DEMO-E006', 'DEMO-COMPANY', 'DEMO-OPS', 'DEMO-GREEN-TEAM', 'TEAM_MEMBER', '作业人员'),
  ('demo-rectifier', 'DEMO-E007', 'DEMO-COMPANY', 'DEMO-EQUIPMENT', 'DEMO-BLUE-TEAM', 'TEAM_MEMBER', '整改负责人'),
  ('demo-acceptor', 'DEMO-E008', 'DEMO-COMPANY', 'DEMO-SAFETY', 'DEMO-SAFETY-TEAM', 'SAFETY_OFFICER', '验收人员'),
  ('demo-member-01', 'DEMO-E009', 'DEMO-COMPANY', 'DEMO-OPS', 'DEMO-MORNING-TEAM', 'TEAM_MEMBER', '装配作业员'),
  ('demo-member-02', 'DEMO-E010', 'DEMO-COMPANY', 'DEMO-OPS', 'DEMO-MORNING-TEAM', 'TEAM_MEMBER', '装配作业员'),
  ('demo-member-03', 'DEMO-E011', 'DEMO-COMPANY', 'DEMO-OPS', 'DEMO-GREEN-TEAM', 'TEAM_MEMBER', '保障作业员'),
  ('demo-member-04', 'DEMO-E012', 'DEMO-COMPANY', 'DEMO-EQUIPMENT', 'DEMO-BLUE-TEAM', 'TEAM_MEMBER', '检修作业员'),
  ('demo-member-05', 'DEMO-E013', 'DEMO-COMPANY', 'DEMO-EQUIPMENT', 'DEMO-EQUIPMENT-TEAM', 'TEAM_MEMBER', '设备运行员'),
  ('demo-member-06', 'DEMO-E014', 'DEMO-COMPANY', 'DEMO-SAFETY', 'DEMO-SAFETY-TEAM', 'TEAM_MEMBER', '安全协调员'))
INSERT INTO sys_user_profile (user_id, employee_code, company_org_id, company_short_name, department_org_id, team_org_id, employee_type, position_name, system_role_code, deleted)
SELECT u.id, p.employee_code, c.id, '演示公司', d.id, t.id, 'FULL_TIME', p.position_name, p.role_code, 0
FROM people p JOIN sys_user u ON u.username = p.username JOIN sys_org c ON c.org_code = p.company_code LEFT JOIN sys_org d ON d.org_code = p.department_code LEFT JOIN sys_org t ON t.org_code = p.team_code
ON CONFLICT (user_id) DO UPDATE SET employee_code = EXCLUDED.employee_code, company_org_id = EXCLUDED.company_org_id, department_org_id = EXCLUDED.department_org_id, team_org_id = EXCLUDED.team_org_id, position_name = EXCLUDED.position_name, system_role_code = EXCLUDED.system_role_code, deleted = 0, updated_at = CURRENT_TIMESTAMP;

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u JOIN (VALUES
  ('portfolio-admin', 'ADMIN'), ('demo-company-lead', 'COMPANY_LEADER'), ('demo-safety', 'SAFETY_OFFICER'), ('demo-workshop-lead', 'WORKSHOP_DIRECTOR'), ('demo-team-lead', 'TEAM_LEADER'), ('demo-worker', 'TEAM_MEMBER'), ('demo-rectifier', 'TEAM_MEMBER'), ('demo-acceptor', 'SAFETY_OFFICER'), ('demo-member-01', 'TEAM_MEMBER'), ('demo-member-02', 'TEAM_MEMBER'), ('demo-member-03', 'TEAM_MEMBER'), ('demo-member-04', 'TEAM_MEMBER'), ('demo-member-05', 'TEAM_MEMBER'), ('demo-member-06', 'TEAM_MEMBER')) x(username, role_code) ON x.username = u.username JOIN sys_role r ON r.role_code = x.role_code
ON CONFLICT (user_id, role_id) DO NOTHING;

INSERT INTO sys_team_member (team_org_id, user_id, username, member_name, member_role, deleted)
SELECT t.id, u.id, u.username, u.real_name, CASE WHEN u.username IN ('demo-team-lead', 'demo-company-lead') THEN 'LEADER' ELSE 'MEMBER' END, 0
FROM sys_user u JOIN sys_user_profile p ON p.user_id = u.id JOIN sys_org t ON t.id = p.team_org_id WHERE p.team_org_id IS NOT NULL
ON CONFLICT (team_org_id, username, member_role) DO UPDATE SET user_id = EXCLUDED.user_id, member_name = EXCLUDED.member_name, deleted = 0;

INSERT INTO sys_check_item_library (risk_type, check_item, applicable_stage, default_check_result, default_rectification_description, default_follow_up_plan, require_image, require_video, sort_order, status, created_by, updated_by, deleted)
VALUES
('触电', '临时用电箱门是否闭合并上锁', 'PRE_SHIFT_INSPECTION', '无隐患', '恢复箱门闭合并完成挂牌', '班中复核用电箱状态', 1, 0, 10, 'ACTIVE', (SELECT id FROM sys_user WHERE username='portfolio-admin'), (SELECT id FROM sys_user WHERE username='portfolio-admin'), 0),
('机械伤害', '设备防护罩和联锁装置是否完好', 'PRE_SHIFT_INSPECTION', '无隐患', '停机检查并恢复防护', '设备管理员跟进确认', 1, 0, 20, 'ACTIVE', (SELECT id FROM sys_user WHERE username='portfolio-admin'), (SELECT id FROM sys_user WHERE username='portfolio-admin'), 0),
('高处坠落', '高处作业临边防护和安全带挂点是否可靠', 'PRE_SHIFT_INSPECTION', '无隐患', '补齐临边防护并重新验收', '作业前由监护人复核', 1, 1, 30, 'ACTIVE', (SELECT id FROM sys_user WHERE username='portfolio-admin'), (SELECT id FROM sys_user WHERE username='portfolio-admin'), 0),
('起重伤害', '吊装区域警戒线和指挥信号是否清晰', 'MID_SHIFT_INSPECTION', '无隐患', '恢复警戒隔离并明确指挥人员', '班中巡查记录', 1, 1, 40, 'ACTIVE', (SELECT id FROM sys_user WHERE username='portfolio-admin'), (SELECT id FROM sys_user WHERE username='portfolio-admin'), 0),
('车辆伤害', '人车分流通道是否畅通', 'MID_SHIFT_INSPECTION', '无隐患', '移除通道内物料并恢复标线', '班后清场确认', 1, 0, 50, 'ACTIVE', (SELECT id FROM sys_user WHERE username='portfolio-admin'), (SELECT id FROM sys_user WHERE username='portfolio-admin'), 0),
('火灾', '动火点灭火器材和监护措施是否到位', 'PRE_SHIFT_MEETING_CONFIRMATION', '无隐患', '补齐灭火器材并完成交底', '动火结束后复查', 1, 1, 60, 'ACTIVE', (SELECT id FROM sys_user WHERE username='portfolio-admin'), (SELECT id FROM sys_user WHERE username='portfolio-admin'), 0),
('物体打击', '高处物料和工具是否采取防坠措施', 'MID_SHIFT_INSPECTION', '无隐患', '清理散放物料并加固工具', '现场负责人复核', 0, 0, 70, 'ACTIVE', (SELECT id FROM sys_user WHERE username='portfolio-admin'), (SELECT id FROM sys_user WHERE username='portfolio-admin'), 0),
('用电安全', '班后是否完成断电和电源整理', 'POST_SHIFT_INSPECTION', '无隐患', '完成断电并记录交接', '下一班接班确认', 0, 0, 80, 'ACTIVE', (SELECT id FROM sys_user WHERE username='portfolio-admin'), (SELECT id FROM sys_user WHERE username='portfolio-admin'), 0);

INSERT INTO sys_team_check_template (name, company_org_id, department_org_id, team_org_id, inspection_stage, status, version, created_by, updated_by, deleted)
SELECT '晨星装配班班前检查模板', c.id, d.id, t.id, 'PRE_SHIFT_INSPECTION', 'ACTIVE', 1, u.id, u.id, 0 FROM sys_org c, sys_org d, sys_org t, sys_user u WHERE c.org_code='DEMO-COMPANY' AND d.org_code='DEMO-OPS' AND t.org_code='DEMO-MORNING-TEAM' AND u.username='portfolio-admin'
UNION ALL SELECT '晨星装配班班中检查模板', c.id, d.id, t.id, 'MID_SHIFT_INSPECTION', 'ACTIVE', 1, u.id, u.id, 0 FROM sys_org c, sys_org d, sys_org t, sys_user u WHERE c.org_code='DEMO-COMPANY' AND d.org_code='DEMO-OPS' AND t.org_code='DEMO-MORNING-TEAM' AND u.username='portfolio-admin'
UNION ALL SELECT '晨星装配班班后检查模板', c.id, d.id, t.id, 'POST_SHIFT_INSPECTION', 'ACTIVE', 1, u.id, u.id, 0 FROM sys_org c, sys_org d, sys_org t, sys_user u WHERE c.org_code='DEMO-COMPANY' AND d.org_code='DEMO-OPS' AND t.org_code='DEMO-MORNING-TEAM' AND u.username='portfolio-admin'
UNION ALL SELECT '青禾保障班班前检查模板', c.id, d.id, t.id, 'PRE_SHIFT_INSPECTION', 'ACTIVE', 1, u.id, u.id, 0 FROM sys_org c, sys_org d, sys_org t, sys_user u WHERE c.org_code='DEMO-COMPANY' AND d.org_code='DEMO-OPS' AND t.org_code='DEMO-GREEN-TEAM' AND u.username='portfolio-admin'
ON CONFLICT (company_org_id, department_org_id, team_org_id, inspection_stage) WHERE deleted = 0 DO UPDATE SET name = EXCLUDED.name, status = 'ACTIVE', updated_at = CURRENT_TIMESTAMP;

INSERT INTO sys_team_check_template_item (template_id, library_item_id, risk_type, check_item, default_check_result, default_rectification_description, default_follow_up_plan, require_image, require_video, sort_order, created_by, updated_by, deleted)
SELECT t.id, i.id, i.risk_type, i.check_item, i.default_check_result, i.default_rectification_description, i.default_follow_up_plan, i.require_image, i.require_video, row_number() OVER (PARTITION BY t.id ORDER BY i.id), u.id, u.id, 0
FROM sys_team_check_template t JOIN sys_check_item_library i ON i.deleted=0 AND ((t.inspection_stage = i.applicable_stage) OR (t.inspection_stage='PRE_SHIFT_INSPECTION' AND i.applicable_stage='PRE_SHIFT_MEETING_CONFIRMATION')) CROSS JOIN sys_user u
WHERE t.deleted=0 AND u.username='portfolio-admin' AND i.id IN (SELECT id FROM sys_check_item_library ORDER BY id LIMIT 4)
ON CONFLICT DO NOTHING;

COMMIT;
