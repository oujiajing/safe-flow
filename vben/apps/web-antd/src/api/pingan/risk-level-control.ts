import { requestClient } from '#/api/request';

export namespace PinganRiskLevelControlApi {
  export type Id = number | string;

  export interface PageResult<T> {
    items: T[];
    total: number;
  }

  export interface LibraryListParams {
    keyword?: string;
    page?: number;
    pageSize?: number;
  }

  export interface HazardListParams {
    page?: number;
    pageSize?: number;
  }

  export interface HazardPayload {
    accidentType?: string;
    company?: string;
    companyId?: Id;
    consequence?: string;
    dangerSource?: string;
    emergencyMeasures?: string;
    engineeringMeasures?: string;
    exposureFrequency?: string;
    likelihood?: string;
    managementMeasures?: string;
    possibleHazard?: string;
    rectificationMeasures?: string;
    responsibleContact?: string;
    responsibleDepartment?: string;
    riskInfluenceFactors?: string;
    riskLevel?: string;
    riskPoint?: string;
    riskValue?: string;
    superiorResponsiblePerson?: string;
  }

  export interface LibraryPayload {
    hazard: HazardPayload;
    name: string;
  }

  export interface Library {
    company: string;
    companyId: Id;
    createdAt?: string;
    hazardCount: number;
    id: Id;
    name: string;
    updatedAt?: string;
  }

  export interface Hazard extends HazardPayload {
    company: string;
    companyId: Id;
    createdAt?: string;
    id: Id;
    libraryId: Id;
    updatedAt?: string;
  }

  export interface ImportResult {
    errors: string[];
    library: Library;
    successRows: number;
  }
}

const basePath = '/pingan/risk-level-control';

export function getRiskControlLibrariesApi(
  params: PinganRiskLevelControlApi.LibraryListParams = {},
) {
  return requestClient.get<
    PinganRiskLevelControlApi.PageResult<PinganRiskLevelControlApi.Library>
  >(`${basePath}/libraries`, { params });
}

export function createRiskControlLibraryApi(
  data: PinganRiskLevelControlApi.LibraryPayload,
) {
  return requestClient.post<PinganRiskLevelControlApi.Library>(
    `${basePath}/libraries`,
    data,
  );
}

export function getRiskControlHazardsApi(
  libraryId: PinganRiskLevelControlApi.Id,
  params: PinganRiskLevelControlApi.HazardListParams = {},
) {
  return requestClient.get<
    PinganRiskLevelControlApi.PageResult<PinganRiskLevelControlApi.Hazard>
  >(`${basePath}/libraries/${libraryId}/hazards`, { params });
}

export function createRiskControlHazardApi(
  libraryId: PinganRiskLevelControlApi.Id,
  data: PinganRiskLevelControlApi.HazardPayload,
) {
  return requestClient.post<PinganRiskLevelControlApi.Hazard>(
    `${basePath}/libraries/${libraryId}/hazards`,
    data,
  );
}

export function updateRiskControlHazardApi(
  libraryId: PinganRiskLevelControlApi.Id,
  hazardId: PinganRiskLevelControlApi.Id,
  data: PinganRiskLevelControlApi.HazardPayload,
) {
  return requestClient.put<PinganRiskLevelControlApi.Hazard>(
    `${basePath}/libraries/${libraryId}/hazards/${hazardId}`,
    data,
  );
}

export function downloadRiskControlTemplateApi() {
  return requestClient.get<Blob>(`${basePath}/template`, {
    responseType: 'blob',
  });
}

export function downloadRiskControlDataApi(
  libraryId: PinganRiskLevelControlApi.Id,
) {
  return requestClient.get<Blob>(`${basePath}/libraries/${libraryId}/export`, {
    responseType: 'blob',
  });
}

export function uploadRiskControlDataApi(file: File) {
  const formData = new FormData();
  formData.append('file', file);

  return requestClient.post<PinganRiskLevelControlApi.ImportResult>(
    `${basePath}/import`,
    formData,
    {
      headers: { 'Content-Type': 'multipart/form-data' },
    },
  );
}
