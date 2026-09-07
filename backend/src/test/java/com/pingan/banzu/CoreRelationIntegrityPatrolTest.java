package com.pingan.banzu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pingan.banzu.service.CoreRelationIntegrityPatrol;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class CoreRelationIntegrityPatrolTest {

  @Autowired JdbcTemplate jdbcTemplate;
  @Autowired CoreRelationIntegrityPatrol patrol;

  @Test
  void reportsPolymorphicOrphansWithoutDeletingAuditHistory() {
    assertThat(patrol.audit()).isEmpty();

    jdbcTemplate.update(
        """
        insert into biz_status_log (
          biz_type, biz_id, to_status, action, operator_id, created_at
        ) values ('HAZARD_SAFETY_CHECK', 9876543210, 'DRAFT', 'TEST', 1, current_timestamp)
        """);

    assertThat(patrol.audit())
        .singleElement()
        .satisfies(
            issue -> {
              assertThat(issue.sourceTable()).isEqualTo("biz_status_log");
              assertThat(issue.relationType()).isEqualTo("HAZARD_SAFETY_CHECK");
              assertThat(issue.issueCount()).isOne();
            });
  }

  @Test
  void rejectsNewStrongRelationOrphans() {
    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    "insert into sys_user_role (user_id, role_id) values (?, ?)",
                    9876543210L,
                    1L))
        .isInstanceOf(DataIntegrityViolationException.class);
  }
}
