CREATE TABLE sys_company_profile (
  id BIGSERIAL PRIMARY KEY,
  org_id BIGINT NOT NULL,
  short_name VARCHAR(120) NULL,
  description TEXT NULL,
  address VARCHAR(255) NULL,
  company_type VARCHAR(32) NULL,
  level1_name VARCHAR(120) NULL,
  level2_name VARCHAR(120) NULL,
  level3_name VARCHAR(120) NULL,
  level4_name VARCHAR(120) NULL,
  safety_manager_username VARCHAR(120) NULL,
  reporter_l1_usernames VARCHAR(500) NULL,
  reporter_l2_usernames VARCHAR(500) NULL,
  reporter_l3_usernames VARCHAR(500) NULL,
  report_l1_time INT NULL,
  report_l2_time INT NULL,
  report_l3_time INT NULL,
  attachment1_url VARCHAR(500) NULL,
  attachment2_url VARCHAR(500) NULL,
  company_intro TEXT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted SMALLINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_sys_company_profile_org ON sys_company_profile (org_id);
CREATE INDEX idx_sys_company_profile_type ON sys_company_profile (company_type);

CREATE TABLE sys_department_profile (
  id BIGSERIAL PRIMARY KEY,
  org_id BIGINT NOT NULL,
  company_org_id BIGINT NOT NULL,
  department_type VARCHAR(32) NULL,
  child_sort_order INT NOT NULL DEFAULT 0,
  leader_username VARCHAR(120) NULL,
  description TEXT NULL,
  top_level_name VARCHAR(120) NULL,
  group_name VARCHAR(120) NULL,
  level1_unit VARCHAR(120) NULL,
  level2_unit VARCHAR(120) NULL,
  leader_level VARCHAR(64) NULL,
  company_sort_order INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted SMALLINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_sys_department_profile_org ON sys_department_profile (org_id);
CREATE INDEX idx_sys_department_profile_company ON sys_department_profile (company_org_id);
CREATE INDEX idx_sys_department_profile_type ON sys_department_profile (department_type);

CREATE TABLE sys_team_profile (
  id BIGSERIAL PRIMARY KEY,
  org_id BIGINT NOT NULL,
  company_org_id BIGINT NOT NULL,
  workshop_org_id BIGINT NULL,
  group_name VARCHAR(120) NULL,
  level1_unit VARCHAR(120) NULL,
  level2_unit VARCHAR(120) NULL,
  workshop_name VARCHAR(120) NULL,
  work_type_code VARCHAR(120) NULL,
  work_type_name VARCHAR(180) NULL,
  leader_username VARCHAR(120) NULL,
  safety_officer_username VARCHAR(120) NULL,
  points INT NOT NULL DEFAULT 0,
  active SMALLINT NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted SMALLINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_sys_team_profile_org ON sys_team_profile (org_id);
CREATE INDEX idx_sys_team_profile_company ON sys_team_profile (company_org_id);
CREATE INDEX idx_sys_team_profile_workshop ON sys_team_profile (workshop_org_id);
CREATE INDEX idx_sys_team_profile_work_type ON sys_team_profile (work_type_code);

CREATE TABLE sys_team_member (
  id BIGSERIAL PRIMARY KEY,
  team_org_id BIGINT NOT NULL,
  user_id BIGINT NULL,
  username VARCHAR(120) NOT NULL,
  member_name VARCHAR(120) NULL,
  member_role VARCHAR(32) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted SMALLINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_sys_team_member_role ON sys_team_member (team_org_id, username, member_role);
CREATE INDEX idx_sys_team_member_user ON sys_team_member (user_id);

CREATE TABLE sys_user_profile (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL,
  employee_code VARCHAR(120) NOT NULL,
  company_org_id BIGINT NULL,
  company_short_name VARCHAR(120) NULL,
  department_org_id BIGINT NULL,
  team_org_id BIGINT NULL,
  points INT NOT NULL DEFAULT 0,
  received_points INT NOT NULL DEFAULT 0,
  employee_type VARCHAR(32) NULL,
  position_name VARCHAR(120) NULL,
  system_role_code VARCHAR(64) NULL,
  department_sort_order INT NOT NULL DEFAULT 0,
  management_weight INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted SMALLINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_sys_user_profile_user ON sys_user_profile (user_id);
CREATE UNIQUE INDEX uk_sys_user_profile_employee_code ON sys_user_profile (employee_code);
CREATE INDEX idx_sys_user_profile_company ON sys_user_profile (company_org_id);
CREATE INDEX idx_sys_user_profile_department ON sys_user_profile (department_org_id);
CREATE INDEX idx_sys_user_profile_team ON sys_user_profile (team_org_id);
CREATE INDEX idx_sys_user_profile_role ON sys_user_profile (system_role_code);

CREATE TABLE sys_menu (
  id BIGSERIAL PRIMARY KEY,
  parent_id BIGINT NULL,
  menu_code VARCHAR(120) NOT NULL,
  title VARCHAR(120) NOT NULL,
  route_path VARCHAR(255) NOT NULL,
  component VARCHAR(255) NOT NULL,
  icon VARCHAR(120) NULL,
  permission_code VARCHAR(120) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  visible SMALLINT NOT NULL DEFAULT 1,
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted SMALLINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_sys_menu_code ON sys_menu (menu_code);
CREATE UNIQUE INDEX uk_sys_menu_permission ON sys_menu (permission_code);
CREATE INDEX idx_sys_menu_parent ON sys_menu (parent_id);
CREATE INDEX idx_sys_menu_status ON sys_menu (status, deleted);

CREATE TABLE sys_role_menu (
  id BIGSERIAL PRIMARY KEY,
  role_id BIGINT NOT NULL,
  menu_id BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_sys_role_menu ON sys_role_menu (role_id, menu_id);
CREATE INDEX idx_sys_role_menu_menu ON sys_role_menu (menu_id);

CREATE TABLE sys_access_log (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NULL,
  username VARCHAR(120) NULL,
  module VARCHAR(64) NOT NULL,
  target_type VARCHAR(64) NOT NULL,
  target_id BIGINT NULL,
  action VARCHAR(64) NOT NULL,
  summary VARCHAR(500) NULL,
  request_path VARCHAR(255) NULL,
  request_method VARCHAR(16) NULL,
  ip_address VARCHAR(64) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sys_access_log_operator ON sys_access_log (user_id, created_at);
CREATE INDEX idx_sys_access_log_module ON sys_access_log (module, action, created_at);
CREATE INDEX idx_sys_access_log_target ON sys_access_log (target_type, target_id);

CREATE TABLE sys_login_attempt (
  id BIGSERIAL PRIMARY KEY,
  username VARCHAR(120) NOT NULL,
  user_id BIGINT NULL,
  success SMALLINT NOT NULL DEFAULT 0,
  failure_reason VARCHAR(255) NULL,
  ip_address VARCHAR(64) NULL,
  attempted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  locked_until TIMESTAMP NULL
);

CREATE INDEX idx_sys_login_attempt_username ON sys_login_attempt (username, attempted_at);
CREATE INDEX idx_sys_login_attempt_user ON sys_login_attempt (user_id, attempted_at);

CREATE TABLE sys_verification_code (
  id BIGSERIAL PRIMARY KEY,
  username VARCHAR(120) NOT NULL,
  mobile VARCHAR(32) NULL,
  code_hash VARCHAR(255) NOT NULL,
  purpose VARCHAR(64) NOT NULL,
  send_channel VARCHAR(32) NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
  expires_at TIMESTAMP NOT NULL,
  verified_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sys_verification_code_username ON sys_verification_code (username, purpose, status);
CREATE INDEX idx_sys_verification_code_expiry ON sys_verification_code (expires_at);

CREATE TABLE sys_import_job (
  id BIGSERIAL PRIMARY KEY,
  module VARCHAR(64) NOT NULL,
  original_filename VARCHAR(255) NOT NULL,
  status VARCHAR(32) NOT NULL,
  total_rows INT NOT NULL DEFAULT 0,
  success_rows INT NOT NULL DEFAULT 0,
  failure_rows INT NOT NULL DEFAULT 0,
  error_summary TEXT NULL,
  created_by BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  completed_at TIMESTAMP NULL
);

CREATE INDEX idx_sys_import_job_module ON sys_import_job (module, status, created_at);
CREATE INDEX idx_sys_import_job_operator ON sys_import_job (created_by, created_at);

INSERT INTO sys_menu (
  id, parent_id, menu_code, title, route_path, component, icon, permission_code, sort_order, visible, status
) VALUES
  (1000, NULL, 'SYSTEM_MANAGEMENT', '系统管理', '/system', 'LAYOUT', 'lucide:settings', 'SYSTEM_MANAGEMENT_VIEW', -90, 1, 'ACTIVE'),
  (1001, 1000, 'SYSTEM_ACCOUNT_MANAGEMENT', '账户管理', '/system/account-management', '/system-management/account-management/index.vue', 'lucide:user-cog', 'SYSTEM_ACCOUNT_VIEW', 1, 1, 'ACTIVE'),
  (1002, 1000, 'SYSTEM_ROLE_PERMISSION', '角色权限', '/system/role-permission', '/system-management/role-permission/index.vue', 'lucide:shield-check', 'SYSTEM_ROLE_VIEW', 2, 1, 'ACTIVE'),
  (1003, 1000, 'SYSTEM_COMPANY_MANAGEMENT', '公司管理', '/system/company-management', '/system-management/company-management/index.vue', 'lucide:building-2', 'SYSTEM_COMPANY_VIEW', 3, 1, 'ACTIVE'),
  (1004, 1000, 'SYSTEM_DEPARTMENT_MANAGEMENT', '部门管理', '/system/department-management', '/system-management/department-management/index.vue', 'lucide:network', 'SYSTEM_DEPARTMENT_VIEW', 4, 1, 'ACTIVE'),
  (1005, 1000, 'SYSTEM_TEAM_MANAGEMENT', '班组管理', '/system/team-management', '/system-management/team-management/index.vue', 'lucide:users', 'SYSTEM_TEAM_VIEW', 5, 1, 'ACTIVE'),
  (1006, 1000, 'SYSTEM_PERSONNEL_MANAGEMENT', '人员管理', '/system/personnel-management', '/system-management/personnel-management/index.vue', 'lucide:id-card', 'SYSTEM_PERSONNEL_VIEW', 6, 1, 'ACTIVE'),
  (1007, 1000, 'SYSTEM_MENU_MANAGEMENT', '菜单管理', '/system/menu-management', '/system-management/menu-management/index.vue', 'lucide:list-tree', 'SYSTEM_MENU_VIEW', 7, 1, 'ACTIVE');

INSERT INTO sys_menu (
  id, parent_id, menu_code, title, route_path, component, icon, permission_code, sort_order, visible, status
) VALUES
  (2000, NULL, 'PINGAN_PERMISSION_GROUP', '平安班组业务权限', '/__permissions/pingan', 'PERMISSION_GROUP', 'lucide:key-round', 'PINGAN_PERMISSION_GROUP', 200, 0, 'ACTIVE'),
  (2001, 2000, 'PINGAN_THREE_CHECK_VIEW', '一班三查查看', '/__permissions/pingan/three-check-view', 'PERMISSION', NULL, 'PINGAN_THREE_CHECK_VIEW', 201, 0, 'ACTIVE'),
  (2002, 2000, 'PINGAN_TEAM_DISPATCH_MANAGE', '班组派班管理', '/__permissions/pingan/team-dispatch-manage', 'PERMISSION', NULL, 'PINGAN_TEAM_DISPATCH_MANAGE', 202, 0, 'ACTIVE'),
  (2003, 2000, 'PINGAN_THREE_CHECK_EXECUTE', '一班三查执行', '/__permissions/pingan/three-check-execute', 'PERMISSION', NULL, 'PINGAN_THREE_CHECK_EXECUTE', 203, 0, 'ACTIVE'),
  (2004, 2000, 'PINGAN_THREE_CHECK_WITHDRAW', '一班三查撤回', '/__permissions/pingan/three-check-withdraw', 'PERMISSION', NULL, 'PINGAN_THREE_CHECK_WITHDRAW', 204, 0, 'ACTIVE'),
  (2005, 2000, 'PINGAN_THREE_CHECK_REMIND', '一班三查催办', '/__permissions/pingan/three-check-remind', 'PERMISSION', NULL, 'PINGAN_THREE_CHECK_REMIND', 205, 0, 'ACTIVE'),
  (2006, 2000, 'PINGAN_HAZARD_RECTIFICATION', '隐患整改闭环', '/__permissions/pingan/hazard-rectification', 'PERMISSION', NULL, 'PINGAN_HAZARD_RECTIFICATION', 206, 0, 'ACTIVE'),
  (2007, 2000, 'PINGAN_HAZARD_REPORT', '随手拍上报', '/__permissions/pingan/hazard-report', 'PERMISSION', NULL, 'PINGAN_HAZARD_REPORT', 207, 0, 'ACTIVE'),
  (2008, 2000, 'PINGAN_TRAINING_STUDY_EXAM', '学习考试', '/__permissions/pingan/training-study-exam', 'PERMISSION', NULL, 'PINGAN_TRAINING_STUDY_EXAM', 208, 0, 'ACTIVE'),
  (2009, 2000, 'PINGAN_SPECIAL_WORK_APPROVE', '特种作业审批', '/__permissions/pingan/special-work-approve', 'PERMISSION', NULL, 'PINGAN_SPECIAL_WORK_APPROVE', 209, 0, 'ACTIVE'),
  (2010, 2000, 'PINGAN_REPORT_VIEW', '安全报表查看', '/__permissions/pingan/report-view', 'PERMISSION', NULL, 'PINGAN_REPORT_VIEW', 210, 0, 'ACTIVE'),
  (2011, 2000, 'PINGAN_GLOBAL_SAFETY_INSPECTION', '安全检查与集团监管', '/__permissions/pingan/global-safety-inspection', 'PERMISSION', NULL, 'PINGAN_GLOBAL_SAFETY_INSPECTION', 211, 0, 'ACTIVE'),
  (2012, 2000, 'PINGAN_RISK_CONFIRM', '风险告知确认', '/__permissions/pingan/risk-confirm', 'PERMISSION', NULL, 'PINGAN_RISK_CONFIRM', 212, 0, 'ACTIVE'),
  (2013, 2000, 'PINGAN_SPECIAL_WORK_REVIEW', '特种作业审核', '/__permissions/pingan/special-work-review', 'PERMISSION', NULL, 'PINGAN_SPECIAL_WORK_REVIEW', 213, 0, 'ACTIVE'),
  (2014, 2000, 'PINGAN_KEY_SITE_INSPECTION', '重点场所巡检', '/__permissions/pingan/key-site-inspection', 'PERMISSION', NULL, 'PINGAN_KEY_SITE_INSPECTION', 214, 0, 'ACTIVE'),
  (2015, 2000, 'PINGAN_TRAINING_MANAGE', '教培管理', '/__permissions/pingan/training-manage', 'PERMISSION', NULL, 'PINGAN_TRAINING_MANAGE', 215, 0, 'ACTIVE');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
CROSS JOIN sys_menu m
WHERE r.role_code = 'ADMIN'
  AND m.menu_code LIKE 'SYSTEM_%';

UPDATE sys_role
SET role_name = '班长', data_scope = 'ORG_AND_CHILDREN'
WHERE role_code = 'TEAM_LEADER';

INSERT INTO sys_role (id, role_code, role_name, data_scope) VALUES
  (4, 'COMPANY_LEADER', '公司领导', 'ORG_AND_CHILDREN'),
  (5, 'ENTERPRISE_LEADER', '企业领导', 'ORG_AND_CHILDREN'),
  (6, 'DEPARTMENT_MANAGER', '部门经理', 'ORG_AND_CHILDREN'),
  (7, 'WORKSHOP_DIRECTOR', '车间主任', 'ORG_AND_CHILDREN'),
  (8, 'TEAM_MEMBER', '组员', 'SELF');

INSERT INTO sys_user (id, username, password_hash, real_name, mobile, org_id, status) VALUES
  (10001, 'company_leader', '{noop}123456', '公司领导', '13810000001', 2, 'ACTIVE'),
  (10002, 'enterprise_leader', '{noop}123456', '企业领导', '13810000002', 8, 'ACTIVE'),
  (10003, 'department_manager', '{noop}123456', '部门经理', '13810000003', 8, 'ACTIVE'),
  (10004, 'workshop_director', '{noop}123456', '车间主任', '13810000004', 8, 'ACTIVE'),
  (10005, 'team_leader', '{noop}123456', '班长', '13810000005', 8, 'ACTIVE'),
  (10006, 'team_member', '{noop}123456', '组员', '13810000006', 8, 'ACTIVE');

INSERT INTO sys_user_role (user_id, role_id) VALUES
  (10001, 4),
  (10002, 5),
  (10003, 6),
  (10004, 7),
  (10005, 3),
  (10006, 8);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
CROSS JOIN sys_menu m
WHERE r.role_code IN (
    'COMPANY_LEADER',
    'ENTERPRISE_LEADER',
    'DEPARTMENT_MANAGER',
    'WORKSHOP_DIRECTOR',
    'TEAM_LEADER',
    'TEAM_MEMBER'
  )
  AND m.menu_code LIKE 'SYSTEM_%';

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
CROSS JOIN sys_menu m
WHERE r.role_code IN ('COMPANY_LEADER', 'ENTERPRISE_LEADER')
  AND m.permission_code IN (
    'PINGAN_THREE_CHECK_VIEW',
    'PINGAN_REPORT_VIEW',
    'PINGAN_GLOBAL_SAFETY_INSPECTION'
  );

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
CROSS JOIN sys_menu m
WHERE r.role_code = 'DEPARTMENT_MANAGER'
  AND m.permission_code IN (
    'PINGAN_THREE_CHECK_VIEW',
    'PINGAN_THREE_CHECK_REMIND',
    'PINGAN_REPORT_VIEW',
    'PINGAN_SPECIAL_WORK_APPROVE'
  );

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
CROSS JOIN sys_menu m
WHERE r.role_code = 'WORKSHOP_DIRECTOR'
  AND m.permission_code IN (
    'PINGAN_THREE_CHECK_VIEW',
    'PINGAN_TEAM_DISPATCH_MANAGE',
    'PINGAN_THREE_CHECK_REMIND',
    'PINGAN_THREE_CHECK_WITHDRAW',
    'PINGAN_HAZARD_RECTIFICATION'
  );

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
CROSS JOIN sys_menu m
WHERE r.role_code = 'SAFETY_OFFICER'
  AND m.permission_code IN (
    'PINGAN_THREE_CHECK_VIEW',
    'PINGAN_THREE_CHECK_REMIND',
    'PINGAN_HAZARD_RECTIFICATION',
    'PINGAN_SPECIAL_WORK_REVIEW',
    'PINGAN_KEY_SITE_INSPECTION',
    'PINGAN_TRAINING_MANAGE'
  );

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
CROSS JOIN sys_menu m
WHERE r.role_code = 'TEAM_LEADER'
  AND m.permission_code IN (
    'PINGAN_THREE_CHECK_VIEW',
    'PINGAN_THREE_CHECK_EXECUTE',
    'PINGAN_THREE_CHECK_WITHDRAW',
    'PINGAN_HAZARD_RECTIFICATION'
  );

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
CROSS JOIN sys_menu m
WHERE r.role_code = 'TEAM_MEMBER'
  AND m.permission_code IN (
    'PINGAN_THREE_CHECK_VIEW',
    'PINGAN_RISK_CONFIRM',
    'PINGAN_HAZARD_REPORT',
    'PINGAN_TRAINING_STUDY_EXAM'
  );
