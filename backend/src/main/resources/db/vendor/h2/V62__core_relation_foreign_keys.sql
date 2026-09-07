ALTER TABLE sys_org
  ADD CONSTRAINT fk_sys_org_parent
  FOREIGN KEY (parent_id) REFERENCES sys_org (id);
ALTER TABLE sys_user
  ADD CONSTRAINT fk_sys_user_org
  FOREIGN KEY (org_id) REFERENCES sys_org (id);
ALTER TABLE sys_user_role
  ADD CONSTRAINT fk_sys_user_role_user
  FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE;
ALTER TABLE sys_user_role
  ADD CONSTRAINT fk_sys_user_role_role
  FOREIGN KEY (role_id) REFERENCES sys_role (id) ON DELETE CASCADE;
ALTER TABLE sys_user_profile
  ADD CONSTRAINT fk_sys_user_profile_user
  FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE;
ALTER TABLE sys_role_menu
  ADD CONSTRAINT fk_sys_role_menu_role
  FOREIGN KEY (role_id) REFERENCES sys_role (id) ON DELETE CASCADE;
ALTER TABLE sys_role_menu
  ADD CONSTRAINT fk_sys_role_menu_menu
  FOREIGN KEY (menu_id) REFERENCES sys_menu (id) ON DELETE CASCADE;
ALTER TABLE sys_team_member
  ADD CONSTRAINT fk_sys_team_member_team
  FOREIGN KEY (team_org_id) REFERENCES sys_org (id) ON DELETE CASCADE;
ALTER TABLE sys_team_member
  ADD CONSTRAINT fk_sys_team_member_user
  FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE SET NULL;

ALTER TABLE shift_task
  ADD CONSTRAINT fk_shift_task_company
  FOREIGN KEY (company_id) REFERENCES sys_org (id);
ALTER TABLE shift_task
  ADD CONSTRAINT fk_shift_task_department
  FOREIGN KEY (department_id) REFERENCES sys_org (id);
ALTER TABLE shift_task
  ADD CONSTRAINT fk_shift_task_team
  FOREIGN KEY (team_id) REFERENCES sys_org (id);

ALTER TABLE three_check_record
  ADD CONSTRAINT fk_three_check_task
  FOREIGN KEY (task_id) REFERENCES shift_task (id) ON DELETE SET NULL;
ALTER TABLE three_check_record
  ADD CONSTRAINT fk_three_check_company
  FOREIGN KEY (company_id) REFERENCES sys_org (id);
ALTER TABLE three_check_record
  ADD CONSTRAINT fk_three_check_department
  FOREIGN KEY (department_id) REFERENCES sys_org (id);
ALTER TABLE three_check_record
  ADD CONSTRAINT fk_three_check_team
  FOREIGN KEY (team_id) REFERENCES sys_org (id);
ALTER TABLE three_check_record
  ADD CONSTRAINT fk_three_check_root_dispatch
  FOREIGN KEY (root_dispatch_record_id) REFERENCES three_check_record (id) ON DELETE SET NULL;

ALTER TABLE risk_control_library
  ADD CONSTRAINT fk_risk_library_company
  FOREIGN KEY (company_id) REFERENCES sys_org (id);
ALTER TABLE risk_control_hazard
  ADD CONSTRAINT fk_risk_hazard_library
  FOREIGN KEY (library_id) REFERENCES risk_control_library (id) ON DELETE CASCADE;
ALTER TABLE risk_control_hazard
  ADD CONSTRAINT fk_risk_hazard_company
  FOREIGN KEY (company_id) REFERENCES sys_org (id);
ALTER TABLE risk_four_color_map
  ADD CONSTRAINT fk_risk_map_company
  FOREIGN KEY (company_id) REFERENCES sys_org (id);

ALTER TABLE training_exam_task
  ADD CONSTRAINT fk_exam_task_company
  FOREIGN KEY (company_id) REFERENCES sys_org (id);
ALTER TABLE training_exam_task
  ADD CONSTRAINT fk_exam_task_department
  FOREIGN KEY (department_id) REFERENCES sys_org (id);
ALTER TABLE training_exam_result
  ADD CONSTRAINT fk_exam_result_task
  FOREIGN KEY (task_id) REFERENCES training_exam_task (id) ON DELETE CASCADE;
ALTER TABLE training_exam_result
  ADD CONSTRAINT fk_exam_result_company
  FOREIGN KEY (company_id) REFERENCES sys_org (id);
ALTER TABLE training_exam_result
  ADD CONSTRAINT fk_exam_result_department
  FOREIGN KEY (department_id) REFERENCES sys_org (id);
ALTER TABLE training_exam_question
  ADD CONSTRAINT fk_exam_question_task
  FOREIGN KEY (task_id) REFERENCES training_exam_task (id) ON DELETE CASCADE;
ALTER TABLE training_exam_result_question
  ADD CONSTRAINT fk_exam_result_question_result
  FOREIGN KEY (result_id) REFERENCES training_exam_result (id) ON DELETE CASCADE;
ALTER TABLE training_exam_result_question
  ADD CONSTRAINT fk_exam_result_question_source
  FOREIGN KEY (task_question_id) REFERENCES training_exam_question (id) ON DELETE SET NULL;

ALTER TABLE hazard_rectification_order_item
  ADD CONSTRAINT fk_rectification_item_order
  FOREIGN KEY (order_id) REFERENCES hazard_rectification_order (id) ON DELETE CASCADE;
ALTER TABLE hazard_rectification_flow_log
  ADD CONSTRAINT fk_rectification_log_order
  FOREIGN KEY (order_id) REFERENCES hazard_rectification_order (id) ON DELETE CASCADE;

ALTER TABLE biz_notification_recipient
  ADD CONSTRAINT fk_notification_recipient_notification
  FOREIGN KEY (notification_id) REFERENCES biz_notification (id) ON DELETE CASCADE;
ALTER TABLE biz_notification_recipient
  ADD CONSTRAINT fk_notification_recipient_user
  FOREIGN KEY (recipient_user_id) REFERENCES sys_user (id) ON DELETE CASCADE;

ALTER TABLE sys_team_check_template
  ADD CONSTRAINT fk_check_template_company
  FOREIGN KEY (company_org_id) REFERENCES sys_org (id);
ALTER TABLE sys_team_check_template
  ADD CONSTRAINT fk_check_template_department
  FOREIGN KEY (department_org_id) REFERENCES sys_org (id);
ALTER TABLE sys_team_check_template
  ADD CONSTRAINT fk_check_template_team
  FOREIGN KEY (team_org_id) REFERENCES sys_org (id);
ALTER TABLE sys_team_check_template_item
  ADD CONSTRAINT fk_check_template_item_template
  FOREIGN KEY (template_id) REFERENCES sys_team_check_template (id) ON DELETE CASCADE;
ALTER TABLE sys_team_check_template_item
  ADD CONSTRAINT fk_check_template_item_library
  FOREIGN KEY (library_item_id) REFERENCES sys_check_item_library (id) ON DELETE SET NULL;
