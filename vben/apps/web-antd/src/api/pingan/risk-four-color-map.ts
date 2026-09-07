import { requestClient } from '#/api/request';

export namespace PinganRiskFourColorMapApi {
  export type Id = number | string;

  export interface Attachment {
    contentType?: string;
    fileKind?: string;
    fileSize?: number;
    id: Id;
    originalName?: string;
    storagePath?: string;
    url?: string;
  }

  export interface PageResult<T> {
    items: T[];
    total: number;
  }

  export interface ListParams {
    companyId?: Id;
    keyword?: string;
    page?: number;
    pageSize?: number;
  }

  export interface Payload {
    companyId?: Id;
    name: string;
    remark?: string;
  }

  export interface FourColorMap {
    backgroundAttachment?: Attachment | null;
    company: string;
    companyId: Id;
    createdAt?: string;
    id: Id;
    name: string;
    remark?: string;
    updatedAt?: string;
  }
}

const basePath = '/pingan/risk-four-color-maps';

export function getRiskFourColorMapsApi(
  params: PinganRiskFourColorMapApi.ListParams = {},
) {
  return requestClient.get<
    PinganRiskFourColorMapApi.PageResult<PinganRiskFourColorMapApi.FourColorMap>
  >(basePath, { params });
}

export function createRiskFourColorMapApi(
  data: PinganRiskFourColorMapApi.Payload,
) {
  return requestClient.post<PinganRiskFourColorMapApi.FourColorMap>(
    basePath,
    data,
  );
}

export function updateRiskFourColorMapApi(
  id: PinganRiskFourColorMapApi.Id,
  data: PinganRiskFourColorMapApi.Payload,
) {
  return requestClient.put<PinganRiskFourColorMapApi.FourColorMap>(
    `${basePath}/${id}`,
    data,
  );
}

export function deleteRiskFourColorMapApi(id: PinganRiskFourColorMapApi.Id) {
  return requestClient.delete(`${basePath}/${id}`);
}

export function uploadRiskFourColorMapBackgroundApi(
  id: PinganRiskFourColorMapApi.Id,
  file: File,
) {
  const formData = new FormData();
  formData.append('file', file);

  return requestClient.post<PinganRiskFourColorMapApi.FourColorMap>(
    `${basePath}/${id}/background`,
    formData,
    {
      headers: { 'Content-Type': 'multipart/form-data' },
    },
  );
}
