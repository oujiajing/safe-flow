-- Remove business test data that was incorrectly shipped in V34, V35, and V41.
-- Published migrations remain immutable; this additive migration makes both
-- existing databases and newly migrated databases end in a production-safe state.

DELETE FROM hazard_rectification_flow_log
WHERE order_id IN (
  SELECT id
  FROM hazard_rectification_order
  WHERE order_no = 'MCTEST-HRO-YC-001'
);

DELETE FROM hazard_rectification_order_item
WHERE order_id IN (
  SELECT id
  FROM hazard_rectification_order
  WHERE order_no = 'MCTEST-HRO-YC-001'
);

DELETE FROM hazard_rectification_order
WHERE order_no = 'MCTEST-HRO-YC-001';

DELETE FROM risk_control_hazard
WHERE id IN (9501, 9502, 9503, 9504, 9505, 9506, 9507, 9508);

DELETE FROM risk_control_library
WHERE id IN (9401, 9402, 9403, 9404);

DELETE FROM training_safety_learning_content
WHERE code LIKE 'MCTEST-LEARN-%';

DELETE FROM special_work_record
WHERE project LIKE 'MCTEST-SW-%';

DELETE FROM three_check_record
WHERE record_no LIKE 'MCTEST-%'
  AND (
    LOWER(client_request_id) LIKE 'mctest-%'
    OR payload_json LIKE '%"source":"monitor-center-test"%'
  );
