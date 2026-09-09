import { requestClient } from '#/api/request';

import type { SystemManagementApi } from './types';

export const getCompanyOptionsApi = () =>
  requestClient.get<SystemManagementApi.Option[]>('/system/options/companies');

export const getDepartmentOptionsApi = (companyOrgId?: SystemManagementApi.Id) =>
  requestClient.get<SystemManagementApi.Option[]>('/system/options/departments', {
    params: { companyOrgId },
  });

export const getTeamOptionsApi = (parentOrgId?: SystemManagementApi.Id) =>
  requestClient.get<SystemManagementApi.Option[]>('/system/options/teams', {
    params: { parentOrgId },
  });

export const getSystemOrganizationTreeApi = () =>
  requestClient.get<SystemManagementApi.OrganizationNode[]>(
    '/pingan/org/company-tree',
  );

export const getSystemFullOrganizationTreeApi = () =>
  requestClient.get<SystemManagementApi.OrganizationNode[]>('/pingan/org/tree');
