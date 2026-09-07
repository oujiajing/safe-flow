import { describe, expect, it } from 'vitest';

import {
  collectExpandableOrganizationKeys,
  defaultExpandedOrganizationKeys,
  applyOrganizationFilterCompany,
  applyOrganizationFilterDepartment,
  applyOrganizationFilterFromDataMapSelection,
  applyOrganizationFilterTeam,
  type AttachmentPreviewDetail,
  createOrganizationFilterParams,
  createOrganizationFilterState,
  filterMeetingRows,
  getThreeCheckTemplateRows,
  flattenOrganizationOptions,
  getCascadedOrganizationOptions,
  getOrganizationIdByKey,
  meetingRows,
  type OrganizationNode,
  organizationTree,
  resolveCreateOrganizationDefaults,
  resolveAttachmentPreviewUrl,
  statusMeta,
  statusOptions,
  upsertAttachmentPreview,
} from './pre-shift-meeting.data';
import {
  oneShiftThreeCheckRouteNames,
  resolvePreShiftMeetingRuntime,
} from './pre-shift-meeting.module';

function flattenTreeTitles(nodes: OrganizationNode[]): string[] {
  return nodes.flatMap((node) => [
    node.title,
    ...flattenTreeTitles(node.children ?? []),
  ]);
}

