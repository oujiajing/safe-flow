package com.pingan.banzu.security;

import java.util.List;

public record CurrentUser(Long userId, String username, String realName, Long orgId, String orgPath, List<String> roles) {

  public boolean isAdmin() {
    return roles != null && roles.contains("ADMIN");
  }
}
