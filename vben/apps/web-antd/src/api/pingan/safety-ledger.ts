import { requestClient } from '#/api/request';

import type { SafetyLedgerSourceKind } from '#/views/pingan/safety-ledger/safety-ledger.config';

export namespace PinganSafetyLedgerApi {
  export type Id = number | string;

  export interface ListParams {
    checkUnit?: string;
    companyId?: Id;
    dateEnd?: string;
    dateStart?: string;
    departmentId?: Id;
    inspectedUnit?: string;
    keyword?: string;
    ledgerKey?: string;
    moduleKey?: string;
    page?: number;
    pageSize?: number;
    sourceKind: SafetyLedgerSourceKind;
    status?: string;
    teamId?: Id;
    uploadedEnd?: string;
    uploadedStart?: string;
    workshopId?: Id;
  }

  export interface Attachment {
    contentType: string;
    fileKind: string;
    fileSize: number;
    id: string;
    originalName: string;
    storagePath: string;
    url: string;
  }

  export interface PageResult<T> {
    items: T[];
    total: number;
  }

  export interface SafetyLedgerDocumentPayload {
    companyId: Id;
    date?: string;
    department?: string;
    file?: File;
    ledgerKey: string;
    name: string;
    richText?: string;
    team?: string;
    type?: string;
  }

  export interface SafetyLedgerRow {
    [key: string]: unknown;
    id: Id;
  }
}

type UnknownRecord = Record<string, any>;

function cleanParams(params: PinganSafetyLedgerApi.ListParams) {
  return Object.fromEntries(
    Object.entries({
      checkUnit: params.checkUnit,
      companyId: params.companyId,
      dateEnd: params.dateEnd,
      dateStart: params.dateStart,
      departmentId: params.departmentId,
      inspectedUnit: params.inspectedUnit,
      keyword: params.keyword,
      ledgerKey: params.ledgerKey,
      page: params.page,
      pageSize: params.pageSize,
      status: params.status,
      teamId: params.teamId,
      uploadedEnd: params.uploadedEnd,
      uploadedStart: params.uploadedStart,
      workshopId: params.workshopId,
    }).filter(
      ([, value]) => value !== '' && value !== undefined && value !== 'all',
    ),
  );
}

function documentLedgerParams(params: PinganSafetyLedgerApi.ListParams) {
  return cleanParams(params);
}

function pageResult(
  response: PinganSafetyLedgerApi.PageResult<UnknownRecord>,
  mapper: (record: UnknownRecord, index: number) => UnknownRecord,
): PinganSafetyLedgerApi.PageResult<PinganSafetyLedgerApi.SafetyLedgerRow> {
  return {
    items: response.items.map((record, index) => ({
      id: record.id ?? `${index + 1}`,
      ...mapper(record, index),
    })),
    total: Number(response.total ?? response.items.length),
  };
}

function payload(record: UnknownRecord) {
  return record.payload && typeof record.payload === 'object'
    ? record.payload
    : {};
}

function mapThreeCheckRecord(record: UnknownRecord, index: number) {
  const extra = payload(record);
  return {
    acceptanceStatus: extra.acceptanceResult ?? extra.rectificationAcceptanceResult ?? '',
    acceptor: extra.acceptancePerson ?? '',
    attachment: extra.attachment ?? extra.safetyActivityRecordUpload ?? '',
    attendees: extra.attendeesText ?? extra.attendees ?? '',
    checkUnit: extra.checkUnit ?? extra.inspectionUnit ?? record.company ?? '',
    company: record.company ?? '',
    createdAt: record.createdAt ?? extra.createdAt ?? '',
    date: record.businessDate ?? record.date ?? extra.date ?? extra.dispatchDate ?? '',
    department: record.department ?? '',
    description: extra.hazardDescription ?? extra.description ?? '',
    enterprise: record.company ?? '',
    expand: '',
    hazardCount: extra.hazardCount ?? extra.detailCount ?? '',
    imageCheck: record.imageCheck ?? extra.imageUpload ?? '',
    imagePreviewUrl: record.imagePreviewUrl ?? extra.imagePreviewUrl ?? '',
    inspectedUnit: extra.inspectedUnit ?? extra.responsibleUnit ?? record.department ?? '',
    inspectionDate: record.businessDate ?? extra.inspectionDate ?? '',
    inspectionObject: extra.inspectionObject ?? extra.inspectionContent ?? '',
    inspectionType: extra.inspectionType ?? extra.routineCheckType ?? '',
    inspector: extra.inspector ?? extra.checkPerson ?? extra.inspectionPersonnel ?? '',
    inspectorCount: extra.inspectorCount ?? '',
    inspectors: extra.inspectors ?? extra.inspectionPersonnel ?? '',
    owner: extra.owner ?? record.owner ?? '',
    rectificationCompletedAt:
      extra.rectificationCompletedAt ?? extra.completionAcceptanceTime ?? '',
    rectificationDeadline: extra.rectificationDeadline ?? '',
    rectifiedCount: extra.rectifiedCount ?? '',
    responsiblePerson: extra.responsiblePerson ?? extra.rectificationResponsiblePerson ?? '',
    rootDispatchRecordId:
      record.rootDispatchRecordId ?? extra.rootDispatchRecordId ?? '',
    sequence: index + 1,
    serialNo: record.recordNo ?? record.id ?? '',
    siteType: extra.siteType ?? '',
    sort: index + 1,
    status: record.statusLabel ?? extra.statusLabel ?? record.status ?? '',
    team: record.team ?? '',
    time: record.businessDate ?? '',
    user: extra.user ?? record.owner ?? '',
    videoCheck: record.videoCheck ?? extra.videoUpload ?? '',
    videoPreviewUrl: record.videoPreviewUrl ?? extra.videoPreviewUrl ?? '',
    ...extra,
  };
}

