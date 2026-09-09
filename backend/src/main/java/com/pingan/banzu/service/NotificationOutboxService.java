package com.pingan.banzu.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pingan.banzu.service.NotificationService.NotificationCommand;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationOutboxService {

  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;
  private final NotificationRecipientResolver recipientResolver;
  private final NotificationTemplateRegistry templateRegistry;
  private final NotificationService notificationService;

  public NotificationOutboxService(
      JdbcTemplate jdbcTemplate,
      ObjectMapper objectMapper,
      NotificationRecipientResolver recipientResolver,
      NotificationTemplateRegistry templateRegistry,
      NotificationService notificationService) {
    this.jdbcTemplate = jdbcTemplate;
    this.objectMapper = objectMapper;
    this.recipientResolver = recipientResolver;
    this.templateRegistry = templateRegistry;
    this.notificationService = notificationService;
  }

  @Transactional
  public void enqueue(DomainNotificationEvent event) {
    if (event == null || event.sourceEventId() == null || event.sourceEventId().isBlank()) {
      throw new IllegalArgumentException("消息事件幂等 ID 不能为空");
    }
    try {
      jdbcTemplate.update(
          "INSERT INTO biz_notification_outbox"
              + " (source_event_id, event_type, payload_json, status, retry_count, created_at, updated_at)"
              + " VALUES (?, ?, ?, 'PENDING', 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
          event.sourceEventId(),
          event.eventType(),
          objectMapper.writeValueAsString(event));
    } catch (DuplicateKeyException ignored) {
      // A retried business transaction must not create another event.
    } catch (JsonProcessingException ex) {
      throw new IllegalArgumentException("消息事件序列化失败", ex);
    }
  }

  @Scheduled(
      initialDelayString = "${pingan.notification.outbox-initial-delay-ms:5000}",
      fixedDelayString = "${pingan.notification.outbox-poll-ms:5000}")
  public void consumePending() {
    List<Long> ids = jdbcTemplate.queryForList(
        "SELECT id FROM biz_notification_outbox"
            + " WHERE status IN ('PENDING', 'FAILED')"
            + " AND (next_retry_at IS NULL OR next_retry_at <= CURRENT_TIMESTAMP)"
            + " ORDER BY id LIMIT 50",
        Long.class);
    ids.forEach(this::consumeOneSafely);
  }

  @Transactional
  public void consumeOne(Long id) throws JsonProcessingException {
    Map<String, Object> row = jdbcTemplate.queryForMap(
        "SELECT source_event_id, payload_json FROM biz_notification_outbox WHERE id = ?",
        id);
    int claimed = jdbcTemplate.update(
        "UPDATE biz_notification_outbox SET status = 'PROCESSING', claimed_at = CURRENT_TIMESTAMP,"
            + " updated_at = CURRENT_TIMESTAMP WHERE id = ? AND status IN ('PENDING', 'FAILED')",
        id);
    if (claimed == 0) return;
    DomainNotificationEvent event =
        objectMapper.readValue(String.valueOf(row.get("payload_json")), DomainNotificationEvent.class);
    NotificationTemplateRegistry.Template template = templateRegistry.require(event.eventType());
    Map<Long, String> recipients = recipientResolver.resolve(event);
    for (Map.Entry<Long, String> recipient : recipients.entrySet()) {
      notificationService.publishToUser(
          new NotificationCommand(
              event.eventType(),
              template.groupType(),
              template.moduleKey(),
              template.title(),
              summary(event),
              event.bizType(),
              event.bizId(),
              template.severity(),
              template.actionKey(),
              event.deadline(),
              event.sourceEventId(),
              event.operatorUserId(),
              event.sourceEventId(),
              event.organizationId(),
              "USER",
              writeSnapshot(event.snapshot()),
              event.deadline()),
          recipient.getKey(),
          recipient.getValue());
    }
    jdbcTemplate.update(
        "UPDATE biz_notification_outbox SET status = 'PROCESSED', processed_at = CURRENT_TIMESTAMP,"
            + " last_error = NULL, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
        id);
  }

  private void consumeOneSafely(Long id) {
    try {
      consumeOne(id);
    } catch (Exception ex) {
      jdbcTemplate.update(
          "UPDATE biz_notification_outbox SET status = 'FAILED', retry_count = retry_count + 1,"
              + " next_retry_at = ?, last_error = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
          LocalDateTime.now().plusMinutes(5),
          truncate(ex.getMessage()),
          id);
    }
  }

  private String summary(DomainNotificationEvent event) {
    if (event.snapshot() == null) return null;
    Object value = event.snapshot().get("summary");
    return value == null ? null : String.valueOf(value);
  }

  private String writeSnapshot(Map<String, Object> snapshot) throws JsonProcessingException {
    return snapshot == null || snapshot.isEmpty() ? null : objectMapper.writeValueAsString(snapshot);
  }

  private String truncate(String value) {
    if (value == null) return "消息生成失败";
    return value.length() <= 1000 ? value : value.substring(0, 1000);
  }
}

