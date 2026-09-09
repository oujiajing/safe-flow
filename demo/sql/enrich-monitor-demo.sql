-- Additional current-day and recent records to make every monitor-center panel visual.
BEGIN;

DO $$
DECLARE
  company_id BIGINT;
  v_team_code TEXT;
  team_id BIGINT;
  department_id BIGINT;
  owner_id BIGINT;
  v_task_id BIGINT;
  v_root_id BIGINT;
  v_root_no TEXT;
  v_task_no TEXT;
  v_stage TEXT;
  v_stage_status TEXT;
  idx INT := 0;
BEGIN
  SELECT id INTO company_id FROM sys_org WHERE org_code='DEMO-COMPANY';
  FOREACH v_team_code IN ARRAY ARRAY['DEMO-MORNING-TEAM','DEMO-GREEN-TEAM','DEMO-BLUE-TEAM','DEMO-SAFETY-TEAM','DEMO-EQUIPMENT-TEAM'] LOOP
    idx := idx + 1;
    SELECT id INTO team_id FROM sys_org WHERE org_code=v_team_code;
    SELECT CASE WHEN p.org_type='DEPARTMENT' THEN p.id ELSE gp.id END INTO department_id
    FROM sys_org p LEFT JOIN sys_org gp ON gp.id=p.parent_id WHERE p.id=(SELECT parent_id FROM sys_org WHERE id=team_id);
    SELECT id INTO owner_id FROM sys_user WHERE username = CASE WHEN v_team_code IN ('DEMO-MORNING-TEAM','DEMO-GREEN-TEAM') THEN 'demo-team-lead' WHEN v_team_code='DEMO-BLUE-TEAM' THEN 'demo-rectifier' ELSE 'demo-safety' END;
    v_task_no := 'DEMO-MONITOR-TASK-' || lpad(idx::text, 2, '0');
    v_root_no := 'DEMO-MONITOR-DISPATCH-' || lpad(idx::text, 2, '0');
    INSERT INTO shift_task (task_no, company_id, department_id, team_id, shift_date, shift_name, leader_user_id, status, task_type, source_channel, source_record_id, client_request_id)
    VALUES (v_task_no, company_id, department_id, team_id, CURRENT_DATE, '今日安全生产班次-' || idx, owner_id, CASE WHEN idx=5 THEN 'DRAFT' ELSE 'OPENED' END, 'ONE_SHIFT_THREE_CHECKS', 'PC', v_task_no, 'portfolio-demo-v1-' || v_task_no)
    ON CONFLICT (task_no) DO UPDATE SET shift_date=EXCLUDED.shift_date,status=EXCLUDED.status,deleted=0,updated_at=CURRENT_TIMESTAMP
    RETURNING id INTO v_task_id;
    INSERT INTO three_check_record (module_key, record_no, task_id, company_id, department_id, team_id, owner_user_id, business_date, status, payload_json, submitted_by, submitted_at, version, created_by, updated_by, source_record_id, client_request_id)
    VALUES ('team-dispatch', v_root_no, v_task_id, company_id, department_id, team_id, owner_id, CURRENT_DATE, CASE WHEN idx=5 THEN 'DRAFT' ELSE 'OPENED' END, json_build_object('teamTask','今日安全生产现场任务','dispatchType',CASE WHEN idx=5 THEN '待提交' ELSE '今日派班' END,'dispatchDate',CURRENT_DATE,'teamName',(SELECT org_name FROM sys_org WHERE id=team_id))::text, CASE WHEN idx=5 THEN NULL ELSE owner_id END, CASE WHEN idx=5 THEN NULL ELSE CURRENT_TIMESTAMP END, CASE WHEN idx=5 THEN 0 ELSE 1 END, owner_id, owner_id, v_root_no, 'portfolio-demo-v1-' || v_root_no)
    ON CONFLICT (record_no) DO UPDATE SET status=EXCLUDED.status,payload_json=EXCLUDED.payload_json,deleted=0,updated_at=CURRENT_TIMESTAMP
    RETURNING id INTO v_root_id;
    FOREACH v_stage IN ARRAY ARRAY['pre-shift-meeting','pre-shift-inspection','mid-shift-inspection','post-shift-inspection'] LOOP
      v_stage_status := CASE WHEN idx=5 OR (idx IN (3,4) AND v_stage IN ('mid-shift-inspection','post-shift-inspection')) THEN 'DRAFT' ELSE 'OPENED' END;
      INSERT INTO three_check_record (module_key, record_no, task_id, company_id, department_id, team_id, owner_user_id, business_date, status, payload_json, submitted_by, submitted_at, version, created_by, updated_by, source_record_id, client_request_id, root_dispatch_record_id)
      VALUES (v_stage, 'DEMO-MONITOR-' || lpad(idx::text,2,'0') || '-' || replace(v_stage,'-','_'), v_task_id, company_id, department_id, team_id, owner_id, CURRENT_DATE, v_stage_status, json_build_object('inspectionType',v_stage,'statusLabel',CASE WHEN v_stage_status='OPENED' THEN '已提交' ELSE '待执行' END,'teamName',(SELECT org_name FROM sys_org WHERE id=team_id))::text, CASE WHEN v_stage_status='OPENED' THEN owner_id ELSE NULL END, CASE WHEN v_stage_status='OPENED' THEN CURRENT_TIMESTAMP ELSE NULL END, CASE WHEN v_stage_status='OPENED' THEN 1 ELSE 0 END, owner_id, owner_id, 'DEMO-MONITOR-' || lpad(idx::text,2,'0') || '-' || replace(v_stage,'-','_'), 'portfolio-demo-v1-' || lpad(idx::text,2,'0') || '-' || replace(v_stage,'-','_'), v_root_id)
      ON CONFLICT (record_no) DO UPDATE SET status=EXCLUDED.status,payload_json=EXCLUDED.payload_json,deleted=0,updated_at=CURRENT_TIMESTAMP;
    END LOOP;
  END LOOP;
