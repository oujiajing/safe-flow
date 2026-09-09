package com.pingan.banzu.security;

import com.pingan.banzu.security.JwtService.ParsedJwt;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AuthSessionService {

  private final JdbcTemplate jdbcTemplate;

  public AuthSessionService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public CurrentUser validate(ParsedJwt jwt) {
    List<UserState> states =
        jdbcTemplate.query(
            "select status, deleted, auth_version from sys_user where id = ?",
            (rs, rowNum) ->
                new UserState(
                    rs.getString("status"), rs.getInt("deleted"), rs.getLong("auth_version")),
            jwt.user().userId());
    if (states.isEmpty()
        || states.get(0).deleted() != 0
        || !"ACTIVE".equals(states.get(0).status())
        || states.get(0).authVersion() != jwt.authVersion()) {
      throw new SecurityException("未登录或登录已过期");
    }
    List<String> currentRoles =
        new ArrayList<>(
            jdbcTemplate.queryForList(
                """
                select distinct r.role_code
                from sys_user_role ur
                join sys_role r on r.id = ur.role_id
                where ur.user_id = ?
                  and r.deleted = 0
                order by r.role_code
                """,
                String.class,
                jwt.user().userId()));
    List<String> tokenRoles = new ArrayList<>(jwt.user().roles());
    tokenRoles.sort(String::compareTo);
    if (!currentRoles.equals(tokenRoles)) {
      throw new SecurityException("未登录或登录已过期");
    }
    return jwt.user();
  }

  private record UserState(String status, int deleted, long authVersion) {}
}
