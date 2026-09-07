import { requestClient } from '#/api/request';

export namespace PinganContentProfileApi {
  export type Id = number | string;

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
    description?: string;
    id: Id;
    imageAttachment?: Attachment;
    orgId: Id;
    orgName?: string;
    orgType?: string;
    status?: string;
    subtitle?: string;
    title: string;
    videoAttachment?: Attachment;
    videoSortOrder?: number;
    videoTitle?: string;
  }

  export interface ScreenVideoParams {
    orgId?: Id;
  }
}

export function getActiveContentProfileByOrgApi(
  orgId: PinganContentProfileApi.Id,
) {
  return requestClient.get<PinganContentProfileApi.ContentProfile | null>(
    `/pingan/content-profiles/by-org/${orgId}`,
  );
}

export function getScreenContentProfileVideosApi(
  params?: PinganContentProfileApi.ScreenVideoParams,
) {
  return requestClient.get<PinganContentProfileApi.ContentProfile[]>(
    '/pingan/content-profiles/screen-videos',
    { params },
  );
}
