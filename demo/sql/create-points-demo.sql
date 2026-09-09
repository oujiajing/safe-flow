BEGIN;

UPDATE sys_user_profile p SET points = x.points, received_points = x.received_points, updated_at=CURRENT_TIMESTAMP
FROM (VALUES ('portfolio-admin',30,30),('demo-safety',24,24),('demo-team-lead',18,18),('demo-worker',12,12),('demo-rectifier',8,8),('demo-acceptor',16,16)) x(username,points,received_points) WHERE p.user_id=(SELECT id FROM sys_user WHERE username=x.username);

INSERT INTO three_check_record (module_key, record_no, company_id, department_id, team_id, owner_user_id, business_date, status, payload_json, version, created_by, updated_by, source_record_id, client_request_id)
SELECT 'points-flow', x.record_no, c.id, d.id, t.id, u.id, CURRENT_DATE-x.offset_days, 'ARCHIVED', json_build_object('pointsReason',x.reason,'pointsChange',x.change,'pointsQuantity',x.quantity,'statusLabel','已归档')::text, 1, u.id, u.id, x.record_no, 'portfolio-demo-v1-' || x.record_no
FROM sys_org c CROSS JOIN (VALUES
('DEMO-POINTS-001','demo-worker','DEMO-GREEN-TEAM',2,'完成安全学习','加分',5),('DEMO-POINTS-002','demo-team-lead','DEMO-MORNING-TEAM',4,'班组检查规范','加分',3),('DEMO-POINTS-003','demo-safety','DEMO-SAFETY-TEAM',1,'主动上报隐患','加分',8),('DEMO-POINTS-004','demo-rectifier','DEMO-BLUE-TEAM',3,'整改及时','加分',6),('DEMO-POINTS-005','demo-worker','DEMO-GREEN-TEAM',7,'违章扣分','扣分',-2),('DEMO-POINTS-006','demo-team-lead','DEMO-MORNING-TEAM',9,'积分兑换','兑换',-3)) x(record_no,username,team_code,offset_days,reason,change,quantity) JOIN sys_org d ON d.org_code='DEMO-OPS' JOIN sys_org t ON t.org_code=x.team_code JOIN sys_user u ON u.username=x.username
WHERE c.org_code='DEMO-COMPANY' AND NOT EXISTS (SELECT 1 FROM three_check_record r WHERE r.record_no=x.record_no);

COMMIT;
