ALTER TABLE biz_notification ADD COLUMN source_event_id VARCHAR(180) NULL;
ALTER TABLE biz_notification ADD COLUMN organization_id BIGINT NULL;
ALTER TABLE biz_notification ADD COLUMN audience_type VARCHAR(24) NOT NULL DEFAULT 'USER';
ALTER TABLE biz_notification ADD COLUMN snapshot_json TEXT NULL;
ALTER TABLE biz_notification ADD COLUMN expires_at TIMESTAMP NULL;
ALTER TABLE biz_notification ADD COLUMN published_at TIMESTAMP NULL;
ALTER TABLE biz_notification ADD COLUMN withdrawn_at TIMESTAMP NULL;

UPDATE biz_notification
SET source_event_id = dedup_key
WHERE source_event_id IS NULL;

CREATE UNIQUE INDEX uk_biz_notification_source_event
  ON biz_notification (source_event_id);
CREATE INDEX idx_biz_notification_org
  ON biz_notification (organization_id, created_at);

ALTER TABLE biz_notification_recipient ADD COLUMN recipient_reason VARCHAR(32) NOT NULL DEFAULT 'ASSIGNEE';
ALTER TABLE biz_notification_recipient ADD COLUMN action_status VARCHAR(20) NOT NULL DEFAULT 'PENDING';
ALTER TABLE biz_notification_recipient ADD COLUMN cancelled_at TIMESTAMP NULL;
ALTER TABLE biz_notification_recipient ADD COLUMN cancel_reason VARCHAR(200) NULL;
ALTER TABLE biz_notification_recipient ADD COLUMN last_delivered_at TIMESTAMP NULL;

UPDATE biz_notification_recipient
SET action_status = CASE handling_status
  WHEN 'HANDLED' THEN 'HANDLED'
  WHEN 'PENDING' THEN 'PENDING'
  ELSE 'HANDLED'
END,
last_delivered_at = created_at;

CREATE INDEX idx_biz_notification_recipient_action
  ON biz_notification_recipient (recipient_user_id, action_status);

CREATE TABLE biz_notification_outbox (
  id BIGSERIAL PRIMARY KEY,
  source_event_id VARCHAR(180) NOT NULL,
  event_type VARCHAR(64) NOT NULL,
  payload_json TEXT NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
  retry_count INTEGER NOT NULL DEFAULT 0,
  next_retry_at TIMESTAMP NULL,
  claimed_at TIMESTAMP NULL,
  processed_at TIMESTAMP NULL,
  last_error VARCHAR(1000) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_biz_notification_outbox_event
  ON biz_notification_outbox (source_event_id);
CREATE INDEX idx_biz_notification_outbox_pending
  ON biz_notification_outbox (status, next_retry_at, id);

CREATE TABLE biz_announcement (
  id BIGSERIAL PRIMARY KEY,
  version_no INTEGER NOT NULL DEFAULT 1,
  previous_version_id BIGINT NULL,
  title VARCHAR(160) NOT NULL,
  content TEXT NOT NULL,
  severity VARCHAR(16) NOT NULL DEFAULT 'NORMAL',
  audience_type VARCHAR(24) NOT NULL,
  audience_json TEXT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
  effective_at TIMESTAMP NULL,
  expires_at TIMESTAMP NULL,
  created_by BIGINT NOT NULL,
  published_by BIGINT NULL,
  withdrawn_by BIGINT NULL,
  published_at TIMESTAMP NULL,
  withdrawn_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_biz_announcement_status
  ON biz_announcement (status, effective_at, expires_at);
