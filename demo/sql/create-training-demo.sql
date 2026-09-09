BEGIN;

INSERT INTO training_safety_learning_content (company_id, category, title, content, learning_date, duration_text, code, draft, status, created_by, updated_by, deleted)
SELECT c.id, x.category, x.title, x.content, x.learning_date, x.duration, x.code, x.draft, x.status, u.id, u.id, 0
FROM sys_org c CROSS JOIN sys_user u JOIN (VALUES
('岗位安全','高处作业防坠落','学习临边防护、安全带使用和监护要求。',CURRENT_DATE-2,'8分钟','DEMO-LEARN-001',NULL,'ACTIVE'),
('风险辨识','临时用电点检','掌握配电箱、漏电保护和挂牌上锁的检查要点。',CURRENT_DATE-1,'6分钟','DEMO-LEARN-002',NULL,'ACTIVE'),
('应急处置','初期火灾处置','熟悉报警、疏散和灭火器材使用流程。',CURRENT_DATE,'10分钟','DEMO-LEARN-003',NULL,'ACTIVE'),
('岗位安全','吊装指挥手势','通过图文示例掌握吊装指挥和警戒要求。',CURRENT_DATE-5,'7分钟','DEMO-LEARN-004',NULL,'ACTIVE'),
('风险辨识','叉车人车分流','了解通道标线、交叉路口和充电区安全规则。',CURRENT_DATE-8,'5分钟','DEMO-LEARN-005','是','DRAFT'),
('应急处置','应急集合流程','演示集合点确认、人员清点与信息上报。',CURRENT_DATE-12,'5分钟','DEMO-LEARN-006',NULL,'INACTIVE')) x(category,title,content,learning_date,duration,code,draft,status) ON c.org_code='DEMO-COMPANY' AND u.username='demo-safety'
ON CONFLICT (code) WHERE deleted=0 DO UPDATE SET category=EXCLUDED.category, title=EXCLUDED.title, content=EXCLUDED.content, learning_date=EXCLUDED.learning_date, status=EXCLUDED.status, deleted=0, updated_at=CURRENT_TIMESTAMP;

INSERT INTO training_exam_task (company_id, department_id, exam, exam_date, status, created_by, updated_by, deleted, code, remark, team_id, duration_minutes)
SELECT c.id, d.id, x.exam, x.exam_date, x.status, u.id, u.id, 0, x.code, x.remark, t.id, x.duration
FROM sys_org c JOIN sys_org d ON d.org_code='DEMO-OPS' JOIN sys_org t ON t.org_code='DEMO-MORNING-TEAM' JOIN sys_user u ON u.username='demo-safety' JOIN (VALUES
('班组安全基础测验',CURRENT_DATE-1,'ACTIVE','DEMO-EXAM-TASK-001','覆盖个人防护、用电和通道安全',20),
('设备保障专项测验',CURRENT_DATE+3,'DRAFT','DEMO-EXAM-TASK-002','覆盖设备点检、检维修和应急处置',30)) x(exam,exam_date,status,code,remark,duration) ON c.org_code='DEMO-COMPANY'
ON CONFLICT DO NOTHING;

INSERT INTO training_exam_question (task_id, question_type, question_text, selected_option, all_options, answer, score, actual_score, sort_order, created_by, updated_by, deleted, options_json, answers_json, answer_explanation)
SELECT t.id, x.question_type, x.question_text, NULL, x.all_options, x.answer, 25, 25, x.sort_order, u.id, u.id, 0, x.all_options, x.answer, x.explanation
FROM training_exam_task t CROSS JOIN sys_user u JOIN (VALUES
('DEMO-EXAM-TASK-001','SINGLE','临时用电箱检查首先应确认什么？','A|箱门闭合、漏保有效|B|只看外观|C|只看标牌|D|无需检查','A',1,'应先确认防护和保护装置有效'),
('DEMO-EXAM-TASK-001','SINGLE','高处作业人员应正确使用什么？','A|安全带和可靠挂点|B|普通绳索|C|无需防护|D|只戴安全帽','A',2,'高处作业需要可靠防坠落措施'),
('DEMO-EXAM-TASK-002','SINGLE','设备检维修前应执行哪项措施？','A|停机隔离并挂牌|B|直接拆卸|C|只口头通知|D|加快作业','A',1,'停机隔离和挂牌上锁是基本要求'),
('DEMO-EXAM-TASK-002','MULTIPLE','发现初期火灾时应采取哪些动作？','A|报警|B|组织疏散|C|在安全条件下灭火|D|围观','A,B,C',2,'报警、疏散和安全处置应同步开展')) x(task_code,question_type,question_text,all_options,answer,sort_order,explanation) ON x.task_code=t.code AND u.username='demo-safety';

INSERT INTO training_exam_result (task_id, company_id, department_id, exam_person_user_id, exam_person_name, score, exam_date, status, created_by, updated_by, deleted, code, remark, submission_request_id, current_question_index, remaining_seconds)
SELECT t.id, c.id, d.id, u.id, u.real_name, coalesce(x.score,0), t.exam_date, x.status, s.id, s.id, 0, x.code, x.remark, x.code, 0, CASE WHEN x.status='PENDING_EXAM' THEN 1200 ELSE 0 END
FROM training_exam_task t JOIN sys_org c ON c.org_code='DEMO-COMPANY' JOIN sys_org d ON d.org_code='DEMO-OPS' JOIN sys_user u ON u.username IN ('demo-worker','demo-member-01','demo-member-02','demo-rectifier') JOIN sys_user s ON s.username='demo-safety' JOIN (VALUES
('DEMO-EXAM-TASK-001','demo-worker',95,'EXAMED','DEMO-RESULT-001','已完成基础测验'),('DEMO-EXAM-TASK-001','demo-member-01',88,'EXAMED','DEMO-RESULT-002','已完成基础测验'),('DEMO-EXAM-TASK-001','demo-member-02',NULL,'PENDING_REVIEW','DEMO-RESULT-003','待人工复核'),('DEMO-EXAM-TASK-002','demo-rectifier',NULL,'PENDING_EXAM','DEMO-RESULT-004','待参加专项测验')) x(task_code,username,score,status,code,remark) ON x.task_code=t.code AND x.username=u.username;

COMMIT;
