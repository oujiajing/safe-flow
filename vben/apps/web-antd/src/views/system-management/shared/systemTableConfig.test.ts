import { describe, expect, it } from 'vitest';

import type { SystemCrudColumn } from './useSystemCrudPage';

import {
  applySystemTableConfigColumns,
  calculateSystemTableAutoWidth,
  createDefaultSystemTableConfig,
  getEnabledSystemTableFilterKeys,
  mergeSystemTableConfig,
  reorderSystemTableConfigFields,
  sortRowsBySystemTableConfig,
} from './systemTableConfig';

const columns: SystemCrudColumn[] = [
  { dataIndex: 'code', fixed: 'left', title: '编码', width: 120 },
  { dataIndex: 'name', fixed: 'left', title: '名称', width: 150 },
  { dataIndex: 'status', title: '状态', width: 100 },
  { dataIndex: 'address', title: '地址', width: 180 },
];

describe('system table configuration', () => {
  it('creates module scoped default views from current columns and filters', () => {
    const config = createDefaultSystemTableConfig('company', '公司', columns, [
      'keyword',
      'status',
    ]);

    expect(config).toMatchObject({
      code: 'COMPANY_STANDARD',
      name: '公司默认视图',
      pinned: false,
      roleIds: [],
    });
    expect(config.fields.map((field) => field.title)).toEqual([
      '编码',
      '名称',
      '状态',
      '地址',
    ]);
    expect(config.fields.every((field) => field.widthMode === 'auto')).toBe(
      true,
    );
    expect(config.filters.map((filter) => filter.key)).toEqual([
      'keyword',
      'status',
    ]);
  });

  it('applies field order, visibility, widths, and sort indicators', () => {
    const config = createDefaultSystemTableConfig('company', '公司', columns, [
      'keyword',
      'status',
    ]);
    config.fields = [
      {
        ...config.fields[1]!,
        order: 1,
        sort: 'ascend',
        width: 220,
        widthMode: 'fixed',
      },
      { ...config.fields[0]!, enabled: false, order: 2 },
      { ...config.fields[3]!, order: 3, width: 260, widthMode: 'fixed' },
      { ...config.fields[2]!, order: 4, widthMode: 'fixed' },
    ];

    expect(
      applySystemTableConfigColumns(columns, config).map((column) => ({
        dataIndex: column.dataIndex,
        sortOrder: column.sortOrder,
        title: column.title,
        width: column.width,
      })),
    ).toEqual([
      { dataIndex: 'name', sortOrder: 'ascend', title: '名称', width: 220 },
      { dataIndex: 'address', sortOrder: undefined, title: '地址', width: 260 },
      { dataIndex: 'status', sortOrder: undefined, title: '状态', width: 100 },
    ]);
  });

  it('sorts rows by the first configured sorted field', () => {
    const config = createDefaultSystemTableConfig('company', '公司', columns, []);
    config.fields = config.fields.map((field) =>
      field.key === 'name' ? { ...field, sort: 'descend' } : field,
    );

    expect(
      sortRowsBySystemTableConfig(
        [
          { code: 'C01', name: 'A公司' },
          { code: 'C02', name: 'C公司' },
          { code: 'C03', name: 'B公司' },
        ],
        config,
      ).map((row) => row.name),
    ).toEqual(['C公司', 'B公司', 'A公司']);
  });

  it('merges saved configuration with fresh module columns', () => {
    const saved = createDefaultSystemTableConfig('company', '公司', columns, [
      'keyword',
      'status',
    ]);
    saved.name = '公司自定义视图';
    saved.fields = [
      { ...saved.fields[3]!, order: 1, width: 240 },
      { ...saved.fields[0]!, enabled: false, order: 2 },
      {
        enabled: true,
        key: 'removed',
        order: 3,
        sort: 'none',
        title: '已删除字段',
        width: 120,
        widthMode: 'auto',
      },
    ];
    saved.filters = [
      { enabled: false, key: 'keyword', title: '关键词' },
      { enabled: true, key: 'status', title: '状态' },
    ];

    const merged = mergeSystemTableConfig(saved, 'company', '公司', columns, [
      'keyword',
      'status',
      'company',
    ]);

    expect(merged.name).toBe('公司自定义视图');
    expect(merged.fields.map((field) => field.key)).toEqual([
      'address',
      'code',
      'name',
      'status',
    ]);
    expect(merged.fields.find((field) => field.key === 'code')?.enabled).toBe(
      false,
    );
    expect(merged.fields.find((field) => field.key === 'address')?.width).toBe(
      240,
    );
    expect(getEnabledSystemTableFilterKeys(merged)).toEqual([
      'status',
      'company',
    ]);
  });

  it('reorders fields after drag sorting and applies the new order', () => {
    const config = createDefaultSystemTableConfig('company', '公司', columns, []);

    config.fields = reorderSystemTableConfigFields(config.fields, 3, 0);

    expect(config.fields.map((field) => field.key)).toEqual([
      'address',
      'code',
      'name',
      'status',
    ]);
    expect(config.fields.map((field) => field.order)).toEqual([1, 2, 3, 4]);
    expect(
      applySystemTableConfigColumns(columns, config).map(
        (column) => column.dataIndex,
      ),
    ).toEqual(['address', 'code', 'name', 'status']);
  });

  it('calculates auto widths from headers and current page text within bounds', () => {
    expect(
      calculateSystemTableAutoWidth(
        { dataIndex: 'status', title: '状态', width: 100 },
        [{ status: '启用' }, { status: '' }],
      ),
    ).toBe(96);

    expect(
      calculateSystemTableAutoWidth(
        { dataIndex: 'address', title: '地址', width: 100 },
        [
          {
            address:
              '广东省深圳市福田区深南大道超长详细地址用于验证宽度上限不会撑爆页面',
          },
        ],
      ),
    ).toBe(320);

    expect(
      calculateSystemTableAutoWidth(
        { dataIndex: 'teamMembers', title: '班组成员', width: 100 },
        [{ teamMembers: ['张三', '李四', '王五'] }],
      ),
    ).toBeGreaterThan(96);
  });

  it('uses fixed widths before auto widths when configured', () => {
    const config = createDefaultSystemTableConfig('company', '公司', columns, []);
    config.fields = config.fields.map((field) =>
      field.key === 'address'
        ? { ...field, width: 220, widthMode: 'fixed' }
        : field,
    );

    expect(
      applySystemTableConfigColumns(
        columns,
        config,
        [
          {
            address:
              '广东省深圳市福田区深南大道超长详细地址用于验证固定宽度优先',
          },
        ],
      ).find((column) => column.dataIndex === 'address')?.width,
    ).toBe(220);
  });

  it('keeps team management table configuration available', () => {
    const config = createDefaultSystemTableConfig('team', '班组管理', columns, [
      'keyword',
      'status',
    ]);

    expect(config.code).toBe('TEAM_STANDARD');
    expect(config.name).toBe('班组管理默认视图');
    expect(config.fields.map((field) => field.widthMode)).toEqual([
      'auto',
      'auto',
      'auto',
      'auto',
    ]);
  });
});
