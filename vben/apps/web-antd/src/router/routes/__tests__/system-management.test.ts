import { describe, expect, it } from 'vitest';

import routes from '../modules/system-management';

function collectTitles(routeList = routes): string[] {
  return routeList.flatMap((route) => [
    String(route.meta?.title),
    ...collectTitles(route.children ?? []),
  ]);
}

describe('system management routes', () => {
  it('registers the system management menu in the required order', () => {
    expect(collectTitles()).toEqual([
      '系统管理',
      '账户管理',
      '角色权限',
      '公司管理',
      '部门管理',
      '班组管理',
      '人员管理',
      '菜单管理',
      '内容配置',
      '公告管理',
      '班组检查项模板',
    ]);
  });

  it('maps every child to the /system route contract', () => {
    const systemRoute = routes[0];

    expect(systemRoute).toMatchObject({
      meta: {
        authority: ['ADMIN'],
      },
      name: 'SystemManagement',
      path: '/system',
      redirect: '/system/account-management',
    });
    expect(systemRoute?.children?.map((route) => route.path)).toEqual([
      '/system/account-management',
      '/system/role-permission',
      '/system/company-management',
      '/system/department-management',
      '/system/team-management',
      '/system/personnel-management',
      '/system/menu-management',
      '/system/content-profiles',
      '/system/announcement-management',
      '/system/team-check-item-templates',
    ]);
    expect(systemRoute?.children?.map((route) => route.name)).toEqual([
      'SystemAccountManagement',
      'SystemRolePermission',
      'SystemCompanyManagement',
      'SystemDepartmentManagement',
      'SystemTeamManagement',
      'SystemPersonnelManagement',
      'SystemMenuManagement',
      'SystemContentProfiles',
      'SystemAnnouncementManagement',
      'SystemTeamCheckItemTemplate',
    ]);
  });
});
