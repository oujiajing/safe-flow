package com.pingan.banzu.mapper;

public final class ThreeCheckRecordSqlProvider {

  private static final String BASE_FROM =
      """
      from three_check_record r
      join sys_org c on c.id = r.company_id and c.deleted = 0
      join sys_org d on d.id = r.department_id and d.deleted = 0
      left join sys_org t on t.id = r.team_id and t.deleted = 0
      left join sys_company_profile cp on cp.org_id = r.company_id and cp.deleted = 0
      """;

  private static final String BASE_WHERE =
      """
      <where>
        r.deleted = 0
        and r.module_key = #{criteria.moduleKey}
        <if test="criteria.statuses != null and criteria.statuses.size > 0">
          and r.status in
          <foreach collection="criteria.statuses" item="status" open="(" close=")" separator=",">
            #{status}
          </foreach>
        </if>
        <if test="criteria.companyId != null">
          and r.company_id = #{criteria.companyId}
        </if>
        <if test="criteria.departmentId != null">
          and r.department_id = #{criteria.departmentId}
        </if>
        <if test="criteria.teamId != null">
          and r.team_id = #{criteria.teamId}
        </if>
        <if test="criteria.rootDispatchRecordId != null">
          and r.root_dispatch_record_id = #{criteria.rootDispatchRecordId}
        </if>
        <if test="criteria.sourceChannel != null and criteria.sourceChannel != ''">
          and r.source_channel = #{criteria.sourceChannel}
        </if>
        <if test="criteria.pointsReason != null and criteria.pointsReason != ''">
          and r.payload_json like concat('%', #{criteria.pointsReason}, '%')
        </if>
        <if test="criteria.dateStart != null">
          and r.business_date &gt;= #{criteria.dateStart}
        </if>
        <if test="criteria.dateEnd != null">
          and r.business_date &lt;= #{criteria.dateEnd}
        </if>
        <if test="criteria.overdue != null and criteria.overdue">
          and r.business_date &lt; #{criteria.overdueBeforeDate}
          and r.status in ('DRAFT', 'WITHDRAWN')
        </if>
        <if test="criteria.overdue != null and !criteria.overdue">
          and not (r.business_date &lt; #{criteria.overdueBeforeDate} and r.status in ('DRAFT', 'WITHDRAWN'))
        </if>
        <if test="criteria.companyKeyword != null and criteria.companyKeyword != ''">
          and (
            c.org_name like concat('%', #{criteria.companyKeyword}, '%')
            or cp.short_name like concat('%', #{criteria.companyKeyword}, '%')
          )
        </if>
        <if test="criteria.departmentKeyword != null and criteria.departmentKeyword != ''">
          and d.org_name like concat('%', #{criteria.departmentKeyword}, '%')
        </if>
        <if test="criteria.teamKeyword != null and criteria.teamKeyword != ''">
          and t.org_name like concat('%', #{criteria.teamKeyword}, '%')
        </if>
        <if test="criteria.accessDenied">
          and 1 = 0
        </if>
        <if test="criteria.selfOwnerUserId != null">
          and r.owner_user_id = #{criteria.selfOwnerUserId}
        </if>
        <if test="criteria.visibleTeamIds != null and criteria.visibleTeamIds.size > 0">
          and (
            r.team_id in
            <foreach collection="criteria.visibleTeamIds" item="teamId" open="(" close=")" separator=",">
              #{teamId}
            </foreach>
            <if test="criteria.visibleOwnerUserIds != null and criteria.visibleOwnerUserIds.size > 0">
              or r.owner_user_id in
              <foreach collection="criteria.visibleOwnerUserIds" item="ownerUserId" open="(" close=")" separator=",">
                #{ownerUserId}
              </foreach>
            </if>
          )
        </if>
        <if test="criteria.visibleTeamIds == null or criteria.visibleTeamIds.size == 0">
          <if test="criteria.visibleOwnerUserIds != null and criteria.visibleOwnerUserIds.size > 0">
            and r.owner_user_id in
            <foreach collection="criteria.visibleOwnerUserIds" item="ownerUserId" open="(" close=")" separator=",">
              #{ownerUserId}
            </foreach>
          </if>
        </if>
        <if test="criteria.curtainWallOnly">
          and (
            r.module_key like '%curtain-wall%'
            or exists (
              select 1
              from three_check_record root
              where root.id = r.root_dispatch_record_id
                and root.deleted = 0
                and root.module_key = 'curtain-wall-team-dispatch'
            )
          )
        </if>
        <if test="criteria.accessOrgIds != null and criteria.accessOrgIds.size > 0">
          and (
            r.company_id in
            <foreach collection="criteria.accessOrgIds" item="orgId" open="(" close=")" separator=",">
              #{orgId}
            </foreach>
            or r.department_id in
            <foreach collection="criteria.accessOrgIds" item="orgId" open="(" close=")" separator=",">
              #{orgId}
            </foreach>
            or r.team_id in
            <foreach collection="criteria.accessOrgIds" item="orgId" open="(" close=")" separator=",">
              #{orgId}
            </foreach>
          )
        </if>
        <if test="criteria.filterOrgDenied">
          and 1 = 0
        </if>
        <if test="criteria.filterOrgIds != null and criteria.filterOrgIds.size > 0">
          and (
            r.company_id in
            <foreach collection="criteria.filterOrgIds" item="orgId" open="(" close=")" separator=",">
              #{orgId}
            </foreach>
            or r.department_id in
            <foreach collection="criteria.filterOrgIds" item="orgId" open="(" close=")" separator=",">
              #{orgId}
            </foreach>
            or r.team_id in
            <foreach collection="criteria.filterOrgIds" item="orgId" open="(" close=")" separator=",">
              #{orgId}
            </foreach>
          )
        </if>
      </where>
      """;

