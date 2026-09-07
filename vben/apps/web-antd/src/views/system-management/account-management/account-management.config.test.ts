import { describe, expect, it } from 'vitest';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

import type { SystemManagementApi } from '#/api/system-management/types';

import {
  accountColumns,
  buildAccountFields,
  flattenOrganizationOptions,
  formatAccountRoleNames,
  normalizeRoleOptions,
  normalizeOrganizationTreeOptions,
  scopeOrganizationTreeToCompany,
} from './account-management.config';

function appSourcePath(path: string) {
  return process.cwd().replaceAll('\\', '/').endsWith('/apps/web-antd')
    ? resolve(path)
    : resolve(`apps/web-antd/${path}`);
}

describe('account management view config', () => {
  it('uses organization tree-select and role multi-select fields', () => {
    const fields = buildAccountFields({
      organizations: [
        {
          children: [{ label: '系统管理演示班组', value: 34 }],
          label: '系统管理演示部门',
          value: 33,
        },
      ],
      roles: [{ label: '班长', value: 3 }],
    });

    expect(fields.find((field) => field.label === '组织ID')).toBeUndefined();
    expect(fields.find((field) => field.valueKey === 'orgId')).toMatchObject({
      component: 'tree-select',
      dropdownMatchSelectWidth: 520,
      label: '组织',
      listHeight: 520,
      required: true,
      showSearch: true,
      treeDefaultExpandAll: true,
    });
    expect(fields.find((field) => field.valueKey === 'orgId')?.options).toEqual([
      {
        children: [{ label: '系统管理演示班组', value: 34 }],
        label: '系统管理演示部门',
        value: 33,
      },
    ]);
    expect(fields.find((field) => field.valueKey === 'roleIds')).toMatchObject({
      component: 'select',
      label: '角色',
      mode: 'multiple',
      options: [{ label: '班长', value: 3 }],
    });
    expect(fields.find((field) => field.valueKey === 'status')).toMatchObject({
      component: 'select',
      label: '状态',
      required: true,
    });
  });

  it('loads the full organization tree so accounts can bind to teams', () => {
    const source = readFileSync(
      appSourcePath('src/views/system-management/account-management/index.vue'),
      'utf8',
    );

    expect(source).toContain('getSystemFullOrganizationTreeApi');
    expect(source).not.toContain('getSystemOrganizationTreeApi');
  });

  it('scopes account organization options to the selected data-map company', () => {
    const nodes: SystemManagementApi.OrganizationNode[] = [
      {
        children: [
          {
            children: [
              {
                children: [
                  {
                    id: 1011001,
                    key: '1011001',
                    orgType: 'TEAM',
                    title: '幕墙组装1班',
                  },
                ],
                id: 101109,
                key: '101109',
                orgType: 'DEPARTMENT',
                title: '幕墙组装',
              },
            ],
            id: 4,
            key: '4',
            orgType: 'COMPANY',
            title: '广晟源成',
          },
          {
            children: [{ id: 8, key: '8', orgType: 'COMPANY', title: '梅州嘉晟' }],
            id: 11,
            key: '11',
            orgType: 'COMPANY',
            title: '广晟矿投',
          },
        ],
        id: 2,
        key: '2',
        orgType: 'GROUP',
        title: '广晟矿业集团',
      },
    ];

    expect(scopeOrganizationTreeToCompany(nodes, 4)).toEqual([
      {
        children: [
          {
            children: [
              {
                id: 1011001,
                key: '1011001',
                orgType: 'TEAM',
                title: '幕墙组装1班',
              },
            ],
            id: 101109,
            key: '101109',
            orgType: 'DEPARTMENT',
            title: '幕墙组装',
          },
        ],
        id: 4,
        key: '4',
        orgType: 'COMPANY',
        title: '广晟源成',
      },
    ]);
  });

  it('formats account roles by display name instead of object text', () => {
    expect(
      formatAccountRoleNames({
        roles: [
          { id: 3, roleCode: 'TEAM_LEADER', roleName: '班长' },
          { id: 8, roleCode: 'TEAM_MEMBER' },
        ],
      }),
    ).toEqual(['班长', 'TEAM_MEMBER']);
  });

  it('normalizes role and organization options for account form selectors', () => {
    expect(
      normalizeRoleOptions([
        { id: 3, roleCode: 'TEAM_LEADER', roleName: '班长' },
        { id: 8, roleCode: 'TEAM_MEMBER', roleName: '组员' },
      ]),
    ).toEqual([
      { label: '班长', value: 3 },
      { label: '组员', value: 8 },
    ]);

    const nodes: SystemManagementApi.OrganizationNode[] = [
      {
        children: [
          {
            id: 34,
            key: '34',
            orgType: 'TEAM',
            title: '系统管理演示班组',
          },
        ],
        id: 33,
        key: '33',
        orgType: 'DEPARTMENT',
        title: '系统管理演示部门',
      },
    ];
    expect(flattenOrganizationOptions(nodes)).toEqual([
      { label: '系统管理演示部门', value: 33 },
      { label: '系统管理演示部门 / 系统管理演示班组', value: 34 },
    ]);
    expect(normalizeOrganizationTreeOptions(nodes)).toEqual([
      {
        children: [{ label: '系统管理演示班组', value: 34 }],
        label: '系统管理演示部门',
        value: 33,
      },
    ]);
  });

  it('keeps the roles column configured for readable tag rendering', () => {
    expect(accountColumns.find((column) => column.dataIndex === 'roles'))
      .toMatchObject({
        dataIndex: 'roles',
        title: '角色',
      });
  });
});
