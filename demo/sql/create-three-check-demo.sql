-- One-shift-three-checks demonstration records for portfolio_demo.
BEGIN;
DO $$
DECLARE
  company_id BIGINT;
  ops_id BIGINT;
  equipment_id BIGINT;
  morning_id BIGINT;
  green_id BIGINT;
  blue_id BIGINT;
  lead_id BIGINT;
  safety_id BIGINT;
  worker_id BIGINT;
  task_id BIGINT;
  root_id BIGINT;
  complete_date DATE := CURRENT_DATE - 2;
  active_date DATE := CURRENT_DATE - 1;
  today_date DATE := CURRENT_DATE;
BEGIN
  SELECT id INTO company_id FROM sys_org WHERE org_code='DEMO-COMPANY';
  SELECT id INTO ops_id FROM sys_org WHERE org_code='DEMO-OPS';
  SELECT id INTO equipment_id FROM sys_org WHERE org_code='DEMO-EQUIPMENT';
  SELECT id INTO morning_id FROM sys_org WHERE org_code='DEMO-MORNING-TEAM';
  SELECT id INTO green_id FROM sys_org WHERE org_code='DEMO-GREEN-TEAM';
  SELECT id INTO blue_id FROM sys_org WHERE org_code='DEMO-BLUE-TEAM';
  SELECT id INTO lead_id FROM sys_user WHERE username='demo-team-lead';
  SELECT id INTO safety_id FROM sys_user WHERE username='demo-safety';
  SELECT id INTO worker_id FROM sys_user WHERE username='demo-worker';

  INSERT INTO shift_task (task_no, company_id, department_id, team_id, shift_date, shift_name, leader_user_id, status, task_type, source_channel, source_record_id, client_request_id)
  VALUES ('DEMO-SHIFT-COMPLETE', company_id, ops_id, morning_id, complete_date, '晨星装配班完成班次', lead_id, 'OPENED', 'ONE_SHIFT_THREE_CHECKS', 'PC', 'DEMO-SHIFT-COMPLETE', 'portfolio-demo-v1-shift-complete')
  ON CONFLICT (task_no) DO UPDATE SET shift_date=EXCLUDED.shift_date, status=EXCLUDED.status, deleted=0, updated_at=CURRENT_TIMESTAMP
  RETURNING id INTO task_id;
  INSERT INTO three_check_record (module_key, record_no, task_id, company_id, department_id, team_id, owner_user_id, business_date, status, payload_json, submitted_by, submitted_at, version, created_by, updated_by, source_record_id, client_request_id, root_dispatch_record_id)
  VALUES ('team-dispatch', 'DEMO-3C-COMPLETE-ROOT', task_id, company_id, ops_id, morning_id, lead_id, complete_date, 'OPENED', json_build_object('teamTask','装配设备安全生产任务','dispatchType','常规派班','dispatchDate',complete_date,'dispatchStatus','生效','managerCount',2)::text, lead_id, CURRENT_TIMESTAMP, 1, lead_id, lead_id, 'DEMO-3C-COMPLETE-ROOT', 'portfolio-demo-v1-3c-complete-root', NULL)
  ON CONFLICT (record_no) DO UPDATE SET status=EXCLUDED.status, payload_json=EXCLUDED.payload_json, deleted=0, updated_at=CURRENT_TIMESTAMP
  RETURNING id INTO root_id;
  INSERT INTO three_check_record (module_key, record_no, task_id, company_id, department_id, team_id, owner_user_id, business_date, status, payload_json, submitted_by, submitted_at, version, created_by, updated_by, source_record_id, client_request_id, root_dispatch_record_id)
  VALUES
  ('pre-shift-meeting','DEMO-3C-COMPLETE-MEETING',task_id,company_id,ops_id,morning_id,lead_id,complete_date,'OPENED',json_build_object('meetingTitle','班前安全交底','attendees','顾晨、叶青、韩松','content','确认作业风险、个人防护和应急联络方式')::text,lead_id,CURRENT_TIMESTAMP,1,lead_id,lead_id,'DEMO-3C-COMPLETE-MEETING','portfolio-demo-v1-3c-complete-meeting',root_id),
  ('pre-shift-safety-activity','DEMO-3C-COMPLETE-ACTIVITY',task_id,company_id,ops_id,morning_id,safety_id,complete_date,'OPENED',json_build_object('activityTitle','班前安全活动','workContent','装配区域点检与通道确认','laborCount',6)::text,safety_id,CURRENT_TIMESTAMP,1,safety_id,safety_id,'DEMO-3C-COMPLETE-ACTIVITY','portfolio-demo-v1-3c-complete-activity',root_id),
  ('pre-shift-inspection','DEMO-3C-COMPLETE-PRE',task_id,company_id,ops_id,morning_id,safety_id,complete_date,'OPENED',json_build_object('inspectionType','班前检查','items','临时用电箱、设备防护、个人防护用品','statusLabel','已检查')::text,safety_id,CURRENT_TIMESTAMP,1,safety_id,safety_id,'DEMO-3C-COMPLETE-PRE','portfolio-demo-v1-3c-complete-pre',root_id),
  ('mid-shift-inspection','DEMO-3C-COMPLETE-MID',task_id,company_id,ops_id,morning_id,safety_id,complete_date,'OPENED',json_build_object('inspectionType','班中检查','items','人车通道、吊装警戒、临时用电','statusLabel','已检查')::text,safety_id,CURRENT_TIMESTAMP,1,safety_id,safety_id,'DEMO-3C-COMPLETE-MID','portfolio-demo-v1-3c-complete-mid',root_id),
  ('post-shift-inspection','DEMO-3C-COMPLETE-POST',task_id,company_id,ops_id,morning_id,lead_id,complete_date,'ARCHIVED',json_build_object('inspectionType','班后检查','items','清场、断电、交接','statusLabel','已归档')::text,lead_id,CURRENT_TIMESTAMP,1,lead_id,lead_id,'DEMO-3C-COMPLETE-POST','portfolio-demo-v1-3c-complete-post',root_id)
  ON CONFLICT (record_no) DO UPDATE SET status=EXCLUDED.status, payload_json=EXCLUDED.payload_json, deleted=0, updated_at=CURRENT_TIMESTAMP;

  INSERT INTO shift_task (task_no, company_id, department_id, team_id, shift_date, shift_name, leader_user_id, status, task_type, source_channel, source_record_id, client_request_id)
  VALUES ('DEMO-SHIFT-ACTIVE', company_id, ops_id, green_id, active_date, '青禾保障班进行中班次', lead_id, 'OPENED', 'ONE_SHIFT_THREE_CHECKS', 'PC', 'DEMO-SHIFT-ACTIVE', 'portfolio-demo-v1-shift-active')
  ON CONFLICT (task_no) DO UPDATE SET shift_date=EXCLUDED.shift_date, status=EXCLUDED.status, deleted=0, updated_at=CURRENT_TIMESTAMP
  RETURNING id INTO task_id;
  INSERT INTO three_check_record (module_key, record_no, task_id, company_id, department_id, team_id, owner_user_id, business_date, status, payload_json, submitted_by, submitted_at, version, created_by, updated_by, source_record_id, client_request_id, root_dispatch_record_id)
  VALUES ('team-dispatch','DEMO-3C-ACTIVE-ROOT',task_id,company_id,ops_id,green_id,lead_id,active_date,'OPENED',json_build_object('teamTask','保障设备巡检任务','dispatchType','今日派班','dispatchDate',active_date,'dispatchStatus','生效')::text,lead_id,CURRENT_TIMESTAMP,1,lead_id,lead_id,'DEMO-3C-ACTIVE-ROOT','portfolio-demo-v1-3c-active-root',NULL)
  ON CONFLICT (record_no) DO UPDATE SET status=EXCLUDED.status, payload_json=EXCLUDED.payload_json, deleted=0, updated_at=CURRENT_TIMESTAMP
  RETURNING id INTO root_id;
  INSERT INTO three_check_record (module_key, record_no, task_id, company_id, department_id, team_id, owner_user_id, business_date, status, payload_json, submitted_by, submitted_at, version, created_by, updated_by, source_record_id, client_request_id, root_dispatch_record_id)
  VALUES
  ('pre-shift-meeting','DEMO-3C-ACTIVE-MEETING',task_id,company_id,ops_id,green_id,lead_id,active_date,'OPENED',json_build_object('meetingTitle','班前安全交底','content','确认设备保障任务和作业边界')::text,lead_id,CURRENT_TIMESTAMP,1,lead_id,lead_id,'DEMO-3C-ACTIVE-MEETING','portfolio-demo-v1-3c-active-meeting',root_id),
  ('pre-shift-safety-activity','DEMO-3C-ACTIVE-ACTIVITY',task_id,company_id,ops_id,green_id,safety_id,active_date,'OPENED',json_build_object('activityTitle','班前安全活动','workContent','设备点检与风险提示')::text,safety_id,CURRENT_TIMESTAMP,1,safety_id,safety_id,'DEMO-3C-ACTIVE-ACTIVITY','portfolio-demo-v1-3c-active-activity',root_id),
  ('pre-shift-inspection','DEMO-3C-ACTIVE-PRE',task_id,company_id,ops_id,green_id,safety_id,active_date,'OPENED',json_build_object('inspectionType','班前检查','statusLabel','已检查')::text,safety_id,CURRENT_TIMESTAMP,1,safety_id,safety_id,'DEMO-3C-ACTIVE-PRE','portfolio-demo-v1-3c-active-pre',root_id),
  ('mid-shift-inspection','DEMO-3C-ACTIVE-MID',task_id,company_id,ops_id,green_id,safety_id,active_date,'DRAFT',json_build_object('inspectionType','班中检查','statusLabel','待执行')::text,NULL,NULL,0,safety_id,safety_id,'DEMO-3C-ACTIVE-MID','portfolio-demo-v1-3c-active-mid',root_id),
  ('post-shift-inspection','DEMO-3C-ACTIVE-POST',task_id,company_id,ops_id,green_id,lead_id,active_date,'DRAFT',json_build_object('inspectionType','班后检查','statusLabel','待执行')::text,NULL,NULL,0,lead_id,lead_id,'DEMO-3C-ACTIVE-POST','portfolio-demo-v1-3c-active-post',root_id)
  ON CONFLICT (record_no) DO UPDATE SET status=EXCLUDED.status, payload_json=EXCLUDED.payload_json, deleted=0, updated_at=CURRENT_TIMESTAMP;

  INSERT INTO shift_task (task_no, company_id, department_id, team_id, shift_date, shift_name, leader_user_id, status, task_type, source_channel, source_record_id, client_request_id)
  VALUES ('DEMO-SHIFT-TODAY', company_id, ops_id, morning_id, today_date, '晨星装配班今日班次', lead_id, 'DRAFT', 'ONE_SHIFT_THREE_CHECKS', 'PC', 'DEMO-SHIFT-TODAY', 'portfolio-demo-v1-shift-today')
  ON CONFLICT (task_no) DO UPDATE SET shift_date=EXCLUDED.shift_date, status=EXCLUDED.status, deleted=0, updated_at=CURRENT_TIMESTAMP
  RETURNING id INTO task_id;
  INSERT INTO three_check_record (module_key, record_no, task_id, company_id, department_id, team_id, owner_user_id, business_date, status, payload_json, version, created_by, updated_by, source_record_id, client_request_id, root_dispatch_record_id)
  VALUES ('team-dispatch','DEMO-3C-TODAY-ROOT',task_id,company_id,ops_id,morning_id,lead_id,today_date,'DRAFT',json_build_object('teamTask','今日装配安全任务','dispatchType','待提交','dispatchDate',today_date,'dispatchStatus','草稿')::text,0,lead_id,lead_id,'DEMO-3C-TODAY-ROOT','portfolio-demo-v1-3c-today-root',NULL)
  ON CONFLICT (record_no) DO UPDATE SET status=EXCLUDED.status, payload_json=EXCLUDED.payload_json, deleted=0, updated_at=CURRENT_TIMESTAMP
  RETURNING id INTO root_id;
  INSERT INTO three_check_record (module_key, record_no, task_id, company_id, department_id, team_id, owner_user_id, business_date, status, payload_json, version, created_by, updated_by, source_record_id, client_request_id, root_dispatch_record_id)
  VALUES
  ('pre-shift-meeting','DEMO-3C-TODAY-MEETING',task_id,company_id,ops_id,morning_id,lead_id,today_date,'DRAFT',json_build_object('meetingTitle','班前安全交底','statusLabel','待执行')::text,0,lead_id,lead_id,'DEMO-3C-TODAY-MEETING','portfolio-demo-v1-3c-today-meeting',root_id),
  ('pre-shift-safety-activity','DEMO-3C-TODAY-ACTIVITY',task_id,company_id,ops_id,morning_id,safety_id,today_date,'DRAFT',json_build_object('activityTitle','班前安全活动','statusLabel','待执行')::text,0,safety_id,safety_id,'DEMO-3C-TODAY-ACTIVITY','portfolio-demo-v1-3c-today-activity',root_id),
  ('pre-shift-inspection','DEMO-3C-TODAY-PRE',task_id,company_id,ops_id,morning_id,safety_id,today_date,'DRAFT',json_build_object('inspectionType','班前检查','statusLabel','待执行')::text,0,safety_id,safety_id,'DEMO-3C-TODAY-PRE','portfolio-demo-v1-3c-today-pre',root_id),
  ('mid-shift-inspection','DEMO-3C-TODAY-MID',task_id,company_id,ops_id,morning_id,safety_id,today_date,'DRAFT',json_build_object('inspectionType','班中检查','statusLabel','待执行')::text,0,safety_id,safety_id,'DEMO-3C-TODAY-MID','portfolio-demo-v1-3c-today-mid',root_id),
  ('post-shift-inspection','DEMO-3C-TODAY-POST',task_id,company_id,ops_id,morning_id,lead_id,today_date,'DRAFT',json_build_object('inspectionType','班后检查','statusLabel','待执行')::text,0,lead_id,lead_id,'DEMO-3C-TODAY-POST','portfolio-demo-v1-3c-today-post',root_id)
  ON CONFLICT (record_no) DO UPDATE SET status=EXCLUDED.status, payload_json=EXCLUDED.payload_json, deleted=0, updated_at=CURRENT_TIMESTAMP;
END $$;
COMMIT;
