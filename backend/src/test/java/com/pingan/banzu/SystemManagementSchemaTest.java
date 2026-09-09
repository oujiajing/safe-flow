package com.pingan.banzu;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class SystemManagementSchemaTest {

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void flywayCreatesSystemManagementTables() {
    assertThat(tableExists("sys_company_profile")).isTrue();
    assertThat(tableExists("sys_department_profile")).isTrue();
    assertThat(tableExists("sys_team_profile")).isTrue();
    assertThat(tableExists("sys_team_member")).isTrue();
    assertThat(tableExists("sys_user_profile")).isTrue();
    assertThat(tableExists("sys_menu")).isTrue();
    assertThat(tableExists("sys_role_menu")).isTrue();
    assertThat(tableExists("sys_access_log")).isTrue();
    assertThat(tableExists("sys_login_attempt")).isTrue();
    assertThat(tableExists("sys_verification_code")).isTrue();
    assertThat(tableExists("sys_import_job")).isTrue();
    assertThat(tableExists("sys_check_item_library")).isTrue();
    assertThat(tableExists("sys_team_check_template")).isTrue();
    assertThat(tableExists("sys_team_check_template_item")).isTrue();
  }

  @Test
  void keepsOrgAndRoleAsSystemManagementCoreTables() {
    assertThat(columnsOf("sys_org"))
        .contains("id", "parent_id", "org_type", "org_code", "org_name", "org_path", "status");
    assertThat(columnsOf("sys_role")).contains("role_code", "role_name", "data_scope");
  }

  @Test
  void seedsSystemManagementMenusAndAdminGrants() {
    assertThat(tableExists("sys_menu")).isTrue();
    assertThat(tableExists("sys_role_menu")).isTrue();

    List<String> menuTitles =
        jdbcTemplate.queryForList(
            """
            select title
            from sys_menu
            where menu_code like 'SYSTEM_%'
            order by sort_order, id
            """,
            String.class);

    assertThat(menuTitles)
        .containsExactly(
            "系统管理",
            "消息中心",
            "账户管理",
            "角色权限",
            "公司管理",
            "部门管理",
            "班组管理",
            "人员管理",
            "菜单管理",
            "内容配置",
            "班组检查项模板");

    Integer adminGrantCount =
        jdbcTemplate.queryForObject(
            """
            select count(*)
            from sys_role_menu rm
            join sys_role r on r.id = rm.role_id
            join sys_menu m on m.id = rm.menu_id
            where r.role_code = 'ADMIN' and m.menu_code like 'SYSTEM_%'
            """,
            Integer.class);
    assertThat(adminGrantCount).isEqualTo(11);

    Integer nonAdminGrantCount =
        jdbcTemplate.queryForObject(
            """
            select count(*)
            from sys_role_menu rm
            join sys_role r on r.id = rm.role_id
            join sys_menu m on m.id = rm.menu_id
            where r.role_code <> 'ADMIN' and m.menu_code like 'SYSTEM_%'
            """,
            Integer.class);
    assertThat(nonAdminGrantCount).isZero();
  }

  private List<String> columnsOf(String tableName) {
    return jdbcTemplate.queryForList(
        """
        select column_name
        from information_schema.columns
        where table_name = ?
        """,
        String.class,
        tableName);
  }

  private boolean tableExists(String tableName) {
    Integer count =
        jdbcTemplate.queryForObject(
            """
            select count(*)
            from information_schema.tables
            where table_name = ?
            """,
            Integer.class,
            tableName);
    return count != null && count > 0;
  }
}
