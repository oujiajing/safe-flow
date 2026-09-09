export namespace SystemManagementApi {
  export type Id = number | string;
  export type Status = 'ACTIVE' | 'DRAFT' | 'INACTIVE';
  export type ImportExportModule =
    | 'company'
    | 'department'
    | 'menu'
    | 'personnel'
    | 'role'
    | 'team';

  export interface PageResult<T> {
    items: T[];
    total: number | string;
  }

  export interface ListParams {
    companyId?: Id;
    departmentId?: Id;
    keyword?: string;
    organizationId?: Id;
    page?: number;
    pageSize?: number;
    status?: Status | 'all';
    teamId?: Id;
    [key: string]: unknown;
  }

  export interface BaseRecord {
    code?: string;
    id: Id;
    name?: string;
    status?: Status;
  }

  export interface Company extends BaseRecord {
    address?: string;
    companyType?: string;
    description?: string;
    attachment1Url?: string;
    attachment2Url?: string;
    companyIntro?: string;
    level1Name?: string;
    level2Name?: string;
    level3Name?: string;
    level4Name?: string;
    reportL1Time?: number;
    reportL2Time?: number;
    reportL3Time?: number;
    reporterL1Usernames?: string;
    reporterL2Usernames?: string;
    reporterL3Usernames?: string;
    safetyManagerUsername?: string;
    safetyAdmin?: string;
    shortName?: string;
    sort?: number;
    sortOrder?: number;
  }

  export interface Department extends BaseRecord {
    childSortOrder?: number;
    companyOrgId?: Id;
    companySortOrder?: number;
    companyName?: string;
    departmentType?: string;
    description?: string;
    groupName?: string;
    leader?: string;
    leaderLevel?: string;
    leaderUsername?: string;
    level1Unit?: string;
    level2Unit?: string;
    sort?: number;
    topLevelName?: string;
    type?: string;
  }

  export interface Team extends BaseRecord {
    active?: boolean;
    applicantName?: string;
    companyOrgId?: Id;
    companyName?: string;
    groupWork?: string;
    groupName?: string;
    leader?: string;
    leaderUsername?: string;
    level1Unit?: string;
    level2Unit?: string;
    points?: number;
    safetyOfficer?: string;
    safetyOfficerUsername?: string;
    submitDate?: string;
    teamMembers?: string[];
    workshop?: string;
    workshopName?: string;
    workshopOrgId?: Id;
    workshopOrgName?: string;
    workTypeCode?: string;
    workTypeName?: string;
  }

  export interface Personnel extends BaseRecord {
    companyOrgId?: Id;
    companyName?: string;
    companyShortName?: string;
    departmentOrgId?: Id;
    departmentName?: string;
    departmentSortOrder?: number;
    employeeCode?: string;
    employeeType?: string;
    applicantName?: string;
    certificateValidUntil?: string;
    joinDate?: string;
    managementWeight?: number;
    mobile?: string;
    post?: string;
    points?: number;
    positionName?: string;
    receivedPoints?: number;
    remark?: string;
    roles?: string[];
    submitDate?: string;
    systemRoleCode?: string;
    teamOrgId?: Id;
    teamName?: string;
    username?: string;
  }

  export interface Account extends BaseRecord {
    lastLoginAt?: string;
    mobile?: string;
    orgId?: Id;
    orgName?: string;
    organizationName?: string;
    password?: string;
    realName?: string;
    roleIds?: Id[];
    roles?: Role[];
    username?: string;
  }

  export interface Role extends BaseRecord {
    dataScope?: string;
    description?: string;
    permissionCodes?: string[];
    roleCode?: string;
    roleName?: string;
  }

  export interface Menu extends BaseRecord {
    children?: Menu[];
    component?: string;
    icon?: string;
    orderNo?: number;
    parentId?: Id;
    path?: string;
    permissionCode?: string;
    title?: string;
    visible?: boolean;
  }

  export interface Option {
    label: string;
    value: Id;
  }

  export interface OrganizationNode {
    children?: OrganizationNode[];
    companyType?: string;
    id: Id;
    key: string;
    orgType: string;
    title: string;
  }
}
