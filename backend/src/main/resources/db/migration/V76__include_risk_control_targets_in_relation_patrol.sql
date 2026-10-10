CREATE OR REPLACE VIEW business_relation_target AS
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
SELECT 'RISK_CONTROL_LIBRARY', id FROM risk_control_library
UNION ALL
SELECT 'RISK_CONTROL_HAZARD', id FROM risk_control_hazard
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
