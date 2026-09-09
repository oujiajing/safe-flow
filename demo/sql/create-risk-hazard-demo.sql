BEGIN;

INSERT INTO risk_control_library (name, company_id, created_by, updated_by, deleted)
SELECT v.name, c.id, u.id, u.id, 0 FROM (VALUES ('演示公司综合风险库'), ('设备保障专项风险库')) v(name), sys_org c, sys_user u
WHERE c.org_code='DEMO-COMPANY' AND u.username='demo-safety'
ON CONFLICT DO NOTHING;

INSERT INTO risk_control_hazard (library_id, company_id, risk_point, danger_source, risk_influence_factors, accident_type, likelihood, exposure_frequency, consequence, risk_value, risk_level, engineering_measures, management_measures, emergency_measures, superior_responsible_person, responsible_department, responsible_contact, possible_hazard, rectification_measures, created_by, updated_by, deleted)
SELECT l.id, c.id, x.risk_point, x.danger_source, x.factors, x.accident_type, x.likelihood, x.exposure, x.consequence, x.risk_value, x.level, x.engineering, x.management, x.emergency, '周宁', x.department, 'demo-safety', x.hazard, x.rectification, u.id, u.id, 0
FROM risk_control_library l CROSS JOIN sys_org c CROSS JOIN sys_user u
JOIN (VALUES
('高处检修平台','临边防护不足','作业面变化','高处坠落','可能','每天','严重','320','重大风险','双道护栏与踢脚板','作业许可和监护确认','启动高处救援预案','设备保障部','临边防护缺失','补齐防护并验收'),
('起重吊装区','吊物下方进入','警戒隔离不足','起重伤害','可能','每天','较重','180','较大风险','警戒线与限位装置','持证指挥和吊前检查','停吊疏散并报告','装配运营部','吊装警戒不完整','恢复隔离并复核'),
('临时用电箱','箱门敞开','漏保和接地检查不足','触电','可能','每天','较重','180','较大风险','防雨箱和漏电保护','日检、上锁、挂牌','断电隔离和急救','设备保障部','箱门未闭合','闭合箱门并完成挂牌'),
('装配设备单元','旋转部位暴露','联锁点检不到位','机械伤害','偶尔','每天','一般','90','一般风险','防护罩与联锁装置','授权进入和设备点检','急停并通知维修','装配运营部','防护罩松动','停机恢复防护'),
('物料转运通道','通道占用','定置管理不到位','车辆伤害','偶尔','每天','一般','80','一般风险','人车分流标线','每班清场','封锁区域并清障','装配运营部','空托盘占道','移除物料恢复通道'),
('手持电动工具','防护罩缺失','使用前点检遗漏','机械伤害','偶尔','每周','一般','70','一般风险','合格防护装置','使用前点检','停机断电报告','装配运营部','防护罩松动','更换防护罩'),
('化学品暂存柜','标签模糊','标识维护不足','火灾','较少','每周','轻微','30','低风险','防泄漏托盘','双人领用台账','围堵吸附并通风','设备保障部','标签褪色','更换通用标识'),
('充电作业区','线缆绊倒','整理和隔离不足','车辆伤害','较少','每周','轻微','20','低风险','线缆桥架','充电区巡查','断电并设警戒','设备保障部','线缆散落','整理线缆并复核')) x(risk_point,danger_source,factors,accident_type,likelihood,exposure,consequence,risk_value,level,engineering,management,emergency,department,hazard,rectification)
ON l.name = CASE WHEN x.risk_point IN ('高处检修平台','起重吊装区','临时用电箱','装配设备单元','物料转运通道') THEN '演示公司综合风险库' ELSE '设备保障专项风险库' END
WHERE c.org_code='DEMO-COMPANY' AND u.username='demo-safety'
  AND NOT EXISTS (SELECT 1 FROM risk_control_hazard h WHERE h.risk_point=x.risk_point AND h.deleted=0);