describe('pre-shift-meeting data', () => {
  it('exposes the Guangsheng organization tree used by the data map', () => {
    expect(organizationTree[0]?.title).toBe('Demo控股集团');
    expect(organizationTree[0]?.children?.[0]?.title).toBe('Demo Safety Holdings');
  });

  it('contains the data-map organizations shown in the first prototype page', () => {
    const titles = flattenTreeTitles(organizationTree);

    expect(titles).toEqual(
      expect.arrayContaining([
        'Demo控股集团',
        'Demo Safety Holdings',
        'Demo Works Company',
        'Demo Works Company',
        'Demo矿投',
        'Demo East Site',
        'Demo North Site',
        'Demo Industrial',
        'Demo Coastal Site',
        'Demo Metallurgy Company',
        'Demo Harvest Site',
        'Demo Storage',
        'Demo Transport',
        'Demo South Depot',
        'Demo East Depot',
        'Demo Metallurgy',
        'Demo Development',
        'Demo Site',
        'Demo Company',
        'Demo Spring Site',
        'Demo Gold Group',
        'Demo Training School',
        'Demo Materials',
        '高力公司',
        'Demo Warehouse',
        'Demo Works Subsidiary',
        'Demo Pacific Company',
      ]),
    );
    expect(titles).not.toEqual(
      expect.arrayContaining([
        'Demo Works Company',
        'Demo HarborA6 / 4000-MQXM001-001',
        '制氧车间',
        '早班',
      ]),
    );
  });

  it('derives expandable organization keys from the active data map tree', () => {
    const expandableKeys = collectExpandableOrganizationKeys(organizationTree);

    expect(expandableKeys).toEqual(
      expect.arrayContaining([
        'group-guangsheng',
        'group-mining',
        'company-muqian',
        'company-mining-invest',
        'company-south-storage',
        'company-guangsheng-metallurgy',
        'company-yaoling',
        'company-new-material',
      ]),
    );
    expect(expandableKeys).not.toContain('company-yuancheng');
    expect(expandableKeys).not.toContain('company-meizhou-jiasheng');
    expect(defaultExpandedOrganizationKeys).toEqual(expandableKeys);
  });

  it('keeps Guangsheng Yuancheng as a fixed data-map leaf while preserving its id', () => {
    const yuancheng = organizationTree[0]?.children?.[0]?.children?.[0]?.children?.[0];

    expect(yuancheng).toMatchObject({
      id: 4,
      key: 'company-yuancheng',
      title: 'Demo Works Company',
    });
    expect(yuancheng?.children ?? []).toEqual([]);
    expect(getOrganizationIdByKey(organizationTree, 'company-yuancheng')).toBe(4);
  });

  it('filters rows by company keyword, status, and meeting date range', () => {
    const rows = filterMeetingRows(meetingRows, {
      company: 'Demo Works Company',
      dateRange: ['2025-04-01', '2025-04-30'],
      status: 'OPENED',
    });

    expect(rows).toHaveLength(1);
    expect(rows[0]).toMatchObject({
      department: 'Demo Works Company',
      team: 'Demo Works Company',
    });
  });

  it('labels draft meetings as not opened in the status controls', () => {
    expect(statusOptions).toEqual(
      expect.arrayContaining([{ label: '待开会议', value: 'DRAFT' }]),
    );
    expect(statusMeta.DRAFT.label).toBe('待开会议');
    expect(statusMeta.OPENED.label).toBe('已开会议');
  });

  it('flattens organization nodes for selectable form fields', () => {
    const options = flattenOrganizationOptions(organizationTree);

    expect(options).toEqual(
      expect.arrayContaining([
        expect.objectContaining({ label: 'Demo Works Company', value: 4 }),
        expect.objectContaining({ label: 'Demo East Site', value: 8 }),
      ]),
    );
  });

  it('limits cascading form options to the matching organization level', () => {
    const nodes: OrganizationNode[] = [
      {
        id: 1,
        key: 'company',
        orgType: 'COMPANY',
        title: '样例公司',
        children: [
          {
            id: 2,
            key: 'department',
            orgType: 'DEPARTMENT',
            title: '样例部门',
            children: [
              {
                id: 3,
                key: 'team',
                orgType: 'TEAM',
                title: '样例班组',
              },
            ],
          },
          {
            id: 4,
            key: 'other-company',
            orgType: 'COMPANY',
            title: '下级公司',
          },
        ],
      },
    ];

    expect(
      getCascadedOrganizationOptions(nodes, undefined, ['COMPANY']),
    ).toEqual([
      expect.objectContaining({ label: '样例公司', value: 1 }),
      expect.objectContaining({ label: '下级公司', value: 4 }),
    ]);
    expect(getCascadedOrganizationOptions(nodes, 1, ['DEPARTMENT'])).toEqual([
      expect.objectContaining({ label: '样例部门', value: 2 }),
    ]);
    expect(getCascadedOrganizationOptions(nodes, 2, ['TEAM'])).toEqual([
      expect.objectContaining({ label: '样例班组', value: 3 }),
    ]);
    expect(getCascadedOrganizationOptions(nodes, 1, ['TEAM'])).toEqual([
      expect.objectContaining({ label: '样例班组', value: 3 }),
    ]);
  });

  it('builds cascading organization filter ids from system organization nodes', () => {
    const nodes: OrganizationNode[] = [
      {
        id: 4,
        key: '4',
        orgType: 'COMPANY',
        title: 'Demo Works Company',
        children: [
          {
            id: 101_109,
            key: '101109',
            orgType: 'DEPARTMENT',
            title: '幕墙组装',
            children: [
              {
                id: 1_011_001,
                key: '1011001',
                orgType: 'TEAM',
                title: '幕墙组装1班',
              },
            ],
          },
        ],
      },
    ];
    const state = createOrganizationFilterState();

    applyOrganizationFilterCompany(state, nodes, 4);
    expect(state).toMatchObject({
      company: 'Demo Works Company',
      companyId: 4,
    });
    expect(state.department).toBe('');
    expect(state.departmentId).toBeUndefined();

    applyOrganizationFilterDepartment(state, nodes, 101_109);
    applyOrganizationFilterTeam(state, nodes, 1_011_001);

    expect(createOrganizationFilterParams(state)).toEqual({
      company: 'Demo Works Company',
      companyId: 4,
      department: '幕墙组装',
      departmentId: 101_109,
      team: '幕墙组装1班',
      teamId: 1_011_001,
    });

    applyOrganizationFilterCompany(state, nodes, undefined);
    expect(createOrganizationFilterParams(state)).toEqual({});
  });

  it('syncs company filters from company data-map clicks', () => {
    const state = createOrganizationFilterState();
    state.company = '旧公司';
    state.companyId = 4;
    state.department = '旧部门';
    state.departmentId = 101_109;
    state.team = '旧班组';
    state.teamId = 1_011_001;
    const systemNodes: OrganizationNode[] = [
      {
        id: 4,
        key: '4',
        orgType: 'COMPANY',
        title: 'Demo Works Company',
        children: [
          {
            id: 101_109,
            key: '101109',
            orgType: 'DEPARTMENT',
            title: '幕墙组装',
            children: [
              {
                id: 1_011_001,
                key: '1011001',
                orgType: 'TEAM',
                title: '幕墙组装1班',
              },
            ],
          },
        ],
      },
    ];
    const yuancheng =
      organizationTree[0]?.children?.[0]?.children?.[0]?.children?.[0];

    expect(yuancheng).toMatchObject({
      key: 'company-yuancheng',
      title: 'Demo Works Company',
    });
    expect(yuancheng?.children ?? []).toEqual([]);

    applyOrganizationFilterFromDataMapSelection(
      state,
      organizationTree,
      systemNodes,
      'company-yuancheng',
    );

    expect(createOrganizationFilterParams(state)).toEqual({
      company: 'Demo Works Company',
      companyId: 4,
    });
  });

  it('syncs cascaded filters from department and team data-map clicks', () => {
    const state = createOrganizationFilterState();
    const nodes: OrganizationNode[] = [
      {
        id: 4,
        key: '4',
        orgType: 'COMPANY',
        title: 'Demo Works Company',
        children: [
          {
            id: 101_109,
            key: '101109',
            orgType: 'DEPARTMENT',
            title: '幕墙组装',
            children: [
              {
                id: 1_011_001,
                key: '1011001',
                orgType: 'TEAM',
                title: '幕墙组装1班',
              },
            ],
          },
        ],
      },
    ];

    applyOrganizationFilterFromDataMapSelection(
      state,
      nodes,
      nodes,
      '101109',
    );

    expect(createOrganizationFilterParams(state)).toEqual({
      company: 'Demo Works Company',
      companyId: 4,
      department: '幕墙组装',
      departmentId: 101_109,
    });

    applyOrganizationFilterFromDataMapSelection(
      state,
      nodes,
      nodes,
      '1011001',
    );

    expect(createOrganizationFilterParams(state)).toEqual({
      company: 'Demo Works Company',
      companyId: 4,
      department: '幕墙组装',
      departmentId: 101_109,
      team: '幕墙组装1班',
      teamId: 1_011_001,
    });
  });

  it('resolves create organization defaults from current filters', () => {
    const nodes: OrganizationNode[] = [
      {
        id: 4,
        key: '4',
        orgType: 'COMPANY',
        title: 'Demo Works Company',
        children: [
          {
            id: 101_109,
            key: '101109',
            orgType: 'DEPARTMENT',
            title: '幕墙组装',
            children: [
              {
                id: 1_011_001,
                key: '1011001',
                orgType: 'TEAM',
                title: '幕墙组装1班',
              },
            ],
          },
        ],
      },
    ];
    const filters = createOrganizationFilterState();
    filters.companyId = 4;
    filters.departmentId = 101_109;
    filters.teamId = 1_011_001;

    expect(resolveCreateOrganizationDefaults(filters, nodes, 8)).toEqual({
      companyId: 4,
      departmentId: 101_109,
      teamId: 1_011_001,
    });
  });

  it('uses the current account organization as create fallback before the first company option', () => {
    const nodes: OrganizationNode[] = [
      {
        id: 4,
        key: '4',
        orgType: 'COMPANY',
        title: 'Demo Works Company',
        children: [
          {
            id: 101_109,
            key: '101109',
            orgType: 'DEPARTMENT',
            title: '幕墙组装',
          },
        ],
      },
      {
        id: 8,
        key: '8',
        orgType: 'COMPANY',
        title: 'Demo East Site',
        children: [
          {
            id: 9,
            key: '9',
            orgType: 'DEPARTMENT',
            title: '制氧车间',
            children: [
              {
                id: 10,
                key: '10',
                orgType: 'TEAM',
                title: '制氧班组',
              },
            ],
          },
        ],
      },
    ];

    expect(resolveCreateOrganizationDefaults(createOrganizationFilterState(), nodes, 10)).toEqual({
      companyId: 8,
      departmentId: 9,
      teamId: 10,
    });
  });

  it('finds organization ids serialized as strings by the backend', () => {
    const nodes = [
      {
        id: '100000000001',
        key: '100000000001',
        orgType: 'GROUP',
        title: '字符串编号集团',
        children: [
          {
            id: '100000000003',
            key: '100000000003',
            orgType: 'COMPANY',
            title: '字符串编号公司',
          },
        ],
      },
    ] as unknown as OrganizationNode[];

    expect(getOrganizationIdByKey(nodes, '100000000003')).toBe(
      '100000000003',
    );
  });

  it('normalizes uploaded attachment paths for preview controls', () => {
    expect(resolveAttachmentPreviewUrl('pre-shift-meeting/1/image/a.jpg')).toBe(
      '/uploads/pre-shift-meeting/1/image/a.jpg',
    );
    expect(resolveAttachmentPreviewUrl('/uploads/pre-shift-meeting/1/image/a.jpg')).toBe(
      '/uploads/pre-shift-meeting/1/image/a.jpg',
    );
    expect(
      resolveAttachmentPreviewUrl(
        'http://localhost:9000/safeteam-portfolio/pre-shift-meeting/1/image/a.jpg?X-Amz-Signature=test',
      ),
    ).toBe(
      'http://localhost:9000/safeteam-portfolio/pre-shift-meeting/1/image/a.jpg?X-Amz-Signature=test',
    );
    expect(
      resolveAttachmentPreviewUrl(
        'MINIO|safeteam-portfolio|pre-shift-meeting/1/image/a.jpg|etag-001',
      ),
    ).toBe('/uploads/pre-shift-meeting/1/image/a.jpg');
    expect(resolveAttachmentPreviewUrl()).toBe('');
  });

  it('adds the uploaded image or video to the open detail before the refetch returns', () => {
    const detail: AttachmentPreviewDetail<any> = {
      attachments: [],
      imageCheck: '未上传',
      videoCheck: '未上传',
    };

    const withImage = upsertAttachmentPreview(detail, {
      contentType: 'image/jpeg',
      fileKind: 'IMAGE',
      fileSize: 128,
      id: 'att-1',
      originalName: '现场.jpg',
      storagePath: 'MINIO|safeteam-portfolio|pre-shift-meeting/1/image/a.jpg|etag',
      url: '',
    });
    const withVideo = upsertAttachmentPreview(withImage, {
      contentType: 'video/mp4',
      fileKind: 'VIDEO',
      fileSize: 256,
      id: 'att-2',
      originalName: '现场.mp4',
      storagePath: 'pre-shift-meeting/1/video/a.mp4',
      url: '',
    });

    expect(withVideo.imageCheck).toBe('现场照片');
    expect(withVideo.videoCheck).toBe('视频已传');
    expect(withVideo.attachments).toHaveLength(2);
    expect(withVideo.attachments[0]?.url).toBe(
      '/uploads/pre-shift-meeting/1/image/a.jpg',
    );
    expect(withVideo.attachments[1]?.url).toBe(
      '/uploads/pre-shift-meeting/1/video/a.mp4',
    );
  });

  it('provides ten unique mock rows for the prototype table', () => {
    expect(meetingRows).toHaveLength(10);
    expect(new Set(meetingRows.map((item) => item.id)).size).toBe(10);
  });

  it('provides at least two default verification rows for every one-shift-three-check module', () => {
    for (const routeName of oneShiftThreeCheckRouteNames) {
      const runtime = resolvePreShiftMeetingRuntime(routeName, routeName);
      const rows = getThreeCheckTemplateRows(routeName);
      const visibleRows = filterMeetingRows(rows, {
        company: 'Demo Works Company',
        status: 'all',
      });
      const fieldNames = runtime.columns.map((column) => column.dataIndex);

      expect(visibleRows.length, routeName).toBeGreaterThanOrEqual(2);
      for (const row of visibleRows.slice(0, 2)) {
        expect(row.id, routeName).toBeTruthy();
        expect(row.organizationPath, routeName).toContain('company-yuancheng');
        for (const fieldName of fieldNames) {
          if (fieldName !== 'actions') {
            expect(row, `${routeName}.${fieldName}`).toHaveProperty(fieldName);
          }
        }
      }
    }
  });

  it('uses the requested status labels in one-shift-three-check verification rows', () => {
    expect(
      getThreeCheckTemplateRows('PinganTeamDispatch').map(
        (row) => row.dispatchStatus,
      ),
    ).toEqual(['生效', '不生效']);
    expect(
      getThreeCheckTemplateRows('PinganCurtainWallTeamDispatch').map((row) => [
        row.dispatchStatus,
        row.dispatchType,
      ]),
    ).toEqual([
      ['生效', '今日'],
      ['不生效', '明日'],
    ]);
    expect(
      getThreeCheckTemplateRows('PinganPreShiftSafetyActivity').map(
        (row) => row.statusLabel,
      ),
    ).toEqual(['待开会议', '已开会议']);
    expect(
      getThreeCheckTemplateRows('PinganPreShiftInspection').map(
        (row) => row.statusLabel,
      ),
    ).toEqual(['待检查', '已检查']);
    expect(
      getThreeCheckTemplateRows('PinganMidShiftInspection').map(
        (row) => row.statusLabel,
      ),
    ).toEqual(['待检查', '已检查']);
    expect(
      getThreeCheckTemplateRows('PinganPostShiftInspection').map(
        (row) => row.statusLabel,
      ),
    ).toEqual(['待检查', '已检查']);
    expect(
      getThreeCheckTemplateRows('PinganKeySites').map((row) => row.statusLabel),
    ).toEqual(['待检查', '已验收']);
  });

  it('returns rows matching the selected team node', () => {
    const rows = filterMeetingRows(meetingRows, {
      organizationKey: 'company-yuancheng',
    });

    expect(rows.map((item) => item.company)).toContain('Demo Works Company');
  });
});