function mapIndividualRanking(record: UnknownRecord) {
  return {
    company: record.company ?? '',
    date: record.lastBusinessDate ?? '',
    department: record.department ?? '',
    name: record.userName ?? '',
    points: record.currentScore ?? '',
  };
}

function mapTeamRanking(record: UnknownRecord) {
  return {
    company: record.company ?? '',
    date: record.lastBusinessDate ?? '',
    department: record.department ?? '',
    leader: record.leader ?? '',
    name: record.rankName ?? '',
    points: record.currentScore ?? '',
  };
}

function mapPointsFlow(record: UnknownRecord) {
  const extra = payload(record);
  return {
    company: record.company ?? '',
    createdAt: record.createdAt ?? record.businessDate ?? '',
    department: record.department ?? '',
    goods: extra.goods ?? extra.product ?? extra.cargo ?? '',
    pointsChange: extra.pointsChange ?? record.statusLabel ?? '',
    pointsQuantity: extra.pointsQuantity ?? '',
    pointsReason: extra.pointsReason ?? '',
    serialNo: record.recordNo ?? record.id ?? '',
    team: record.team ?? '',
    user: extra.user ?? record.owner ?? '',
    vendingMachine: extra.vendingMachine ?? '',
  };
}

function mapSpecialWork(record: UnknownRecord) {
  return {
    date: record.applicationTime ?? '',
    enterprise: record.company ?? '',
    operationType: record.workType ?? '',
  };
}

function mapSystemAccount(record: UnknownRecord) {
  return {
    company: record.companyName ?? record.organizationName ?? record.orgName ?? '',
    companyShortName: record.companyShortName ?? '',
    department: record.departmentName ?? '',
    employeeType: record.employeeType ?? '',
    mobile: record.mobile ?? '',
    name: record.realName ?? record.name ?? record.username ?? '',
    post: record.post ?? record.positionName ?? '',
    receivedPoints: record.receivedPoints ?? record.points ?? '',
    status: record.status ?? '',
    systemRole: Array.isArray(record.roles)
      ? record.roles.map((role: UnknownRecord) => role.roleName ?? role.name).join('、')
      : '',
    team: record.teamName ?? '',
  };
}

function attachmentName(record: UnknownRecord) {
  const attachment = record.attachment;
  if (attachment && typeof attachment === 'object') {
    return attachment.originalName ?? attachment.name ?? '';
  }
  return record.fileName ?? record.file ?? '';
}

function attachmentUrl(record: UnknownRecord) {
  const attachment = record.attachment;
  if (attachment && typeof attachment === 'object') {
    return attachment.url ?? '';
  }
  return record.fileUrl ?? '';
}

function displayDateTime(value: unknown) {
  if (typeof value !== 'string' || value.length === 0) {
    return value ?? '';
  }
  return value.replace('T', ' ').replace(/\.\d+$/, '').slice(0, 19);
}

function mapDocumentLedger(record: UnknownRecord, index: number) {
  return {
    company: record.company ?? record.companyName ?? '',
    companyId: record.companyId ?? '',
    date: record.date ?? record.documentDate ?? '',
    department: record.department ?? record.departmentName ?? '',
    enterprise: record.enterprise ?? record.company ?? record.companyName ?? '',
    file: attachmentName(record),
    fileUrl: attachmentUrl(record),
    name: record.name ?? '',
    richText: record.richText ?? record.content ?? '',
    sequence: index + 1,
    team: record.team ?? record.teamName ?? '',
    type: record.type ?? record.documentType ?? '',
    uploadedAt: displayDateTime(record.uploadedAt ?? record.createdAt),
  };
}

function appendIfPresent(formData: FormData, key: string, value: unknown) {
  if (value !== undefined && value !== null && value !== '') {
    formData.append(key, String(value));
  }
}