WITH orders(order_no, status, module_key, record_no, team_code, requirement, rectifier, acceptor, description, acceptance_result, action, from_status, business_date) AS (VALUES
('DEMO-HZ-001','PENDING_ASSIGN','pre-shift-inspection','DEMO-3C-COMPLETE-PRE','DEMO-MORNING-TEAM','关闭临时用电箱门并完成挂牌','demo-rectifier','demo-acceptor','临时用电箱门未闭合',NULL,'CREATE',NULL,CURRENT_DATE-2),
('DEMO-HZ-002','PENDING_RECTIFY','mid-shift-inspection','DEMO-3C-ACTIVE-MID','DEMO-GREEN-TEAM','移除通道内空托盘并恢复标线','demo-rectifier','demo-acceptor','转运通道有空托盘占用',NULL,'ISSUE_RECTIFICATION','PENDING_ASSIGN',CURRENT_DATE-1),
('DEMO-HZ-003','RECTIFIED','pre-shift-inspection','DEMO-3C-COMPLETE-PRE','DEMO-MORNING-TEAM','更换并紧固工具防护罩','demo-rectifier','demo-acceptor','手持工具防护罩松动','整改已完成','MARK_RECTIFIED','PENDING_RECTIFY',CURRENT_DATE-2),
('DEMO-HZ-004','PENDING_ACCEPTANCE','mid-shift-inspection','DEMO-3C-COMPLETE-MID','DEMO-MORNING-TEAM','补齐高处平台踢脚板并提交验收','demo-rectifier','demo-acceptor','高处平台踢脚板局部松动',NULL,'REQUEST_ACCEPTANCE','RECTIFIED',CURRENT_DATE-2),
('DEMO-HZ-005','CLOSED','pre-shift-inspection','DEMO-3C-COMPLETE-PRE','DEMO-MORNING-TEAM','更换褪色警示标识并完成验收','demo-rectifier','demo-acceptor','设备警示标识褪色','验收通过','ACCEPT','PENDING_ACCEPTANCE',CURRENT_DATE-2),
('DEMO-HZ-006','CANCELLED','mid-shift-inspection','DEMO-3C-ACTIVE-MID','DEMO-GREEN-TEAM','取消重复派发的整改任务','demo-rectifier','demo-acceptor','重复记录已合并','已作废','CANCEL','PENDING_RECTIFY',CURRENT_DATE-1),
('DEMO-HZ-007','PENDING_RECTIFY','post-shift-inspection','DEMO-3C-COMPLETE-POST','DEMO-MORNING-TEAM','重新固定清场区域标识后提交验收','demo-rectifier','demo-acceptor','清场标识固定不牢','验收驳回','REJECT_ACCEPTANCE','PENDING_ACCEPTANCE',CURRENT_DATE-2))
INSERT INTO hazard_rectification_order (order_no, source_type, source_module_key, source_record_id, source_record_no, root_dispatch_record_id, company_id, department_id, team_id, business_date, hazard_count, status, rectification_department_id, rectification_responsible_user_id, rectification_requirement, rectification_deadline, issued_by, issued_at, rectified_by, rectified_at, rectification_description, acceptance_user_id, acceptance_department_id, acceptance_at, acceptance_result, acceptance_remark, closed_at, version, created_by, updated_by, deleted)
SELECT o.order_no, 'MANUAL', o.module_key, NULL, r.record_no, r.root_dispatch_record_id, c.id, r.department_id, t.id, o.business_date, 1, o.status, d.id, ru.id, o.requirement, CURRENT_TIMESTAMP + interval '3 days', s.id, CURRENT_TIMESTAMP, CASE WHEN o.status IN ('RECTIFIED','PENDING_ACCEPTANCE','CLOSED','PENDING_RECTIFY') THEN ru.id ELSE NULL END, CASE WHEN o.status IN ('RECTIFIED','PENDING_ACCEPTANCE','CLOSED','PENDING_RECTIFY') THEN CURRENT_TIMESTAMP ELSE NULL END, CASE WHEN o.status IN ('RECTIFIED','PENDING_ACCEPTANCE','CLOSED','PENDING_RECTIFY') THEN o.description || '，已完成整改说明' ELSE NULL END, au.id, sd.id, CASE WHEN o.status IN ('CLOSED','PENDING_RECTIFY') THEN CURRENT_TIMESTAMP ELSE NULL END, o.acceptance_result, CASE WHEN o.status='PENDING_RECTIFY' THEN '验收人员反馈：请补充整改措施后重新提交。' ELSE NULL END, CASE WHEN o.status='CLOSED' THEN CURRENT_TIMESTAMP ELSE NULL END, 1, s.id, s.id, 0
FROM orders o JOIN three_check_record r ON r.record_no=o.record_no JOIN sys_org c ON c.org_code='DEMO-COMPANY' JOIN sys_org t ON t.org_code=o.team_code JOIN sys_org d ON d.org_code='DEMO-EQUIPMENT' JOIN sys_user s ON s.username='demo-safety' JOIN sys_user ru ON ru.username=o.rectifier JOIN sys_user au ON au.username=o.acceptor JOIN sys_org sd ON sd.org_code='DEMO-SAFETY'
ON CONFLICT (order_no) WHERE deleted=0 DO UPDATE SET status=EXCLUDED.status, rectification_requirement=EXCLUDED.rectification_requirement, rectification_description=EXCLUDED.rectification_description, acceptance_result=EXCLUDED.acceptance_result, acceptance_remark=EXCLUDED.acceptance_remark, deleted=0, updated_at=CURRENT_TIMESTAMP;

