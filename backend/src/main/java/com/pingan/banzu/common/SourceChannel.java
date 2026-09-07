package com.pingan.banzu.common;

import java.util.Set;

public final class SourceChannel {
  public static final String PC = "PC";
  public static final String WECHAT_MINI_PROGRAM = "WECHAT_MINI_PROGRAM";
  public static final String API_IMPORT = "API_IMPORT";

  private static final Set<String> SUPPORTED = Set.of(PC, WECHAT_MINI_PROGRAM, API_IMPORT);

  private SourceChannel() {}

  public static boolean supports(String sourceChannel) {
    return SUPPORTED.contains(sourceChannel);
  }
}
