import { beforeEach, describe, expect, it, vi } from 'vitest';

import { requestClient } from '#/api/request';
import { downloadFileFromBlob } from '@vben/utils';

import {
  createCompanyApi,
  deleteDepartmentApi,
  getCompanyListApi,
  updatePersonnelStatusApi,
  updateTeamApi,
} from './master-data';
import {
  createContentProfileApi,
  deleteContentProfileApi,
  getContentProfileListApi,
  removeContentProfileVideoApi,
  updateContentProfileApi,
  updateContentProfileStatusApi,
  uploadContentProfileImageApi,
  uploadContentProfileVideoApi,
} from './content-profile';
import {
  createTeamCheckItemLibraryApi,
  createTeamCheckTemplateApi,
  deleteTeamCheckTemplateApi,
  getTeamCheckTemplateDetailApi,
  getTeamCheckItemLibraryApi,
  getTeamCheckTemplateResolveApi,
  getTeamCheckTemplatesApi,
  copyTeamCheckTemplateApi,
  updateTeamCheckTemplateStatusApi,
} from './team-check-item-template';
import {
  downloadSystemDataApi,
  downloadSystemTemplateApi,
  uploadSystemDataApi,
} from './import-export';
import {
  freezeAccountApi,
  getAccountListApi,
  getRoleListApi,
  getMenuTreeApi,
  getRoleMenusApi,
  resetAccountPasswordApi,
  updateAccountApi,
  updateAccountRolesApi,
  updateRoleMenusApi,
} from './security';
import {
  getSystemFullOrganizationTreeApi,
  getSystemOrganizationTreeApi,
} from './options';
import {
  createAnnouncementApi,
  getAnnouncementDeliveriesApi,
  getAnnouncementListApi,
  publishAnnouncementApi,
  updateAnnouncementApi,
  withdrawAnnouncementApi,
} from './announcement';

vi.mock('#/api/request', () => ({
  requestClient: {
    delete: vi.fn(),
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    request: vi.fn(),
  },
}));

vi.mock('@vben/utils', () => ({
  downloadFileFromBlob: vi.fn(),
}));

