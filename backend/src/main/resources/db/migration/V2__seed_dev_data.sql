INSERT INTO sys_org (id, parent_id, org_type, org_code, org_name, org_path, sort_order, status) VALUES
  (1, NULL, 'GROUP', 'GSKG', 'Demo控股集团', '/1/', 1, 'ACTIVE'),
  (2, 1, 'GROUP', 'GSKY', 'Demo Safety Holdings', '/1/2/', 1, 'ACTIVE'),
  (3, 2, 'COMPANY', 'GSMQ', 'Demo Works Company', '/1/2/3/', 1, 'ACTIVE'),
  (4, 3, 'COMPANY', 'YCGS', 'Demo Works Company', '/1/2/3/4/', 1, 'ACTIVE'),
  (5, 4, 'COMPANY', 'SZGSMQ', 'Demo Works Company', '/1/2/3/4/5/', 1, 'ACTIVE'),
  (6, 5, 'DEPARTMENT', 'HBA6', '湖贝A6', '/1/2/3/4/5/6/', 1, 'ACTIVE'),
  (7, 6, 'TEAM', '4000-MQXM001-001', '4000-MQXM001-001', '/1/2/3/4/5/6/7/', 1, 'ACTIVE'),
  (8, 2, 'COMPANY', 'MZJS', 'Demo East Site', '/1/2/8/', 2, 'ACTIVE'),
  (9, 8, 'DEPARTMENT', 'ZYCJ', '制氧车间', '/1/2/8/9/', 1, 'ACTIVE'),
  (10, 9, 'TEAM', 'MZJS-EARLY', '早班', '/1/2/8/9/10/', 1, 'ACTIVE');

INSERT INTO sys_role (id, role_code, role_name, data_scope) VALUES
  (1, 'ADMIN', '管理员', 'ALL'),
  (2, 'SAFETY_OFFICER', '安全员', 'ORG_AND_CHILDREN'),
  (3, 'TEAM_LEADER', '班长', 'ORG_AND_CHILDREN');

INSERT INTO sys_user (id, username, password_hash, real_name, mobile, org_id, status) VALUES
  (1, 'admin', '{noop}DISABLED_UNTIL_LOCAL_INIT', '系统管理员', NULL, 1, 'ACTIVE'),
  (2, 'HB_MONITOR', '{noop}DISABLED_UNTIL_LOCAL_INIT', '湖贝班长', NULL, 7, 'ACTIVE'),
  (3, 'MQ_SAFE', '{noop}DISABLED_UNTIL_LOCAL_INIT', '幕墙安全员', NULL, 5, 'ACTIVE'),
  (4, 'ZY_SUPERVISOR', '{noop}DISABLED_UNTIL_LOCAL_INIT', '制氧主管', NULL, 10, 'ACTIVE');

INSERT INTO sys_user_role (user_id, role_id) VALUES
  (1, 1),
  (2, 3),
  (3, 2),
  (4, 3);

INSERT INTO shift_task (id, task_no, company_id, department_id, team_id, shift_date, shift_name, leader_user_id, status) VALUES
  (1001, 'TASK-20250512-001', 5, 6, 7, '2025-05-12', '早班', 2, 'OPEN'),
  (1002, 'TASK-20250423-001', 5, 6, 7, '2025-04-23', '早班', 2, 'OPEN'),
  (1003, 'TASK-20250418-001', 8, 9, 10, '2025-04-18', '早班', 4, 'OPEN');

INSERT INTO pre_shift_meeting (
  id, task_id, meeting_no, company_id, department_id, team_id, owner_user_id,
  meeting_date, meeting_content, attendees_count, image_check_status, video_check_status,
  status, submitted_by, submitted_at, reminder_count
) VALUES
  (2001, 1001, 'BQM-20250512-001', 5, 6, 7, 2, '2025-05-12', '交底吊篮作业风险和临边防护要求。', 5, '现场照片', '视频已传', 'OPENED', 2, '2025-05-12 08:20:00', 0),
  (2002, 1002, 'BQM-20250423-001', 5, 6, 7, 2, '2025-04-23', '确认材料转运路线和动火审批要求。', 5, '现场照片', '视频已传', 'ARCHIVED', 2, '2025-04-23 08:18:00', 1),
  (2003, 1003, 'BQM-20250418-001', 8, 9, 10, 4, '2025-04-18', '待补充班前会照片和视频。', 8, '待补传', '未上传', 'DRAFT', NULL, NULL, 0);

INSERT INTO pre_shift_meeting_attendee (meeting_id, user_id, attendee_name, sign_status) VALUES
  (2001, 2, '湖贝班长', 'SIGNED'),
  (2001, NULL, '安装工一', 'SIGNED'),
  (2001, NULL, '安装工二', 'SIGNED'),
  (2002, 2, '湖贝班长', 'SIGNED'),
  (2003, 4, '制氧主管', 'SIGNED');

INSERT INTO biz_status_log (biz_type, biz_id, from_status, to_status, action, operator_id, remark) VALUES
  ('PRE_SHIFT_MEETING', 2001, NULL, 'OPENED', 'SUBMIT', 2, '种子数据提交'),
  ('PRE_SHIFT_MEETING', 2002, NULL, 'ARCHIVED', 'ARCHIVE', 2, '种子数据归档'),
  ('PRE_SHIFT_MEETING', 2003, NULL, 'DRAFT', 'CREATE', 4, '种子数据创建');


