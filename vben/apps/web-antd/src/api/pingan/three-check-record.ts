import { requestClient } from '#/api/request';
import type { PinganHazardRectificationOrderApi } from './hazard-rectification-order';

export namespace PinganThreeCheckRecordApi {
  export type Id = number | string;
  export type BatchAction = 'DELETE' | 'REMIND' | 'SUBMIT' | 'WITHDRAW';
  export type QuickShotWorkflowAction = 'APPROVE' | 'REJECT';
  export type RecordStatus =
    | 'ACCEPTED'
    | 'APPROVED'
    | 'ARCHIVED'
    | 'DRAFT'
    | 'OPENED'
    | 'PENDING_ACCEPTANCE'
    | 'PENDING_APPROVAL'
    | 'PENDING_RECTIFICATION'
    | 'PENDING_REVIEW'
    | 'REJECTED'
    | 'RECTIFIED'
    | 'REVIEWED'
    | 'WITHDRAWN';
  export type SourceChannel = 'API_IMPORT' | 'PC' | 'WECHAT_MINI_PROGRAM';

  export interface RecordListParams {
    company?: string;
    companyId?: Id;
    dateEnd?: string;
    dateStart?: string;
    department?: string;
    departmentId?: Id;
    organizationId?: Id;
    overdue?: boolean;
    page?: number;
    pageSize?: number;
    pointsReason?: string;
    rootDispatchRecordId?: Id;
    sourceChannel?: SourceChannel;
    status?: RecordStatus | 'all';
    team?: string;
    teamId?: Id;
  }

  export interface PageResult<T> {
    items: T[];
    total: number;
  }

  export interface RecordRow {
    businessDate: string;
    canCreateRectificationOrder: boolean;
    canRemind: boolean;
    canSubmit: boolean;
    canWithdraw: boolean;
    company: string;
    companyId?: Id;
    date: string;
    department: string;
    departmentId?: Id;
    id: string;
    imageCheck: string;
    imagePreviewUrl?: string;
    moduleKey: string;
    owner: string;
    ownerUserId?: number;
    overdue: boolean;
    payload: Record<string, unknown>;
    recordNo: string;
    rootDispatchRecordId?: string;
    sourceChannel: SourceChannel;
    status: RecordStatus;
    statusLabel?: string;
    taskId?: string;
    team: string;
    teamId?: Id;
    version: number;
    videoCheck: string;
    videoPreviewUrl?: string;
  }

  export interface RecordDetail extends RecordRow {
    attachments: Array<{
      contentType: string;
      fileKind: 'IMAGE' | 'PDF' | 'VIDEO' | string;
      fileSize: number;
      id: string;
      originalName: string;
      storagePath: string;
      url: string;
    }>;
    clientRequestId?: string;
    clientUpdatedAt?: string;
    lastSyncedAt?: string;
    reminderCount: number;
    sourceRecordId?: string;
  }

  export interface DocumentFlowStatusLog {
    action: string;
    createdAt: string;
    fromStatus?: string;
    fromStatusLabel?: string;
    id: string;
    operatorId?: string;
    operatorName?: string;
    payload?: Record<string, unknown>;
    remark?: string;
    toStatus?: string;
    toStatusLabel?: string;
  }

  export interface DocumentFlowResponse {
    linkedRectificationOrder?: PinganHazardRectificationOrderApi.OrderDetail;
    record: RecordDetail;
    statusLogs: DocumentFlowStatusLog[];
  }

  export interface ChangeHistoryItem {
    action: string;
    afterValue?: string;
    beforeValue?: string;
    createdAt: string;
    fieldKey: string;
    fieldLabel: string;
    id: string;
    operatorId?: string;
    operatorName?: string;
    remark?: string;
    valueType?: 'IMAGE' | 'STATUS' | 'TEXT' | 'VIDEO' | string;
    version: number;
  }

  export interface ChangeHistoryResponse {
    items: ChangeHistoryItem[];
    record: RecordDetail;
  }

