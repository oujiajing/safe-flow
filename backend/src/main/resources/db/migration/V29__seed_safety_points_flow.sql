INSERT INTO three_check_record (
  module_key, record_no, company_id, department_id, team_id, owner_user_id,
  business_date, status, payload_json, image_check_status, video_check_status,
  submitted_by, submitted_at, reminder_count, created_by, updated_by,
  source_channel, client_request_id, last_synced_at, created_at, updated_at, deleted
) VALUES
  (
    'points-flow', 'PF-20260522-001', 4, 101109, 1011001, 2,
    '2026-05-22', 'OPENED',
    '{"createdAt":"2026-05-22 09:20:00","user":"湖贝班长","pointsReason":"安全学习奖励","pointsChange":"加分","pointsQuantity":5,"statusLabel":"加分"}',
    '未上传', '未上传', 1, '2026-05-22 09:20:00', 0, 1, 1, 'PC', 'seed-points-flow-001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
  ),
  (
    'points-flow', 'PF-20260522-002', 4, 101110, 1011003, 3,
    '2026-05-22', 'OPENED',
    '{"createdAt":"2026-05-22 10:15:00","user":"幕墙安全员","pointsReason":"违章扣分","pointsChange":"扣分","pointsQuantity":2,"statusLabel":"扣分"}',
    '未上传', '未上传', 1, '2026-05-22 10:15:00', 0, 1, 1, 'PC', 'seed-points-flow-002', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
  ),
  (
    'points-flow', 'PF-20260521-003', 4, 101109, 1011001, 2,
    '2026-05-21', 'OPENED',
    '{"createdAt":"2026-05-21 17:30:00","user":"湖贝班长","pointsReason":"自动售货机兑换","pointsChange":"兑换","pointsQuantity":3,"vendingMachine":"一号机","goods":"矿泉水","statusLabel":"兑换"}',
    '未上传', '未上传', 1, '2026-05-21 17:30:00', 0, 1, 1, 'PC', 'seed-points-flow-003', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0
  );
