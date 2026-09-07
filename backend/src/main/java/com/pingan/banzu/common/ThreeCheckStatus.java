package com.pingan.banzu.common;

import java.util.Set;

public final class ThreeCheckStatus {
  public static final String DRAFT = "DRAFT";
  public static final String OPENED = "OPENED";
  public static final String WITHDRAWN = "WITHDRAWN";
  public static final String ARCHIVED = "ARCHIVED";
  public static final String PENDING_REVIEW = "PENDING_REVIEW";
  public static final String REVIEWED = "REVIEWED";
  public static final String PENDING_RECTIFICATION = "PENDING_RECTIFICATION";
  public static final String RECTIFIED = "RECTIFIED";
  public static final String PENDING_ACCEPTANCE = "PENDING_ACCEPTANCE";
  public static final String ACCEPTED = "ACCEPTED";
  public static final String REJECTED = "REJECTED";

  public static final Set<String> EDITABLE = Set.of(DRAFT, WITHDRAWN, PENDING_RECTIFICATION, RECTIFIED);
  public static final Set<String> WITHDRAWABLE = Set.of(OPENED, ARCHIVED);

  private ThreeCheckStatus() {}
}
