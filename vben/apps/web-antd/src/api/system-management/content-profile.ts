import { requestClient } from '#/api/request';

import type { SystemManagementApi } from './types';

export namespace SystemContentProfileApi {
  export type Id = SystemManagementApi.Id;
  export type Status = SystemManagementApi.Status;

  export interface Attachment {
    contentType: string;
    fileKind: 'IMAGE' | 'VIDEO' | string;
    fileSize: number;
    id: string;
    originalName: string;
    storagePath: string;
    url: string;
  }

  export interface ContentProfile {
    createdAt?: string;
    description?: string;
    id: Id;
    imageAttachment?: Attachment;
    orgId: Id;
    orgName?: string;
    orgType?: string;
    status?: Status;
    subtitle?: string;
    title: string;
    updatedAt?: string;
    videoAttachment?: Attachment;
    videoSortOrder?: number;
    videoTitle?: string;
  }

  export interface ContentProfilePayload {
    description?: string;
    orgId?: Id;
    status?: Status;
    subtitle?: string;
    title?: string;
    videoSortOrder?: number;
    videoTitle?: string;
  }
}

type Id = SystemContentProfileApi.Id;

function multipart(file: File) {
  const formData = new FormData();
  formData.append('file', file);
  return formData;
}

export const getContentProfileListApi = (
  params: SystemManagementApi.ListParams = {},
) =>
  requestClient.get<
    SystemManagementApi.PageResult<SystemContentProfileApi.ContentProfile>
  >('/system/content-profiles', { params });

export const createContentProfileApi = (
  data: SystemContentProfileApi.ContentProfilePayload,
) =>
  requestClient.post<SystemContentProfileApi.ContentProfile>(
    '/system/content-profiles',
    data,
  );

export const updateContentProfileApi = (
  id: Id,
  data: SystemContentProfileApi.ContentProfilePayload,
) =>
  requestClient.put<SystemContentProfileApi.ContentProfile>(
    `/system/content-profiles/${id}`,
    data,
  );

export const updateContentProfileStatusApi = (
  id: Id,
  status: SystemContentProfileApi.Status,
) =>
  requestClient.request<SystemContentProfileApi.ContentProfile>(
    `/system/content-profiles/${id}/status`,
    { data: { status }, method: 'PATCH' },
  );

export const deleteContentProfileApi = (id: Id) =>
  requestClient.delete(`/system/content-profiles/${id}`);

export const uploadContentProfileImageApi = (id: Id, file: File) =>
  requestClient.post<SystemContentProfileApi.ContentProfile>(
    `/system/content-profiles/${id}/image`,
    multipart(file),
    { headers: { 'Content-Type': 'multipart/form-data' } },
  );

export const removeContentProfileImageApi = (id: Id) =>
  requestClient.delete<SystemContentProfileApi.ContentProfile>(
    `/system/content-profiles/${id}/image`,
  );

export const uploadContentProfileVideoApi = (id: Id, file: File) =>
  requestClient.post<SystemContentProfileApi.ContentProfile>(
    `/system/content-profiles/${id}/video`,
    multipart(file),
    { headers: { 'Content-Type': 'multipart/form-data' } },
  );

export const removeContentProfileVideoApi = (id: Id) =>
  requestClient.delete<SystemContentProfileApi.ContentProfile>(
    `/system/content-profiles/${id}/video`,
  );
