INSERT INTO three_check_record (
  module_key, record_no, company_id, department_id, team_id, owner_user_id,
  business_date, status, payload_json, image_check_status, video_check_status,
  submitted_by, submitted_at, reminder_count, created_by, updated_by,
  source_channel, client_request_id, last_synced_at, created_at, updated_at, deleted
)
SELECT
  'team-dispatch', 'MCTEST-TD-YC-001', 4, 101109, 1011001, 2,
  DATE '2026-06-01', 'OPENED',
  '{"teamTask":"幕墙单元板装配复核","dispatchType":"今日","managerCount":"2","dispatchDate":"2026-06-01","dispatchStatus":"生效","source":"monitor-center-test"}',
  '现场照片', '视频已传', 1, TIMESTAMP '2026-06-01 07:35:00', 0, 1, 1,
  'PC', 'mctest-team-dispatch-yc-001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (SELECT 1 FROM three_check_record WHERE record_no = 'MCTEST-TD-YC-001');

INSERT INTO three_check_record (
  module_key, record_no, company_id, department_id, team_id, owner_user_id,
  business_date, status, payload_json, image_check_status, video_check_status,
  submitted_by, submitted_at, reminder_count, created_by, updated_by,
  source_channel, client_request_id, last_synced_at, created_at, updated_at, deleted
)
SELECT
  'curtain-wall-team-dispatch', 'MCTEST-TD-YC-002', 4, 101109, 1011002, 2,
  DATE '2026-06-01', 'OPENED',
  '{"teamTask":"幕墙龙骨安装检查","dispatchType":"今日","managerCount":"2","dispatchDate":"2026-06-01","dispatchStatus":"生效","source":"monitor-center-test"}',
  '现场照片', '视频已传', 1, TIMESTAMP '2026-06-01 07:38:00', 0, 1, 1,
  'PC', 'mctest-team-dispatch-yc-002', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (SELECT 1 FROM three_check_record WHERE record_no = 'MCTEST-TD-YC-002');

INSERT INTO three_check_record (
  module_key, record_no, company_id, department_id, team_id, owner_user_id,
  business_date, status, payload_json, image_check_status, video_check_status,
  submitted_by, submitted_at, reminder_count, created_by, updated_by,
  source_channel, client_request_id, last_synced_at, created_at, updated_at, deleted
)
SELECT
  module_key, record_no, 4, 101109, team_id, 2,
  DATE '2026-06-01', 'OPENED', payload_json,
  '现场照片', '视频已传', 1, submitted_at, 0, 1, 1,
  'PC', client_request_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
FROM (
  SELECT 'pre-shift-meeting' AS module_key, 'MCTEST-PRE-YC-001' AS record_no, 1011001 AS team_id,
    '{"meetingContent":"强调高处作业安全带、临边防护和吊装协同。","source":"monitor-center-test"}' AS payload_json,
    TIMESTAMP '2026-06-01 07:50:00' AS submitted_at, 'mctest-pre-shift-meeting-yc-001' AS client_request_id
  UNION ALL SELECT 'pre-shift-meeting', 'MCTEST-PRE-YC-002', 1011002,
    '{"meetingContent":"复盘周末隐患整改闭环，布置今日班前检查。","source":"monitor-center-test"}',
    TIMESTAMP '2026-06-01 07:52:00', 'mctest-pre-shift-meeting-yc-002'
  UNION ALL SELECT 'pre-shift-inspection', 'MCTEST-PSI-YC-001', 1011001,
    '{"owner":"monitor14","statusLabel":"已检查","source":"monitor-center-test"}',
    TIMESTAMP '2026-06-01 08:05:00', 'mctest-pre-shift-inspection-yc-001'
  UNION ALL SELECT 'pre-shift-inspection', 'MCTEST-PSI-YC-002', 1011002,
    '{"owner":"monitor15","statusLabel":"已检查","source":"monitor-center-test"}',
    TIMESTAMP '2026-06-01 08:08:00', 'mctest-pre-shift-inspection-yc-002'
  UNION ALL SELECT 'mid-shift-inspection', 'MCTEST-MSI-YC-001', 1011001,
    '{"owner":"monitor14","statusLabel":"已检查","source":"monitor-center-test"}',
    TIMESTAMP '2026-06-01 12:10:00', 'mctest-mid-shift-inspection-yc-001'
  UNION ALL SELECT 'mid-shift-inspection', 'MCTEST-MSI-YC-002', 1011002,
    '{"owner":"monitor15","statusLabel":"已检查","source":"monitor-center-test"}',
    TIMESTAMP '2026-06-01 12:15:00', 'mctest-mid-shift-inspection-yc-002'
  UNION ALL SELECT 'post-shift-inspection', 'MCTEST-POST-YC-002', 1011002,
    '{"owner":"monitor15","handoverStatus":"已交班","statusLabel":"已检查","source":"monitor-center-test"}',
    TIMESTAMP '2026-06-01 18:05:00', 'mctest-post-shift-inspection-yc-002'
) seed
WHERE NOT EXISTS (SELECT 1 FROM three_check_record r WHERE r.record_no = seed.record_no);

INSERT INTO three_check_record (
  module_key, record_no, company_id, department_id, team_id, owner_user_id,
  business_date, status, payload_json, image_check_status, video_check_status,
  submitted_by, submitted_at, reminder_count, created_by, updated_by,
  source_channel, client_request_id, last_synced_at, created_at, updated_at, deleted
)
SELECT
  module_key, record_no, 4, 101109, team_id, 2,
  DATE '2026-06-01', status, payload_json,
  '现场照片', video_check_status, submitted_by, submitted_at, reminder_count, 1, 1,
  'PC', client_request_id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
