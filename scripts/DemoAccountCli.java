import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Local-only helper used by init-demo.ps1 and reset-demo.ps1. */
public final class DemoAccountCli {
  private DemoAccountCli() {}

  public static void main(String[] args) throws Exception {
    if (args.length != 4) {
      throw new IllegalArgumentException("Expected: <init|reset> <username> <dbUrl> <dbUser>");
    }
    String action = args[0];
    String username = args[1];
    String dbUrl = args[2];
    String dbUser = args[3];
    String dbPassword = requiredEnv("SAFE_DEMO_DB_PASSWORD");
    String rawPassword = requiredEnv("SAFE_DEMO_PASSWORD");
    String passwordHash = "{bcrypt}" + new BCryptPasswordEncoder().encode(rawPassword);

    try (Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword)) {
      connection.setAutoCommit(false);
      Long orgId = firstId(connection, "select id from sys_org where deleted = 0 order by id limit 1");
      Long roleId = firstId(connection, "select id from sys_role where role_code = 'ADMIN' and deleted = 0 limit 1");
      if (orgId == null || roleId == null) {
        throw new IllegalStateException("Demo schema is not initialized with an organization and ADMIN role");
      }
      Long userId = firstId(connection, "select id from sys_user where username = ? and deleted = 0", username);
      if (userId != null && "init".equals(action)) {
        connection.rollback();
        System.out.println("EXISTS");
        return;
      }
      if (userId == null && "reset".equals(action)) {
        connection.rollback();
        throw new IllegalStateException("Demo account does not exist; run init-demo first");
      }
      if (userId == null) {
        try (PreparedStatement statement = connection.prepareStatement(
            "insert into sys_user (username, password_hash, real_name, mobile, org_id, status, deleted) values (?, ?, ?, null, ?, 'ACTIVE', 0)",
            PreparedStatement.RETURN_GENERATED_KEYS)) {
          statement.setString(1, username);
          statement.setString(2, passwordHash);
          statement.setString(3, "Portfolio Admin");
          statement.setLong(4, orgId);
          statement.executeUpdate();
          try (ResultSet keys = statement.getGeneratedKeys()) {
            if (!keys.next()) throw new IllegalStateException("Cannot read created account id");
            userId = keys.getLong(1);
          }
        }
      } else {
        try (PreparedStatement statement = connection.prepareStatement(
            "update sys_user set password_hash = ?, status = 'ACTIVE', deleted = 0, updated_at = CURRENT_TIMESTAMP where id = ?")) {
          statement.setString(1, passwordHash);
          statement.setLong(2, userId);
          statement.executeUpdate();
        }
      }
      try (PreparedStatement statement = connection.prepareStatement(
          "delete from sys_login_attempt where username = ?")) {
        statement.setString(1, username);
        statement.executeUpdate();
      }
      try (PreparedStatement statement = connection.prepareStatement(
          "insert into sys_user_role (user_id, role_id) select ?, ? where not exists (select 1 from sys_user_role where user_id = ? and role_id = ?)")) {
        statement.setLong(1, userId);
        statement.setLong(2, roleId);
        statement.setLong(3, userId);
        statement.setLong(4, roleId);
        statement.executeUpdate();
      }
      connection.commit();
      System.out.println(userId == null ? "CREATED" : "UPDATED");
    }
  }

  private static Long firstId(Connection connection, String sql, Object... args) throws Exception {
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      for (int index = 0; index < args.length; index++) statement.setObject(index + 1, args[index]);
      try (ResultSet result = statement.executeQuery()) {
        return result.next() ? result.getLong(1) : null;
      }
    }
  }

  private static String requiredEnv(String name) {
    String value = System.getenv(name);
    if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required");
    return value;
  }
}
