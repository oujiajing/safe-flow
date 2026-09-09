import { describe, expect, it } from 'vitest';

import {
  MASTER_DATA_TEMPLATE_FILENAMES,
  MASTER_DATA_TEMPLATE_HEADERS,
  getMasterDataColumns,
  getMasterDataFields,
  getPersonnelManagementFields,
  getTeamManagementFields,
} from './masterDataTemplates';

describe('system master data template contract', () => {
  it('keeps fixed import headers from the official templates', () => {
    expect(MASTER_DATA_TEMPLATE_HEADERS).toEqual({
      company: [
        '编码',
        '排序',
        '名称',
        '简称',
        '描述',
        '状态',
        '地址',
        '公司类型',
        '一级',
        '二级',
        '三级',
        '四级',
        '安全管理员',
        '一级上报人',
        '二级上报人',
        '三级上报人',
        'L1上报时间',
        'L2上报时间',
        'L3上报时间',
        '附属文件1',
        '附属文件2',
        '公司介绍',
      ],
      department: [
        '编码',
        '名称',
        '所属公司',
        '类型',
        '子排序',
        '负责人',
        '描述',
        '状态',
        '顶级',
        '集团',
        '一级单位',
        '二级单位',
        '领导级别',
        '公司排序',
      ],
      personnel: [
        '编码',
        '姓名',
        '所属公司',
        '企业简称',
        '所属部门',
        '所属班组',
        '积分',
        '领用积分',
        '员工类型',
        '岗位',
        '手机号',
        '状态',
        '系统角色',
        '部门排序',
        '管理权重',
      ],
      team: [
        '名称',
        '公司',
        '集团',
        '一级单位',
        '二级单位',
        '车间',
        '班组作业',
        '状态',
        '班组长',
        '班组成员',
        '安全员',
        '积分',
        '编码',
        '活跃',
      ],
    });
  });

  it('uses business template filenames and matching table titles', () => {
    expect(MASTER_DATA_TEMPLATE_FILENAMES).toEqual({
      company: '公司管理_导入模板.xlsx',
      department: '部门管理_导入模板.xlsx',
      personnel: '人员管理_导入模板.xlsx',
      team: '班组管理_导入模板.xlsx',
    });

    expect(getMasterDataColumns('company').map((column) => column.title)).toEqual(
      MASTER_DATA_TEMPLATE_HEADERS.company.filter((title) => title !== '排序'),
    );
    expect(
      getMasterDataColumns('department').map((column) => column.title),
    ).toEqual(MASTER_DATA_TEMPLATE_HEADERS.department);
    expect(getMasterDataColumns('team').map((column) => column.title)).toEqual(
      MASTER_DATA_TEMPLATE_HEADERS.team,
    );
    expect(
      getMasterDataColumns('personnel').map((column) => column.title),
    ).toEqual(MASTER_DATA_TEMPLATE_HEADERS.personnel);
  });

  it('keeps left fixed table columns contiguous for Ant Table layout', () => {
    expect(
      getMasterDataColumns('company')
        .filter((column) => column.fixed === 'left')
        .map((column) => column.title),
    ).toEqual(['编码', '名称']);
    expect(
      getMasterDataColumns('personnel')
        .filter((column) => column.fixed === 'left')
        .map((column) => column.title),
    ).toEqual(['编码', '姓名']);
    expect(
      getMasterDataColumns('team')
        .filter((column) => column.fixed === 'left')
        .map((column) => column.title),
    ).toEqual(['名称']);
  });

  it('keeps team import headers stable while adding management-only fields', () => {
    expect(MASTER_DATA_TEMPLATE_HEADERS.team).not.toContain('提交日期');
    expect(MASTER_DATA_TEMPLATE_HEADERS.team).not.toContain('申请人');
    expect(MASTER_DATA_TEMPLATE_HEADERS.team).not.toContain('部门');

    const fields = getTeamManagementFields([
      { label: '启用', value: 'ACTIVE' },
      { label: '草稿', value: 'DRAFT' },
      { label: '停用', value: 'INACTIVE' },
    ]);

    expect(fields.map((field) => field.label)).toEqual([
      '公司',
      '部门',
      '车间',
      '编码',
      '名称',
      '集团',
      '一级单位',
      '二级单位',
      '班组作业',
      '状态',
      '班组长',
      '班组成员',
      '安全员',
      '提交日期',
      '申请人',
    ]);
    expect(
      fields
        .filter((field) => field.readonly)
        .map((field) => field.label),
    ).toEqual(['公司', '集团', '一级单位', '二级单位']);
    expect(fields.find((field) => field.label === '部门')).toMatchObject({
      component: 'select',
      valueKey: 'workshopOrgId',
    });
    expect(fields.find((field) => field.label === '车间')).toMatchObject({
      placeholder: '请输入车间',
      valueKey: 'workshopName',
    });
  });

  it('keeps personnel import headers stable while adding management-only fields', () => {
    expect(MASTER_DATA_TEMPLATE_HEADERS.personnel).not.toContain('提交日期');
    expect(MASTER_DATA_TEMPLATE_HEADERS.personnel).not.toContain('申请人');
    expect(MASTER_DATA_TEMPLATE_HEADERS.personnel).not.toContain('备注');
    expect(MASTER_DATA_TEMPLATE_HEADERS.personnel).not.toContain('上岗证有效期');
    expect(MASTER_DATA_TEMPLATE_HEADERS.personnel).not.toContain('加入时间');
    expect(MASTER_DATA_TEMPLATE_HEADERS.personnel).not.toContain('附件');

    const fields = getPersonnelManagementFields([
      { label: '启用', value: 'ACTIVE' },
      { label: '草稿', value: 'DRAFT' },
      { label: '停用', value: 'INACTIVE' },
    ]);

    expect(fields.map((field) => field.label)).toEqual([
      '公司',
      '部门',
      '班组',
      '编码',
      '姓名',
      '系统角色',
      '用户名',
      '手机号',
      '员工类型',
      '岗位',
      '上岗证有效期',
      '加入时间',
      '备注',
      '状态',
      '提交日期',
      '申请人',
    ]);
    expect(
      fields
        .filter((field) => field.required)
        .map((field) => field.label),
    ).toEqual(['编码', '姓名', '状态']);
    expect(fields.find((field) => field.label === '公司')).toMatchObject({
      readonly: true,
      valueKey: 'companyName',
    });
    expect(fields.find((field) => field.label === '部门')).toMatchObject({
      component: 'select',
      valueKey: 'departmentOrgId',
    });
    expect(fields.find((field) => field.label === '班组')).toMatchObject({
      component: 'select',
      valueKey: 'teamOrgId',
    });
    expect(fields.find((field) => field.label === '系统角色')?.options?.map((option) => option.label)).toEqual([
      '部门安全员',
      '企业后台',
      '部门经理',
      '工程管理中心',
      '后台管理',
      '安全员',
      '公司领导',
      '组员',
      '总集团安环部',
      '项目部',
      '班组长',
      '公司级安环部',
      '项目经理',
      '安全经理',
      '班长',
      '安环部',
      '生产经理',
      '演示角色',
      '其他部门人员',
      '维修员',
      '工长',
    ]);
    expect(fields.find((field) => field.label === '岗位')?.options).toHaveLength(34);
    expect(fields.filter((field) => field.type === 'date').map((field) => field.label)).toEqual([
      '上岗证有效期',
      '加入时间',
      '提交日期',
    ]);
  });

  it('hides the company sort column from the management table', () => {
    expect(getMasterDataColumns('company').map((column) => column.title)).not.toContain(
      '排序',
    );
    expect(MASTER_DATA_TEMPLATE_HEADERS.company).toContain('排序');
  });

  it('uses company form rules from the organization navigation standard', () => {
    const fields = getMasterDataFields('company', [
      { label: '启用', value: 'ACTIVE' },
      { label: '草稿', value: 'DRAFT' },
      { label: '停用', value: 'INACTIVE' },
    ]);

    expect(
      fields
        .filter((field) => field.required)
        .map((field) => field.label),
    ).toEqual(['编码', '名称', '状态', '公司类型']);
    expect(fields.find((field) => field.label === '公司类型')).toMatchObject({
      component: 'select',
      options: [
        { label: '集团', value: '集团' },
        { label: '分公司', value: '分公司' },
        { label: '子公司', value: '子公司' },
      ],
    });
  });

  it('marks every system management status field as required in create forms', () => {
    const statusOptions = [
      { label: '启用', value: 'ACTIVE' },
      { label: '草稿', value: 'DRAFT' },
      { label: '停用', value: 'INACTIVE' },
    ];
    const formGroups = [
      getMasterDataFields('company', statusOptions),
      getMasterDataFields('department', statusOptions),
      getMasterDataFields('personnel', statusOptions),
      getMasterDataFields('team', statusOptions),
      getTeamManagementFields(statusOptions),
      getPersonnelManagementFields(statusOptions),
    ];

    for (const fields of formGroups) {
      expect(fields.find((field) => field.label === '状态')).toMatchObject({
        required: true,
      });
    }
  });
});
