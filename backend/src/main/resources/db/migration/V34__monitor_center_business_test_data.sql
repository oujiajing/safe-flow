INSERT INTO three_check_record (
  module_key, record_no, company_id, department_id, team_id, owner_user_id,
  business_date, status, payload_json, image_check_status, video_check_status,
  submitted_by, submitted_at, reminder_count, created_by, updated_by,
  source_channel, client_request_id, last_synced_at, created_at, updated_at, deleted
) VALUES
  (
    'team-dispatch', 'MCTEST-TD-YC-001', 4, 101109, 1011001, 2,
    DATE '2026-06-01', 'OPENED',
    '{"teamTask":"幕墙单元板装配复核","dispatchType":"今日","managerCount":"2","dispatchDate":"2026-06-01","dispatchStatus":"生效","source":"monitor-center-test"}',
    '现场照片', '视频已传', 1, TIMESTAMP '2026-06-01 07:35:00', 0, 1, 1,
    'PC', 'mctest-team-dispatch-yc-001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
  ),
  (
    'curtain-wall-team-dispatch', 'MCTEST-TD-YC-002', 4, 101109, 1011002, 2,
    DATE '2026-06-01', 'OPENED',
    '{"teamTask":"幕墙龙骨安装检查","dispatchType":"今日","managerCount":"2","dispatchDate":"2026-06-01","dispatchStatus":"生效","source":"monitor-center-test"}',
    '现场照片', '视频已传', 1, TIMESTAMP '2026-06-01 07:38:00', 0, 1, 1,
    'PC', 'mctest-team-dispatch-yc-002', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
  ),
  (
    'pre-shift-meeting', 'MCTEST-PRE-YC-001', 4, 101109, 1011001, 2,
    DATE '2026-06-01', 'OPENED',
    '{"meetingContent":"强调高处作业安全带、临边防护和吊装协同。","source":"monitor-center-test"}',
    '现场照片', '视频已传', 1, TIMESTAMP '2026-06-01 07:50:00', 0, 1, 1,
    'PC', 'mctest-pre-shift-meeting-yc-001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
  ),
  (
    'pre-shift-meeting', 'MCTEST-PRE-YC-002', 4, 101109, 1011002, 2,
    DATE '2026-06-01', 'OPENED',
    '{"meetingContent":"复盘周末隐患整改闭环，布置今日班前检查。","source":"monitor-center-test"}',
    '现场照片', '视频已传', 1, TIMESTAMP '2026-06-01 07:52:00', 0, 1, 1,
    'PC', 'mctest-pre-shift-meeting-yc-002', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
  ),
  (
    'pre-shift-inspection', 'MCTEST-PSI-YC-001', 4, 101109, 1011001, 2,
    DATE '2026-06-01', 'OPENED',
    '{"owner":"monitor14","statusLabel":"已检查","source":"monitor-center-test"}',
    '现场照片', '视频已传', 1, TIMESTAMP '2026-06-01 08:05:00', 0, 1, 1,
    'PC', 'mctest-pre-shift-inspection-yc-001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
  ),
  (
    'pre-shift-inspection', 'MCTEST-PSI-YC-002', 4, 101109, 1011002, 2,
    DATE '2026-06-01', 'OPENED',
    '{"owner":"monitor15","statusLabel":"已检查","source":"monitor-center-test"}',
    '现场照片', '视频已传', 1, TIMESTAMP '2026-06-01 08:08:00', 0, 1, 1,
    'PC', 'mctest-pre-shift-inspection-yc-002', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
  ),
  (
    'mid-shift-inspection', 'MCTEST-MSI-YC-001', 4, 101109, 1011001, 2,
    DATE '2026-06-01', 'OPENED',
    '{"owner":"monitor14","statusLabel":"已检查","source":"monitor-center-test"}',
    '现场照片', '视频已传', 1, TIMESTAMP '2026-06-01 12:10:00', 0, 1, 1,
    'PC', 'mctest-mid-shift-inspection-yc-001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
  ),
  (
    'mid-shift-inspection', 'MCTEST-MSI-YC-002', 4, 101109, 1011002, 2,
    DATE '2026-06-01', 'OPENED',
    '{"owner":"monitor15","statusLabel":"已检查","source":"monitor-center-test"}',
    '现场照片', '视频已传', 1, TIMESTAMP '2026-06-01 12:15:00', 0, 1, 1,
    'PC', 'mctest-mid-shift-inspection-yc-002', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
  ),
  (
    'post-shift-inspection', 'MCTEST-POST-YC-002', 4, 101109, 1011002, 2,
    DATE '2026-06-01', 'OPENED',
    '{"owner":"monitor15","handoverStatus":"已交班","statusLabel":"已检查","source":"monitor-center-test"}',
    '现场照片', '视频已传', 1, TIMESTAMP '2026-06-01 18:05:00', 0, 1, 1,
    'PC', 'mctest-post-shift-inspection-yc-002', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
  ),
  (
    'hazard-rectification', 'MCTEST-HR-YC-001', 4, 101109, 1011001, 2,
    DATE '2026-06-01', 'DRAFT',
    '{"hazard":"监控中心测试-脚手架通道材料堆放","rectificationOwner":"monitor14","source":"monitor-center-test"}',
    '现场照片', '未上传', NULL, NULL, 1, 1, 1,
    'PC', 'mctest-hazard-rectification-yc-001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
  ),
  (
    'hazard-rectification', 'MCTEST-HR-YC-002', 4, 101109, 1011002, 2,
    DATE '2026-06-01', 'ARCHIVED',
    '{"hazard":"监控中心测试-吊装警戒线缺口已整改","rectificationOwner":"monitor15","source":"monitor-center-test"}',
    '现场照片', '视频已传', 1, TIMESTAMP '2026-06-01 16:40:00', 0, 1, 1,
    'PC', 'mctest-hazard-rectification-yc-002', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
  ),
  (
    'safety-check', 'MCTEST-SC-YC-001', 4, 101109, 1011001, 2,
    DATE '2026-06-01', 'DRAFT',
    '{"hazardDescription":"监控中心测试-临边防护栏杆松动","source":"monitor-center-test"}',
    '现场照片', '未上传', NULL, NULL, 1, 1, 1,
    'PC', 'mctest-safety-check-yc-001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
  ),
  (
    'quick-shot', 'MCTEST-QS-YC-001', 4, 101109, 1011002, 2,
    DATE '2026-06-01', 'OPENED',
    '{"hazard":"监控中心测试-材料转运通道临时占用已清理","source":"monitor-center-test"}',
    '现场照片', '视频已传', 1, TIMESTAMP '2026-06-01 15:20:00', 0, 1, 1,
    'PC', 'mctest-quick-shot-yc-001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
  ),
  (
    'team-dispatch', 'MCTEST-TD-MQ-001', 3, 101109, 1011001, 2,
    DATE '2026-06-01', 'OPENED',
    '{"teamTask":"幕墙项目巡检抽查","dispatchType":"今日","source":"monitor-center-test"}',
    '现场照片', '视频已传', 1, TIMESTAMP '2026-06-01 07:45:00', 0, 1, 1,
    'PC', 'mctest-team-dispatch-mq-001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
  ),
  (
    'pre-shift-meeting', 'MCTEST-PRE-MQ-001', 3, 101109, 1011001, 2,
    DATE '2026-06-01', 'OPENED',
    '{"meetingContent":"区域项目班前风险提示。","source":"monitor-center-test"}',
    '现场照片', '视频已传', 1, TIMESTAMP '2026-06-01 07:55:00', 0, 1, 1,
    'PC', 'mctest-pre-shift-meeting-mq-001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
  ),
  (
    'pre-shift-inspection', 'MCTEST-PSI-MQ-001', 3, 101109, 1011001, 2,
    DATE '2026-06-01', 'DRAFT',
    '{"owner":"monitor14","statusLabel":"待复查","source":"monitor-center-test"}',
    '待补传', '未上传', NULL, NULL, 1, 1, 1,
    'PC', 'mctest-pre-shift-inspection-mq-001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
  ),
  (
    'hazard-rectification', 'MCTEST-HR-MQ-001', 3, 101109, 1011001, 2,
    DATE '2026-06-01', 'DRAFT',
    '{"hazard":"监控中心测试-高处作业安全绳固定点待复核","source":"monitor-center-test"}',
    '现场照片', '未上传', NULL, NULL, 1, 1, 1,
    'PC', 'mctest-hazard-rectification-mq-001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
  ),
  (
    'team-dispatch', 'MCTEST-TD-ZY-001', 24, 101109, 1011001, 2,
    DATE '2026-06-01', 'OPENED',
    '{"teamTask":"资源公司安全巡检联动","dispatchType":"今日","source":"monitor-center-test"}',
    '现场照片', '视频已传', 1, TIMESTAMP '2026-06-01 08:00:00', 0, 1, 1,
    'PC', 'mctest-team-dispatch-zy-001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
  ),
  (
    'pre-shift-meeting', 'MCTEST-PRE-ZY-001', 24, 101109, 1011001, 2,
    DATE '2026-06-01', 'OPENED',
    '{"meetingContent":"资源公司班前安全提示。","source":"monitor-center-test"}',
    '现场照片', '视频已传', 1, TIMESTAMP '2026-06-01 08:10:00', 0, 1, 1,
    'PC', 'mctest-pre-shift-meeting-zy-001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
  ),
  (
    'hazard-rectification', 'MCTEST-HR-ZY-001', 24, 101109, 1011001, 2,
    DATE '2026-06-01', 'ARCHIVED',
    '{"hazard":"监控中心测试-消防通道标识补贴完成","source":"monitor-center-test"}',
    '现场照片', '视频已传', 1, TIMESTAMP '2026-06-01 16:30:00', 0, 1, 1,
    'PC', 'mctest-hazard-rectification-zy-001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
  ),
  (
    'curtain-wall-team-dispatch', 'MCTEST-TD-XC-001', 28, 101109, 1011001, 2,
    DATE '2026-06-01', 'DRAFT',
    '{"teamTask":"广晟新材设备点检提醒","dispatchType":"今日","source":"monitor-center-test"}',
    '未上传', '未上传', NULL, NULL, 1, 1, 1,
    'PC', 'mctest-team-dispatch-xc-001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
  ),
  (
    'quick-shot', 'MCTEST-QS-XC-001', 28, 101109, 1011001, 2,
    DATE '2026-06-01', 'DRAFT',
    '{"hazard":"监控中心测试-临时用电箱门未关闭","source":"monitor-center-test"}',
    '现场照片', '未上传', NULL, NULL, 1, 1, 1,
    'PC', 'mctest-quick-shot-xc-001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
  );

