-- First-run cleanup for the isolated portfolio_demo database only.
-- Preconditions: the database was created from docker-compose.portfolio-demo.yml,
-- Flyway has reached V71, and its system menu/role baseline must be retained.
-- This intentionally removes migration-seeded business samples before any Portfolio
-- fixture is created. It never targets demo_safeteam or another database.

BEGIN;

DELETE FROM biz_notification_recipient;
DELETE FROM biz_notification_outbox;
DELETE FROM biz_notification;
DELETE FROM biz_announcement;
DELETE FROM biz_attachment;
DELETE FROM biz_change_history;
DELETE FROM biz_remind_record;
DELETE FROM biz_status_log;

DELETE FROM hazard_rectification_flow_log;
DELETE FROM hazard_rectification_order_item;
DELETE FROM hazard_rectification_order;

DELETE FROM training_exam_result_question;
DELETE FROM training_exam_result;
DELETE FROM training_exam_question;
DELETE FROM training_exam_task;
DELETE FROM training_exam_paper;
DELETE FROM training_exam_question_bank;
DELETE FROM training_safety_learning_content;

DELETE FROM three_check_record;
DELETE FROM shift_task;
DELETE FROM special_work_record;

DELETE FROM risk_four_color_map;
DELETE FROM risk_control_hazard;
DELETE FROM risk_control_library;
DELETE FROM safety_ledger_document;

DELETE FROM sys_team_check_template_item;
DELETE FROM sys_team_check_template;
DELETE FROM sys_check_item_library;
DELETE FROM sys_org_content_profile;
DELETE FROM sys_team_member;
DELETE FROM sys_team_profile;
DELETE FROM sys_department_profile;
DELETE FROM sys_company_profile;
DELETE FROM sys_user_profile;
DELETE FROM sys_user_role;
DELETE FROM sys_verification_code;
DELETE FROM sys_login_attempt;
DELETE FROM sys_access_log;
DELETE FROM sys_import_job;
DELETE FROM sys_user;
DELETE FROM sys_org;

COMMIT;