export async function getSafetyLedgerRowsApi(
  params: PinganSafetyLedgerApi.ListParams,
): Promise<PinganSafetyLedgerApi.PageResult<PinganSafetyLedgerApi.SafetyLedgerRow>> {
  const requestParams = cleanParams(params);

  if (params.sourceKind === 'EMPTY') {
    return { items: [], total: 0 };
  }

  if (params.sourceKind === 'DOCUMENT_LEDGER') {
    const documentParams = documentLedgerParams(params);
    const response = await requestClient.get<
      PinganSafetyLedgerApi.PageResult<UnknownRecord>
    >('/pingan/safety-ledger/documents', { params: documentParams });
    return pageResult(response, mapDocumentLedger);
  }

  if (params.sourceKind === 'THREE_CHECK_RECORD' && params.moduleKey) {
    const response = await requestClient.get<
      PinganSafetyLedgerApi.PageResult<UnknownRecord>
    >(`/pingan/three-checks/${params.moduleKey}/records`, {
      params: requestParams,
    });
    return pageResult(response, mapThreeCheckRecord);
  }

  if (params.sourceKind === 'POINTS_INDIVIDUAL_RANKING') {
    const response = await requestClient.get<
      PinganSafetyLedgerApi.PageResult<UnknownRecord>
    >('/pingan/safety-points/ranking/individual', { params: requestParams });
    return pageResult(response, mapIndividualRanking);
  }

  if (params.sourceKind === 'POINTS_TEAM_RANKING') {
    const response = await requestClient.get<
      PinganSafetyLedgerApi.PageResult<UnknownRecord>
    >('/pingan/safety-points/ranking/team', { params: requestParams });
    return pageResult(response, mapTeamRanking);
  }

  if (params.sourceKind === 'POINTS_FLOW') {
    const response = await requestClient.get<
      PinganSafetyLedgerApi.PageResult<UnknownRecord>
    >('/pingan/three-checks/points-flow/records', { params: requestParams });
    return pageResult(response, mapPointsFlow);
  }

  if (params.sourceKind === 'SPECIAL_WORK') {
    const response = await requestClient.get<
      PinganSafetyLedgerApi.PageResult<UnknownRecord>
    >('/pingan/special-work/records', { params: requestParams });
    return pageResult(response, mapSpecialWork);
  }

  if (params.sourceKind === 'SYSTEM_ACCOUNTS') {
    const response = await requestClient.get<
      PinganSafetyLedgerApi.PageResult<UnknownRecord>
    >('/system/accounts', { params: requestParams });
    return pageResult(response, mapSystemAccount);
  }

  return { items: [], total: 0 };
}

export function createSafetyLedgerDocumentApi(
  data: PinganSafetyLedgerApi.SafetyLedgerDocumentPayload,
) {
  const formData = new FormData();
  formData.append('ledgerKey', data.ledgerKey);
  formData.append('name', data.name);
  formData.append('companyId', String(data.companyId));
  if (data.file) {
    formData.append('file', data.file);
  }
  appendIfPresent(formData, 'richText', data.richText);
  appendIfPresent(formData, 'department', data.department);
  appendIfPresent(formData, 'team', data.team);
  appendIfPresent(formData, 'type', data.type);
  appendIfPresent(formData, 'date', data.date);

  return requestClient.post<PinganSafetyLedgerApi.SafetyLedgerRow>(
    '/pingan/safety-ledger/documents',
    formData,
    { headers: { 'Content-Type': 'multipart/form-data' } },
  );
}

export function updateSafetyLedgerDocumentApi(
  id: PinganSafetyLedgerApi.Id,
  data: PinganSafetyLedgerApi.SafetyLedgerDocumentPayload,
) {
  const formData = new FormData();
  formData.append('ledgerKey', data.ledgerKey);
  formData.append('name', data.name);
  formData.append('companyId', String(data.companyId));
  if (data.file) {
    formData.append('file', data.file);
  }
  appendIfPresent(formData, 'richText', data.richText);
  appendIfPresent(formData, 'department', data.department);
  appendIfPresent(formData, 'team', data.team);
  appendIfPresent(formData, 'type', data.type);
  appendIfPresent(formData, 'date', data.date);

  return requestClient.post<PinganSafetyLedgerApi.SafetyLedgerRow>(
    `/pingan/safety-ledger/documents/${id}`,
    formData,
    { headers: { 'Content-Type': 'multipart/form-data' } },
  );
}

export function deleteSafetyLedgerDocumentApi(id: PinganSafetyLedgerApi.Id) {
  return requestClient.delete<void>(`/pingan/safety-ledger/documents/${id}`);
}

export function batchDeleteSafetyLedgerDocumentsApi(
  ids: PinganSafetyLedgerApi.Id[],
) {
  return requestClient.post<void>('/pingan/safety-ledger/documents/batch-delete', {
    ids,
  });
}
