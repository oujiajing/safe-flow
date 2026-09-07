CREATE TABLE three_check_record (
  id BIGSERIAL PRIMARY KEY,
  module_key VARCHAR(80) NOT NULL,
  record_no VARCHAR(80) NOT NULL,
  task_id BIGINT NULL,
  company_id BIGINT NOT NULL,
  department_id BIGINT NOT NULL,
  team_id BIGINT NOT NULL,
  owner_user_id BIGINT NOT NULL,
  business_date DATE NOT NULL,
  status VARCHAR(32) NOT NULL,
  payload_json TEXT NULL,
  image_check_status VARCHAR(32) NOT NULL DEFAULT '未上传',
  video_check_status VARCHAR(32) NOT NULL DEFAULT '未上传',
  submitted_by BIGINT NULL,
  submitted_at TIMESTAMP NULL,
  withdrawn_by BIGINT NULL,
  withdrawn_at TIMESTAMP NULL,
  withdraw_reason VARCHAR(255) NULL,
  reminder_count INT NOT NULL DEFAULT 0,
  last_reminded_at TIMESTAMP NULL,
  version INT NOT NULL DEFAULT 0,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  source_channel VARCHAR(32) NOT NULL DEFAULT 'PC',
  source_record_id VARCHAR(120) NULL,
  client_request_id VARCHAR(120) NULL,
  client_updated_at TIMESTAMP NULL,
  last_synced_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted SMALLINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_three_check_record_no ON three_check_record (record_no);
CREATE UNIQUE INDEX uk_three_check_record_source_record ON three_check_record (module_key, source_channel, source_record_id);
CREATE UNIQUE INDEX uk_three_check_record_client_request ON three_check_record (module_key, source_channel, client_request_id);
CREATE INDEX idx_three_check_record_module_date ON three_check_record (module_key, business_date);
CREATE INDEX idx_three_check_record_scope ON three_check_record (company_id, department_id, team_id, business_date);
CREATE INDEX idx_three_check_record_status ON three_check_record (module_key, status);
CREATE INDEX idx_three_check_record_owner ON three_check_record (owner_user_id);

INSERT INTO three_check_record (
  module_key, record_no, company_id, department_id, team_id, owner_user_id,
  business_date, status, payload_json, image_check_status, video_check_status,
  submitted_by, submitted_at, reminder_count, created_by, updated_by,
  source_channel, client_request_id, last_synced_at, created_at, updated_at, deleted
) VALUES
  (
    'team-dispatch', 'TCR-TD-20260515-001', 4, 101109, 1011001, 2,
    '2026-05-15', 'DRAFT',
    '{"teamTask":"幕墙组装巡检","dispatchType":"常规派班","managerCount":"2","dispatchDate":"2026-05-15","dispatchStatus":"生效","dispatchTime":"2026-05-15 07:30"}',
    '未上传', '未上传', NULL, NULL, 0, 1, 1, 'PC', 'seed-team-dispatch-001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
  ),
  (
    'team-dispatch', 'TCR-TD-20260514-002', 4, 101110, 1011003, 2,
    '2026-05-14', 'OPENED',
    '{"teamTask":"型材卸车备料","dispatchType":"常规派班","managerCount":"1","dispatchDate":"2026-05-14","dispatchStatus":"不生效","dispatchTime":"2026-05-14 07:35"}',
    '未上传', '未上传', 1, '2026-05-14 07:36:00', 0, 1, 1, 'PC', 'seed-team-dispatch-002', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
  ),
  (
    'curtain-wall-team-dispatch', 'TCR-CWTD-20260515-001', 4, 101111, 1011007, 2,
    '2026-05-15', 'DRAFT',
    '{"teamTask":"幕墙单元板装配","dispatchType":"今日","managerCount":"3","dispatchDate":"2026-05-15","dispatchStatus":"生效","dispatchTime":"2026-05-15 07:35"}',
    '未上传', '未上传', NULL, NULL, 0, 1, 1, 'PC', 'seed-curtain-wall-team-dispatch-001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
  ),
  (
    'curtain-wall-team-dispatch', 'TCR-CWTD-20260514-002', 4, 101112, 1011012, 2,
    '2026-05-14', 'OPENED',
    '{"teamTask":"型材切割开料","dispatchType":"明日","managerCount":"2","dispatchDate":"2026-05-14","dispatchStatus":"不生效","dispatchTime":"2026-05-14 07:50"}',
    '未上传', '未上传', 1, '2026-05-14 07:51:00', 0, 1, 1, 'PC', 'seed-curtain-wall-team-dispatch-002', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
  ),
  (
    'pre-shift-safety-activity', 'TCR-PSSA-20260515-001', 4, 101109, 1011001, 2,
    '2026-05-15', 'DRAFT',
    '{"workContent":"吊装作业、吊篮作业、高处作业、动火作业、临时用电、机械作业、脚手架作业","laborTeamCount":"12","equipmentInspection":"是","safetyActivityRecordUpload":"待上传","imageUpload":"未上传","videoUpload":"未上传","createdAt":"2026-05-15 07:45","statusLabel":"待开会议"}',
    '未上传', '未上传', NULL, NULL, 0, 1, 1, 'PC', 'seed-pre-shift-safety-activity-001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
  ),
  (
    'pre-shift-safety-activity', 'TCR-PSSA-20260514-002', 4, 101110, 1011004, 2,
    '2026-05-14', 'OPENED',
    '{"workContent":"吊装作业、机械作业","laborTeamCount":"8","equipmentInspection":"是","safetyActivityRecordUpload":"已上传","imageUpload":"现场照片","videoUpload":"视频已传","createdAt":"2026-05-14 07:40","statusLabel":"已开会议"}',
    '现场照片', '视频已传', 1, '2026-05-14 07:41:00', 0, 1, 1, 'PC', 'seed-pre-shift-safety-activity-002', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
  ),
  (
    'pre-shift-inspection', 'TCR-PSI-20260515-001', 4, 101109, 1011001, 2,
    '2026-05-15', 'DRAFT',
    '{"owner":"湖贝班长","statusLabel":"待检查"}',
    '未上传', '未上传', NULL, NULL, 0, 1, 1, 'PC', 'seed-pre-shift-inspection-001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
  ),
  (
    'pre-shift-inspection', 'TCR-PSI-20260514-002', 4, 101112, 1011012, 2,
    '2026-05-14', 'OPENED',
    '{"owner":"幕墙安全员","statusLabel":"已检查"}',
    '现场照片', '未上传', 1, '2026-05-14 08:05:00', 0, 1, 1, 'PC', 'seed-pre-shift-inspection-002', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
  ),
  (
    'mid-shift-inspection', 'TCR-MSI-20260515-001', 4, 101109, 1011001, 2,
    '2026-05-15', 'DRAFT',
    '{"owner":"湖贝班长","statusLabel":"待检查"}',
    '现场照片', '视频已传', NULL, NULL, 0, 1, 1, 'PC', 'seed-mid-shift-inspection-001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
  ),
  (
    'mid-shift-inspection', 'TCR-MSI-20260514-002', 4, 101110, 1011003, 2,
    '2026-05-14', 'OPENED',
    '{"owner":"幕墙安全员","statusLabel":"已检查"}',
    '待补传', '未上传', 1, '2026-05-14 12:10:00', 0, 1, 1, 'PC', 'seed-mid-shift-inspection-002', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
  ),
  (
    'post-shift-inspection', 'TCR-POSTSI-20260515-001', 4, 101109, 1011001, 2,
    '2026-05-15', 'DRAFT',
    '{"owner":"湖贝班长","handoverStatus":"待交班","statusLabel":"待检查"}',
    '现场照片', '未上传', NULL, NULL, 0, 1, 1, 'PC', 'seed-post-shift-inspection-001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
  ),
  (
    'post-shift-inspection', 'TCR-POSTSI-20260514-002', 4, 101113, 1011016, 2,
    '2026-05-14', 'OPENED',
    '{"owner":"门窗班长","handoverStatus":"已交班","statusLabel":"已检查"}',
    '现场照片', '未上传', 1, '2026-05-14 18:05:00', 0, 1, 1, 'PC', 'seed-post-shift-inspection-002', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
  ),
  (
    'key-sites', 'TCR-KS-20260515-001', 4, 101105, 1011001, 2,
    '2026-05-15', 'DRAFT',
    '{"siteType":"吊装作业区","inspectionDepartment":"安全质量职卫部","responsibleDepartment":"幕墙组装","responsiblePerson":"湖贝班长","acceptancePerson":"幕墙安全员","statusLabel":"待检查"}',
    '未上传', '未上传', NULL, NULL, 0, 1, 1, 'PC', 'seed-key-sites-001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
  ),
  (
    'key-sites', 'TCR-KS-20260514-002', 4, 101108, 1011003, 2,
    '2026-05-14', 'ARCHIVED',
    '{"siteType":"临边防护区","inspectionDepartment":"生产部","responsibleDepartment":"资材仓库","responsiblePerson":"幕墙安全员","acceptancePerson":"系统管理员","statusLabel":"已验收"}',
    '未上传', '未上传', 1, '2026-05-14 17:20:00', 0, 1, 1, 'PC', 'seed-key-sites-002', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
  );