FROM (
  SELECT 'hazard-rectification' AS module_key, 'MCTEST-HR-YC-001' AS record_no, 1011001 AS team_id,
    'DRAFT' AS status, '{"hazard":"监控中心测试-脚手架通道材料堆放","rectificationOwner":"monitor14","source":"monitor-center-test"}' AS payload_json,
    '未上传' AS video_check_status, NULL AS submitted_by, NULL AS submitted_at, 1 AS reminder_count,
    'mctest-hazard-rectification-yc-001' AS client_request_id
  UNION ALL SELECT 'hazard-rectification', 'MCTEST-HR-YC-002', 1011002,
    'ARCHIVED', '{"hazard":"监控中心测试-吊装警戒线缺口已整改","rectificationOwner":"monitor15","source":"monitor-center-test"}',
    '视频已传', 1, TIMESTAMP '2026-06-01 16:40:00', 0, 'mctest-hazard-rectification-yc-002'
  UNION ALL SELECT 'safety-check', 'MCTEST-SC-YC-001', 1011001,
    'DRAFT', '{"hazardDescription":"监控中心测试-临边防护栏杆松动","source":"monitor-center-test"}',
    '未上传', NULL, NULL, 1, 'mctest-safety-check-yc-001'
  UNION ALL SELECT 'quick-shot', 'MCTEST-QS-YC-001', 1011002,
    'OPENED', '{"hazard":"监控中心测试-材料转运通道临时占用已清理","source":"monitor-center-test"}',
    '视频已传', 1, TIMESTAMP '2026-06-01 15:20:00', 0, 'mctest-quick-shot-yc-001'
) seed
WHERE NOT EXISTS (SELECT 1 FROM three_check_record r WHERE r.record_no = seed.record_no);

INSERT INTO risk_control_library (id, name, company_id, created_by, updated_by, created_at, updated_at, deleted)
SELECT 9401, '监控中心测试-Demo Works幕墙风险库', 4, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (SELECT 1 FROM risk_control_library WHERE id = 9401);

INSERT INTO risk_control_hazard (
  id, library_id, company_id, risk_point, danger_source, risk_influence_factors,
  accident_type, risk_level, possible_hazard, rectification_measures,
  created_by, updated_by, created_at, updated_at, deleted
)
SELECT id, 9401, 4, risk_point, danger_source, risk_influence_factors,
  accident_type, risk_level, possible_hazard, rectification_measures,
  1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
FROM (
  SELECT 9501 AS id, '监控中心测试-吊篮作业面' AS risk_point, '高处临边作业' AS danger_source, '防护缺失' AS risk_influence_factors,
    '高处坠落' AS accident_type, '重大风险' AS risk_level, '人员坠落' AS possible_hazard, '补齐防护并验收' AS rectification_measures
  UNION ALL SELECT 9502, '监控中心测试-龙骨吊装区', '吊物摆动', '警戒不足', '起重伤害', '较大风险', '吊物碰撞', '完善吊装警戒'
  UNION ALL SELECT 9503, '监控中心测试-切割设备', '设备旋转部位', '设备防护缺失', '机械伤害', '一般风险', '机械卷入', '补装防护罩'
  UNION ALL SELECT 9504, '监控中心测试-临时配电箱', '带电部位外露', '临电不规范', '触电', '低风险', '人员触电', '规范箱门上锁'
) seed
WHERE NOT EXISTS (SELECT 1 FROM risk_control_hazard h WHERE h.id = seed.id);

INSERT INTO training_safety_learning_content (
  company_id, category, title, content, learning_date, duration_text,
  code, draft, status, created_by, updated_by, created_at, updated_at, deleted
)
SELECT 4, '班组安全学习', title, content, DATE '2026-06-01', duration_text,
  code, '否', 'ACTIVE', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
FROM (
  SELECT '监控中心测试-班前会风险提示' AS title, '今日重点关注高处坠落、起重伤害和触电风险。' AS content,
    '15分钟' AS duration_text, 'MCTEST-LEARN-YC-20260601-A' AS code
  UNION ALL SELECT '监控中心测试-事故案例一分钟', '结合事故看板复盘脚手架通道占用导致的绊跌风险。',
    '6分钟', 'MCTEST-LEARN-YC-20260601-B'
) seed
WHERE NOT EXISTS (SELECT 1 FROM training_safety_learning_content c WHERE c.code = seed.code AND c.deleted = 0);

INSERT INTO special_work_record (
  company_id, project, work_type, application_time, work_content, work_location,
  risk_identification_result, status, created_by, updated_by, created_at, updated_at, deleted
)
SELECT 4, project, work_type, application_time, work_content, work_location,
  risk_identification_result, status, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
FROM (
  SELECT 'MCTEST-SW-YC-001' AS project, '动火作业' AS work_type, TIMESTAMP '2026-06-01 08:20:00' AS application_time,
    '幕墙连接件补焊' AS work_content, '幕墙组装一区' AS work_location, '已识别火花飞溅和临电风险' AS risk_identification_result, 'COMPLETED' AS status
  UNION ALL SELECT 'MCTEST-SW-YC-002', '高处作业', TIMESTAMP '2026-06-01 09:00:00',
    '高处幕墙龙骨校正', '幕墙组装二区', '已识别高处坠落风险', 'APPROVED'
) seed
WHERE NOT EXISTS (SELECT 1 FROM special_work_record r WHERE r.project = seed.project AND r.deleted = 0);

