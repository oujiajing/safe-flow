ALTER TABLE hazard_rectification_order
  ADD COLUMN IF NOT EXISTS acceptance_requirement TEXT NULL;
