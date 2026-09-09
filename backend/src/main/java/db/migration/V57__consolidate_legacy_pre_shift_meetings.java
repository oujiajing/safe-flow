package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V57__consolidate_legacy_pre_shift_meetings extends BaseJavaMigration {

  private static final String BIZ_TYPE = "PRE_SHIFT_MEETING";

  @Override
  public void migrate(Context context) throws Exception {
    Connection connection = context.getConnection();
    rejectAmbiguousIds(connection);
    List<LegacyMeeting> meetings = readMeetings(connection);
    for (LegacyMeeting meeting : meetings) {
      long recordId = insertGenericRecord(connection, meeting);
      retarget(connection, "biz_attachment", meeting.id(), recordId);
      retarget(connection, "biz_status_log", meeting.id(), recordId);
      retarget(connection, "biz_remind_record", meeting.id(), recordId);
      retarget(connection, "biz_notification", meeting.id(), recordId);
    }
    try (Statement statement = connection.createStatement()) {
      statement.execute("DROP TABLE pre_shift_meeting_attendee");
      statement.execute("DROP TABLE pre_shift_meeting");
    }
  }

  private void rejectAmbiguousIds(Connection connection) throws Exception {
    try (Statement statement = connection.createStatement();
        ResultSet rows =
            statement.executeQuery(
                "SELECT COUNT(*) FROM pre_shift_meeting p JOIN three_check_record r"
                    + " ON r.id = p.id AND r.module_key = 'pre-shift-meeting'")) {
      rows.next();
      if (rows.getLong(1) > 0) {
        throw new IllegalStateException(
            "旧班前会与通用三查记录存在 ID 冲突，无法安全判定附件和日志归属，请先人工消歧");
      }
    }
  }

  private List<LegacyMeeting> readMeetings(Connection connection) throws Exception {
    List<LegacyMeeting> meetings = new ArrayList<>();
    try (Statement statement = connection.createStatement();
        ResultSet rows =
            statement.executeQuery(
                "SELECT id, task_id, meeting_no, company_id, department_id, team_id,"
                    + " owner_user_id, meeting_date, meeting_content, image_check_status,"
                    + " video_check_status, status, submitted_by, submitted_at, withdrawn_by,"
                    + " withdrawn_at, withdraw_reason, reminder_count, last_reminded_at, version,"
                    + " created_by, updated_by, source_channel, source_record_id, client_request_id,"
                    + " client_updated_at, last_synced_at, created_at, updated_at, deleted"
                    + " FROM pre_shift_meeting ORDER BY id")) {
      while (rows.next()) {
        long id = rows.getLong("id");
        meetings.add(
            new LegacyMeeting(
                id,
                readAttendees(connection, id),
                rows.getObject("task_id"),
                rows.getString("meeting_no"),
                rows.getObject("company_id"),
                rows.getObject("department_id"),
                rows.getObject("team_id"),
                rows.getObject("owner_user_id"),
                rows.getObject("meeting_date"),
                rows.getString("meeting_content"),
                rows.getString("image_check_status"),
                rows.getString("video_check_status"),
                rows.getString("status"),
                rows.getObject("submitted_by"),
                rows.getObject("submitted_at"),
                rows.getObject("withdrawn_by"),
                rows.getObject("withdrawn_at"),
                rows.getString("withdraw_reason"),
                rows.getObject("reminder_count"),
                rows.getObject("last_reminded_at"),
                rows.getObject("version"),
                rows.getObject("created_by"),
                rows.getObject("updated_by"),
                rows.getString("source_channel"),
                rows.getString("source_record_id"),
                rows.getString("client_request_id"),
                rows.getObject("client_updated_at"),
                rows.getObject("last_synced_at"),
                rows.getObject("created_at"),
                rows.getObject("updated_at"),
                rows.getObject("deleted")));
      }
    }
    return meetings;
  }

  private List<String> readAttendees(Connection connection, long meetingId) throws Exception {
    List<String> attendees = new ArrayList<>();
    try (PreparedStatement statement =
        connection.prepareStatement(
            "SELECT attendee_name FROM pre_shift_meeting_attendee"
                + " WHERE meeting_id = ? ORDER BY id")) {
      statement.setLong(1, meetingId);
      try (ResultSet rows = statement.executeQuery()) {
        while (rows.next()) attendees.add(rows.getString(1));
      }
    }
    return attendees;
  }

  private long insertGenericRecord(Connection connection, LegacyMeeting meeting) throws Exception {
    String sql =
        "INSERT INTO three_check_record"
            + " (module_key, record_no, task_id, company_id, department_id, team_id, owner_user_id,"
            + " business_date, status, payload_json, image_check_status, video_check_status,"
            + " submitted_by, submitted_at, withdrawn_by, withdrawn_at, withdraw_reason,"
            + " reminder_count, last_reminded_at, version, created_by, updated_by, source_channel,"
            + " source_record_id, client_request_id, client_updated_at, last_synced_at, created_at,"
            + " updated_at, deleted) VALUES"
            + " (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    try (PreparedStatement statement =
        connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      Object[] values = {
        "pre-shift-meeting",
        uniqueRecordNo(meeting),
        meeting.taskId(),
        meeting.companyId(),
        meeting.departmentId(),
        meeting.teamId(),
        meeting.ownerUserId(),
        meeting.meetingDate(),
        meeting.status(),
        payload(meeting),
        meeting.imageCheckStatus(),
        meeting.videoCheckStatus(),
        meeting.submittedBy(),
        meeting.submittedAt(),
        meeting.withdrawnBy(),
        meeting.withdrawnAt(),
        meeting.withdrawReason(),
        meeting.reminderCount(),
        meeting.lastRemindedAt(),
        meeting.version(),
        meeting.createdBy(),
        meeting.updatedBy(),
        "LEGACY_MIGRATION",
        "pre-shift-meeting:" + meeting.id(),
        null,
        meeting.clientUpdatedAt(),
        meeting.lastSyncedAt(),
        meeting.createdAt(),
        meeting.updatedAt(),
        meeting.deleted()
      };
      for (int index = 0; index < values.length; index++) {
        if (values[index] == null) statement.setNull(index + 1, Types.NULL);
        else statement.setObject(index + 1, values[index]);
      }
      statement.executeUpdate();
      try (ResultSet keys = statement.getGeneratedKeys()) {
        if (!keys.next()) throw new IllegalStateException("迁移旧班前会时未取得新记录 ID");
        return keys.getLong(1);
      }
    }
  }

  private String uniqueRecordNo(LegacyMeeting meeting) {
    String prefix = "LEGACY-PSM-" + meeting.id() + "-";
    String suffix = safe(meeting.meetingNo());
    return prefix + suffix.substring(0, Math.min(suffix.length(), 80 - prefix.length()));
  }

  private String payload(LegacyMeeting meeting) {
    String attendees = meeting.attendees().stream().map(this::jsonString).reduce((a, b) -> a + "," + b).orElse("");
    return "{\"meetingContent\":"
        + jsonString(meeting.meetingContent())
        + ",\"attendees\":["
        + attendees
        + "],\"attendeesText\":"
        + jsonString(String.join("、", meeting.attendees()))
        + ",\"legacyMeetingId\":"
        + meeting.id()
        + ",\"legacySourceChannel\":"
        + jsonString(meeting.sourceChannel())
        + ",\"legacySourceRecordId\":"
        + jsonString(meeting.sourceRecordId())
        + ",\"legacyClientRequestId\":"
        + jsonString(meeting.clientRequestId())
        + "}";
  }

  private String jsonString(String value) {
    if (value == null) return "\"\"";
    return "\""
        + value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\r", "\\r")
            .replace("\n", "\\n")
            .replace("\t", "\\t")
        + "\"";
  }

  private String safe(String value) {
    return value == null ? "UNKNOWN" : value.replaceAll("[^A-Za-z0-9_-]", "_");
  }

  private void retarget(Connection connection, String table, long oldId, long newId)
      throws Exception {
    try (PreparedStatement statement =
        connection.prepareStatement(
            "UPDATE " + table + " SET biz_id = ? WHERE biz_type = ? AND biz_id = ?")) {
      statement.setLong(1, newId);
      statement.setString(2, BIZ_TYPE);
      statement.setLong(3, oldId);
      statement.executeUpdate();
    }
  }

  private record LegacyMeeting(
      long id,
      List<String> attendees,
      Object taskId,
      String meetingNo,
      Object companyId,
      Object departmentId,
      Object teamId,
      Object ownerUserId,
      Object meetingDate,
      String meetingContent,
      String imageCheckStatus,
      String videoCheckStatus,
      String status,
      Object submittedBy,
      Object submittedAt,
      Object withdrawnBy,
      Object withdrawnAt,
      String withdrawReason,
      Object reminderCount,
      Object lastRemindedAt,
      Object version,
      Object createdBy,
      Object updatedBy,
      String sourceChannel,
      String sourceRecordId,
      String clientRequestId,
      Object clientUpdatedAt,
      Object lastSyncedAt,
      Object createdAt,
      Object updatedAt,
      Object deleted) {}
}
