BEGIN;

-- Risk map and organization content profiles.
INSERT INTO risk_four_color_map (name, company_id, remark, created_by, updated_by, deleted)
SELECT '演示公司风险四色分布图', c.id, '虚构厂区示意图：红橙黄蓝四色区域仅用于演示。', u.id, u.id, 0
FROM sys_org c CROSS JOIN sys_user u WHERE c.org_code='DEMO-COMPANY' AND u.username='demo-safety'
  AND NOT EXISTS (SELECT 1 FROM risk_four_color_map m WHERE m.name='演示公司风险四色分布图' AND m.deleted=0);

INSERT INTO sys_org_content_profile (org_id, title, subtitle, description, video_title, status, created_by, updated_by, deleted)
SELECT o.id, x.title, x.subtitle, x.description, x.video_title, x.status, u.id, u.id, 0
FROM sys_org o CROSS JOIN sys_user u JOIN (VALUES
('DEMO-COMPANY','演示公司安全运营总览','安全生产数字化管控平台','展示组织、风险、检查和整改闭环的统一安全视图。','安全生产数字化管控平台演示','ACTIVE'),
('DEMO-ASSEMBLY-WORKSHOP','总装一车间安全看板','装配作业安全管理','展示装配班组的班次执行、风险辨识和隐患整改。','总装车间安全演示','ACTIVE'),
('DEMO-MAINTENANCE-WORKSHOP','检维修车间安全看板','检维修作业安全管理','展示设备保障班组的作业许可和现场检查。','检维修车间安全演示','DRAFT')) x(org_code,title,subtitle,description,video_title,status) ON o.org_code=x.org_code AND u.username='demo-safety'
ON CONFLICT (org_id) WHERE deleted=0 DO UPDATE SET title=EXCLUDED.title, subtitle=EXCLUDED.subtitle, description=EXCLUDED.description, status=EXCLUDED.status, updated_at=CURRENT_TIMESTAMP;

-- Special work: one record for each supported lifecycle state.
INSERT INTO special_work_record (company_id, project, work_type, application_time, work_content, work_location, risk_identification_result, implementation_start_time, implementation_end_time, safety_disclosure_person, guardian, disclosure_receiver, completion_acceptor, completion_acceptance_time, status, created_by, updated_by, deleted)
SELECT c.id, x.project, x.work_type, CURRENT_TIMESTAMP - x.offset_days * interval '1 day', x.content, x.location, x.risk, CASE WHEN x.status IN ('IN_PROGRESS','PENDING_ACCEPTANCE','COMPLETED') THEN CURRENT_TIMESTAMP - interval '2 hours' ELSE NULL END, CASE WHEN x.status='COMPLETED' THEN CURRENT_TIMESTAMP - interval '1 hour' ELSE NULL END, '周宁', '唐越', '顾晨', CASE WHEN x.status='COMPLETED' THEN '苏晴' ELSE NULL END, CASE WHEN x.status='COMPLETED' THEN CURRENT_TIMESTAMP - interval '30 minutes' ELSE NULL END, x.status, u.id, u.id, 0
FROM sys_org c CROSS JOIN sys_user u JOIN (VALUES
('演示动火作业-001','动火作业',0,'检维修区域临时动火','检维修车间东侧作业点','火花飞溅和可燃物隔离','PENDING_APPROVAL'),
('演示高处作业-001','高处作业',1,'高处平台护栏检修','总装一车间高处平台','临边防护和坠落风险','IN_PROGRESS'),
('演示临时用电-001','临时用电作业',2,'临时配电箱改造','设备保障部配电间','触电和误送电风险','PENDING_ACCEPTANCE'),
('演示吊装作业-001','吊装作业',3,'设备部件吊装转运','装配物流通道','吊物摆动和警戒不足','COMPLETED')) x(project,work_type,offset_days,content,location,risk,status) ON c.org_code='DEMO-COMPANY' AND u.username='demo-safety'
WHERE NOT EXISTS (SELECT 1 FROM special_work_record s WHERE s.project=x.project AND s.deleted=0);