  private ThreeCheckRecordSqlProvider() {}

  public static String selectPage() {
    return script("select r.* " + BASE_FROM + BASE_WHERE + " order by r.business_date desc, r.id desc limit #{limit} offset #{offset}");
  }

  public static String countRecords() {
    return script("select count(*) " + BASE_FROM + BASE_WHERE);
  }

  public static String countByStatus() {
    return script("select r.status as status, count(*) as count " + BASE_FROM + BASE_WHERE + " group by r.status");
  }

  public static String countAttachments() {
    return script(
        """
        select
          count(*) as total,
          sum(case when coalesce(r.image_check_status, '') not in ('', '未上传', '待上传') then 1 else 0 end) as image_uploaded,
          sum(case when coalesce(r.image_check_status, '') in ('', '未上传', '待上传') then 1 else 0 end) as image_missing,
          sum(case when coalesce(r.video_check_status, '') not in ('', '未上传', '待上传') then 1 else 0 end) as video_uploaded,
          sum(case when coalesce(r.video_check_status, '') in ('', '未上传', '待上传') then 1 else 0 end) as video_missing
        """
            + BASE_FROM
            + BASE_WHERE);
  }

  public static String countByDate() {
    return script(
        "select r.business_date as bucket_date, count(*) as count "
            + BASE_FROM
            + BASE_WHERE
            + " group by r.business_date order by r.business_date asc");
  }

  public static String countByCompany() {
    return countByOrganization("r.company_id", "c.org_name", "COMPANY");
  }

  public static String countByDepartment() {
    return countByOrganization("r.department_id", "d.org_name", "DEPARTMENT");
  }

  public static String countByTeam() {
    return countByOrganization("r.team_id", "t.org_name", "TEAM");
  }

  private static String countByOrganization(String idColumn, String nameColumn, String type) {
    return script(
        "select "
            + idColumn
            + " as organization_id, "
            + nameColumn
            + " as organization_name, '"
            + type
            + "' as organization_type, count(*) as count "
            + BASE_FROM
            + BASE_WHERE
            + " group by "
            + idColumn
            + ", "
            + nameColumn
            + " order by count(*) desc, "
            + nameColumn
            + " asc");
  }

  private static String script(String sql) {
    return "<script>" + sql + "</script>";
  }
}
