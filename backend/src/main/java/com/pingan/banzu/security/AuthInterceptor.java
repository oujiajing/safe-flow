package com.pingan.banzu.security;

import com.pingan.banzu.system.security.SystemPermissionService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

  private static final String ORGANIZATION_VIEW_PERMISSION = "PINGAN_ORGANIZATION_VIEW";
  private static final String ORGANIZATION_ENTRY_PERMISSION = "PINGAN_ORGANIZATION_ENTRY";

  private final JwtService jwtService;
  private final AuthSessionService sessionService;
  private final SystemPermissionService permissionService;

  public AuthInterceptor(
      JwtService jwtService,
      AuthSessionService sessionService,
      SystemPermissionService permissionService) {
    this.jwtService = jwtService;
    this.sessionService = sessionService;
    this.permissionService = permissionService;
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
      return true;
    }
    String authorization = request.getHeader("Authorization");
    if (authorization == null || !authorization.startsWith("Bearer ")) {
      throw new SecurityException("未登录或登录已过期");
    }
    try {
      CurrentUser currentUser =
          sessionService.validate(jwtService.parse(authorization.substring("Bearer ".length())));
      CurrentUserContext.set(currentUser);
      if (request.getRequestURI().equals("/actuator/prometheus") && !currentUser.isAdmin()) {
        CurrentUserContext.clear();
        throw new SecurityException("无权访问运行指标");
      }
      if (request.getRequestURI().startsWith("/api/system/")
          && !currentUser.isAdmin()
          && !canAccessDelegatedSystemEndpoint(request)) {
        CurrentUserContext.clear();
        throw new SecurityException("无权访问系统管理");
      }
    } catch (IllegalArgumentException | JwtException exception) {
      throw new SecurityException("未登录或登录已过期");
    }
    return true;
  }

  private boolean canAccessDelegatedSystemEndpoint(HttpServletRequest request) {
    if (!"GET".equalsIgnoreCase(request.getMethod())) {
      return false;
    }
    String uri = request.getRequestURI();
    boolean organizationEndpoint =
        uri.equals("/api/system/companies")
            || uri.startsWith("/api/system/companies/")
            || uri.equals("/api/system/departments")
            || uri.startsWith("/api/system/departments/")
            || uri.equals("/api/system/teams")
            || uri.startsWith("/api/system/teams/")
            || uri.equals("/api/system/personnel")
            || uri.startsWith("/api/system/personnel/");
    if (organizationEndpoint) {
      return permissionService.hasAnyPermission(ORGANIZATION_ENTRY_PERMISSION, ORGANIZATION_VIEW_PERMISSION);
    }
    if (uri.equals("/api/system/announcements") || uri.startsWith("/api/system/announcements/")
        || uri.equals("/api/system/notification-deliveries")) {
      boolean publishAction = uri.endsWith("/publish") || uri.endsWith("/withdraw");
      return publishAction
          ? permissionService.hasPermission("PINGAN_NOTIFICATION_ANNOUNCEMENT_PUBLISH")
          : permissionService.hasPermission("PINGAN_NOTIFICATION_ANNOUNCEMENT_MANAGE");
    }
    return false;
  }

  @Override
  public void afterCompletion(
      HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
    CurrentUserContext.clear();
  }
}
