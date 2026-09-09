ALTER TABLE three_check_record ADD COLUMN IF NOT EXISTS root_dispatch_record_id BIGINT NULL;

UPDATE three_check_record
SET root_dispatch_record_id = id
WHERE module_key IN ('team-dispatch', 'curtain-wall-team-dispatch')
  AND root_dispatch_record_id IS NULL;

UPDATE three_check_record r
SET root_dispatch_record_id = (
  SELECT min(d.id)
  FROM three_check_record d
  WHERE d.deleted = 0
    AND d.module_key IN ('team-dispatch', 'curtain-wall-team-dispatch')
    AND d.company_id = r.company_id
    AND d.department_id = r.department_id
    AND d.team_id = r.team_id
    AND d.business_date = r.business_date
)
WHERE r.module_key IN (
    'pre-shift-meeting',
    'pre-shift-inspection',
    'mid-shift-inspection',
    'post-shift-inspection'
  )
  AND r.root_dispatch_record_id IS NULL
  AND (
    SELECT count(*)
    FROM three_check_record d
    WHERE d.deleted = 0
      AND d.module_key IN ('team-dispatch', 'curtain-wall-team-dispatch')
      AND d.company_id = r.company_id
      AND d.department_id = r.department_id
      AND d.team_id = r.team_id
      AND d.business_date = r.business_date
  ) = 1;

CREATE INDEX IF NOT EXISTS idx_three_check_record_root_dispatch
  ON three_check_record (root_dispatch_record_id, module_key, deleted);
