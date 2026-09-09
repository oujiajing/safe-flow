package com.pingan.banzu.service;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class NotificationTemplateRegistry {

  private static final Map<String, Template> TEMPLATES =
      Map.ofEntries(
          entry("SPECIAL_WORK_APPROVAL_REQUESTED", "ACTION", "special-work", "特殊作业待审批", "APPROVE", "IMPORTANT"),
          entry("SPECIAL_WORK_ACCEPTANCE_REQUESTED", "ACTION", "special-work", "特殊作业待验收", "REVIEW", "IMPORTANT"),
          entry("SPECIAL_WORK_REJECTED", "BUSINESS", "special-work", "特殊作业已驳回", "VIEW", "IMPORTANT"),
          entry("SPECIAL_WORK_COMPLETED", "BUSINESS", "special-work", "特殊作业已完成", "VIEW", "NORMAL"),
          entry("EXAM_ASSIGNED", "ACTION", "safety-exam", "安全考试任务已下发", "TAKE_EXAM", "IMPORTANT"),
          entry("EXAM_DUE_SOON", "ACTION", "safety-exam", "安全考试即将截止", "TAKE_EXAM", "IMPORTANT"),
          entry("EXAM_OVERDUE", "ACTION", "safety-exam", "安全考试已逾期", "TAKE_EXAM", "URGENT"),
          entry("EXAM_SUBMITTED", "BUSINESS", "safety-exam", "安全考试已提交", "VIEW", "NORMAL"),
          entry("EXAM_RESULT_PUBLISHED", "BUSINESS", "safety-exam", "安全考试成绩已发布", "VIEW", "NORMAL"),
          entry("TEAM_DISPATCH_ASSIGNED", "BUSINESS", "team-dispatch", "班组派班已下发", "VIEW", "IMPORTANT"),
          entry("TEAM_DISPATCH_CHANGED", "BUSINESS", "team-dispatch", "班组派班已变更", "VIEW", "IMPORTANT"),
          entry("TEAM_DISPATCH_CANCELLED", "BUSINESS", "team-dispatch", "班组派班已取消", "VIEW", "NORMAL"),
          entry("HAZARD_SOURCE_REVIEW_REQUESTED", "ACTION", "hazard-source", "隐患来源待审核", "REVIEW", "IMPORTANT"),
          entry("HAZARD_RECTIFICATION_OVERDUE", "ACTION", "hazard-rectification", "隐患整改已逾期", "RECTIFY", "URGENT"),
          entry("LEARNING_ASSIGNED", "ACTION", "safety-learning", "安全学习内容已下发", "VIEW", "IMPORTANT"),
          entry("LEARNING_COMPLETED", "BUSINESS", "safety-learning", "安全学习已完成", "VIEW", "NORMAL"),
          entry("RISK_CREATED", "BUSINESS", "risk-control", "新增风险管控事项", "VIEW", "IMPORTANT"),
          entry("RISK_LEVEL_CHANGED", "BUSINESS", "risk-control", "风险等级已变更", "VIEW", "IMPORTANT"),
          entry("RISK_LIBRARY_REVIEW_REQUESTED", "ACTION", "risk-control", "风险库待维护复核", "REVIEW", "IMPORTANT"),
          entry("KEY_SITE_INSPECTION_ASSIGNED", "ACTION", "key-site", "重点场所检查已分派", "VIEW", "IMPORTANT"),
          entry("POINTS_CHANGED", "BUSINESS", "safety-points", "安全积分发生变动", "VIEW", "NORMAL"),
          entry("LEDGER_PUBLISHED", "BUSINESS", "safety-ledger", "安全台账已发布", "VIEW", "NORMAL"),
          entry("ACCOUNT_STATUS_CHANGED", "SYSTEM", "system-account", "账号状态已变更", "VIEW", "IMPORTANT"),
          entry("ROLE_PERMISSION_CHANGED", "SYSTEM", "system-account", "账号角色权限已变更", "VIEW", "IMPORTANT"),
          entry("SESSION_REVOKED", "SYSTEM", "system-account", "账号会话已失效", "VIEW", "URGENT"),
          entry("IMPORT_FINISHED", "SYSTEM", "system-import", "数据导入已完成", "VIEW", "NORMAL"));

  public Template require(String eventType) {
    Template template = TEMPLATES.get(eventType);
    if (template == null) {
      throw new IllegalArgumentException("未配置消息模板: " + eventType);
    }
    return template;
  }

  private static Map.Entry<String, Template> entry(
      String eventType, String groupType, String moduleKey, String title, String actionKey, String severity) {
    return Map.entry(eventType, new Template(groupType, moduleKey, title, actionKey, severity));
  }

  public record Template(
      String groupType, String moduleKey, String title, String actionKey, String severity) {}
}
