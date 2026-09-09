import type { RouteRecordRaw } from 'vue-router';

const AccountManagement = () =>
  import('#/views/system-management/account-management/index.vue');
const RolePermission = () =>
  import('#/views/system-management/role-permission/index.vue');
const CompanyManagement = () =>
  import('#/views/system-management/company-management/index.vue');
const DepartmentManagement = () =>
  import('#/views/system-management/department-management/index.vue');
const TeamManagement = () =>
  import('#/views/system-management/team-management/index.vue');
const PersonnelManagement = () =>
  import('#/views/system-management/personnel-management/index.vue');
const MenuManagement = () =>
  import('#/views/system-management/menu-management/index.vue');
const ContentProfiles = () =>
  import('#/views/system-management/content-profiles/index.vue');
const TeamCheckItemTemplates = () =>
  import('#/views/system-management/team-check-item-templates/index.vue');
const AnnouncementManagement = () =>
  import('#/views/system-management/announcement-management/index.vue');

const routes: RouteRecordRaw[] = [
  {
    meta: {
      authority: ['ADMIN'],
      icon: 'lucide:settings',
      order: -90,
      title: '系统管理',
    },
    name: 'SystemManagement',
    path: '/system',
    redirect: '/system/account-management',
    children: [
      {
        component: AccountManagement,
        meta: { title: '账户管理' },
        name: 'SystemAccountManagement',
        path: '/system/account-management',
      },
      {
        component: RolePermission,
        meta: { title: '角色权限' },
        name: 'SystemRolePermission',
        path: '/system/role-permission',
      },
      {
        component: CompanyManagement,
        meta: { title: '公司管理' },
        name: 'SystemCompanyManagement',
        path: '/system/company-management',
      },
      {
        component: DepartmentManagement,
        meta: { title: '部门管理' },
        name: 'SystemDepartmentManagement',
        path: '/system/department-management',
      },
      {
        component: TeamManagement,
        meta: { title: '班组管理' },
        name: 'SystemTeamManagement',
        path: '/system/team-management',
      },
      {
        component: PersonnelManagement,
        meta: { title: '人员管理' },
        name: 'SystemPersonnelManagement',
        path: '/system/personnel-management',
      },
      {
        component: MenuManagement,
        meta: { title: '菜单管理' },
        name: 'SystemMenuManagement',
        path: '/system/menu-management',
      },
      {
        component: ContentProfiles,
        meta: { title: '内容配置' },
        name: 'SystemContentProfiles',
        path: '/system/content-profiles',
      },
      {
        component: AnnouncementManagement,
        meta: { title: '公告管理' },
        name: 'SystemAnnouncementManagement',
        path: '/system/announcement-management',
      },
      {
        component: TeamCheckItemTemplates,
        meta: { title: '班组检查项模板' },
        name: 'SystemTeamCheckItemTemplate',
        path: '/system/team-check-item-templates',
      },
    ],
  },
];

export default routes;
