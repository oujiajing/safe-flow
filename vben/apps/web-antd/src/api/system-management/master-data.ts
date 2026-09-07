import { requestClient } from '#/api/request';

import type { SystemManagementApi } from './types';

type Id = SystemManagementApi.Id;
type ListParams = SystemManagementApi.ListParams;
type Status = SystemManagementApi.Status;

function getList<T>(module: string, params: ListParams) {
  return requestClient.get<SystemManagementApi.PageResult<T>>(
    `/system/${module}`,
    { params },
  );
}

function createRecord<T>(module: string, data: Partial<T>) {
  return requestClient.post<T>(`/system/${module}`, data);
}

function updateRecord<T>(module: string, id: Id, data: Partial<T>) {
  return requestClient.put<T>(`/system/${module}/${id}`, data);
}

function deleteRecord(module: string, id: Id) {
  return requestClient.delete(`/system/${module}/${id}`);
}

function updateStatus<T>(module: string, id: Id, status: Status) {
  return requestClient.request<T>(`/system/${module}/${id}/status`, {
    data: { status },
    method: 'PATCH',
  });
}

export const getCompanyListApi = (params: ListParams = {}) =>
  getList<SystemManagementApi.Company>('companies', params);
export const createCompanyApi = (data: Partial<SystemManagementApi.Company>) =>
  createRecord<SystemManagementApi.Company>('companies', data);
export const updateCompanyApi = (
  id: Id,
  data: Partial<SystemManagementApi.Company>,
) => updateRecord<SystemManagementApi.Company>('companies', id, data);
export const deleteCompanyApi = (id: Id) => deleteRecord('companies', id);
export const updateCompanyStatusApi = (id: Id, status: Status) =>
  updateStatus<SystemManagementApi.Company>('companies', id, status);

export const getDepartmentListApi = (params: ListParams = {}) =>
  getList<SystemManagementApi.Department>('departments', params);
export const createDepartmentApi = (
  data: Partial<SystemManagementApi.Department>,
) => createRecord<SystemManagementApi.Department>('departments', data);
export const updateDepartmentApi = (
  id: Id,
  data: Partial<SystemManagementApi.Department>,
) => updateRecord<SystemManagementApi.Department>('departments', id, data);
export const deleteDepartmentApi = (id: Id) => deleteRecord('departments', id);
export const updateDepartmentStatusApi = (id: Id, status: Status) =>
  updateStatus<SystemManagementApi.Department>('departments', id, status);

export const getTeamListApi = (params: ListParams = {}) =>
  getList<SystemManagementApi.Team>('teams', params);
export const createTeamApi = (data: Partial<SystemManagementApi.Team>) =>
  createRecord<SystemManagementApi.Team>('teams', data);
export const updateTeamApi = (id: Id, data: Partial<SystemManagementApi.Team>) =>
  updateRecord<SystemManagementApi.Team>('teams', id, data);
export const deleteTeamApi = (id: Id) => deleteRecord('teams', id);
export const updateTeamStatusApi = (id: Id, status: Status) =>
  updateStatus<SystemManagementApi.Team>('teams', id, status);

export const getPersonnelListApi = (params: ListParams = {}) =>
  getList<SystemManagementApi.Personnel>('personnel', params);
export const createPersonnelApi = (
  data: Partial<SystemManagementApi.Personnel>,
) => createRecord<SystemManagementApi.Personnel>('personnel', data);
export const updatePersonnelApi = (
  id: Id,
  data: Partial<SystemManagementApi.Personnel>,
) => updateRecord<SystemManagementApi.Personnel>('personnel', id, data);
export const deletePersonnelApi = (id: Id) => deleteRecord('personnel', id);
export const updatePersonnelStatusApi = (id: Id, status: Status) =>
  updateStatus<SystemManagementApi.Personnel>('personnel', id, status);
