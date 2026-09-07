UPDATE shift_task
SET company_id = 4, department_id = 4, team_id = 4
WHERE company_id IN (5, 6, 7)
   OR department_id IN (5, 6, 7)
   OR team_id IN (5, 6, 7);

UPDATE pre_shift_meeting
SET company_id = 4, department_id = 4, team_id = 4
WHERE company_id IN (5, 6, 7)
   OR department_id IN (5, 6, 7)
   OR team_id IN (5, 6, 7);

UPDATE sys_user SET org_id = 4 WHERE org_id IN (5, 6, 7);

UPDATE shift_task
SET company_id = 8, department_id = 8, team_id = 8
WHERE company_id IN (9, 10)
   OR department_id IN (9, 10)
   OR team_id IN (9, 10);

UPDATE pre_shift_meeting
SET company_id = 8, department_id = 8, team_id = 8
WHERE company_id IN (9, 10)
   OR department_id IN (9, 10)
   OR team_id IN (9, 10);

UPDATE sys_user SET org_id = 8 WHERE org_id IN (9, 10);

UPDATE sys_org
SET status = 'INACTIVE', deleted = 1
WHERE id IN (5, 6, 7, 9, 10);
