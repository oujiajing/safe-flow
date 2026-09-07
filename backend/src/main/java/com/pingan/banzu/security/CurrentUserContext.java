package com.pingan.banzu.security;

public final class CurrentUserContext {

  private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

  private CurrentUserContext() {}

  public static CurrentUser require() {
    CurrentUser user = HOLDER.get();
    if (user == null) {
      throw new SecurityException("未登录或登录已过期");
    }
    return user;
  }

  public static void set(CurrentUser user) {
    HOLDER.set(user);
  }

  public static void clear() {
    HOLDER.remove();
  }
}
