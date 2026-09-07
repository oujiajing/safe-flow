package com.pingan.banzu.service;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class NotificationEscalationJob {

  private final JdbcTemplate jdbcTemplate;
  private final NotificationOutboxService outboxService;

  public NotificationEscalationJob(
      JdbcTemplate jdbcTemplate, NotificationOutboxService outboxService) {
    this.jdbcTemplate = jdbcTemplate;
    this.outboxService = outboxService;
  }

  @Scheduled(
      initialDelayString = "${pingan.notification.escalation-initial-delay-ms:30000}",
      fixedDelayString = "${pingan.notification.escalation-poll-ms:3600000}")
  public void publishExamReminders() {
    LocalDate today = LocalDate.now();
    jdbcTemplate
        .query(
            """
            SELECT t.id task_id, t.exam, t.exam_date, t.company_id, t.department_id, t.created_by,
                   r.id result_id, r.exam_person_user_id
            FROM training_exam_task t
            JOIN training_exam_result r ON r.task_id = t.id AND r.deleted = 0
            WHERE t.deleted = 0
              AND t.status = 'ACTIVE'
              AND r.status = 'PENDING_EXAM'
              AND t.exam_date <= ?
            """,
            (rs, rowNum) ->
                new PendingExam(
                    rs.getLong("task_id"),
                    rs.getString("exam"),
                    rs.getDate("exam_date").toLocalDate(),
                    rs.getLong("company_id"),
                    nullableLong(rs.getObject("department_id")),
                    rs.getLong("created_by"),
                    rs.getLong("result_id"),
                    rs.getLong("exam_person_user_id")),
            Date.valueOf(today.plusDays(1)))
        .forEach(exam -> enqueueReminder(exam, today));
    publishOverdueRectifications();
  }

  private void publishOverdueRectifications() {
    jdbcTemplate
        .query(
            """
            SELECT id, order_no, company_id, department_id, team_id, rectification_responsible_user_id,
                   acceptance_user_id, created_by, rectification_deadline
            FROM hazard_rectification_order
            WHERE deleted = 0
              AND status IN ('PENDING_RECTIFY', 'RECTIFIED', 'PENDING_ACCEPTANCE')
              AND rectification_deadline < CURRENT_TIMESTAMP
            """,
            (rs, rowNum) -> {
              List<Long> recipients = new java.util.ArrayList<>();
              Long responsible = nullableLong(rs.getObject("rectification_responsible_user_id"));
              Long acceptance = nullableLong(rs.getObject("acceptance_user_id"));
              if (responsible != null) recipients.add(responsible);
              if (acceptance != null && !recipients.contains(acceptance)) recipients.add(acceptance);
              Long companyId = nullableLong(rs.getObject("company_id"));
              Long departmentId = nullableLong(rs.getObject("department_id"));
              Long teamId = nullableLong(rs.getObject("team_id"));
              return new OverdueRectification(
                  rs.getLong("id"),
                  rs.getString("order_no"),
                  companyId,
                  departmentId,
                  teamId,
                  recipients,
                  nullableLong(rs.getObject("created_by")),
                  rs.getTimestamp("rectification_deadline").toLocalDateTime());
            })
        .forEach(order ->
            outboxService.enqueue(
                new DomainNotificationEvent(
                    "hazard-rectification:overdue:" + order.id() + ":" + LocalDate.now(),
                    "HAZARD_RECTIFICATION_OVERDUE",
                    "HAZARD_RECTIFICATION_ORDER",
                    order.id(),
                    order.teamId() != null
                        ? order.teamId()
                        : (order.departmentId() != null ? order.departmentId() : order.companyId()),
                    order.recipients(),
                    List.of(),
                    order.createdBy(),
                    null,
                    order.deadline(),
                    Map.of("summary", order.orderNo() + " · 已超过整改期限"))));
  }

  private void enqueueReminder(PendingExam exam, LocalDate today) {
    boolean overdue = exam.examDate().isBefore(today);
    String eventType = overdue ? "EXAM_OVERDUE" : "EXAM_DUE_SOON";
    outboxService.enqueue(
        new DomainNotificationEvent(
            "exam:" + eventType + ":" + exam.taskId() + ":" + exam.userId() + ":" + today,
            eventType,
            "TRAINING_EXAM_TASK",
            exam.taskId(),
            exam.departmentId() == null ? exam.companyId() : exam.departmentId(),
            List.of(exam.userId()),
            List.of(),
            exam.createdBy(),
            null,
            exam.examDate().atTime(LocalTime.MAX),
            Map.of("summary", exam.exam() + " · 截止日期 " + exam.examDate())));
  }

  private static Long nullableLong(Object value) {
    return value instanceof Number number ? number.longValue() : null;
  }

  private record PendingExam(
      Long taskId,
      String exam,
      LocalDate examDate,
      Long companyId,
      Long departmentId,
      Long createdBy,
      Long resultId,
      Long userId) {}

  private record OverdueRectification(
      Long id,
      String orderNo,
      Long companyId,
      Long departmentId,
      Long teamId,
      List<Long> recipients,
      Long createdBy,
      java.time.LocalDateTime deadline) {}
}
