package com.pingan.banzu.service;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class CoreRelationIntegrityPatrol {

  private static final Logger log = LoggerFactory.getLogger(CoreRelationIntegrityPatrol.class);

  private final JdbcTemplate jdbcTemplate;

  public CoreRelationIntegrityPatrol(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public List<RelationIssueSummary> audit() {
    return jdbcTemplate.query(
        """
        select source_table, relation_type, count(*) issue_count
        from core_relation_integrity_issue
        group by source_table, relation_type
        order by source_table, relation_type
        """,
        (rs, rowNum) ->
            new RelationIssueSummary(
                rs.getString("source_table"),
                rs.getString("relation_type"),
                rs.getLong("issue_count")));
  }

  @Scheduled(
      initialDelayString = "${pingan.data-integrity.patrol-initial-delay-ms:300000}",
      fixedDelayString = "${pingan.data-integrity.patrol-interval-ms:21600000}")
  public void auditAndLog() {
    List<RelationIssueSummary> issues = audit();
    long total = issues.stream().mapToLong(RelationIssueSummary::issueCount).sum();
    if (total == 0) {
      log.info("Core relation integrity patrol completed without orphaned references");
      return;
    }
    log.warn(
        "Core relation integrity patrol found {} orphaned references across {} relation groups",
        total,
        issues.size());
    issues.forEach(
        issue ->
            log.warn(
                "Orphaned relation group sourceTable={} relationType={} count={}",
                issue.sourceTable(),
                issue.relationType(),
                issue.issueCount()));
  }

  public record RelationIssueSummary(String sourceTable, String relationType, long issueCount) {}
}
