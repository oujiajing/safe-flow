CREATE TABLE special_work_record (
  id BIGSERIAL PRIMARY KEY,
  company_id BIGINT NOT NULL,
  project VARCHAR(255) NOT NULL,
  work_type VARCHAR(120) NOT NULL,
  application_time TIMESTAMP NOT NULL,
  image_attachment_id BIGINT NULL,
  work_content VARCHAR(1000) NULL,
  work_location VARCHAR(255) NULL,
  risk_identification_result VARCHAR(1000) NULL,
  implementation_start_time TIMESTAMP NULL,
  implementation_end_time TIMESTAMP NULL,
  safety_disclosure_person VARCHAR(120) NULL,
  guardian VARCHAR(120) NULL,
  disclosure_receiver VARCHAR(120) NULL,
  completion_acceptor VARCHAR(120) NULL,
  completion_acceptance_time TIMESTAMP NULL,
  status VARCHAR(32) NOT NULL,
  created_by BIGINT NULL,
  updated_by BIGINT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted SMALLINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_special_work_company_time ON special_work_record (company_id, application_time);
CREATE INDEX idx_special_work_status ON special_work_record (status);
