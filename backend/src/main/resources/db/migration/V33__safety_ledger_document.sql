CREATE TABLE safety_ledger_document (
  id BIGSERIAL PRIMARY KEY,
  ledger_key VARCHAR(120) NOT NULL,
  name VARCHAR(200) NOT NULL,
  rich_text TEXT NULL,
  company_id BIGINT NOT NULL,
  department VARCHAR(160) NULL,
  team VARCHAR(160) NULL,
  document_type VARCHAR(120) NULL,
  document_date DATE NULL,
  attachment_id BIGINT NULL,
  created_by BIGINT NOT NULL,
  updated_by BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  deleted SMALLINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_safety_ledger_document_ledger ON safety_ledger_document (ledger_key, deleted);
CREATE INDEX idx_safety_ledger_document_company ON safety_ledger_document (company_id, deleted);
