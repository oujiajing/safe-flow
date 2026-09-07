UPDATE three_check_record
SET payload_json = REPLACE(payload_json, '"pointsReason":"安全学习奖励"', '"pointsReason":"考试通过"')
WHERE module_key = 'points-flow'
  AND record_no = 'PF-20260522-001'
  AND payload_json LIKE '%"pointsReason":"安全学习奖励"%';

UPDATE three_check_record
SET payload_json = REPLACE(payload_json, '"pointsReason":"违章扣分"', '"pointsReason":"考试不通过"')
WHERE module_key = 'points-flow'
  AND record_no = 'PF-20260522-002'
  AND payload_json LIKE '%"pointsReason":"违章扣分"%';

UPDATE three_check_record
SET payload_json = REPLACE(payload_json, '"pointsReason":"自动售货机兑换"', '"pointsReason":"积分兑换"')
WHERE module_key = 'points-flow'
  AND record_no = 'PF-20260521-003'
  AND payload_json LIKE '%"pointsReason":"自动售货机兑换"%';
