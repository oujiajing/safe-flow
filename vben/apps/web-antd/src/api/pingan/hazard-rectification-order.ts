import { requestClient } from '#/api/request';

export namespace PinganHazardRectificationOrderApi {
  export type Id = number | string;
  export type SourceType =
    | 'MANUAL'
    | 'QUICK_SHOT'
    | 'SAFETY_INSPECTION'
    | 'THREE_CHECK'
    | string;
  export type Status =
    | 'CANCELLED'
    | 'CLOSED'
    | 'PENDING_ACCEPTANCE'
    | 'PENDING_ASSIGN'
    | 'PENDING_RECTIFY'
    | 'RECTIFIED';
  export type Action =
    | 'ACCEPT'
    | 'CANCEL'
    | 'ISSUE_RECTIFICATION'
    | 'MARK_RECTIFIED'
    | 'REJECT_ACCEPTANCE'
    | 'REQUEST_ACCEPTANCE';

  export interface PageResult<T> {
    items: T[];
    total: number;
  }

  export interface OrderListParams {
    companyId?: Id;
    dateEnd?: string;
    dateStart?: string;
    departmentId?: Id;
    page?: number;
    pageSize?: number;
    responsibleUserId?: Id;
    rootDispatchRecordId?: Id;
    sourceModuleKey?: string;
    sourceRecordId?: Id;
    sourceType?: SourceType;
    status?: Status | 'all';
    teamId?: Id;
  }

  export interface OrderRow {
    businessDate: string;
    closedAt?: string;
    company?: string;
    companyId?: Id;
    department?: string;
    departmentId?: Id;
    hazardCount: number;
    id: string;
    issuedAt?: string;
    orderNo: string;
    rectificationDeadline?: string;
    rectifiedAt?: string;
    rootDispatchRecordId?: string;
    sourceModuleKey: string;
    sourceRecordId: string;
    sourceRecordNo?: string;
    sourceType: SourceType;
    status: Status;
    statusLabel: string;
    team?: string;
    teamId?: Id;
    version: number;
  }

  export interface OrderItem {
    aiEnabled?: string;
    beforePhoto?: string;
    beforeVideo?: string;
    checkItem: string;
    closedAt?: string;
    defaultFollowUpPlan?: string;
    hazardDescription?: string;
    id: string;
    libraryItemId?: string;
    rectificationStatus: Status;
    rectificationStatusLabel: string;
    riskType?: string;
    sortOrder: number;
    sourceLineId: string;
    sourceLineIndex: number;
    sourceSnapshot?: Record<string, unknown>;
  }

  export interface FlowLog {
    action: string;
    actionLabel: string;
    createdAt: string;
    fromStatus?: Status;
    fromStatusLabel?: string;
    id: string;
    operatorId?: string;
    operatorName?: string;
    payload?: Record<string, unknown>;
    remark?: string;
    toStatus: Status;
    toStatusLabel: string;
  }

  export interface OrderDetail extends OrderRow {
    acceptanceAt?: string;
    acceptanceDepartmentId?: Id;
    acceptanceRemark?: string;
    acceptanceResult?: string;
    acceptanceUserId?: Id;
    acceptanceUserName?: string;
    flowLogs: FlowLog[];
    issuedBy?: Id;
    items: OrderItem[];
    rectificationDepartmentId?: Id;
    rectificationAfterPhoto?: string;
    rectificationDescription?: string;
    rectificationResponsibleUserId?: Id;
    rectificationResponsibleUserName?: string;
    rectificationRequirement?: string;
    rectifiedBy?: Id;
  }

  export interface ActionRequest {
    action: Action;
    payload?: Record<string, unknown>;
    remark?: string;
    version: number;
  }

  export interface CreateItemRequest {
    beforePhoto?: string;
    beforeVideo?: string;
    checkItem: string;
    defaultFollowUpPlan?: string;
    hazardDescription: string;
    riskType?: string;
  }

  export interface CreateRequest {
    businessDate: string;
    companyId: Id;
    departmentId: Id;
    items: CreateItemRequest[];
    teamId: Id;
  }

  export type AttachmentFileKind = 'RECTIFICATION_AFTER_PHOTO';

  export interface BatchDeleteRequest {
    ids: Id[];
  }

  export interface Attachment {
    contentType?: string;
    fileKind: AttachmentFileKind | string;
    fileSize?: number;
    id: string;
    originalName?: string;
    storagePath?: string;
    url: string;
  }
}

const ordersPath = '/pingan/hazard-rectification/orders';

export async function getHazardRectificationOrdersApi(
  params: PinganHazardRectificationOrderApi.OrderListParams,
) {
  return requestClient.get<
    PinganHazardRectificationOrderApi.PageResult<PinganHazardRectificationOrderApi.OrderRow>
  >(ordersPath, { params });
}

export async function getHazardRectificationOrderDetailApi(id: string) {
  return requestClient.get<PinganHazardRectificationOrderApi.OrderDetail>(
    `${ordersPath}/${id}`,
  );
}

export async function actionHazardRectificationOrderApi(
  id: string,
  data: PinganHazardRectificationOrderApi.ActionRequest,
) {
  return requestClient.post<PinganHazardRectificationOrderApi.OrderDetail>(
    `${ordersPath}/${id}/actions`,
    data,
  );
}

export async function createHazardRectificationOrderApi(
  data: PinganHazardRectificationOrderApi.CreateRequest,
) {
  return requestClient.post<PinganHazardRectificationOrderApi.OrderDetail>(
    ordersPath,
    data,
  );
}

export async function batchDeleteHazardRectificationOrdersApi(
  ids: PinganHazardRectificationOrderApi.Id[],
) {
  return requestClient.post<void>(`${ordersPath}/batch-delete`, { ids });
}

export async function uploadHazardRectificationOrderAttachmentApi(
  id: string,
  fileKind: PinganHazardRectificationOrderApi.AttachmentFileKind,
  file: File,
) {
  const formData = new FormData();
  formData.append('fileKind', fileKind);
  formData.append('file', file);
  return requestClient.post<PinganHazardRectificationOrderApi.Attachment>(
    `${ordersPath}/${id}/attachments`,
    formData,
    {
      headers: { 'Content-Type': 'multipart/form-data' },
    },
  );
}