-- Hazard inspection source modules.
INSERT INTO three_check_record (module_key, record_no, company_id, department_id, team_id, owner_user_id, business_date, status, payload_json, version, created_by, updated_by, source_record_id, client_request_id, root_dispatch_record_id)
SELECT x.module_key, x.record_no, c.id, d.id, t.id, u.id, CURRENT_DATE - x.offset_days, x.status, p.payload, 1, u.id, u.id, x.record_no, 'portfolio-demo-v1-' || x.record_no, NULL
FROM sys_org c JOIN sys_org d ON d.org_code='DEMO-OPS' JOIN sys_org t ON t.org_code='DEMO-MORNING-TEAM' JOIN sys_user u ON u.username='demo-safety' JOIN (VALUES
('safety-check','DEMO-HAZARD-SAFETY-001',2,'OPENED','综合安全检查','临时用电箱门未闭合'),('safety-check','DEMO-HAZARD-SAFETY-002',1,'ARCHIVED','班组专项检查','设备防护罩松动'),
('quick-shot','DEMO-HAZARD-QUICK-001',0,'OPENED','随手拍','转运通道空托盘占用'),('quick-shot','DEMO-HAZARD-QUICK-002',1,'DRAFT','随手拍','安全标识褪色'),
('curtain-wall-penalty','DEMO-HAZARD-PENALTY-001',3,'OPENED','现场检查处理','作业区域警戒线缺失'),
('curtain-wall-routine-check','DEMO-HAZARD-ROUTINE-001',0,'OPENED','日检','设备点检记录完整'),('curtain-wall-routine-check','DEMO-HAZARD-ROUTINE-002',1,'ARCHIVED','周检','通道标线状态良好'),
('hazard-rectification','DEMO-HAZARD-LEGACY-001',4,'ARCHIVED','隐患整改兼容记录','整改闭环记录')) x(module_key,record_no,offset_days,status,inspection_type,description) ON c.org_code='DEMO-COMPANY'
CROSS JOIN LATERAL (SELECT json_build_object('inspectionType',x.inspection_type,'description',x.description,'statusLabel',x.status)::text AS payload) p
WHERE NOT EXISTS (SELECT 1 FROM three_check_record r WHERE r.record_no=x.record_no);

-- Safety learning question bank.
INSERT INTO training_exam_question_bank (company_id, department_id, question_type, question_text, selected_option, all_options, answer, score, actual_score, created_by, updated_by, deleted, options_json, answers_json, answer_explanation)
SELECT c.id, d.id, x.question_type, x.question_text, NULL, x.options, x.answer, 10, 10, u.id, u.id, 0, x.options, x.answer, x.explanation
FROM sys_org c JOIN sys_org d ON d.org_code='DEMO-OPS' JOIN sys_user u ON u.username='demo-safety' CROSS JOIN (VALUES
('SINGLE','作业前检查个人防护用品的主要目的是什么？','安全防护|提高速度|减少记录|替代培训','安全防护','确认基本防护有效'),
('SINGLE','发现临时用电异常时首先应做什么？','断电隔离|继续作业|拍照即可|等待下班','断电隔离','先隔离风险再处理'),
('MULTIPLE','班前会应覆盖哪些内容？','作业任务|风险提示|应急联络|与作业无关内容','作业任务,风险提示,应急联络','班前交底需要完整覆盖任务和风险'),
('SINGLE','高处作业安全带应连接到哪里？','可靠挂点|临时管线|活动物料|无须连接','可靠挂点','安全带必须连接可靠挂点')) x(question_type,question_text,options,answer,explanation)
WHERE c.org_code='DEMO-COMPANY' AND NOT EXISTS (SELECT 1 FROM training_exam_question_bank q WHERE q.question_text=x.question_text AND q.deleted=0);

