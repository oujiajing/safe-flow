package com.pingan.banzu.service;

import com.pingan.banzu.common.ConflictException;
import com.pingan.banzu.domain.ThreeCheckRecord;
import com.pingan.banzu.mapper.ThreeCheckRecordMapper;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
class ThreeCheckRecordConcurrentUpdater {

  private static final String CONFLICT_MESSAGE = "记录已被其他端更新，请刷新后重试";
  private static final int MAX_RETRIES = 5;

  private final ThreeCheckRecordMapper recordMapper;

  ThreeCheckRecordConcurrentUpdater(ThreeCheckRecordMapper recordMapper) {
    this.recordMapper = recordMapper;
  }

  void update(ThreeCheckRecord record) {
    int expectedVersion = currentVersion(record);
    record.version = expectedVersion + 1;
    record.updatedAt = LocalDateTime.now();
    if (!tryUpdate(record, expectedVersion)) {
      throw new ConflictException(CONFLICT_MESSAGE);
    }
  }

  void softDelete(ThreeCheckRecord record, Long updatedBy) {
    int expectedVersion = currentVersion(record);
    LocalDateTime updatedAt = LocalDateTime.now();
    if (recordMapper.softDeleteByIdAndVersion(
            record.id, expectedVersion, updatedBy, updatedAt)
        != 1) {
      throw new ConflictException(CONFLICT_MESSAGE);
    }
    record.deleted = 1;
    record.version = expectedVersion + 1;
    record.updatedBy = updatedBy;
    record.updatedAt = updatedAt;
  }

  ReminderIncrement incrementReminder(Long id, Long updatedBy) {
    for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
      ThreeCheckRecord record = recordMapper.selectById(id);
      if (record == null || Integer.valueOf(1).equals(record.deleted)) {
        throw new ConflictException(CONFLICT_MESSAGE);
      }
      int expectedVersion = currentVersion(record);
      int beforeCount = record.reminderCount == null ? 0 : record.reminderCount;
      record.reminderCount = beforeCount + 1;
      record.lastRemindedAt = LocalDateTime.now();
      record.updatedBy = updatedBy;
      record.version = expectedVersion + 1;
      record.updatedAt = LocalDateTime.now();
      if (tryUpdate(record, expectedVersion)) {
        return new ReminderIncrement(record, beforeCount);
      }
    }
    throw new ConflictException(CONFLICT_MESSAGE);
  }

  private boolean tryUpdate(ThreeCheckRecord record, int expectedVersion) {
    return recordMapper.updateByIdAndVersion(record, expectedVersion) == 1;
  }

  private int currentVersion(ThreeCheckRecord record) {
    if (record == null || record.id == null || record.version == null) {
      throw new ConflictException(CONFLICT_MESSAGE);
    }
    return record.version;
  }

  record ReminderIncrement(ThreeCheckRecord record, int beforeCount) {}
}
