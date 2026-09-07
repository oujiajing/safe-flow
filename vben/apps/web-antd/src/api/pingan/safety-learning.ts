import { requestClient } from '#/api/request';

export namespace PinganSafetyLearningApi {
  export type Id = number | string;
  export type Status = 'ACTIVE' | 'DRAFT' | 'INACTIVE';

  export interface PageResult<T> {
    items: T[];
    total: number;
  }

  export interface ListParams {
    category?: string;
    companyId?: Id;
    dateEnd?: string;
    dateStart?: string;
    keyword?: string;
    page?: number;
    pageSize?: number;
    status?: Status | 'all';
  }

  export interface Attachment {
    contentType: string;
    fileKind: 'IMAGE' | 'PDF' | 'VIDEO' | string;
    fileSize: number;
    id: string;
    originalName: string;
    storagePath: string;
    url: string;
  }

  export interface ContentPayload {
    attachmentText?: string;
    category: string;
    code?: string;
    companyId: Id;
    content?: string;
    coverImage?: string;
    draft?: string;
    durationText?: string;
    htmlExtract?: string;
    learningDate: string;
    status: Status | string;
    title: string;
    video?: string;
  }

  export interface Content extends ContentPayload {
    attachment?: Attachment;
    company: string;
    coverImageAttachment?: Attachment;
    createdAt?: string;
    id: Id;
    status: Status;
    statusLabel: string;
    updatedAt?: string;
    videoAttachment?: Attachment;
  }

  export interface ImportResult {
    errors: string[];
    successRows: number;
  }
}

const basePath = '/pingan/training/safety-learning/contents';

export function getSafetyLearningContentsApi(
  params: PinganSafetyLearningApi.ListParams = {},
) {
  return requestClient.get<
    PinganSafetyLearningApi.PageResult<PinganSafetyLearningApi.Content>
  >(basePath, { params });
}

export function createSafetyLearningContentApi(
  data: PinganSafetyLearningApi.ContentPayload,
) {
  return requestClient.post<PinganSafetyLearningApi.Content>(basePath, data);
}

export function updateSafetyLearningContentApi(
  id: PinganSafetyLearningApi.Id,
  data: PinganSafetyLearningApi.ContentPayload,
) {
  return requestClient.put<PinganSafetyLearningApi.Content>(
    `${basePath}/${id}`,
    data,
  );
}

export function deleteSafetyLearningContentApi(id: PinganSafetyLearningApi.Id) {
  return requestClient.delete<void>(`${basePath}/${id}`);
}

export function batchDeleteSafetyLearningContentsApi(
  ids: PinganSafetyLearningApi.Id[],
) {
  return requestClient.post<void>(`${basePath}/batch-delete`, { ids });
}

export function uploadSafetyLearningAttachmentApi(
  id: PinganSafetyLearningApi.Id,
  file: File,
) {
  return requestClient.upload<PinganSafetyLearningApi.Content>(
    `${basePath}/${id}/attachment`,
    { file },
  );
}

export function removeSafetyLearningAttachmentApi(
  id: PinganSafetyLearningApi.Id,
) {
  return requestClient.delete<PinganSafetyLearningApi.Content>(
    `${basePath}/${id}/attachment`,
  );
}

export function uploadSafetyLearningCoverImageApi(
  id: PinganSafetyLearningApi.Id,
  file: File,
) {
  return requestClient.upload<PinganSafetyLearningApi.Content>(
    `${basePath}/${id}/cover-image`,
    { file },
  );
}

export function removeSafetyLearningCoverImageApi(
  id: PinganSafetyLearningApi.Id,
) {
  return requestClient.delete<PinganSafetyLearningApi.Content>(
    `${basePath}/${id}/cover-image`,
  );
}

export function uploadSafetyLearningVideoApi(
  id: PinganSafetyLearningApi.Id,
  file: File,
) {
  return requestClient.upload<PinganSafetyLearningApi.Content>(
    `${basePath}/${id}/video`,
    { file },
  );
}

export function removeSafetyLearningVideoApi(id: PinganSafetyLearningApi.Id) {
  return requestClient.delete<PinganSafetyLearningApi.Content>(
    `${basePath}/${id}/video`,
  );
}

export function downloadSafetyLearningTemplateApi() {
  return requestClient.get<Blob>(`${basePath}/template`, {
    responseType: 'blob',
  });
}

export function downloadSafetyLearningContentsApi(
  params: PinganSafetyLearningApi.ListParams = {},
) {
  return requestClient.get<Blob>(`${basePath}/export`, {
    params,
    responseType: 'blob',
  });
}

export function importSafetyLearningWorkbookApi(file: File) {
  const formData = new FormData();
  formData.append('file', file);

  return requestClient.post<PinganSafetyLearningApi.ImportResult>(
    `${basePath}/import`,
    formData,
    {
      headers: { 'Content-Type': 'multipart/form-data' },
    },
  );
}