  export interface BatchRequest {
    action: BatchAction;
    ids: Id[];
    reason?: string;
  }

  export interface BatchResult {
    id: string;
    message: string;
    success: boolean;
  }

  export interface BatchResponse {
    failureCount: number;
    results: BatchResult[];
    successCount: number;
    total: number;
  }

  export interface WorkflowActionRequest {
    action: QuickShotWorkflowAction;
    payload?: Record<string, unknown>;
    remark?: string;
    version?: number;
  }

  export interface RecordPayload {
    businessDate: string;
    clientRequestId?: string;
    clientUpdatedAt?: string;
    companyId: Id;
    departmentId: Id;
    ownerUserId?: number;
    payload?: Record<string, unknown>;
    rootDispatchRecordId?: Id;
    sourceChannel?: SourceChannel;
    sourceRecordId?: string;
    status?: RecordStatus | string;
    taskId?: number;
    teamId: Id;
    version?: number;
  }

  export interface RecordStatistics {
    attachmentCounts: {
      imageMissing: number;
      imageUploaded: number;
      videoMissing: number;
      videoUploaded: number;
    };
    companyCounts: OrganizationCount[];
    dateCounts: Array<{
      count: number;
      date: string;
    }>;
    departmentCounts: OrganizationCount[];
    statusCounts: Partial<Record<RecordStatus, number>>;
    teamCounts: OrganizationCount[];
    total: number;
  }

  export interface OrganizationCount {
    count: number;
    organizationId: Id;
    organizationName: string;
    organizationType: 'COMPANY' | 'DEPARTMENT' | 'TEAM';
  }

  export interface WorkflowItem {
    action: string;
    actionLabel: string;
    fromStatus?: null | RecordStatus | string;
    fromStatusLabel?: null | string;
    id: string;
    occurredAt: string;
    operatorId?: Id | null;
    operatorName?: string;
    remark?: null | string;
    toStatus?: null | RecordStatus | string;
    toStatusLabel?: null | string;
  }

  export interface RecordWorkflow {
    changeHistory: WorkflowItem[];
    documentFlow: WorkflowItem[];
  }

  export interface RecordFlowStage {
    moduleKey: string;
    record?: null | RecordRow;
    stageKey:
      | 'midShiftInspection'
      | 'postShiftInspection'
      | 'preShiftInspection'
      | 'preShiftMeeting'
      | 'teamDispatch';
    stageLabel: string;
  }

  export interface RecordFlow {
    businessDate: string;
    company: string;
    department: string;
    rootDispatchModuleKey: string;
    rootDispatchRecordId: string;
    stages: RecordFlowStage[];
    team: string;
  }
}

function recordsPath(moduleKey: string) {
  return `/pingan/three-checks/${moduleKey}/records`;
}

export async function getThreeCheckRecordsApi(
  moduleKey: string,
  params: PinganThreeCheckRecordApi.RecordListParams,
) {
  return requestClient.get<
    PinganThreeCheckRecordApi.PageResult<PinganThreeCheckRecordApi.RecordRow>
  >(recordsPath(moduleKey), { params });
}

export async function getThreeCheckRecordDetailApi(
  moduleKey: string,
  id: string,
) {
  return requestClient.get<PinganThreeCheckRecordApi.RecordDetail>(
    `${recordsPath(moduleKey)}/${id}`,
  );
}

export async function getHazardInspectionDocumentFlowApi(
  moduleKey: string,
  id: string,
) {
  return requestClient.get<PinganThreeCheckRecordApi.DocumentFlowResponse>(
    `/pingan/hazard-inspection/${moduleKey}/records/${id}/document-flow`,
  );
}

export async function getHazardInspectionChangeHistoryApi(
  moduleKey: string,
  id: string,
) {
  return requestClient.get<PinganThreeCheckRecordApi.ChangeHistoryResponse>(
    `/pingan/hazard-inspection/${moduleKey}/records/${id}/change-history`,
  );
}

