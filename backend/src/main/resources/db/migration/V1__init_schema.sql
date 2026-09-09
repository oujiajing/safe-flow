CREATE TABLE sys_org (
  id BIGSERIAL PRIMARY KEY,
  parent_id BIGINT NULL,
  org_type VARCHAR(32) NOT NULL,
  org_code VARCHAR(64) NOT NULL,
  org_name VARCHAR(120) NOT NULL,
  org_path VARCHAR(512) NOT NULL,
  sort_order INT NOT NULL DEFAULT 0,
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted SMALLINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_sys_org_code ON sys_org (org_code);
CREATE INDEX idx_sys_org_parent ON sys_org (parent_id);
CREATE INDEX idx_sys_org_path ON sys_org (org_path);

CREATE TABLE sys_user (
  id BIGSERIAL PRIMARY KEY,
  username VARCHAR(64) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  real_name VARCHAR(80) NOT NULL,
  mobile VARCHAR(32) NULL,
  org_id BIGINT NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted SMALLINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_sys_user_username ON sys_user (username);
CREATE INDEX idx_sys_user_org ON sys_user (org_id);

CREATE TABLE sys_role (
  id BIGSERIAL PRIMARY KEY,
  role_code VARCHAR(64) NOT NULL,
  role_name VARCHAR(80) NOT NULL,
  data_scope VARCHAR(32) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted SMALLINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_sys_role_code ON sys_role (role_code);

CREATE TABLE sys_user_role (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_sys_user_role ON sys_user_role (user_id, role_id);

CREATE TABLE shift_task (
  id BIGSERIAL PRIMARY KEY,
  task_no VARCHAR(64) NOT NULL,
  company_id BIGINT NOT NULL,
  department_id BIGINT NOT NULL,
  team_id BIGINT NOT NULL,
  shift_date DATE NOT NULL,
  shift_name VARCHAR(64) NOT NULL,
  leader_user_id BIGINT NOT NULL,
  status VARCHAR(32) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted SMALLINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_shift_task_no ON shift_task (task_no);
CREATE INDEX idx_shift_task_scope ON shift_task (company_id, department_id, team_id, shift_date);

CREATE TABLE pre_shift_meeting (
  id BIGSERIAL PRIMARY KEY,
  task_id BIGINT NOT NULL,
  meeting_no VARCHAR(64) NOT NULL,
  company_id BIGINT NOT NULL,
  department_id BIGINT NOT NULL,
  team_id BIGINT NOT NULL,
  owner_user_id BIGINT NOT NULL,
  meeting_date DATE NOT NULL,
  meeting_content TEXT NULL,
  attendees_count INT NOT NULL DEFAULT 0,
  image_check_status VARCHAR(32) NOT NULL DEFAULT '未上传',
  video_check_status VARCHAR(32) NOT NULL DEFAULT '未上传',
  status VARCHAR(32) NOT NULL,
  submitted_by BIGINT NULL,
  submitted_at TIMESTAMP NULL,
  withdrawn_by BIGINT NULL,
  withdrawn_at TIMESTAMP NULL,
  withdraw_reason VARCHAR(255) NULL,
  reminder_count INT NOT NULL DEFAULT 0,
  last_reminded_at TIMESTAMP NULL,
  version INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted SMALLINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_pre_shift_meeting_no ON pre_shift_meeting (meeting_no);
CREATE INDEX idx_pre_shift_meeting_task ON pre_shift_meeting (task_id);
CREATE INDEX idx_pre_shift_meeting_scope ON pre_shift_meeting (company_id, department_id, team_id, meeting_date);
CREATE INDEX idx_pre_shift_meeting_status ON pre_shift_meeting (status);

CREATE TABLE pre_shift_meeting_attendee (
  id BIGSERIAL PRIMARY KEY,
  meeting_id BIGINT NOT NULL,
  user_id BIGINT NULL,
  attendee_name VARCHAR(80) NOT NULL,
  sign_status VARCHAR(32) NOT NULL DEFAULT 'SIGNED',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_pre_shift_attendee_meeting ON pre_shift_meeting_attendee (meeting_id);

CREATE TABLE biz_attachment (
  id BIGSERIAL PRIMARY KEY,
  biz_type VARCHAR(64) NOT NULL,
  biz_id BIGINT NOT NULL,
  file_kind VARCHAR(32) NOT NULL,
  original_name VARCHAR(255) NOT NULL,
  storage_path VARCHAR(500) NOT NULL,
  storage_provider VARCHAR(32) NOT NULL DEFAULT 'LOCAL',
  bucket_name VARCHAR(120) NULL,
  object_key VARCHAR(500) NULL,
  etag VARCHAR(255) NULL,
  content_type VARCHAR(120) NOT NULL,
  file_size BIGINT NOT NULL,
  uploaded_by BIGINT NOT NULL,
  uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted SMALLINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_biz_attachment_target ON biz_attachment (biz_type, biz_id);

CREATE TABLE biz_status_log (
  id BIGSERIAL PRIMARY KEY,
  biz_type VARCHAR(64) NOT NULL,
  biz_id BIGINT NOT NULL,
  from_status VARCHAR(32) NULL,
  to_status VARCHAR(32) NOT NULL,
  action VARCHAR(64) NOT NULL,
  operator_id BIGINT NOT NULL,
  remark VARCHAR(255) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_biz_status_target ON biz_status_log (biz_type, biz_id);

CREATE TABLE biz_remind_record (
  id BIGSERIAL PRIMARY KEY,
  biz_type VARCHAR(64) NOT NULL,
  biz_id BIGINT NOT NULL,
  recipient_user_id BIGINT NOT NULL,
  remind_channel VARCHAR(32) NOT NULL,
  content VARCHAR(255) NOT NULL,
  send_status VARCHAR(32) NOT NULL,
  created_by BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_biz_remind_target ON biz_remind_record (biz_type, biz_id);
