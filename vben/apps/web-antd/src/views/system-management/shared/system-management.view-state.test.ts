import { readFileSync } from 'node:fs';
import { join } from 'node:path';

import { describe, expect, it, vi } from 'vitest';

import {
  createSystemTableScrollConfig,
  createSystemCrudState,
  getSystemRowKey,
  serializeSystemFilters,
  systemCrudTableScrollConfig,
} from './useSystemCrudPage';
import {
  filterCompanyDrawerFields,
  normalizeCompanyPayloadForSubmit,
  shouldDisableCompanyCreateForPath,
} from './companyHierarchy';
import {
  filterDepartmentDrawerFields,
  findDefaultDepartmentCompanyPath,
  normalizeDepartmentPayloadForSubmit,
} from './departmentHierarchy';
import {
  createTeamDefaultValues,
  normalizeTeamPayloadForSubmit,
} from './teamHierarchy';
import {
  createPersonnelDefaultValues,
  normalizePersonnelPayloadForSubmit,
} from './personnelHierarchy';
import { normalizeSystemDataMapNodes } from './systemDataMapTree';

function appSourcePath(path: string) {
  return join(
    process.cwd(),
    process.cwd().endsWith(join('apps', 'web-antd')) ? path : `apps/web-antd/${path}`,
  );
}

