CREATE TABLE biz_notification (
  id BIGSERIAL PRIMARY KEY,
  event_type VARCHAR(64) NOT NULL,
  group_type VARCHAR(16) NOT NULL,
  module_key VARCHAR(64) NOT NULL,
  title VARCHAR(160) NOT NULL,
  summary VARCHAR(500) NULL,
  biz_type VARCHAR(64) NOT NULL,
  biz_id BIGINT NOT NULL,
  severity VARCHAR(16) NOT NULL DEFAULT 'NORMAL',
  action_key VARCHAR(64) NULL,
  deadline TIMESTAMP NULL,
  dedup_key VARCHAR(180) NOT NULL,
  triggered_by BIGINT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_biz_notification_dedup ON biz_notification (dedup_key);
CREATE INDEX idx_biz_notification_biz ON biz_notification (biz_type, biz_id);
CREATE INDEX idx_biz_notification_created ON biz_notification (created_at);

CREATE TABLE biz_notification_recipient (
  id BIGSERIAL PRIMARY KEY,
  notification_id BIGINT NOT NULL,
  recipient_user_id BIGINT NOT NULL,
  read_at TIMESTAMP NULL,
  handling_status VARCHAR(16) NOT NULL DEFAULT 'NONE',
  handled_at TIMESTAMP NULL,
  archived SMALLINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_biz_notification_recipient
  ON biz_notification_recipient (notification_id, recipient_user_id);
CREATE INDEX idx_biz_notification_recipient_user
  ON biz_notification_recipient (recipient_user_id, archived, read_at);
CREATE INDEX idx_biz_notification_recipient_handling
  ON biz_notification_recipient (recipient_user_id, handling_status);
