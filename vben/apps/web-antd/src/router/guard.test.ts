import { describe, expect, it } from 'vitest';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

import {
  invalidateAccessRoutes,
  resolveRouteAccessAuthorities,
} from './guard';

function appSourcePath(path: string) {
  return process.cwd().replaceAll('\\', '/').endsWith('/apps/web-antd')
    ? resolve(path)
    : resolve(`apps/web-antd/${path}`);
}

describe('router guard access authorities', () => {
  it('combines user roles and backend permission codes for route filtering', () => {
    expect(
      resolveRouteAccessAuthorities(
        ['TEAM_LEADER'],
        ['PINGAN_THREE_CHECK_ENTRY', 'TEAM_LEADER'],
      ),
    ).toEqual(['TEAM_LEADER', 'PINGAN_THREE_CHECK_ENTRY']);
  });

  it('feeds backend permission codes into frontend route generation', () => {
    const guardSource = readFileSync(appSourcePath('src/router/guard.ts'), 'utf8');

    expect(guardSource).toContain('getAccessCodesApi()');
    expect(guardSource).toMatch(
      /resolveRouteAccessAuthorities\(\s*userRoles,\s*accessCodes,\s*\)/,
    );
    expect(guardSource).toContain('roles: routeAccessAuthorities');
  });

  it('exposes an invalidation helper for permission changes', () => {
    expect(typeof invalidateAccessRoutes).toBe('function');
  });

  it('rebuilds access menus and routes after role permissions are saved', () => {
    const rolePermissionSource = readFileSync(
      appSourcePath('src/views/system-management/role-permission/index.vue'),
      'utf8',
    );

    expect(rolePermissionSource).toContain('refreshAccessRoutes(router, resetRoutes)');
    expect(rolePermissionSource).toContain("message.success('保存授权成功')");
    expect(rolePermissionSource).not.toContain('setAccessCodes(await getAccessCodesApi())');
  });

  it('refresh helper rebuilds menus instead of leaving access store empty', () => {
    const guardSource = readFileSync(appSourcePath('src/router/guard.ts'), 'utf8');

    expect(guardSource).toContain('async function refreshAccessRoutes');
    expect(guardSource).toContain('accessStore.setAccessMenus(accessibleMenus)');
    expect(guardSource).toContain('accessStore.setAccessRoutes(accessibleRoutes)');
    expect(guardSource).toContain('accessStore.setIsAccessChecked(true)');
  });
});
