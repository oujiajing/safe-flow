import { requestClient } from '#/api/request';

import type { SystemManagementApi } from './types';

export namespace SystemTeamCheckItemTemplateApi {
  export type Id = SystemManagementApi.Id;
  export type InspectionStage =
    | 'KEY_SITES'
    | 'MID_SHIFT_INSPECTION'
    | 'POST_SHIFT_INSPECTION'
    | 'PRE_SHIFT_MEETING_CONFIRMATION'
    | 'PRE_SHIFT_INSPECTION';
  export type Status = SystemManagementApi.Status;

  export interface LibraryItem {
    applicableStage: InspectionStage;
    applicableStageLabel?: string;
    checkItem: string;
    createdAt?: string;
    defaultCheckResult?: string;
    defaultFollowUpPlan?: string;
    defaultRectificationDescription?: string;
    id: Id;
    requireImage?: boolean;
    requireVideo?: boolean;
    riskType: string;
    sortOrder?: number;
    status?: Status;
    statusLabel?: string;
    updatedAt?: string;
  }

  export interface LibraryItemPayload {
    applicableStage?: InspectionStage;
    checkItem?: string;
    defaultCheckResult?: string;
    defaultFollowUpPlan?: string;
    defaultRectificationDescription?: string;
    requireImage?: boolean;
    requireVideo?: boolean;
    riskType?: string;
    sortOrder?: number;
    status?: Status;
  }

  export interface TemplateItem {
    checkItem: string;
    defaultCheckResult?: string;
    defaultFollowUpPlan?: string;
    defaultRectificationDescription?: string;
    id?: Id;
    libraryItemId?: Id;
    requireImage?: boolean;
    requireVideo?: boolean;
    riskType: string;
    sortOrder?: number;
  }

  export interface Template {
    companyName?: string;
    companyOrgId: Id;
    createdAt?: string;
    departmentName?: string;
    departmentOrgId?: Id;
    id: Id;
    inspectionStage: InspectionStage;
    inspectionStageLabel?: string;
    items: TemplateItem[];
    name: string;
    scope?: 'COMPANY' | 'DEPARTMENT' | 'TEAM';
    scopeName?: string;
    status?: Status;
    statusLabel?: string;
    teamName?: string;
    teamOrgId?: Id;
    updatedAt?: string;
    version?: number;
  }

  export interface TemplatePayload {
    companyOrgId?: Id;
    departmentOrgId?: Id;
    inspectionStage?: InspectionStage;
    items?: TemplateItem[];
    name?: string;
    status?: Status;
    teamOrgId?: Id;
    version?: number;
  }

  export interface TemplateQuery extends SystemManagementApi.ListParams {
    companyOrgId?: Id;
    departmentOrgId?: Id;
    exactScope?: boolean;
    stage?: InspectionStage | 'all';
    teamOrgId?: Id;
  }

  export interface ResolveQuery {
    companyOrgId: Id;
    departmentOrgId?: Id;
    stage: InspectionStage;
    teamOrgId?: Id;
  }
}

type Id = SystemTeamCheckItemTemplateApi.Id;
type Status = SystemTeamCheckItemTemplateApi.Status;

export const getTeamCheckItemLibraryApi = (
  params: SystemManagementApi.ListParams = {},
) =>
  requestClient.get<
    SystemManagementApi.PageResult<SystemTeamCheckItemTemplateApi.LibraryItem>
  >('/system/team-check-item-templates/library', { params });

export const createTeamCheckItemLibraryApi = (
  data: SystemTeamCheckItemTemplateApi.LibraryItemPayload,
) =>
  requestClient.post<SystemTeamCheckItemTemplateApi.LibraryItem>(
    '/system/team-check-item-templates/library',
    data,
  );

export const updateTeamCheckItemLibraryApi = (
  id: Id,
  data: SystemTeamCheckItemTemplateApi.LibraryItemPayload,
) =>
  requestClient.put<SystemTeamCheckItemTemplateApi.LibraryItem>(
    `/system/team-check-item-templates/library/${id}`,
    data,
  );

export const updateTeamCheckItemLibraryStatusApi = (id: Id, status: Status) =>
  requestClient.request<SystemTeamCheckItemTemplateApi.LibraryItem>(
    `/system/team-check-item-templates/library/${id}/status`,
    { data: { status }, method: 'PATCH' },
  );

export const deleteTeamCheckItemLibraryApi = (id: Id) =>
  requestClient.delete(`/system/team-check-item-templates/library/${id}`);

export const getTeamCheckTemplatesApi = (
  params: SystemTeamCheckItemTemplateApi.TemplateQuery = {},
) =>
  requestClient.get<
    SystemManagementApi.PageResult<SystemTeamCheckItemTemplateApi.Template>
  >('/system/team-check-item-templates/templates', { params });

export const createTeamCheckTemplateApi = (
  data: SystemTeamCheckItemTemplateApi.TemplatePayload,
) =>
  requestClient.post<SystemTeamCheckItemTemplateApi.Template>(
    '/system/team-check-item-templates/templates',
    data,
  );

export const getTeamCheckTemplateDetailApi = (id: Id) =>
  requestClient.get<SystemTeamCheckItemTemplateApi.Template>(
    `/system/team-check-item-templates/templates/${id}`,
  );

export const updateTeamCheckTemplateApi = (
  id: Id,
  data: SystemTeamCheckItemTemplateApi.TemplatePayload,
) =>
  requestClient.put<SystemTeamCheckItemTemplateApi.Template>(
    `/system/team-check-item-templates/templates/${id}`,
    data,
  );

export const updateTeamCheckTemplateStatusApi = (id: Id, status: Status) =>
  requestClient.request<SystemTeamCheckItemTemplateApi.Template>(
    `/system/team-check-item-templates/templates/${id}/status`,
    { data: { status }, method: 'PATCH' },
  );

export const deleteTeamCheckTemplateApi = (id: Id) =>
  requestClient.delete(`/system/team-check-item-templates/templates/${id}`);

export const copyTeamCheckTemplateApi = (
  id: Id,
  data: SystemTeamCheckItemTemplateApi.TemplatePayload,
) =>
  requestClient.post<SystemTeamCheckItemTemplateApi.Template>(
    `/system/team-check-item-templates/templates/${id}/copy`,
    data,
  );

export const getTeamCheckTemplateResolveApi = (
  params: SystemTeamCheckItemTemplateApi.ResolveQuery,
) =>
  requestClient.get<SystemTeamCheckItemTemplateApi.Template>(
    '/system/team-check-item-templates/resolve',
    { params },
  );