describe('system management shared view state', () => {
  it('serializes keyword, status, and cascade filters to API params', () => {
    expect(
      serializeSystemFilters({
        companyId: '1',
        departmentId: '2',
        keyword: '湖贝',
        organizationId: 9,
        status: 'ACTIVE',
        teamId: '3',
      }),
    ).toEqual({
      companyId: '1',
      departmentId: '2',
      keyword: '湖贝',
      organizationId: 9,
      status: 'ACTIVE',
      teamId: '3',
    });
  });

  it('serializes cascade filters with backend parameter aliases', () => {
    expect(
      serializeSystemFilters(
        {
          companyId: 8,
          departmentId: 9,
          keyword: '维修',
          status: 'ACTIVE',
          teamId: 10,
        },
        {
          companyId: 'companyOrgId',
          departmentId: 'departmentOrgId',
          teamId: 'teamOrgId',
        },
      ),
    ).toEqual({
      companyOrgId: 8,
      departmentOrgId: 9,
      keyword: '维修',
      status: 'ACTIVE',
      teamOrgId: 10,
    });
  });

  it('omits cascade filters that are hidden for the current page', () => {
    expect(
      serializeSystemFilters(
        {
          companyId: 8,
          departmentId: 9,
          status: 'all',
          teamId: 10,
        },
        {
          companyId: 'companyOrgId',
          departmentId: null,
          teamId: null,
        },
      ),
    ).toEqual({
      companyOrgId: 8,
    });
  });

  it('resets child cascade selections when parent filters change', () => {
    const state = createSystemCrudState({ fetchList: vi.fn() });

    state.filters.companyId = '1';
    state.filters.departmentId = '2';
    state.filters.teamId = '3';

    state.setCompany('5');
    expect(state.filters).toMatchObject({
      companyId: '5',
      departmentId: undefined,
      teamId: undefined,
    });

    state.filters.departmentId = '6';
    state.filters.teamId = '7';
    state.setDepartment('8');
    expect(state.filters).toMatchObject({
      companyId: '5',
      departmentId: '8',
      teamId: undefined,
    });
  });

  it('enables row actions only when a row is selected', () => {
    const state = createSystemCrudState({ fetchList: vi.fn() });

    expect(state.selectedRow.value).toBeUndefined();
    expect(state.canUseRowActions.value).toBe(false);
    expect(state.canUseSingleRowAction.value).toBe(false);
    expect(state.selectedCount.value).toBe(0);

    state.selectRow({ id: '4', name: 'Demo Works Company' });
    expect(state.canUseRowActions.value).toBe(true);
    expect(state.canUseSingleRowAction.value).toBe(true);
    expect(state.selectedCount.value).toBe(1);
  });

  it('supports batch delete and status operations for selected rows', async () => {
    const deleteRecord = vi.fn().mockResolvedValue(undefined);
    const fetchList = vi.fn().mockResolvedValue({ items: [], total: 0 });
    const updateStatus = vi.fn().mockResolvedValue(undefined);
    const state = createSystemCrudState({
      deleteRecord,
      fetchList,
      updateStatus,
    });

    state.selectRows([
      { id: '4', name: 'Demo Works Company' },
      { id: '8', name: 'Demo East Site' },
    ]);

    expect(state.selectedIds.value).toEqual(['4', '8']);
    expect(state.selectedRow.value).toBeUndefined();
    expect(state.canUseRowActions.value).toBe(true);
    expect(state.canUseSingleRowAction.value).toBe(false);
    expect(state.selectedCount.value).toBe(2);

    await state.handleDelete();
    expect(deleteRecord).toHaveBeenCalledWith('4');
    expect(deleteRecord).toHaveBeenCalledWith('8');
    expect(fetchList).toHaveBeenCalledTimes(1);

    state.selectRows([
      { id: '4', name: 'Demo Works Company' },
      { id: '8', name: 'Demo East Site' },
    ]);
    await state.handleStatus('INACTIVE');
    expect(updateStatus).toHaveBeenCalledWith('4', 'INACTIVE');
    expect(updateStatus).toHaveBeenCalledWith('8', 'INACTIVE');
  });

  it('deletes rows with large string ids without numeric coercion', async () => {
    const deleteRecord = vi.fn().mockResolvedValue(undefined);
    const fetchList = vi.fn().mockResolvedValue({ items: [], total: 0 });
    const state = createSystemCrudState({
      deleteRecord,
      fetchList,
    });

    state.selectRows([
      { id: '2055946189550952450', name: '浏览器大整数公司' },
    ]);

    await state.handleDelete();

    expect(deleteRecord).toHaveBeenCalledWith('2055946189550952450');
  });

  it('refreshes dependent master data after mutating rows', async () => {
    const afterMutation = vi.fn().mockResolvedValue(undefined);
    const createRecord = vi.fn().mockResolvedValue(undefined);
    const deleteRecord = vi.fn().mockResolvedValue(undefined);
    const fetchList = vi.fn().mockResolvedValue({ items: [], total: 0 });
    const state = createSystemCrudState({
      afterMutation,
      createRecord,
      deleteRecord,
      fetchList,
    });

    await state.submit({ name: '新增公司' });
    expect(afterMutation).toHaveBeenCalledTimes(1);
    expect(state.selectedIds.value).toEqual([]);

    state.selectRows([{ id: 99, name: '新增公司' }]);
    await state.handleDelete();
    expect(afterMutation).toHaveBeenCalledTimes(2);
  });

  it('preserves row key value type for controlled table selection', () => {
    expect(getSystemRowKey({ id: 4, name: 'Demo Works Company' }, 'id')).toBe(4);
    expect(getSystemRowKey({ id: '4', name: 'Demo Works Company' }, 'id')).toBe('4');
    expect(getSystemRowKey({ name: 'Demo Works Company' }, 'id')).toBeUndefined();
  });

  it('loads data with configured filter aliases', async () => {
    const fetchList = vi.fn().mockResolvedValue({ items: [], total: 0 });
    const state = createSystemCrudState({
      fetchList,
      filterParamMap: {
        companyId: 'companyOrgId',
        departmentId: 'workshopOrgId',
        teamId: null,
      },
    });

    state.filters.companyId = 8;
    state.filters.departmentId = 9;
    state.filters.teamId = 10;

    await state.load();

    expect(fetchList).toHaveBeenCalledWith({
      companyOrgId: 8,
      workshopOrgId: 9,
    });
  });

  it('loads data for the selected organization from the data map', async () => {
    const fetchList = vi.fn().mockResolvedValue({ items: [], total: 0 });
    const state = createSystemCrudState({ fetchList });

    state.setOrganization(8);
    await state.load();

    expect(fetchList).toHaveBeenCalledWith({ organizationId: 8 });
  });

  it('emits selected data-map organization changes for page-specific drawer scoping', () => {
    const crudPage = readFileSync(
      appSourcePath('src/views/system-management/shared/SystemCrudPage.vue'),
      'utf8',
    );

    expect(crudPage).toContain(
      'organizationSelect: [id: SystemManagementApi.Id | undefined, keys: string[]]',
    );
    expect(crudPage).toContain("emit('organizationSelect', id, keys)");
  });

  it('loads data with configured default list parameters', async () => {
    const fetchList = vi.fn().mockResolvedValue({ items: [], total: 0 });
    const state = createSystemCrudState({
      defaultListParams: { pageSize: 100 },
      fetchList,
    });

    state.setOrganization(1);
    await state.load();

    expect(fetchList).toHaveBeenCalledWith({
      organizationId: 1,
      pageSize: 100,
    });
  });

  it('normalizes string totals returned after backend long serialization', async () => {
    const fetchList = vi.fn().mockResolvedValue({
      items: [{ id: '100000000001', name: '字符串总数公司' }],
      total: '1',
    });
    const state = createSystemCrudState({ fetchList });

    await state.load();

    expect(state.total.value).toBe(1);
  });

  it('drops selected rows that are no longer present after loading table data', async () => {
    const fetchList = vi.fn().mockResolvedValue({
      items: [{ id: 1, name: '当前公司' }],
      total: 1,
    });
    const state = createSystemCrudState({ fetchList });

    state.selectRows([
      { id: 1, name: '当前公司' },
      { id: 2, name: '已过滤公司' },
    ]);

    await state.load();

    expect(state.selectedIds.value).toEqual([1]);
  });

  it('clears selected rows after submitting edits even when the row remains loaded', async () => {
    const fetchList = vi.fn().mockResolvedValue({
      items: [{ id: '2055946189550952450', name: '更新后公司' }],
      total: 1,
    });
    const updateRecord = vi.fn().mockResolvedValue(undefined);
    const state = createSystemCrudState({ fetchList, updateRecord });

    state.selectRows([
      { id: '2055946189550952450', name: '更新前公司' },
    ]);
    state.openEdit({ id: '2055946189550952450', name: '更新前公司' });
    await state.submit({ name: '更新后公司' });

    expect(state.selectedIds.value).toEqual([]);
  });

  it('shows company management data-map branches in one visible page', () => {
    const companyPage = readFileSync(
      appSourcePath('src/views/system-management/company-management/index.vue'),
      'utf8',
    );
    const crudPage = readFileSync(
      appSourcePath('src/views/system-management/shared/SystemCrudPage.vue'),
      'utf8',
    );

    expect(companyPage).toContain('defaultListParams: { pageSize: 100 }');
    expect(companyPage).toContain(':table-page-size="100"');
    expect(crudPage).toContain('afterMutation: refreshMasterDataAfterMutation');
    expect(crudPage).toContain('tablePageSize');
    expect(crudPage).toContain('pageSize: tablePageSize');
  });

  it('keeps system management tables vertically scrollable by mouse wheel', () => {
    expect(systemCrudTableScrollConfig).toMatchObject({
      y: 'calc(100vh - 360px)',
    });
  });

  it('derives horizontal table scroll width from visible columns', () => {
    expect(
      createSystemTableScrollConfig([
        { dataIndex: 'name', title: '名称', width: 112 },
        { dataIndex: 'status', title: '状态', width: 96 },
        { dataIndex: 'actions', fixed: 'right', title: '操作', width: 136 },
      ]),
    ).toMatchObject({
      x: 392,
      y: 'calc(100vh - 360px)',
    });
  });

  it('keeps the data map fixed by pruning department and team children', () => {
    expect(
      normalizeSystemDataMapNodes([
        {
          children: [
            {
              id: 33,
              key: '33',
              orgType: 'DEPARTMENT',
              title: '系统管理演示部门',
              children: [
                {
                  id: 34,
                  key: '34',
                  orgType: 'TEAM',
                  title: '系统管理演示班组',
                },
              ],
            },
          ],
          id: 8,
          key: '8',
          orgType: 'COMPANY',
          title: 'Demo East Site',
        },
      ]),
    ).toEqual([
      {
        children: [],
        id: 8,
        key: '8',
        orgType: 'COMPANY',
        title: 'Demo East Site',
      },
    ]);
  });

  it('preserves backend company-tree string ids in the system data map', () => {
    const unsafeId = '2055946189550952450';

    expect(
      normalizeSystemDataMapNodes([
        {
          children: [],
          companyType: '子公司',
          id: unsafeId,
          key: unsafeId,
          orgType: 'COMPANY',
          title: '浏览器大整数公司',
        },
      ]),
    ).toEqual([
      {
        children: [],
        companyType: '子公司',
        id: unsafeId,
        key: unsafeId,
        orgType: 'COMPANY',
        title: '浏览器大整数公司',
      },
    ]);
  });

  it('derives a legal subsidiary create payload from the selected data-map branch', () => {
    const result = normalizeCompanyPayloadForSubmit(
      {
        code: 'AUTO-SUB',
        name: '自动新增子公司',
        status: 'ACTIVE',
      },
      {
        mode: 'create',
        selectedPath: [
          {
            companyType: '集团',
            id: '1',
            key: '1',
            orgType: 'GROUP',
            title: 'Demo控股集团',
          },
          {
            companyType: '集团',
            id: '2',
            key: '2',
            orgType: 'GROUP',
            title: 'Demo Safety Holdings',
          },
          {
            companyType: '分公司',
            id: '3',
            key: '3',
            orgType: 'COMPANY',
            title: 'Demo Works Company',
          },
        ],
      },
    );

    expect(result).toEqual({
      data: {
        code: 'AUTO-SUB',
        companyType: '子公司',
        level1Name: 'Demo控股集团',
        level2Name: 'Demo Safety Holdings',
        level3Name: 'Demo Works Company',
        level4Name: '自动新增子公司',
        name: '自动新增子公司',
        status: 'ACTIVE',
      },
    });
  });

  it('hides auto-derived company hierarchy fields while creating a company', () => {
    const fields = [
      { label: '编码', valueKey: 'code' },
      { label: '名称', valueKey: 'name' },
      { label: '状态', valueKey: 'status' },
      { label: '公司类型', valueKey: 'companyType' },
      { label: '一级名称', valueKey: 'level1Name' },
      { label: '二级名称', valueKey: 'level2Name' },
      { label: '三级名称', valueKey: 'level3Name' },
      { label: '四级名称', valueKey: 'level4Name' },
      { label: '简称', valueKey: 'shortName' },
    ];

    expect(
      filterCompanyDrawerFields(fields, {
        companyHierarchyMode: true,
        editing: false,
      }).map((field) => field.valueKey),
    ).toEqual(['code', 'name', 'status', 'shortName']);

    expect(
      filterCompanyDrawerFields(fields, {
        companyHierarchyMode: true,
        editing: true,
      }).map((field) => field.valueKey),
    ).toEqual([
      'code',
      'name',
      'status',
      'companyType',
      'level1Name',
      'level2Name',
      'level3Name',
      'level4Name',
      'shortName',
    ]);
  });

  it('rejects creating another company below a fourth-level subsidiary', () => {
    const selectedPath = [
      {
        companyType: '集团',
        id: '1',
        key: '1',
        orgType: 'GROUP',
        title: 'Demo控股集团',
      },
      {
        companyType: '集团',
        id: '2',
        key: '2',
        orgType: 'GROUP',
        title: 'Demo Safety Holdings',
      },
      {
        companyType: '分公司',
        id: '3',
        key: '3',
        orgType: 'COMPANY',
        title: 'Demo Works Company',
      },
      {
        companyType: '子公司',
        id: '4',
        key: '4',
        orgType: 'COMPANY',
        title: 'Demo Works Company',
      },
    ];

    expect(shouldDisableCompanyCreateForPath(selectedPath)).toBe(true);
    expect(
      normalizeCompanyPayloadForSubmit(
        { code: 'CHILD', name: '错误层级', status: 'ACTIVE' },
        { mode: 'create', selectedPath },
      ),
    ).toEqual({
      error: '四级子公司下不能继续新增公司',
    });
  });

  it('derives department create payload from the selected company data-map branch', () => {
    const selectedPath = [
      {
        companyType: '集团',
        id: '1',
        key: '1',
        orgType: 'GROUP',
        title: 'Demo控股集团',
      },
      {
        companyType: '集团',
        id: '2',
        key: '2',
        orgType: 'GROUP',
        title: 'Demo Safety Holdings',
      },
      {
        companyType: '分公司',
        id: '3',
        key: '3',
        orgType: 'COMPANY',
        title: 'Demo Works Company',
      },
    ];

    expect(
      normalizeDepartmentPayloadForSubmit(
        {
          code: 'DEPT-AUTO',
          departmentType: '车间',
          name: '自动部门',
          status: 'ACTIVE',
        },
        { selectedPath },
      ),
    ).toEqual({
      data: {
        code: 'DEPT-AUTO',
        companyOrgId: '3',
        departmentType: '车间',
        groupName: 'Demo Safety Holdings',
        level1Unit: 'Demo Works Company',
        name: '自动部门',
        status: 'ACTIVE',
        topLevelName: 'Demo控股集团',
      },
    });
  });

  it('derives department create payload for a fourth-level subsidiary', () => {
    const selectedPath = [
      {
        companyType: '集团',
        id: '1',
        key: '1',
        orgType: 'GROUP',
        title: 'Demo控股集团',
      },
      {
        companyType: '集团',
        id: '2',
        key: '2',
        orgType: 'GROUP',
        title: 'Demo Safety Holdings',
      },
      {
        companyType: '分公司',
        id: '3',
        key: '3',
        orgType: 'COMPANY',
        title: 'Demo Works Company',
      },
      {
        companyType: '子公司',
        id: '4',
        key: '4',
        orgType: 'COMPANY',
        title: 'Demo Works Company',
      },
    ];

    expect(
      normalizeDepartmentPayloadForSubmit(
        { code: 'DEPT-SUB', name: '子公司部门', status: 'ACTIVE' },
        { selectedPath },
      ),
    ).toEqual({
      data: {
        code: 'DEPT-SUB',
        companyOrgId: '4',
        groupName: 'Demo Safety Holdings',
        level1Unit: 'Demo Works Company',
        level2Unit: 'Demo Works Company',
        name: '子公司部门',
        status: 'ACTIVE',
        topLevelName: 'Demo控股集团',
      },
    });
  });

  it('uses the deepest company node from current user orgPath when no data-map node is selected', () => {
    const nodes = normalizeSystemDataMapNodes([
      {
        children: [
          {
            children: [
              {
                children: [
                  {
                    children: [],
                    companyType: '子公司',
                    id: '4',
                    key: '4',
                    orgType: 'COMPANY',
                    title: 'Demo Works Company',
                  },
                ],
                companyType: '分公司',
                id: '3',
                key: '3',
                orgType: 'COMPANY',
                title: 'Demo Works Company',
              },
            ],
            companyType: '集团',
            id: '2',
            key: '2',
            orgType: 'GROUP',
            title: 'Demo Safety Holdings',
          },
        ],
        companyType: '集团',
        id: '1',
        key: '1',
        orgType: 'GROUP',
        title: 'Demo控股集团',
      },
    ]);

    expect(
      findDefaultDepartmentCompanyPath(nodes, '/1/2/3/4/101109/1011001/').map(
        (node) => node.id,
      ),
    ).toEqual(['1', '2', '3', '4']);
  });

  it('hides auto-derived department company fields while creating a department', () => {
    const fields = [
      { label: '编码', valueKey: 'code' },
      { label: '名称', valueKey: 'name' },
      { label: '所属公司', valueKey: 'companyOrgId' },
      { label: '状态', valueKey: 'status' },
      { label: '顶级', valueKey: 'topLevelName' },
      { label: '集团', valueKey: 'groupName' },
      { label: '一级单位', valueKey: 'level1Unit' },
      { label: '二级单位', valueKey: 'level2Unit' },
      { label: '公司排序', valueKey: 'companySortOrder' },
      { label: '类型', valueKey: 'departmentType' },
    ];

    expect(
      filterDepartmentDrawerFields(fields, {
        departmentHierarchyMode: true,
        editing: false,
      }).map((field) => field.valueKey),
    ).toEqual(['code', 'name', 'status', 'departmentType']);

    expect(
      filterDepartmentDrawerFields(fields, {
        departmentHierarchyMode: true,
        editing: true,
      }).map((field) => field.valueKey),
    ).toContain('companyOrgId');
  });

  it('derives visible team create fields from the selected company branch', () => {
    const selectedPath = [
      {
        companyType: '集团',
        id: '1',
        key: '1',
        orgType: 'GROUP',
        title: 'Demo控股集团',
      },
      {
        companyType: '集团',
        id: '2',
        key: '2',
        orgType: 'GROUP',
        title: 'Demo Safety Holdings',
      },
      {
        companyType: '分公司',
        id: '3',
        key: '3',
        orgType: 'COMPANY',
        title: 'Demo Works Company',
      },
      {
        companyType: '子公司',
        id: '4',
        key: '4',
        orgType: 'COMPANY',
        title: 'Demo Works Company',
      },
    ];

    expect(
      normalizeTeamPayloadForSubmit(
        {
          applicantName: '管理员',
          code: 'TEAM-AUTO',
          name: '自动班组',
          status: 'ACTIVE',
          submitDate: '2026-05-17',
          workshopName: '制氧车间',
          workshopOrgId: '101109',
          workTypeName: '检修作业',
        },
        { selectedPath },
      ),
    ).toEqual({
      data: {
        applicantName: '管理员',
        code: 'TEAM-AUTO',
        companyOrgId: '4',
        companyName: 'Demo Works Company',
        groupName: 'Demo Safety Holdings',
        level1Unit: 'Demo Works Company',
        level2Unit: 'Demo Works Company',
        name: '自动班组',
        status: 'ACTIVE',
        submitDate: '2026-05-17',
        workshopName: '制氧车间',
        workshopOrgId: '101109',
        workTypeName: '检修作业',
      },
    });
  });

  it('defaults team submit date and applicant from the current user', () => {
    expect(
      createTeamDefaultValues({
        today: '2026-05-17',
        userInfo: { realName: '系统管理员', username: 'admin' },
      }),
    ).toEqual({
      applicantName: '系统管理员',
      submitDate: '2026-05-17',
    });
  });

  it('derives personnel create payload from the selected company branch', () => {
    const selectedPath = [
      {
        companyType: '集团',
        id: '1',
        key: '1',
        orgType: 'GROUP',
        title: 'Demo控股集团',
      },
      {
        companyType: '集团',
        id: '2',
        key: '2',
        orgType: 'GROUP',
        title: 'Demo Safety Holdings',
      },
      {
        companyType: '分公司',
        id: '3',
        key: '3',
        orgType: 'COMPANY',
        title: 'Demo Works Company',
      },
      {
        companyType: '子公司',
        id: '4',
        key: '4',
        orgType: 'COMPANY',
        title: 'Demo Works Company',
      },
    ];

    expect(
      normalizePersonnelPayloadForSubmit(
        {
          applicantName: '管理员',
          departmentOrgId: '101109',
          joinDate: '2026-05-18',
          name: '新增人员',
          positionName: '工长',
          remark: '备注',
          status: 'ACTIVE',
          submitDate: '2026-05-18',
          systemRoleCode: '工长',
          teamOrgId: '1011001',
        },
        { selectedPath },
      ),
    ).toEqual({
      data: {
        applicantName: '管理员',
        companyName: 'Demo Works Company',
        companyOrgId: '4',
        departmentOrgId: '101109',
        joinDate: '2026-05-18',
        name: '新增人员',
        positionName: '工长',
        remark: '备注',
        status: 'ACTIVE',
        submitDate: '2026-05-18',
        systemRoleCode: '工长',
        teamOrgId: '1011001',
      },
    });
  });

  it('defaults personnel submit date and applicant from the current user', () => {
    expect(
      createPersonnelDefaultValues({
        today: '2026-05-18',
        userInfo: { realName: '系统管理员', username: 'admin' },
      }),
    ).toEqual({
      applicantName: '系统管理员',
      submitDate: '2026-05-18',
    });
  });

  it('keeps team management wired to hierarchy mode and management-only fields', () => {
    const teamPage = readFileSync(
      appSourcePath('src/views/system-management/team-management/index.vue'),
      'utf8',
    );
    const crudPage = readFileSync(
      appSourcePath('src/views/system-management/shared/SystemCrudPage.vue'),
      'utf8',
    );
    const drawer = readFileSync(
      appSourcePath('src/views/system-management/shared/SystemUpsertDrawer.vue'),
      'utf8',
    );

    expect(teamPage).toContain('getTeamManagementFields');
    expect(teamPage).toContain(':team-hierarchy-mode="true"');
    expect(crudPage).toContain('normalizeTeamPayloadForSubmit');
    expect(crudPage).toContain('createTeamDefaultValues');
    expect(drawer).toContain(':disabled="field.readonly"');
  });

  it('keeps personnel management wired to hierarchy mode and management-only fields', () => {
    const personnelPage = readFileSync(
      appSourcePath('src/views/system-management/personnel-management/index.vue'),
      'utf8',
    );
    const crudPage = readFileSync(
      appSourcePath('src/views/system-management/shared/SystemCrudPage.vue'),
      'utf8',
    );

    expect(personnelPage).toContain('getPersonnelManagementFields');
    expect(personnelPage).toContain(':personnel-hierarchy-mode="true"');
    expect(crudPage).toContain('normalizePersonnelPayloadForSubmit');
    expect(crudPage).toContain('createPersonnelDefaultValues');
    expect(crudPage).toContain('loadPersonnelSelectOptions');
  });

  it('does not reset open drawer form values when select options refresh', () => {
    const drawer = readFileSync(
      appSourcePath('src/views/system-management/shared/SystemUpsertDrawer.vue'),
      'utf8',
    );

    expect(drawer).toContain(
      '() => [props.modelValue, props.record] as const',
    );
    expect(drawer).not.toContain('props.fields] as const');
  });

  it('rebuilds select controls when dependent options change', () => {
    const drawer = readFileSync(
      appSourcePath('src/views/system-management/shared/SystemUpsertDrawer.vue'),
      'utf8',
    );
    const crudPage = readFileSync(
      appSourcePath('src/views/system-management/shared/SystemCrudPage.vue'),
      'utf8',
    );

    expect(drawer).toContain('function fieldOptionsRenderKey');
    expect(drawer).toContain(':key="fieldOptionsRenderKey(field)"');
    const fieldChangeHandler = crudPage.slice(
      crudPage.indexOf('async function handleDrawerFieldChange'),
    );
    expect(
      fieldChangeHandler.indexOf('formState.teamOrgId = undefined'),
    ).toBeLessThan(
      fieldChangeHandler.indexOf('await getTeamOptionsApi'),
    );
  });

  it('renders wide searchable tree-select fields in upsert drawers', () => {
    const drawer = readFileSync(
      appSourcePath('src/views/system-management/shared/SystemUpsertDrawer.vue'),
      'utf8',
    );

    expect(drawer).toContain('TreeSelect');
    expect(drawer).toContain("field.component === 'tree-select'");
    expect(drawer).toContain(':dropdown-match-select-width="field.dropdownMatchSelectWidth"');
    expect(drawer).toContain(':list-height="field.listHeight"');
    expect(drawer).toContain(':tree-default-expand-all="field.treeDefaultExpandAll"');
  });

  it('keeps newly created group and company nodes in the system data map', () => {
    expect(
      normalizeSystemDataMapNodes([
        {
          children: [
            {
              children: [
                {
                  id: 901,
                  key: '901',
                  orgType: 'COMPANY',
                  title: '新增子公司',
                },
              ],
              id: 900,
              key: '900',
              orgType: 'GROUP',
              title: '新增集团',
            },
          ],
          id: 1,
          key: '1',
          orgType: 'GROUP',
          title: 'Demo控股集团',
        },
      ]),
    ).toEqual([
      {
        children: [
          {
            children: [
              {
                children: [],
                id: 901,
                key: '901',
                orgType: 'COMPANY',
                title: '新增子公司',
              },
            ],
            id: 900,
            key: '900',
            orgType: 'GROUP',
            title: '新增集团',
          },
        ],
        id: 1,
        key: '1',
        orgType: 'GROUP',
        title: 'Demo控股集团',
      },
    ]);
  });

  it('normalizes the backend company-tree without special-casing Guangsheng Yuancheng', () => {
    expect(
      normalizeSystemDataMapNodes([
        {
          children: [
            {
              children: [
                {
                  id: 101109,
                  key: '101109',
                  orgType: 'DEPARTMENT',
                  title: '幕墙组装',
                },
                {
                  id: 999,
                  key: '999',
                  orgType: 'COMPANY',
                  title: '新增子公司',
                },
              ],
              id: 4,
              key: '4',
              orgType: 'COMPANY',
              title: 'Demo Works Company',
            },
          ],
          id: 3,
          key: '3',
          orgType: 'COMPANY',
          title: 'Demo Works Company',
        },
      ]),
    ).toEqual([
      {
        children: [
          {
            children: [
              {
                children: [],
                id: 999,
                key: '999',
                orgType: 'COMPANY',
                title: '新增子公司',
              },
            ],
            id: 4,
            key: '4',
            orgType: 'COMPANY',
            title: 'Demo Works Company',
          },
        ],
        id: 3,
        key: '3',
        orgType: 'COMPANY',
        title: 'Demo Works Company',
      },
    ]);
  });

  it('keeps backend company-tree parentage instead of moving Guangsheng Yuancheng by name', () => {
    expect(
      normalizeSystemDataMapNodes([
        {
          children: [
            {
              children: [],
              id: 3,
              key: '3',
              orgType: 'COMPANY',
              title: 'Demo Works Company',
            },
            {
              children: [
                {
                  id: 101109,
                  key: '101109',
                  orgType: 'DEPARTMENT',
                  title: '幕墙组装',
                },
              ],
              id: 4,
              key: '4',
              orgType: 'COMPANY',
              title: 'Demo Works Company',
            },
          ],
          id: 2,
          key: '2',
          orgType: 'GROUP',
          title: 'Demo Safety Holdings',
        },
      ]),
    ).toEqual([
      {
        children: [
          {
            children: [],
            id: 3,
            key: '3',
            orgType: 'COMPANY',
            title: 'Demo Works Company',
          },
          {
            children: [],
            id: 4,
            key: '4',
            orgType: 'COMPANY',
            title: 'Demo Works Company',
          },
        ],
        id: 2,
        key: '2',
        orgType: 'GROUP',
        title: 'Demo Safety Holdings',
      },
    ]);
  });

  it('does not inject Guangsheng Yuancheng when the backend company-tree omits it', () => {
    expect(
      normalizeSystemDataMapNodes([
        {
          children: [
            {
              children: [],
              id: 3,
              key: '3',
              orgType: 'COMPANY',
              title: 'Demo Works Company',
            },
          ],
          id: 2,
          key: '2',
          orgType: 'GROUP',
          title: 'Demo Safety Holdings',
        },
      ]),
    ).toEqual([
      {
        children: [
          {
            children: [],
            id: 3,
            key: '3',
            orgType: 'COMPANY',
            title: 'Demo Works Company',
          },
        ],
        id: 2,
        key: '2',
        orgType: 'GROUP',
        title: 'Demo Safety Holdings',
      },
    ]);
  });

  it('calls configured import and export actions from toolbar handlers', async () => {
    const downloadTemplate = vi.fn();
    const downloadData = vi.fn();
    const uploadData = vi.fn();
    const state = createSystemCrudState({
      downloadData,
      downloadTemplate,
      fetchList: vi.fn(),
      uploadData,
    });
    const file = new File(['code'], 'company.xlsx');

    await state.handleDownloadTemplate();
    await state.handleDownloadData();
    await state.handleUploadData(file);

    expect(downloadTemplate).toHaveBeenCalledTimes(1);
    expect(downloadData).toHaveBeenCalledWith({});
    expect(uploadData).toHaveBeenCalledWith(file);
  });

  it('keeps table configuration visible for team management', () => {
    const teamPage = readFileSync(
      appSourcePath('src/views/system-management/team-management/index.vue'),
      'utf8',
    );
    const crudPage = readFileSync(
      appSourcePath('src/views/system-management/shared/SystemCrudPage.vue'),
      'utf8',
    );
    const toolbar = readFileSync(
      appSourcePath('src/views/system-management/shared/SystemToolbar.vue'),
      'utf8',
    );

    expect(teamPage).toContain('table-config-module="team"');
    expect(crudPage).toContain(
      ':show-table-config="Boolean(props.tableConfigModule)"',
    );
    expect(toolbar).toContain('v-if="showTableConfig"');
    expect(toolbar).not.toContain("isVisible('tableConfig')");
  });

  it('merges the original record when submitting edits', async () => {
    const fetchList = vi.fn().mockResolvedValue({ items: [], total: 0 });
    const updateRecord = vi.fn();
    const state = createSystemCrudState({ fetchList, updateRecord });

    state.openEdit({
      id: 12,
      name: '旧班组',
      workshopName: '维修车间',
      workTypeCode: 'REPAIR',
    });
    await state.submit({ name: '新班组' });

    expect(updateRecord).toHaveBeenCalledWith(12, {
      id: 12,
      name: '新班组',
      workshopName: '维修车间',
      workTypeCode: 'REPAIR',
    });
  });
});