END $$;

-- Current-day hazards distributed across teams for team analysis and the accident board.
WITH hazards(order_no, team_code, department_code, status, description) AS (VALUES
('DEMO-MONITOR-HZ-001','DEMO-MORNING-TEAM','DEMO-OPS','PENDING_RECTIFY','装配通道有空托盘占用'),
('DEMO-MONITOR-HZ-002','DEMO-GREEN-TEAM','DEMO-OPS','RECTIFIED','设备点检标识缺失'),
('DEMO-MONITOR-HZ-003','DEMO-BLUE-TEAM','DEMO-EQUIPMENT','PENDING_ACCEPTANCE','检修平台踢脚板松动'),
('DEMO-MONITOR-HZ-004','DEMO-SAFETY-TEAM','DEMO-SAFETY','CLOSED','安全出口标识已恢复'),
('DEMO-MONITOR-HZ-005','DEMO-EQUIPMENT-TEAM','DEMO-EQUIPMENT','CANCELLED','重复上报记录已合并'))
INSERT INTO hazard_rectification_order (order_no, source_type, source_module_key, source_record_id, source_record_no, company_id, department_id, team_id, business_date, hazard_count, status, rectification_responsible_user_id, rectification_requirement, rectification_deadline, issued_by, issued_at, rectified_by, rectified_at, rectification_description, acceptance_user_id, acceptance_at, acceptance_result, acceptance_remark, closed_at, version, created_by, updated_by, deleted)
SELECT h.order_no, 'MANUAL', 'monitor-center', NULL, h.order_no, c.id, d.id, t.id, CURRENT_DATE, 1, h.status, r.id, '完成现场整改并保留复核记录', CURRENT_TIMESTAMP + interval '2 days', s.id, CURRENT_TIMESTAMP, CASE WHEN h.status IN ('RECTIFIED','PENDING_ACCEPTANCE','CLOSED') THEN r.id ELSE NULL END, CASE WHEN h.status IN ('RECTIFIED','PENDING_ACCEPTANCE','CLOSED') THEN CURRENT_TIMESTAMP ELSE NULL END, CASE WHEN h.status IN ('RECTIFIED','PENDING_ACCEPTANCE','CLOSED') THEN h.description || '，已完成整改。' ELSE NULL END, a.id, CASE WHEN h.status IN ('PENDING_ACCEPTANCE','CLOSED') THEN CURRENT_TIMESTAMP ELSE NULL END, CASE WHEN h.status='CLOSED' THEN 'ACCEPTED' ELSE NULL END, CASE WHEN h.status='CLOSED' THEN '验收通过' ELSE NULL END, CASE WHEN h.status='CLOSED' THEN CURRENT_TIMESTAMP ELSE NULL END, 1, s.id, s.id, 0
FROM hazards h JOIN sys_org c ON c.org_code='DEMO-COMPANY' JOIN sys_org t ON t.org_code=h.team_code JOIN sys_org d ON d.org_code=h.department_code JOIN sys_user s ON s.username='demo-safety' JOIN sys_user r ON r.username='demo-rectifier' JOIN sys_user a ON a.username='demo-acceptor'
ON CONFLICT (order_no) WHERE deleted=0 DO UPDATE SET status=EXCLUDED.status, business_date=CURRENT_DATE, rectification_description=EXCLUDED.rectification_description, deleted=0, updated_at=CURRENT_TIMESTAMP;