-- Nine Chinese ledger documents.
INSERT INTO safety_ledger_document (ledger_key, name, rich_text, company_id, department, team, document_type, document_date, created_by, updated_by, deleted)
SELECT x.ledger_key, x.name, t.rich_text, c.id, '安全质量部', '安全管理组', 'DEMO_TEXT_DOCUMENT', CURRENT_DATE, u.id, u.id, 0
FROM sys_org c CROSS JOIN sys_user u JOIN (VALUES
('safety-rules','演示公司安全管理制度'),('operating-procedures','装配设备安全操作规程'),('post-duties','班组岗位安全职责'),('full-responsibility','全员安全责任清单'),('annual-plan','年度安全工作计划'),('special-activity-plan','高处作业专项活动方案'),('risk-control-document','风险分级管控说明'),('return-to-work-six-ones','复工复产“六个一”材料'),('hazard-governance-document','隐患排查治理制度')) x(ledger_key,name) ON c.org_code='DEMO-COMPANY' AND u.username='demo-safety'
CROSS JOIN LATERAL (SELECT 'PORTFOLIO DEMO｜虚构数据\n' || x.name || '：用于演示安全生产数字化管理流程。' AS rich_text) t
WHERE NOT EXISTS (SELECT 1 FROM safety_ledger_document d WHERE d.ledger_key=x.ledger_key AND d.deleted=0);

-- Three announcements and notification inbox samples.
INSERT INTO biz_announcement (title, content, severity, audience_type, audience_json, status, effective_at, created_by, published_by)
SELECT x.title, x.content, x.severity, x.audience_type, x.audience_json, x.status, CASE WHEN x.status='PUBLISHED' THEN CURRENT_TIMESTAMP ELSE NULL END, u.id, CASE WHEN x.status='PUBLISHED' THEN u.id ELSE NULL END
FROM sys_user u JOIN (VALUES
('下月应急演练安排','请各班组按计划完成应急集合和疏散演练。','NORMAL','ALL','{}','DRAFT'),
('本周高处作业专项提醒','高处作业前请确认临边防护、安全带和监护安排。','IMPORTANT','ORG','{"orgCode":"DEMO-COMPANY"}','PUBLISHED'),
('已取消的临时停电通知','原定临时停电安排已取消，请以最新通知为准。','URGENT','USER','{"username":"demo-worker"}','WITHDRAWN')) x(title,content,severity,audience_type,audience_json,status) ON u.username='demo-safety';

INSERT INTO biz_notification (event_type, group_type, module_key, title, summary, biz_type, biz_id, severity, action_key, dedup_key, triggered_by, organization_id, audience_type, snapshot_json, published_at)
SELECT 'DEMO_NOTICE', 'BUSINESS', x.module_key, x.title, x.summary, x.biz_type, x.biz_id, x.severity, 'VIEW', x.dedup_key, u.id, c.id, 'USER', '{}', CURRENT_TIMESTAMP
FROM sys_user u CROSS JOIN sys_org c JOIN (VALUES
('hazard-rectification','整改待处理提醒','有一张隐患工单等待整改。','HAZARD_RECTIFICATION',1,'IMPORTANT','DEMO-NOTICE-001'),
('training','安全学习提醒','今日有一条安全学习内容。','TRAINING_LEARNING',1,'NORMAL','DEMO-NOTICE-002'),
('three-check','班次待执行提醒','今日班次还有班中和班后检查待执行。','THREE_CHECK',1,'NORMAL','DEMO-NOTICE-003')) x(module_key,title,summary,biz_type,biz_id,severity,dedup_key) ON u.username='demo-safety' AND c.org_code='DEMO-COMPANY'
ON CONFLICT (dedup_key) DO NOTHING;

INSERT INTO biz_notification_recipient (notification_id, recipient_user_id, handling_status, action_status, recipient_reason)
SELECT n.id, u.id, 'NONE', 'PENDING', 'ASSIGNEE' FROM biz_notification n CROSS JOIN sys_user u WHERE u.username='demo-safety' AND n.dedup_key LIKE 'DEMO-NOTICE-%'
ON CONFLICT (notification_id, recipient_user_id) DO NOTHING;

COMMIT;
