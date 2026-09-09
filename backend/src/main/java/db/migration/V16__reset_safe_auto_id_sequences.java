package db.migration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Locale;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V16__reset_safe_auto_id_sequences extends BaseJavaMigration {
  private static final long SAFE_AUTO_ID_START = 100_000_000_000L;
  private static final String[] AUTO_ID_TABLES = {
    "sys_org",
    "sys_user",
    "sys_role",
    "sys_user_role",
    "shift_task",
    "pre_shift_meeting",
    "pre_shift_meeting_attendee",
    "biz_attachment",
    "biz_status_log",
    "biz_remind_record",
    "sys_company_profile",
    "sys_department_profile",
    "sys_team_profile",
    "sys_team_member",
    "sys_user_profile",
    "sys_menu",
    "sys_role_menu",
    "sys_access_log",
    "sys_login_attempt",
    "sys_verification_code",
    "sys_import_job",
    "three_check_record"
  };

  @Override
  public void migrate(Context context) throws Exception {
    Connection connection = context.getConnection();
    String productName =
        connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT);
    try (Statement statement = connection.createStatement()) {
      for (String table : AUTO_ID_TABLES) {
        long restartWith = Math.max(SAFE_AUTO_ID_START, maxExistingId(connection, table) + 1);
        if (productName.contains("h2")) {
          statement.execute(
              "ALTER TABLE " + table + " ALTER COLUMN id RESTART WITH " + restartWith);
        } else if (productName.contains("postgresql")) {
          statement.execute(
              "ALTER SEQUENCE "
                  + table
                  + "_id_seq RESTART WITH "
                  + restartWith);
        }
      }
    }
  }

  private long maxExistingId(Connection connection, String table) throws Exception {
    try (Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("SELECT COALESCE(MAX(id), 0) FROM " + table)) {
      return resultSet.next() ? resultSet.getLong(1) : 0L;
    }
  }
}
