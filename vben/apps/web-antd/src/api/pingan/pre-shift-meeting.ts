import { requestClient } from '#/api/request';

export namespace PinganPreShiftMeetingApi {
  export type Id = number | string;
  export type MeetingStatus =
    | 'ACCEPTED'
    | 'APPROVED'
    | 'ARCHIVED'
    | 'DRAFT'
    | 'OPENED'
    | 'PENDING_ACCEPTANCE'
    | 'PENDING_APPROVAL'
    | 'PENDING_RECTIFICATION'
    | 'PENDING_REVIEW'
    | 'RECTIFIED'
    | 'REJECTED'
    | 'REVIEWED'
    | 'WITHDRAWN';
  export type SourceChannel = 'API_IMPORT' | 'PC' | 'WECHAT_MINI_PROGRAM';

  export interface OrgNode {
    children?: OrgNode[];
    companyType?: string;
    id: Id;
    key: string;
    orgType: string;
    title: string;
  }

  export interface UserOption {
    id: number;
    orgId: number;
    realName: string;
    username: string;
  }

  export interface MeetingListParams {
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
    sourceChannel?: SourceChannel;
    status?: MeetingStatus | 'all';
    team?: string;
    teamId?: Id;
  }

  export interface MeetingRow {
    attendees: string;
    canDelete: boolean;
    canRemind: boolean;
    canSubmit: boolean;
    canWithdraw: boolean;
    company: string;
    date: string;
    department: string;
    id: string;
    imageCheck: string;
    imagePreviewUrl?: string;
    owner: string;
    overdue: boolean;
    sourceChannel: SourceChannel;
    status: MeetingStatus;
    team: string;
    version: number;
    videoCheck: string;
    videoPreviewUrl?: string;
  }

  export interface PageResult<T> {
    items: T[];
    total: number;
  }

  export interface MeetingDetail {
    attachments: Array<{
      contentType: string;
      fileKind: 'IMAGE' | 'PDF' | 'VIDEO' | string;
      fileSize: number;
      id: string;
      originalName: string;
      storagePath: string;
      url: string;
    }>;
    attendees: string[];
    attendeesCount: number;
    canDelete: boolean;
    canRemind: boolean;
    canSubmit: boolean;
    canWithdraw: boolean;
    clientRequestId?: string;
    clientUpdatedAt?: string;
    company: string;
    department: string;
    id: string;
    imageCheck: string;
    meetingContent: string;
    meetingDate: string;
    meetingNo: string;
    owner: string;
    overdue: boolean;
    reminderCount: number;
    sourceChannel: SourceChannel;
    sourceRecordId?: string;
    status: MeetingStatus;
    team: string;
    lastSyncedAt?: string;
    version: number;
    videoCheck: string;
  }

  export interface MeetingPayload {
    attendees?: string[];
    clientRequestId?: string;
    clientUpdatedAt?: string;
    companyId: Id;
    departmentId: Id;
    meetingContent?: string;
    meetingDate: string;
    ownerUserId?: number;
    sourceChannel?: SourceChannel;
    sourceRecordId?: string;
    taskId?: number;
    teamId: Id;
    version?: number;
  }
}

export async function getPinganOrgTreeApi() {
  return requestClient.get<PinganPreShiftMeetingApi.OrgNode[]>(
    '/pingan/org/tree',
  );
}

export async function getPinganCompanyOrgTreeApi() {
  return requestClient.get<PinganPreShiftMeetingApi.OrgNode[]>(
    '/pingan/org/company-tree',
  );
}

export async function getPinganUsersApi() {
  return requestClient.get<PinganPreShiftMeetingApi.UserOption[]>(
    '/pingan/users',
  );
}

export async function getPreShiftMeetingsApi(
  params: PinganPreShiftMeetingApi.MeetingListParams,
) {
  return requestClient.get<
    PinganPreShiftMeetingApi.PageResult<PinganPreShiftMeetingApi.MeetingRow>
  >('/pingan/pre-shift-meetings', { params });
}

export async function getPreShiftMeetingDetailApi(id: string) {
  return requestClient.get<PinganPreShiftMeetingApi.MeetingDetail>(
    `/pingan/pre-shift-meetings/${id}`,
  );
}

export async function createPreShiftMeetingApi(
  data: PinganPreShiftMeetingApi.MeetingPayload,
) {
  return requestClient.post<PinganPreShiftMeetingApi.MeetingDetail>(
    '/pingan/pre-shift-meetings',
    data,
  );
}

export async function updatePreShiftMeetingApi(
  id: string,
  data: PinganPreShiftMeetingApi.MeetingPayload,
) {
  return requestClient.put<PinganPreShiftMeetingApi.MeetingDetail>(
    `/pingan/pre-shift-meetings/${id}`,
    data,
  );
}

export async function submitPreShiftMeetingApi(id: string) {
  return requestClient.post<PinganPreShiftMeetingApi.MeetingDetail>(
    `/pingan/pre-shift-meetings/${id}/submit`,
  );
}

export async function withdrawPreShiftMeetingApi(id: string, reason: string) {
  return requestClient.post<PinganPreShiftMeetingApi.MeetingDetail>(
    `/pingan/pre-shift-meetings/${id}/withdraw`,
    { reason },
  );
}

export async function remindPreShiftMeetingApi(id: string) {
  return requestClient.post<PinganPreShiftMeetingApi.MeetingDetail>(
    `/pingan/pre-shift-meetings/${id}/remind`,
  );
}

export async function deletePreShiftMeetingApi(id: string) {
  return requestClient.delete<void>(`/pingan/pre-shift-meetings/${id}`);
}

export async function uploadPreShiftMeetingAttachmentApi(
  id: string,
  fileKind: 'IMAGE' | 'VIDEO',
  file: File,
) {
  const formData = new FormData();
  formData.append('fileKind', fileKind);
  formData.append('file', file);

  return requestClient.post<
    PinganPreShiftMeetingApi.MeetingDetail['attachments'][number]
  >(
    `/pingan/pre-shift-meetings/${id}/attachments`,
    formData,
    {
      headers: { 'Content-Type': 'multipart/form-data' },
    },
  );
}

export async function deletePreShiftMeetingAttachmentApi(
  id: string,
  attachmentId: string,
) {
  return requestClient.delete<void>(
    `/pingan/pre-shift-meetings/${id}/attachments/${attachmentId}`,
  );
}
