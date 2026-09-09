import { requestClient } from '#/api/request';

export namespace SystemAnnouncementApi {
  export type AudienceType = 'ALL' | 'ORG' | 'ROLE' | 'USER';
  export type Severity = 'INFO' | 'IMPORTANT' | 'URGENT';
  export type Status = 'DRAFT' | 'PUBLISHED' | 'WITHDRAWN';

  export interface Announcement {
    audienceType: AudienceType;
    content: string;
    createdAt: string;
    deliveryCount: number;
    effectiveAt?: string;
    expiresAt?: string;
    id: string;
    organizationIds: number[];
    previousVersionId?: string;
    publishedAt?: string;
    readCount: number;
    roleIds: number[];
    severity: Severity;
    status: Status;
    title: string;
    updatedAt: string;
    userIds: number[];
    versionNo: number;
    withdrawnAt?: string;
  }

  export interface AnnouncementInput {
    audienceType: AudienceType;
    content: string;
    effectiveAt?: string;
    expiresAt?: string;
    organizationIds: number[];
    roleIds: number[];
    severity: Severity;
    title: string;
    userIds: number[];
  }

  export interface Delivery {
    announcementId: string;
    deliveredAt: string;
    notificationId: string;
    organizationName?: string;
    read: boolean;
    readAt?: string;
    recipientName: string;
    recipientReason: string;
    recipientUserId: string;
  }

  export interface PageResult<T> {
    items: T[];
    total: number;
  }
}

export const getAnnouncementListApi = (
  params: { page?: number; pageSize?: number; status?: string } = {},
) =>
  requestClient.get<
    SystemAnnouncementApi.PageResult<SystemAnnouncementApi.Announcement>
  >('/system/announcements', { params });

export const getAnnouncementApi = (id: string) =>
  requestClient.get<SystemAnnouncementApi.Announcement>(
    `/system/announcements/${id}`,
  );

export const createAnnouncementApi = (
  data: SystemAnnouncementApi.AnnouncementInput,
) =>
  requestClient.post<SystemAnnouncementApi.Announcement>(
    '/system/announcements',
    data,
  );

export const updateAnnouncementApi = (
  id: string,
  data: SystemAnnouncementApi.AnnouncementInput,
) =>
  requestClient.put<SystemAnnouncementApi.Announcement>(
    `/system/announcements/${id}`,
    data,
  );

export const publishAnnouncementApi = (id: string) =>
  requestClient.post<SystemAnnouncementApi.Announcement>(
    `/system/announcements/${id}/publish`,
  );

export const withdrawAnnouncementApi = (id: string) =>
  requestClient.post<SystemAnnouncementApi.Announcement>(
    `/system/announcements/${id}/withdraw`,
  );

export const getAnnouncementDeliveriesApi = (
  params: { announcementId: string; page?: number; pageSize?: number },
) =>
  requestClient.get<
    SystemAnnouncementApi.PageResult<SystemAnnouncementApi.Delivery>
  >('/system/notification-deliveries', { params });
