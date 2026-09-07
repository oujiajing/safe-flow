import { requestClient } from '#/api/request';

export namespace PinganSpecialWorkApi {
  export type Id = number | string;
  export type Status =
    | 'COMPLETED'
    | 'IN_PROGRESS'
    | 'PENDING_ACCEPTANCE'
    | 'PENDING_APPROVAL';

  export interface PageResult<T> {
    items: T[];
    total: number;
  }

  export interface ListParams {
    companyId?: Id;
    dateEnd?: string;
    dateStart?: string;
    page?: number;
    pageSize?: number;
    status?: Status | 'all';
  }

  export interface Attachment {
    contentType: string;
    fileKind: 'IMAGE' | string;
    fileSize: number;
    id: string;
    originalName: string;
    storagePath: string;
    url: string;
  }

  export interface RecordPayload {
    applicationTime: string;
    companyId: Id;
    completionAcceptanceTime?: string;
    completionAcceptor?: string;
    disclosureReceiver?: string;
    guardian?: string;
    implementationEndTime?: string;
    implementationStartTime?: string;
    project: string;
    riskIdentificationResult?: string;
    safetyDisclosurePerson?: string;
    status?: Status | string;
    workContent?: string;
    workLocation?: string;
    workType: string;
  }

  export type Action =
    | 'APPROVE_AND_START'
    | 'COMPLETE_ACCEPTANCE'
    | 'SUBMIT_ACCEPTANCE';

  export interface ActionPayload {
    completionAcceptanceTime?: string;
    completionAcceptor?: string;
    disclosureReceiver?: string;
    guardian?: string;
    implementationEndTime?: string;
    implementationStartTime?: string;
    safetyDisclosurePerson?: string;
  }

  export interface Record extends RecordPayload {
    company: string;
    createdAt?: string;
    id: Id;
    image?: Attachment;
    status: Status;
    statusLabel: string;
    updatedAt?: string;
  }
}

const basePath = '/pingan/special-work/records';

export function getSpecialWorkRecordsApi(
  params: PinganSpecialWorkApi.ListParams = {},
) {
  return requestClient.get<
    PinganSpecialWorkApi.PageResult<PinganSpecialWorkApi.Record>
  >(basePath, { params });
}

export function createSpecialWorkRecordApi(
  data: PinganSpecialWorkApi.RecordPayload,
) {
  return requestClient.post<PinganSpecialWorkApi.Record>(basePath, data);
}

export function updateSpecialWorkRecordApi(
  id: PinganSpecialWorkApi.Id,
  data: PinganSpecialWorkApi.RecordPayload,
) {
  return requestClient.put<PinganSpecialWorkApi.Record>(
    `${basePath}/${id}`,
    data,
  );
}

export function executeSpecialWorkActionApi(
  id: PinganSpecialWorkApi.Id,
  action: PinganSpecialWorkApi.Action,
  data: PinganSpecialWorkApi.ActionPayload,
) {
  return requestClient.post<PinganSpecialWorkApi.Record>(
    `${basePath}/${id}/actions/${action}`,
    data,
  );
}

export function deleteSpecialWorkRecordApi(id: PinganSpecialWorkApi.Id) {
  return requestClient.delete<void>(`${basePath}/${id}`);
}

export function batchDeleteSpecialWorkRecordsApi(
  ids: PinganSpecialWorkApi.Id[],
) {
  return requestClient.post<void>(`${basePath}/batch-delete`, { ids });
}

export function uploadSpecialWorkImageApi(
  id: PinganSpecialWorkApi.Id,
  file: File,
) {
  return requestClient.upload<PinganSpecialWorkApi.Record>(
    `${basePath}/${id}/image`,
    { file },
  );
}

export function downloadSpecialWorkRecordsApi(
  params: PinganSpecialWorkApi.ListParams = {},
) {
  return requestClient.get<Blob>(`${basePath}/export`, {
    params,
    responseType: 'blob',
  });
}
