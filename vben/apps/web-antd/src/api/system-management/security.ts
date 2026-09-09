import { requestClient } from '#/api/request';

import type { SystemManagementApi } from './types';

type Id = SystemManagementApi.Id;
type ListParams = SystemManagementApi.ListParams;

export const getAccountListApi = (params: ListParams = {}) =>
  requestClient.get<SystemManagementApi.PageResult<SystemManagementApi.Account>>(
    '/system/accounts',
    { params },
  );

export const createAccountApi = (
  data: Partial<SystemManagementApi.Account>,
) => requestClient.post<SystemManagementApi.Account>('/system/accounts', data);

export const updateAccountApi = (
  id: Id,
  data: Partial<SystemManagementApi.Account>,
) =>
  requestClient.put<SystemManagementApi.Account>(`/system/accounts/${id}`, data);

export const deleteAccountApi = (id: Id) =>
  requestClient.delete(`/system/accounts/${id}`);

export const freezeAccountApi = (id: Id) =>
  requestClient.post(`/system/accounts/${id}/freeze`);

export const unfreezeAccountApi = (id: Id) =>
  requestClient.post(`/system/accounts/${id}/unfreeze`);

export const resetAccountPasswordApi = (id: Id, password: string) =>
  requestClient.post(`/system/accounts/${id}/reset-password`, { password });

export const updateAccountRolesApi = (id: Id, roleIds: Id[]) =>
  requestClient.put(`/system/accounts/${id}/roles`, { roleIds });

export const getRoleListApi = (params: ListParams = {}) =>
  requestClient.get<SystemManagementApi.PageResult<SystemManagementApi.Role>>(
    '/system/roles',
    { params },
  );

export const createRoleApi = (data: Partial<SystemManagementApi.Role>) =>
  requestClient.post<SystemManagementApi.Role>('/system/roles', data);

export const updateRoleApi = (
  id: Id,
  data: Partial<SystemManagementApi.Role>,
) => requestClient.put<SystemManagementApi.Role>(`/system/roles/${id}`, data);

export const deleteRoleApi = (id: Id) =>
  requestClient.delete(`/system/roles/${id}`);

export const getRoleMenusApi = (id: Id) =>
  requestClient.get<Id[]>(`/system/roles/${id}/menus`);

export const updateRoleMenusApi = (id: Id, menuIds: Id[]) =>
  requestClient.put(`/system/roles/${id}/menus`, { menuIds });

export const getMenuListApi = (params: ListParams = {}) =>
  requestClient.get<SystemManagementApi.PageResult<SystemManagementApi.Menu>>(
    '/system/menus',
    { params },
  );

export const getMenuTreeApi = () =>
  requestClient.get<SystemManagementApi.Menu[]>('/system/menus/tree');

export const createMenuApi = (data: Partial<SystemManagementApi.Menu>) =>
  requestClient.post<SystemManagementApi.Menu>('/system/menus', data);

export const updateMenuApi = (
  id: Id,
  data: Partial<SystemManagementApi.Menu>,
) => requestClient.put<SystemManagementApi.Menu>(`/system/menus/${id}`, data);

export const deleteMenuApi = (id: Id) =>
  requestClient.delete(`/system/menus/${id}`);
