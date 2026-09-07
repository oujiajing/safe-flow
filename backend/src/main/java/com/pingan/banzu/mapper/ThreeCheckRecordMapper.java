package com.pingan.banzu.mapper;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pingan.banzu.domain.ThreeCheckRecord;
import com.pingan.banzu.dto.ThreeCheckRecordSqlCriteria;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;

public interface ThreeCheckRecordMapper extends BaseMapper<ThreeCheckRecord> {

  default int updateByIdAndVersion(ThreeCheckRecord record, int expectedVersion) {
    return update(
        record,
        new UpdateWrapper<ThreeCheckRecord>()
            .eq("id", record.id)
            .eq("version", expectedVersion)
            .eq("deleted", 0));
  }

  default int softDeleteByIdAndVersion(
      Long id, int expectedVersion, Long updatedBy, LocalDateTime updatedAt) {
    return update(
        null,
        new UpdateWrapper<ThreeCheckRecord>()
            .eq("id", id)
            .eq("version", expectedVersion)
            .eq("deleted", 0)
            .set("deleted", 1)
            .set("version", expectedVersion + 1)
            .set("updated_by", updatedBy)
            .set("updated_at", updatedAt));
  }

  @SelectProvider(type = ThreeCheckRecordSqlProvider.class, method = "selectPage")
  List<ThreeCheckRecord> selectEnterprisePage(
      @Param("criteria") ThreeCheckRecordSqlCriteria criteria,
      @Param("limit") int limit,
      @Param("offset") int offset);

  @SelectProvider(type = ThreeCheckRecordSqlProvider.class, method = "countRecords")
  long countEnterpriseRecords(@Param("criteria") ThreeCheckRecordSqlCriteria criteria);

  @SelectProvider(type = ThreeCheckRecordSqlProvider.class, method = "countByStatus")
  List<Map<String, Object>> countEnterpriseRecordsByStatus(
      @Param("criteria") ThreeCheckRecordSqlCriteria criteria);

  @SelectProvider(type = ThreeCheckRecordSqlProvider.class, method = "countAttachments")
  Map<String, Object> countEnterpriseRecordAttachments(
      @Param("criteria") ThreeCheckRecordSqlCriteria criteria);

  @SelectProvider(type = ThreeCheckRecordSqlProvider.class, method = "countByDate")
  List<Map<String, Object>> countEnterpriseRecordsByDate(
      @Param("criteria") ThreeCheckRecordSqlCriteria criteria);

  @SelectProvider(type = ThreeCheckRecordSqlProvider.class, method = "countByCompany")
  List<Map<String, Object>> countEnterpriseRecordsByCompany(
      @Param("criteria") ThreeCheckRecordSqlCriteria criteria);

  @SelectProvider(type = ThreeCheckRecordSqlProvider.class, method = "countByDepartment")
  List<Map<String, Object>> countEnterpriseRecordsByDepartment(
      @Param("criteria") ThreeCheckRecordSqlCriteria criteria);

  @SelectProvider(type = ThreeCheckRecordSqlProvider.class, method = "countByTeam")
  List<Map<String, Object>> countEnterpriseRecordsByTeam(
      @Param("criteria") ThreeCheckRecordSqlCriteria criteria);
}
