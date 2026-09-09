package com.pingan.banzu.system.audit;

import com.pingan.banzu.security.CurrentUser;
import com.pingan.banzu.security.CurrentUserContext;
import com.pingan.banzu.system.common.SystemModule;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class AuditLogService {

  private final JdbcTemplate jdbcTemplate;

  public AuditLogService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public void record(SystemModule module, String targetType, Long targetId, String action, String summary) {
    CurrentUser user = currentUserOrNull();
    ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    String path = attrs == null ? null : attrs.getRequest().getRequestURI();
    String method = attrs == null ? null : attrs.getRequest().getMethod();
    String ip = attrs == null ? null : attrs.getRequest().getRemoteAddr();
    jdbcTemplate.update(
        """
        insert into sys_access_log
          (user_id, username, module, target_type, target_id, action, summary, request_path, request_method, ip_address)
        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        user == null ? null : user.userId(),
        user == null ? null : user.username(),
        module.name(),
        targetType,
        targetId,
        action,
        summary,
        path,
        method,
        ip);
  }

  private CurrentUser currentUserOrNull() {
    try {
      return CurrentUserContext.require();
    } catch (SecurityException exception) {
      return null;
    }
  }
}