INSERT INTO risk_control_library (id, name, company_id, created_by, updated_by, created_at, updated_at, deleted) VALUES
  (9401, '监控中心测试-源成幕墙风险库', 4, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
  (9402, '监控中心测试-广晟幕墙风险库', 3, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
  (9403, '监控中心测试-资源公司风险库', 24, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
  (9404, '监控中心测试-广晟新材风险库', 28, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

INSERT INTO risk_control_hazard (
  id, library_id, company_id, risk_point, danger_source, risk_influence_factors,
  accident_type, likelihood, exposure_frequency, consequence, risk_value, risk_level,
  engineering_measures, management_measures, emergency_measures,
  superior_responsible_person, responsible_department, responsible_contact,
  possible_hazard, rectification_measures, created_by, updated_by, created_at, updated_at, deleted
) VALUES
  (9501, 9401, 4, '监控中心测试-吊篮作业面', '高处临边作业', '防护缺失', '高处坠落', '可能', '每天', '严重', '320', '重大风险', '设置双道防护', '班前专项交底', '启动坠落伤害预案', '安全质量职卫部', '幕墙组装', 'monitor14', '人员坠落', '补齐防护并验收', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
  (9502, 9401, 4, '监控中心测试-龙骨吊装区', '吊物摆动', '警戒不足', '起重伤害', '可能', '每天', '较重', '180', '较大风险', '设置警戒隔离', '吊装旁站监护', '启动吊装事故预案', '生产部', '幕墙组装', 'monitor15', '吊物碰撞', '完善吊装警戒', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
  (9503, 9401, 4, '监控中心测试-切割设备', '设备旋转部位', '设备防护缺失', '机械伤害', '偶尔', '每天', '一般', '90', '一般风险', '安装防护罩', '执行点检确认', '停机处置', '设备管理', '幕墙组装', 'monitor14', '机械卷入', '补装防护罩', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
  (9504, 9401, 4, '监控中心测试-临时配电箱', '带电部位外露', '临电不规范', '触电', '偶尔', '每周', '一般', '60', '低风险', '加装漏保', '电工每日巡查', '触电急救', '设备管理', '幕墙组装', 'monitor15', '人员触电', '规范箱门上锁', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
  (9505, 9402, 3, '监控中心测试-项目临边', '高处作业', '防护缺失', '高处坠落', '可能', '每天', '严重', '300', '重大风险', '挂设安全网', '专项检查', '坠落应急', '项目安全部', '幕墙项目', 'monitor14', '高处坠落', '加密巡检', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
  (9506, 9402, 3, '监控中心测试-材料转运', '叉车交叉作业', '人车混行', '车辆伤害', '偶尔', '每天', '较重', '150', '较大风险', '划分人车通道', '设置引导员', '车辆伤害应急', '项目安全部', '幕墙项目', 'monitor15', '车辆碰撞', '完善隔离线', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
  (9507, 9403, 24, '监控中心测试-仓储通道', '物料堆垛', '通道占用', '物体打击', '偶尔', '每天', '一般', '80', '一般风险', '限高堆放', '仓库日清', '现场急救', '仓储部', '资源公司', 'monitor14', '物体打击', '清理占道物料', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
  (9508, 9404, 28, '监控中心测试-试验电源', '临时用电', '临电不规范', '触电', '偶尔', '每周', '一般', '60', '低风险', '漏电保护', '持证接电', '触电急救', '设备部', '广晟新材', 'monitor15', '人员触电', '关闭箱门并挂牌', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

INSERT INTO training_safety_learning_content (
  company_id, category, title, content, learning_date, duration_text,
  code, draft, status, created_by, updated_by, created_at, updated_at, deleted
) VALUES
  (4, '班组安全学习', '监控中心测试-高处作业防坠落提示', '高处作业前检查安全带、安全绳和临边防护。', DATE '2026-05-26', '12分钟', 'MCTEST-LEARN-YC-20260526', '否', 'ACTIVE', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
  (4, '班组安全学习', '监控中心测试-吊装作业警戒要求', '吊装半径内设置警戒线并安排专人监护。', DATE '2026-05-27', '10分钟', 'MCTEST-LEARN-YC-20260527', '否', 'ACTIVE', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
  (4, '班组安全学习', '监控中心测试-临时用电检查要点', '配电箱箱门关闭，漏电保护器试跳正常。', DATE '2026-05-28', '9分钟', 'MCTEST-LEARN-YC-20260528', '否', 'ACTIVE', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
  (4, '班组安全学习', '监控中心测试-机械设备防护', '切割设备运转前确认防护罩和急停装置。', DATE '2026-05-29', '11分钟', 'MCTEST-LEARN-YC-20260529', '否', 'ACTIVE', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
  (4, '班组安全学习', '监控中心测试-通道和材料堆放', '保持脚手架通道、消防通道和转运通道畅通。', DATE '2026-05-30', '8分钟', 'MCTEST-LEARN-YC-20260530', '否', 'ACTIVE', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
  (4, '班组安全学习', '监控中心测试-隐患闭环复盘', '复盘隐患整改照片、责任人和复查结果。', DATE '2026-05-31', '13分钟', 'MCTEST-LEARN-YC-20260531', '否', 'ACTIVE', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
  (4, '班组安全学习', '监控中心测试-班前会风险提示', '今日重点关注高处坠落、起重伤害和触电风险。', DATE '2026-06-01', '15分钟', 'MCTEST-LEARN-YC-20260601-A', '否', 'ACTIVE', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
  (4, '班组安全学习', '监控中心测试-事故案例一分钟', '结合事故看板复盘脚手架通道占用导致的绊跌风险。', DATE '2026-06-01', '6分钟', 'MCTEST-LEARN-YC-20260601-B', '否', 'ACTIVE', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
  (3, '班组安全学习', '监控中心测试-项目安全晨读', '项目级安全学习内容。', DATE '2026-06-01', '10分钟', 'MCTEST-LEARN-MQ-20260601', '否', 'ACTIVE', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
  (24, '班组安全学习', '监控中心测试-仓储通道治理', '仓储通道治理学习内容。', DATE '2026-06-01', '10分钟', 'MCTEST-LEARN-ZY-20260601', '否', 'ACTIVE', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
  (28, '班组安全学习', '监控中心测试-临电箱管理', '临电箱管理学习内容。', DATE '2026-06-01', '10分钟', 'MCTEST-LEARN-XC-20260601', '否', 'ACTIVE', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);

INSERT INTO special_work_record (
  company_id, project, work_type, application_time, work_content, work_location,
  risk_identification_result, implementation_start_time, implementation_end_time,
  safety_disclosure_person, guardian, disclosure_receiver, completion_acceptor,
  completion_acceptance_time, status, created_by, updated_by, created_at, updated_at, deleted
) VALUES
  (4, 'MCTEST-SW-YC-001', '动火作业', TIMESTAMP '2026-06-01 08:20:00', '幕墙连接件补焊', '幕墙组装一区', '已识别火花飞溅和临电风险', TIMESTAMP '2026-06-01 09:00:00', TIMESTAMP '2026-06-01 11:30:00', 'monitor14', 'monitor15', 'member52', 'monitor14', TIMESTAMP '2026-06-01 11:45:00', 'COMPLETED', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
  (4, 'MCTEST-SW-YC-002', '高处作业', TIMESTAMP '2026-06-01 09:00:00', '高处幕墙龙骨校正', '幕墙组装二区', '已识别高处坠落风险', TIMESTAMP '2026-06-01 09:30:00', TIMESTAMP '2026-06-01 17:30:00', 'monitor15', 'monitor14', 'member70', NULL, NULL, 'APPROVED', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
  (4, 'MCTEST-SW-YC-003', '吊装作业', TIMESTAMP '2026-06-01 10:00:00', '单元板吊装转运', '幕墙组装吊装区', '已识别吊物摆动和警戒不足风险', TIMESTAMP '2026-06-01 10:30:00', TIMESTAMP '2026-06-01 15:30:00', 'monitor14', 'monitor15', 'member53', NULL, NULL, 'PENDING', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
  (3, 'MCTEST-SW-MQ-001', '临时用电', TIMESTAMP '2026-06-01 08:40:00', '项目临时配电接入', '项目加工区', '已识别触电风险', TIMESTAMP '2026-06-01 09:10:00', TIMESTAMP '2026-06-01 12:00:00', 'monitor14', 'monitor15', 'member54', NULL, NULL, 'APPROVED', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
  (24, 'MCTEST-SW-ZY-001', '受限空间作业', TIMESTAMP '2026-06-01 09:20:00', '仓储罐体检查', '资源公司仓储区', '已识别通风不足风险', TIMESTAMP '2026-06-01 10:00:00', TIMESTAMP '2026-06-01 14:00:00', 'monitor14', 'monitor15', 'member55', NULL, NULL, 'APPROVED', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0),
  (28, 'MCTEST-SW-XC-001', '设备检修作业', TIMESTAMP '2026-06-01 10:30:00', '试验设备检修', '广晟新材试验区', '已识别机械伤害风险', TIMESTAMP '2026-06-01 11:00:00', TIMESTAMP '2026-06-01 16:00:00', 'monitor14', 'monitor15', 'member57', NULL, NULL, 'PENDING', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);