INSERT INTO hazard_rectification_order_item (order_id, source_line_id, source_line_index, risk_type, check_item, hazard_description, before_photo, default_follow_up_plan, source_snapshot_json, rectification_status, closed_at, sort_order, deleted)
SELECT o.id, o.order_no || '-ITEM-01', 0, '现场安全', h.description, h.description, 'synthetic://monitor/' || o.order_no || '.jpg', '当日复核', '{}', o.status, CASE WHEN o.status='CLOSED' THEN CURRENT_TIMESTAMP ELSE NULL END, 1, 0
FROM hazard_rectification_order o JOIN (VALUES ('DEMO-MONITOR-HZ-001','装配通道有空托盘占用'),('DEMO-MONITOR-HZ-002','设备点检标识缺失'),('DEMO-MONITOR-HZ-003','检修平台踢脚板松动'),('DEMO-MONITOR-HZ-004','安全出口标识已恢复'),('DEMO-MONITOR-HZ-005','重复上报记录已合并')) h(order_no,description) ON h.order_no=o.order_no
ON CONFLICT (order_id, source_line_id) WHERE deleted=0 DO UPDATE SET rectification_status=EXCLUDED.rectification_status, deleted=0, updated_at=CURRENT_TIMESTAMP;

-- Current-day special work cards and learning cards for the default dashboard range.
INSERT INTO special_work_record (company_id, project, work_type, application_time, work_content, work_location, risk_identification_result, implementation_start_time, implementation_end_time, safety_disclosure_person, guardian, disclosure_receiver, completion_acceptor, completion_acceptance_time, status, created_by, updated_by, deleted)
SELECT c.id, x.project, x.work_type, CURRENT_TIMESTAMP - interval '4 hours', x.content, x.location, x.risk, CASE WHEN x.status IN ('IN_PROGRESS','PENDING_ACCEPTANCE','COMPLETED') THEN CURRENT_TIMESTAMP - interval '2 hours' ELSE NULL END, CASE WHEN x.status='COMPLETED' THEN CURRENT_TIMESTAMP - interval '30 minutes' ELSE NULL END, '周宁', '唐越', '顾晨', CASE WHEN x.status='COMPLETED' THEN '苏晴' ELSE NULL END, CASE WHEN x.status='COMPLETED' THEN CURRENT_TIMESTAMP - interval '15 minutes' ELSE NULL END, x.status, u.id, u.id, 0
FROM sys_org c CROSS JOIN sys_user u JOIN (VALUES ('演示今日动火作业','动火作业','动火作业申请和监护','检维修车间','火花飞溅风险','PENDING_APPROVAL'),('演示今日高处作业','高处作业','高处平台护栏检修','总装一车间','高处坠落风险','IN_PROGRESS'),('演示今日临时用电','临时用电作业','配电箱改造验收','设备保障部','触电风险','PENDING_ACCEPTANCE'),('演示今日吊装作业','吊装作业','设备部件吊装转运','装配物流通道','吊物摆动风险','COMPLETED')) x(project,work_type,content,location,risk,status) ON c.org_code='DEMO-COMPANY' AND u.username='demo-safety'
WHERE NOT EXISTS (SELECT 1 FROM special_work_record s WHERE s.project=x.project AND s.deleted=0);

INSERT INTO training_safety_learning_content (company_id, category, title, content, learning_date, duration_text, code, status, created_by, updated_by, deleted)
SELECT c.id, x.category, x.title, x.content, CURRENT_DATE, x.duration, x.code, 'ACTIVE', u.id, u.id, 0
FROM sys_org c CROSS JOIN sys_user u JOIN (VALUES ('风险辨识','今日班前风险提示','演示今日班前风险辨识重点。','5分钟','DEMO-LEARN-TODAY-001'),('岗位安全','今日个人防护确认','演示今日个人防护用品确认流程。','4分钟','DEMO-LEARN-TODAY-002'),('应急处置','今日应急联络卡','演示应急联络和现场上报流程。','6分钟','DEMO-LEARN-TODAY-003')) x(category,title,content,duration,code) ON c.org_code='DEMO-COMPANY' AND u.username='demo-safety'
WHERE NOT EXISTS (SELECT 1 FROM training_safety_learning_content l WHERE l.code=x.code AND l.deleted=0);

COMMIT;
