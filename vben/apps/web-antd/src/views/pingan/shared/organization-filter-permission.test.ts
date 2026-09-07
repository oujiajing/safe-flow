import { describe, expect, it } from 'vitest';

import type { DataMapNode } from './data-map-state';

import {
  applyScopedPinganOrganizationDefaults,
  getVisiblePinganOrganizationFilterKeys,
  isPinganOrganizationFilterLocked,
  resolveScopedPinganOrganizationDefaults,
} from './organization-filter-permission';

const orgTree: DataMapNode[] = [
  {
    id: 1,
    key: 'group',
    orgType: 'GROUP',
    title: '广晟控股集团',
    children: [
      {
        id: 4,
        key: 'company-yuancheng',
        orgType: 'COMPANY',
        title: '广晟源成',
        children: [
          {
            id: 101_109,
            key: 'department-machining',
            orgType: 'DEPARTMENT',
            title: '门窗机加',
            children: [
              {
                id: 1_011_001,
                key: 'team-1',
                orgType: 'TEAM',
                title: '门窗机加1班',
              },
            ],
          },
        ],
      },
    ],
  },
];

describe('pingan organization filter permission', () => {
  it('hides organization filters for member roles', () => {
    expect(getVisiblePinganOrganizationFilterKeys(['TEAM_MEMBER'])).toEqual([]);
    expect(getVisiblePinganOrganizationFilterKeys(['CURTAIN_WALL_MEMBER'])).toEqual([]);
  });

  it('locks company and department for department scoped roles', () => {
    expect(isPinganOrganizationFilterLocked(['DEPARTMENT_MANAGER'], 'company')).toBe(true);
    expect(isPinganOrganizationFilterLocked(['DEPARTMENT_MANAGER'], 'department')).toBe(true);
    expect(isPinganOrganizationFilterLocked(['DEPARTMENT_MANAGER'], 'team')).toBe(false);
    expect(isPinganOrganizationFilterLocked(['WORKSHOP_DIRECTOR'], 'department')).toBe(true);
  });

  it('locks company for company scoped leaders and leaves lower filters selectable', () => {
    expect(isPinganOrganizationFilterLocked(['COMPANY_LEADER'], 'company')).toBe(true);
    expect(isPinganOrganizationFilterLocked(['COMPANY_LEADER'], 'department')).toBe(false);
    expect(isPinganOrganizationFilterLocked(['COMPANY_LEADER'], 'team')).toBe(false);
    expect(isPinganOrganizationFilterLocked(['ENTERPRISE_LEADER'], 'company')).toBe(true);
  });

  it('resolves company department and team defaults from the current user organization', () => {
    expect(resolveScopedPinganOrganizationDefaults(101_109, orgTree)).toEqual({
      companyId: 4,
      departmentId: 101_109,
      teamId: undefined,
    });
    expect(resolveScopedPinganOrganizationDefaults(1_011_001, orgTree)).toEqual({
      companyId: 4,
      departmentId: 101_109,
      teamId: 1_011_001,
    });
  });

  it('applies scoped defaults without overriding selectable lower levels', () => {
    const defaults = {
      companyId: 4,
      departmentId: 101_109,
      teamId: 1_011_001,
    };

    expect(
      applyScopedPinganOrganizationDefaults({}, defaults, ['TEAM_MEMBER']),
    ).toEqual(defaults);
    expect(
      applyScopedPinganOrganizationDefaults(
        { teamId: 9 },
        defaults,
        ['DEPARTMENT_MANAGER'],
      ),
    ).toEqual({
      companyId: 4,
      departmentId: 101_109,
      teamId: 9,
    });
    expect(
      applyScopedPinganOrganizationDefaults(
        { departmentId: 8, teamId: 9 },
        defaults,
        ['COMPANY_LEADER'],
      ),
    ).toEqual({
      companyId: 4,
      departmentId: 8,
      teamId: 9,
    });
  });
});
