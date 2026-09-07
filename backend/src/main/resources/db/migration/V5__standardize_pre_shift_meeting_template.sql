ALTER TABLE shift_task ADD COLUMN task_type VARCHAR(32) NOT NULL DEFAULT 'ONE_SHIFT_THREE_CHECKS';
ALTER TABLE shift_task ADD COLUMN source_channel VARCHAR(32) NOT NULL DEFAULT 'PC';
ALTER TABLE shift_task ADD COLUMN source_record_id VARCHAR(120) NULL;
ALTER TABLE shift_task ADD COLUMN client_request_id VARCHAR(120) NULL;

ALTER TABLE pre_shift_meeting ADD COLUMN created_by BIGINT NULL;
ALTER TABLE pre_shift_meeting ADD COLUMN updated_by BIGINT NULL;
ALTER TABLE pre_shift_meeting ADD COLUMN archived_by BIGINT NULL;
ALTER TABLE pre_shift_meeting ADD COLUMN archived_at TIMESTAMP NULL;
ALTER TABLE pre_shift_meeting ADD COLUMN source_channel VARCHAR(32) NOT NULL DEFAULT 'PC';
ALTER TABLE pre_shift_meeting ADD COLUMN source_record_id VARCHAR(120) NULL;
ALTER TABLE pre_shift_meeting ADD COLUMN client_request_id VARCHAR(120) NULL;
ALTER TABLE pre_shift_meeting ADD COLUMN client_updated_at TIMESTAMP NULL;
ALTER TABLE pre_shift_meeting ADD COLUMN last_synced_at TIMESTAMP NULL;

CREATE INDEX idx_shift_task_type ON shift_task (task_type, source_channel);
CREATE INDEX idx_pre_shift_meeting_owner ON pre_shift_meeting (owner_user_id);
CREATE UNIQUE INDEX uk_pre_shift_meeting_source_record ON pre_shift_meeting (source_channel, source_record_id);
CREATE UNIQUE INDEX uk_pre_shift_meeting_client_request ON pre_shift_meeting (source_channel, client_request_id);