export async function getThreeCheckRecordStatisticsApi(
  moduleKey: string,
  params: PinganThreeCheckRecordApi.RecordListParams,
) {
  return requestClient.get<PinganThreeCheckRecordApi.RecordStatistics>(
    `${recordsPath(moduleKey)}/statistics`,
    { params },
  );
}

export async function getThreeCheckRecordWorkflowApi(
  moduleKey: string,
  id: string,
) {
  return requestClient.get<PinganThreeCheckRecordApi.RecordWorkflow>(
    `${recordsPath(moduleKey)}/${id}/workflow`,
  );
}

export async function getThreeCheckRecordFlowApi(rootDispatchRecordId: string) {
  return requestClient.get<PinganThreeCheckRecordApi.RecordFlow>(
    `/pingan/three-checks/flows/${rootDispatchRecordId}`,
  );
}

export async function createThreeCheckRecordApi(
  moduleKey: string,
  data: PinganThreeCheckRecordApi.RecordPayload,
) {
  return requestClient.post<PinganThreeCheckRecordApi.RecordDetail>(
    recordsPath(moduleKey),
    data,
  );
}

export async function updateThreeCheckRecordApi(
  moduleKey: string,
  id: string,
  data: PinganThreeCheckRecordApi.RecordPayload,
) {
  return requestClient.put<PinganThreeCheckRecordApi.RecordDetail>(
    `${recordsPath(moduleKey)}/${id}`,
    data,
  );
}

export async function deleteThreeCheckRecordApi(moduleKey: string, id: string) {
  return requestClient.delete<void>(`${recordsPath(moduleKey)}/${id}`);
}

export async function batchThreeCheckRecordsApi(
  moduleKey: string,
  data: PinganThreeCheckRecordApi.BatchRequest,
) {
  return requestClient.post<PinganThreeCheckRecordApi.BatchResponse>(
    `${recordsPath(moduleKey)}/batch`,
    data,
  );
}

export async function submitThreeCheckRecordApi(moduleKey: string, id: string) {
  return requestClient.post<PinganThreeCheckRecordApi.RecordDetail>(
    `${recordsPath(moduleKey)}/${id}/submit`,
  );
}

export async function withdrawThreeCheckRecordApi(
  moduleKey: string,
  id: string,
  reason: string,
) {
  return requestClient.post<PinganThreeCheckRecordApi.RecordDetail>(
    `${recordsPath(moduleKey)}/${id}/withdraw`,
    { reason },
  );
}

export async function remindThreeCheckRecordApi(moduleKey: string, id: string) {
  return requestClient.post<PinganThreeCheckRecordApi.RecordDetail>(
    `${recordsPath(moduleKey)}/${id}/remind`,
  );
}

export async function openThreeCheckRectificationOrderApi(
  moduleKey: string,
  id: string,
) {
  return requestClient.post<PinganHazardRectificationOrderApi.OrderDetail>(
    `${recordsPath(moduleKey)}/${id}/rectification-order`,
  );
}

export async function workflowActionThreeCheckRecordApi(
  moduleKey: string,
  id: string,
  data: PinganThreeCheckRecordApi.WorkflowActionRequest,
) {
  return requestClient.post<PinganThreeCheckRecordApi.RecordDetail>(
    `${recordsPath(moduleKey)}/${id}/workflow-actions`,
    data,
  );
}

export async function uploadThreeCheckRecordAttachmentApi(
  moduleKey: string,
  id: string,
  fileKind: 'IMAGE' | 'VIDEO',
  file: File,
) {
  const formData = new FormData();
  formData.append('fileKind', fileKind);
  formData.append('file', file);

  return requestClient.post<
    PinganThreeCheckRecordApi.RecordDetail['attachments'][number]
  >(`${recordsPath(moduleKey)}/${id}/attachments`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
}

export async function deleteThreeCheckRecordAttachmentApi(
  moduleKey: string,
  id: string,
  attachmentId: string,
) {
  return requestClient.delete<void>(
    `${recordsPath(moduleKey)}/${id}/attachments/${attachmentId}`,
  );
}
