-- Remove only unambiguous dangling junction rows before enabling foreign keys.
-- Historical/polymorphic business records are reported by the views below and
-- are deliberately not deleted automatically.
DELETE FROM sys_user_role child
WHERE NOT EXISTS (SELECT 1 FROM sys_user parent WHERE parent.id = child.user_id)
   OR NOT EXISTS (SELECT 1 FROM sys_role parent WHERE parent.id = child.role_id);

DELETE FROM sys_role_menu child
WHERE NOT EXISTS (SELECT 1 FROM sys_role parent WHERE parent.id = child.role_id)
   OR NOT EXISTS (SELECT 1 FROM sys_menu parent WHERE parent.id = child.menu_id);

CREATE VIEW business_relation_target AS
SELECT
  CASE module_key
    WHEN 'team-dispatch' THEN 'THREE_CHECK_TEAM_DISPATCH'
    WHEN 'curtain-wall-team-dispatch' THEN 'THREE_CHECK_CURTAIN_WALL_TEAM_DISPATCH'
    WHEN 'pre-shift-meeting' THEN 'PRE_SHIFT_MEETING'
    WHEN 'pre-shift-safety-activity' THEN 'THREE_CHECK_PRE_SHIFT_SAFETY_ACTIVITY'
    WHEN 'pre-shift-inspection' THEN 'THREE_CHECK_PRE_SHIFT_INSPECTION'
    WHEN 'mid-shift-inspection' THEN 'THREE_CHECK_MID_SHIFT_INSPECTION'
    WHEN 'post-shift-inspection' THEN 'THREE_CHECK_POST_SHIFT_INSPECTION'
    WHEN 'key-sites' THEN 'THREE_CHECK_KEY_SITES'
    WHEN 'safety-check' THEN 'HAZARD_SAFETY_CHECK'
    WHEN 'hazard-rectification' THEN 'HAZARD_RECTIFICATION'
    WHEN 'quick-shot' THEN 'HAZARD_QUICK_SHOT'
    WHEN 'curtain-wall-penalty' THEN 'HAZARD_CURTAIN_WALL_PENALTY'
    WHEN 'curtain-wall-routine-check' THEN 'HAZARD_CURTAIN_WALL_ROUTINE_CHECK'
    WHEN 'points-flow' THEN 'SAFETY_POINTS_FLOW'
  END AS biz_type,
  id AS biz_id
FROM three_check_record
UNION ALL
SELECT 'HAZARD_RECTIFICATION_ORDER', id FROM hazard_rectification_order
UNION ALL
SELECT 'RISK_FOUR_COLOR_MAP', id FROM risk_four_color_map
UNION ALL
SELECT 'SAFETY_LEDGER_DOCUMENT', id FROM safety_ledger_document
UNION ALL
SELECT 'SPECIAL_WORK', id FROM special_work_record
UNION ALL
SELECT 'TRAINING_SAFETY_LEARNING', id FROM training_safety_learning_content
UNION ALL
SELECT 'SYS_ORG_CONTENT_PROFILE', id FROM sys_org_content_profile
UNION ALL
SELECT 'USER_AVATAR', id FROM sys_user;

CREATE VIEW core_relation_integrity_issue AS
SELECT
  refs.source_table,
  refs.source_id,
  refs.biz_type AS relation_type,
  refs.biz_id AS missing_target_id
FROM (
  SELECT 'biz_attachment' AS source_table, id AS source_id, biz_type, biz_id
  FROM biz_attachment
  UNION ALL
  SELECT 'biz_status_log', id, biz_type, biz_id FROM biz_status_log
  UNION ALL
  SELECT 'biz_change_history', id, biz_type, biz_id FROM biz_change_history
  UNION ALL
  SELECT 'biz_remind_record', id, biz_type, biz_id FROM biz_remind_record
  UNION ALL
  SELECT 'biz_notification', id, biz_type, biz_id FROM biz_notification
) refs
WHERE NOT EXISTS (
  SELECT 1
  FROM business_relation_target target
  WHERE target.biz_type = refs.biz_type
    AND target.biz_id = refs.biz_id
)
UNION ALL
SELECT
  'hazard_rectification_order',
  source.id,
  'SOURCE_' || source.source_type,
  source.source_record_id
FROM hazard_rectification_order source
WHERE source.source_record_id IS NOT NULL
  AND source.source_type <> 'MANUAL'
  AND NOT EXISTS (
    SELECT 1 FROM three_check_record target WHERE target.id = source.source_record_id
  )
UNION ALL
SELECT 'shift_task', source.id, 'LEADER_USER', source.leader_user_id
FROM shift_task source
WHERE NOT EXISTS (SELECT 1 FROM sys_user target WHERE target.id = source.leader_user_id)
UNION ALL
SELECT 'three_check_record', source.id, 'OWNER_USER', source.owner_user_id
FROM three_check_record source
WHERE NOT EXISTS (SELECT 1 FROM sys_user target WHERE target.id = source.owner_user_id)
UNION ALL
SELECT 'training_exam_result', source.id, 'EXAM_PERSON_USER', source.exam_person_user_id
FROM training_exam_result source
WHERE NOT EXISTS (SELECT 1 FROM sys_user target WHERE target.id = source.exam_person_user_id)
UNION ALL
SELECT 'biz_attachment', source.id, 'UPLOADED_BY_USER', source.uploaded_by
FROM biz_attachment source
WHERE NOT EXISTS (SELECT 1 FROM sys_user target WHERE target.id = source.uploaded_by)
UNION ALL
SELECT 'biz_status_log', source.id, 'OPERATOR_USER', source.operator_id
FROM biz_status_log source
WHERE NOT EXISTS (SELECT 1 FROM sys_user target WHERE target.id = source.operator_id)
UNION ALL
SELECT 'biz_change_history', source.id, 'OPERATOR_USER', source.operator_id
FROM biz_change_history source
WHERE source.operator_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_user target WHERE target.id = source.operator_id)
UNION ALL
SELECT 'hazard_rectification_flow_log', source.id, 'OPERATOR_USER', source.operator_id
FROM hazard_rectification_flow_log source
WHERE source.operator_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_user target WHERE target.id = source.operator_id);
