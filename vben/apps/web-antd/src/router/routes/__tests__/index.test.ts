import { describe, expect, it } from 'vitest';

import { accessRoutes } from '..';
import systemManagementRoutes from '../modules/system-management';

function collectRouteNames(routes = accessRoutes): string[] {
  return routes.flatMap((route) => [
    String(route.name),
    ...collectRouteNames(route.children ?? []),
  ]);
}

describe('web-antd access routes', () => {
  it('does not expose demo, project, or about navigation routes', () => {
    expect(collectRouteNames()).not.toEqual(
      expect.arrayContaining(['Demos', 'VbenProject', 'VbenAbout']),
    );
  });

  it('keeps profile as a hidden non-navigation route', () => {
    const profileRoute = accessRoutes.find((route) => route.name === 'Profile');

    expect(profileRoute).toMatchObject({
      meta: {
        hideInMenu: true,
      },
      path: '/profile',
    });
  });

  it('registers system management children in the global access routes', () => {
    const systemRoute = accessRoutes.find(
      (route) => route.name === 'SystemManagement',
    );
    const canonicalSystemRoute = systemManagementRoutes.find(
      (route) => route.name === 'SystemManagement',
    );

    expect(systemRoute?.children?.map((route) => route.name)).toEqual(
      canonicalSystemRoute?.children?.map((route) => route.name),
    );
  });
});
