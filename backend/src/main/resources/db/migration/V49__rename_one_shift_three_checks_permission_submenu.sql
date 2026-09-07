UPDATE sys_menu
SET title = '三查',
    updated_at = CURRENT_TIMESTAMP
WHERE menu_code = 'PINGAN_ONE_SHIFT_THREE_CHECKS_MODULE'
  AND deleted = 0;
