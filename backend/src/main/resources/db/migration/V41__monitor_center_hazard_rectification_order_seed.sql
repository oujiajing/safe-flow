INSERT INTO hazard_rectification_order (
  order_no, source_type, source_module_key, source_record_id, source_record_no,
  root_dispatch_record_id, company_id, department_id, team_id, business_date,
  hazard_count, status, rectification_department_id, rectification_responsible_user_id,
  rectification_requirement, rectification_deadline, issued_by, issued_at,
  created_by, updated_by, created_at, updated_at, deleted
)
SELECT
  'MCTEST-HRO-YC-001', 'MANUAL', 'manual', NULL, NULL,
  NULL, 4, 101109, 1011001, DATE '2026-06-01',
  1, 'PENDING_RECTIFY', 101109, 2,
  '监控中心测试-脚手架通道材料堆放', TIMESTAMP '2026-06-02 18:00:00', 1,
  TIMESTAMP '2026-06-01 09:30:00', 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (
  SELECT 1
  FROM hazard_rectification_order
  WHERE order_no = 'MCTEST-HRO-YC-001'
    AND deleted = 0
);

INSERT INTO hazard_rectification_order_item (
  order_id, source_line_id, source_line_index, risk_type, check_item,
  hazard_description, rectification_status, sort_order, created_at, updated_at, deleted
)
SELECT
  o.id, 'mctest-hro-yc-001-line-1', 0, '现场环境', '脚手架通道',
  '脚手架通道材料堆放', 'PENDING_RECTIFY', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
FROM hazard_rectification_order o
WHERE o.order_no = 'MCTEST-HRO-YC-001'
  AND o.deleted = 0
  AND NOT EXISTS (
    SELECT 1
    FROM hazard_rectification_order_item i
    WHERE i.order_id = o.id
      AND i.source_line_id = 'mctest-hro-yc-001-line-1'
      AND i.deleted = 0
  );

INSERT INTO hazard_rectification_flow_log (
  order_id, from_status, to_status, action, action_label, operator_id, remark,
  payload_json, created_at
)
SELECT
  o.id, NULL, 'PENDING_ASSIGN', 'CREATE', '手工创建', 1,
  '监控中心业务测试数据', '{"source":"monitor-center-test"}', TIMESTAMP '2026-06-01 09:20:00'
FROM hazard_rectification_order o
WHERE o.order_no = 'MCTEST-HRO-YC-001'
  AND o.deleted = 0
  AND NOT EXISTS (
    SELECT 1
    FROM hazard_rectification_flow_log l
    WHERE l.order_id = o.id
      AND l.action = 'CREATE'
      AND l.action_label = '手工创建'
  );

INSERT INTO hazard_rectification_flow_log (
  order_id, from_status, to_status, action, action_label, operator_id, remark,
  payload_json, created_at
)
SELECT
  o.id, 'PENDING_ASSIGN', 'PENDING_RECTIFY', 'ISSUE_RECTIFICATION', '下发整改', 1,
  '请清理脚手架通道材料堆放并保持通道畅通', '{"source":"monitor-center-test"}',
  TIMESTAMP '2026-06-01 09:30:00'
FROM hazard_rectification_order o
WHERE o.order_no = 'MCTEST-HRO-YC-001'
  AND o.deleted = 0
  AND NOT EXISTS (
    SELECT 1
    FROM hazard_rectification_flow_log l
    WHERE l.order_id = o.id
      AND l.action = 'ISSUE_RECTIFICATION'
      AND l.action_label = '下发整改'
  );