INSERT INTO hazard_rectification_order_item (order_id, source_line_id, source_line_index, risk_type, check_item, hazard_description, before_photo, default_follow_up_plan, source_snapshot_json, rectification_status, closed_at, sort_order, deleted)
SELECT o.id, o.order_no || '-ITEM-01', 0, '现场安全', x.description, x.description, 'synthetic://before/' || o.order_no || '.jpg', '整改后复核并保留记录', json_build_object('source','portfolio-demo-v1','description',x.description)::text, o.status, CASE WHEN o.status='CLOSED' THEN CURRENT_TIMESTAMP ELSE NULL END, 1, 0
FROM hazard_rectification_order o JOIN (VALUES ('DEMO-HZ-001','临时用电箱门未闭合'),('DEMO-HZ-002','转运通道有空托盘占用'),('DEMO-HZ-003','手持工具防护罩松动'),('DEMO-HZ-004','高处平台踢脚板局部松动'),('DEMO-HZ-005','设备警示标识褪色'),('DEMO-HZ-006','重复记录已合并'),('DEMO-HZ-007','清场标识固定不牢')) x(order_no,description) ON x.order_no=o.order_no
ON CONFLICT (order_id, source_line_id) WHERE deleted=0 DO UPDATE SET hazard_description=EXCLUDED.hazard_description, rectification_status=EXCLUDED.rectification_status, deleted=0, updated_at=CURRENT_TIMESTAMP;

INSERT INTO hazard_rectification_flow_log (order_id, from_status, to_status, action, action_label, operator_id, remark, payload_json)
SELECT o.id, x.from_status, o.status, x.action, x.action_label, u.id, x.remark, json_build_object('source','portfolio-demo-v1')::text
FROM hazard_rectification_order o JOIN sys_user u ON u.username='demo-safety' JOIN (VALUES
('DEMO-HZ-001',NULL,'PENDING_ASSIGN','CREATE','创建整改单','记录来源于班前检查'),('DEMO-HZ-002','PENDING_ASSIGN','PENDING_RECTIFY','ISSUE_RECTIFICATION','下发整改','已分派整改负责人'),('DEMO-HZ-003','PENDING_RECTIFY','RECTIFIED','MARK_RECTIFIED','整改完成','整改负责人提交完成说明'),('DEMO-HZ-004','RECTIFIED','PENDING_ACCEPTANCE','REQUEST_ACCEPTANCE','提交验收','等待验收人员处理'),('DEMO-HZ-005','PENDING_ACCEPTANCE','CLOSED','ACCEPT','验收关闭','验收通过并关闭'),('DEMO-HZ-006','PENDING_RECTIFY','CANCELLED','CANCEL','作废','重复记录已合并'),('DEMO-HZ-007','PENDING_ACCEPTANCE','PENDING_RECTIFY','REJECT_ACCEPTANCE','验收驳回','请补充整改措施')) x(order_no,from_status,to_status,action,action_label,remark) ON x.order_no=o.order_no
WHERE NOT EXISTS (SELECT 1 FROM hazard_rectification_flow_log l WHERE l.order_id=o.id AND l.action=x.action);

COMMIT;
