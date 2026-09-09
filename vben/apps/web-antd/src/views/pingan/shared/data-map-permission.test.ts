import { describe, expect, it } from 'vitest';

import { canShowPinganDataMap } from './data-map-permission';

describe('pingan data map permission', () => {
  it('allows only leader and admin role codes to see data maps', () => {
    for (const role of [
      'ADMIN',
      'CURTAIN_WALL_LEADER',
      'GROUP_LEADER',
      'ENTERPRISE_LEADER',
      'COMPANY_LEADER',
    ]) {
      expect(canShowPinganDataMap([role]), role).toBe(true);
    }

    for (const role of [
      'DEPARTMENT_MANAGER',
      'WORKSHOP_DIRECTOR',
      'TEAM_LEADER',
      'TEAM_MEMBER',
      'CURTAIN_WALL_MEMBER',
      'SAFETY_OFFICER',
    ]) {
      expect(canShowPinganDataMap([role]), role).toBe(false);
    }
  });
});