describe('system management api clients', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('maps announcement lifecycle and delivery audit calls', async () => {
    const input = {
      audienceType: 'ORG' as const,
      content: '请完成汛期安全检查',
      organizationIds: [12],
      roleIds: [],
      severity: 'IMPORTANT' as const,
      title: '汛期安全提醒',
      userIds: [],
    };

    await getAnnouncementListApi({ page: 1, pageSize: 20, status: 'DRAFT' });
    await createAnnouncementApi(input);
    await updateAnnouncementApi('31', input);
    await publishAnnouncementApi('31');
    await withdrawAnnouncementApi('31');
    await getAnnouncementDeliveriesApi({
      announcementId: '31',
      page: 1,
      pageSize: 20,
    });

    expect(requestClient.get).toHaveBeenCalledWith('/system/announcements', {
      params: { page: 1, pageSize: 20, status: 'DRAFT' },
    });
    expect(requestClient.post).toHaveBeenCalledWith(
      '/system/announcements',
      input,
    );
    expect(requestClient.put).toHaveBeenCalledWith(
      '/system/announcements/31',
      input,
    );
    expect(requestClient.post).toHaveBeenCalledWith(
      '/system/announcements/31/publish',
    );
    expect(requestClient.post).toHaveBeenCalledWith(
      '/system/announcements/31/withdraw',
    );
    expect(requestClient.get).toHaveBeenCalledWith(
      '/system/notification-deliveries',
      { params: { announcementId: '31', page: 1, pageSize: 20 } },
    );
  });

  it('maps master data CRUD calls to /system endpoints', async () => {
    await getCompanyListApi({ keyword: 'Demo Works', page: 1, pageSize: 20 });
    await createCompanyApi({ code: 'YC', name: 'Demo Works Company', status: 'ACTIVE' });
    await updateTeamApi('7', { name: '湖贝班组', status: 'ACTIVE' });
    await deleteDepartmentApi('3');
    await updatePersonnelStatusApi('9', 'INACTIVE');

    expect(requestClient.get).toHaveBeenCalledWith('/system/companies', {
      params: { keyword: 'Demo Works', page: 1, pageSize: 20 },
    });
    expect(requestClient.post).toHaveBeenCalledWith('/system/companies', {
      code: 'YC',
      name: 'Demo Works Company',
      status: 'ACTIVE',
    });
    expect(requestClient.put).toHaveBeenCalledWith('/system/teams/7', {
      name: '湖贝班组',
      status: 'ACTIVE',
    });
    expect(requestClient.delete).toHaveBeenCalledWith('/system/departments/3');
    expect(requestClient.request).toHaveBeenCalledWith(
      '/system/personnel/9/status',
      { data: { status: 'INACTIVE' }, method: 'PATCH' },
    );
  });

  it('maps account, role, and menu security calls', async () => {
    await getAccountListApi({ organizationId: '4', status: 'ACTIVE' });
    await updateAccountApi('11', {
      mobile: '13900000001',
      orgId: 34,
      realName: '班组成员',
      roleIds: [3, 8],
      status: 'ACTIVE',
      username: 'team_member',
    });
    await updateAccountRolesApi('11', [3, 8]);
    await getRoleListApi({ keyword: '班长' });
    await freezeAccountApi('11');
    await resetAccountPasswordApi('11', 'P@ssw0rd');
    await getRoleMenusApi('2');
    await updateRoleMenusApi('2', [1003]);
    await getMenuTreeApi();

    expect(requestClient.get).toHaveBeenNthCalledWith(1, '/system/accounts', {
      params: { organizationId: '4', status: 'ACTIVE' },
    });
    expect(requestClient.put).toHaveBeenNthCalledWith(
      1,
      '/system/accounts/11',
      {
        mobile: '13900000001',
        orgId: 34,
        realName: '班组成员',
        roleIds: [3, 8],
        status: 'ACTIVE',
        username: 'team_member',
      },
    );
    expect(requestClient.put).toHaveBeenNthCalledWith(
      2,
      '/system/accounts/11/roles',
      {
        roleIds: [3, 8],
      },
    );
    expect(requestClient.get).toHaveBeenNthCalledWith(2, '/system/roles', {
      params: { keyword: '班长' },
    });
    expect(requestClient.post).toHaveBeenNthCalledWith(
      1,
      '/system/accounts/11/freeze',
    );
    expect(requestClient.post).toHaveBeenNthCalledWith(
      2,
      '/system/accounts/11/reset-password',
      { password: 'P@ssw0rd' },
    );
    expect(requestClient.get).toHaveBeenNthCalledWith(
      3,
      '/system/roles/2/menus',
    );
    expect(requestClient.put).toHaveBeenNthCalledWith(
      3,
      '/system/roles/2/menus',
      {
        menuIds: [1003],
      },
    );
    expect(requestClient.get).toHaveBeenNthCalledWith(
      4,
      '/system/menus/tree',
    );
  });

  it('maps system organization tree option calls', async () => {
    await getSystemOrganizationTreeApi();
    await getSystemFullOrganizationTreeApi();

    expect(requestClient.get).toHaveBeenNthCalledWith(
      1,
      '/pingan/org/company-tree',
    );
    expect(requestClient.get).toHaveBeenNthCalledWith(2, '/pingan/org/tree');
  });

  it('maps import export calls with stable modules and multipart upload', async () => {
    const file = new File(['code,name'], 'company.xlsx');
    vi.mocked(requestClient.get)
      .mockResolvedValueOnce(new Blob(['template']))
      .mockResolvedValueOnce(new Blob(['data']));

    await downloadSystemTemplateApi('company');
    await downloadSystemDataApi('personnel', { status: 'ACTIVE' });
    await uploadSystemDataApi('department', file);

    expect(requestClient.get).toHaveBeenNthCalledWith(
      1,
      '/system/company/template',
      { responseReturn: 'body', responseType: 'blob' },
    );
    expect(requestClient.get).toHaveBeenNthCalledWith(
      2,
      '/system/personnel/export',
      {
        params: { status: 'ACTIVE' },
        responseReturn: 'body',
        responseType: 'blob',
      },
    );
    expect(requestClient.post).toHaveBeenCalledWith(
      '/system/department/import',
      expect.any(FormData),
      {
        headers: { 'Content-Type': 'multipart/form-data' },
      },
    );
    expect(downloadFileFromBlob).toHaveBeenNthCalledWith(1, {
      fileName: 'system-company-template.xlsx',
      source: expect.any(Blob),
    });
    expect(downloadFileFromBlob).toHaveBeenNthCalledWith(2, {
      fileName: 'system-personnel-data.xlsx',
      source: expect.any(Blob),
    });
  });

  it('maps content profile CRUD and media upload calls', async () => {
    const image = new File(['image'], '简介.png', { type: 'image/png' });
    const video = new File(['video'], '宣传片.mp4', { type: 'video/mp4' });

    await getContentProfileListApi({ keyword: 'Demo Works', status: 'ACTIVE' });
    await createContentProfileApi({
      description: '简介正文',
      orgId: 4,
      status: 'ACTIVE',
      title: 'Demo Works简介',
      videoSortOrder: 7,
      videoTitle: 'Demo Works宣传片',
    });
    await updateContentProfileApi('8', { title: '更新简介' });
    await updateContentProfileStatusApi('8', 'INACTIVE');
    await uploadContentProfileImageApi('8', image);
    await uploadContentProfileVideoApi('8', video);
    await removeContentProfileVideoApi('8');
    await deleteContentProfileApi('8');

    expect(requestClient.get).toHaveBeenCalledWith(
      '/system/content-profiles',
      { params: { keyword: 'Demo Works', status: 'ACTIVE' } },
    );
    expect(requestClient.post).toHaveBeenCalledWith(
      '/system/content-profiles',
      {
        description: '简介正文',
        orgId: 4,
        status: 'ACTIVE',
        title: 'Demo Works简介',
        videoSortOrder: 7,
        videoTitle: 'Demo Works宣传片',
      },
    );
    expect(requestClient.put).toHaveBeenCalledWith(
      '/system/content-profiles/8',
      { title: '更新简介' },
    );
    expect(requestClient.request).toHaveBeenCalledWith(
      '/system/content-profiles/8/status',
      { data: { status: 'INACTIVE' }, method: 'PATCH' },
    );
    expect(requestClient.post).toHaveBeenCalledWith(
      '/system/content-profiles/8/image',
      expect.any(FormData),
      { headers: { 'Content-Type': 'multipart/form-data' } },
    );
    expect(requestClient.post).toHaveBeenCalledWith(
      '/system/content-profiles/8/video',
      expect.any(FormData),
      { headers: { 'Content-Type': 'multipart/form-data' } },
    );
    expect(requestClient.delete).toHaveBeenCalledWith(
      '/system/content-profiles/8/video',
    );
    expect(requestClient.delete).toHaveBeenCalledWith(
      '/system/content-profiles/8',
    );
  });

  it('maps team check item template calls', async () => {
    await getTeamCheckItemLibraryApi({
      keyword: '机械',
      stage: 'PRE_SHIFT_INSPECTION',
    });
    await createTeamCheckItemLibraryApi({
      applicableStage: 'PRE_SHIFT_INSPECTION',
      checkItem: '检查机械设备是否处于良好状态',
      riskType: '机械伤害',
      status: 'ACTIVE',
    });
    await getTeamCheckTemplatesApi({
      companyOrgId: 4,
      exactScope: true,
      stage: 'PRE_SHIFT_INSPECTION',
      status: 'all',
      teamOrgId: 1011001,
    });
    await createTeamCheckTemplateApi({
      companyOrgId: 4,
      inspectionStage: 'PRE_SHIFT_INSPECTION',
      items: [],
      name: '幕墙组装1班班前检查模板',
      status: 'ACTIVE',
    });
    await getTeamCheckTemplateDetailApi('8');
    await copyTeamCheckTemplateApi('8', {
      companyOrgId: 4,
      inspectionStage: 'PRE_SHIFT_INSPECTION',
      name: '复制模板',
      teamOrgId: 1011001,
    });
    await updateTeamCheckTemplateStatusApi('8', 'INACTIVE');
    await getTeamCheckTemplateResolveApi({
      companyOrgId: 4,
      departmentOrgId: 101109,
      stage: 'PRE_SHIFT_INSPECTION',
      teamOrgId: 1011001,
    });
    await deleteTeamCheckTemplateApi('8');

    expect(requestClient.get).toHaveBeenCalledWith(
      '/system/team-check-item-templates/library',
      { params: { keyword: '机械', stage: 'PRE_SHIFT_INSPECTION' } },
    );
    expect(requestClient.post).toHaveBeenCalledWith(
      '/system/team-check-item-templates/library',
      {
        applicableStage: 'PRE_SHIFT_INSPECTION',
        checkItem: '检查机械设备是否处于良好状态',
        riskType: '机械伤害',
        status: 'ACTIVE',
      },
    );
    expect(requestClient.get).toHaveBeenCalledWith(
      '/system/team-check-item-templates/templates',
      {
        params: {
          companyOrgId: 4,
          exactScope: true,
          stage: 'PRE_SHIFT_INSPECTION',
          status: 'all',
          teamOrgId: 1011001,
        },
      },
    );
    expect(requestClient.post).toHaveBeenCalledWith(
      '/system/team-check-item-templates/templates',
      {
        companyOrgId: 4,
        inspectionStage: 'PRE_SHIFT_INSPECTION',
        items: [],
        name: '幕墙组装1班班前检查模板',
        status: 'ACTIVE',
      },
    );
    expect(requestClient.get).toHaveBeenCalledWith(
      '/system/team-check-item-templates/templates/8',
    );
    expect(requestClient.post).toHaveBeenCalledWith(
      '/system/team-check-item-templates/templates/8/copy',
      {
        companyOrgId: 4,
        inspectionStage: 'PRE_SHIFT_INSPECTION',
        name: '复制模板',
        teamOrgId: 1011001,
      },
    );
    expect(requestClient.request).toHaveBeenCalledWith(
      '/system/team-check-item-templates/templates/8/status',
      { data: { status: 'INACTIVE' }, method: 'PATCH' },
    );
    expect(requestClient.get).toHaveBeenCalledWith(
      '/system/team-check-item-templates/resolve',
      {
        params: {
          companyOrgId: 4,
          departmentOrgId: 101109,
          stage: 'PRE_SHIFT_INSPECTION',
          teamOrgId: 1011001,
        },
      },
    );
    expect(requestClient.delete).toHaveBeenCalledWith(
      '/system/team-check-item-templates/templates/8',
    );
  });
});


