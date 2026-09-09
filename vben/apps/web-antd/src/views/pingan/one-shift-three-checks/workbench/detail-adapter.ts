import type {
  ThreeCheckRecordEditableDetail,
  ThreeCheckTableColumn,
} from './module-config';
import type { PinganThreeCheckRecordApi } from '#/api/pingan/three-check-record';

import {
  createThreeCheckDetailEditForm,
  createThreeCheckRecordUpdatePayloadFromDetail,
  isThreeCheckDetailEditable,
} from './module-config';

export interface OneShiftThreeCheckDetailAdapter {
  createEditForm: (
    detail: ThreeCheckRecordEditableDetail,
    columns: ThreeCheckTableColumn[],
  ) => Record<string, unknown>;
  createUpdatePayload: (
    detail: ThreeCheckRecordEditableDetail,
    form: Record<string, unknown>,
  ) => {
    businessDate: string;
    companyId?: number | string;
    departmentId?: number | string;
    ownerUserId?: number;
    payload: Record<string, unknown>;
    rootDispatchRecordId?: number | string;
    status: string;
    teamId?: number | string;
    version?: number;
  };
  isEditable: (
    detail?: Pick<ThreeCheckRecordEditableDetail, 'canSubmit' | 'status'>,
  ) => boolean;
  mapWorkflow: (
    workflow?: PinganThreeCheckRecordApi.RecordWorkflow,
  ) => OneShiftThreeCheckWorkflowPanelState;
}

export interface OneShiftThreeCheckWorkflowPanelItem {
  key: string;
  label: string;
  meta: string;
  value: string;
}

export interface OneShiftThreeCheckWorkflowPanelState {
  changeHistoryItems: OneShiftThreeCheckWorkflowPanelItem[];
  documentFlowItems: OneShiftThreeCheckWorkflowPanelItem[];
}

function statusText(item: PinganThreeCheckRecordApi.WorkflowItem) {
  const from = item.fromStatusLabel || item.fromStatus;
  const to = item.toStatusLabel || item.toStatus;
  if (from && to && from !== to) {
    return `${from} → ${to}`;
  }
  return String(to || from || item.actionLabel || item.action || '状态变更');
}

function workflowItemValue(item: PinganThreeCheckRecordApi.WorkflowItem) {
  const remark = item.remark?.trim();
  return remark ? `${statusText(item)} · ${remark}` : statusText(item);
}

function workflowItemMeta(item: PinganThreeCheckRecordApi.WorkflowItem) {
  return `${item.operatorName || '系统'} · ${item.occurredAt || '未记录时间'}`;
}

function mapWorkflowItem(item: PinganThreeCheckRecordApi.WorkflowItem) {
  return {
    key: item.id,
    label: item.actionLabel || item.action,
    meta: workflowItemMeta(item),
    value: workflowItemValue(item),
  };
}

function sortWorkflowItems(
  items: PinganThreeCheckRecordApi.WorkflowItem[],
  direction: 'asc' | 'desc',
) {
  return [...items].sort((left, right) => {
    const timeDelta =
      new Date(left.occurredAt).getTime() - new Date(right.occurredAt).getTime();
    if (timeDelta !== 0) {
      return direction === 'asc' ? timeDelta : -timeDelta;
    }
    return direction === 'asc'
      ? String(left.id).localeCompare(String(right.id))
      : String(right.id).localeCompare(String(left.id));
  });
}

export function createOneShiftThreeCheckWorkflowPanelState(
  workflow?: PinganThreeCheckRecordApi.RecordWorkflow,
): OneShiftThreeCheckWorkflowPanelState {
  return {
    changeHistoryItems: sortWorkflowItems(
      workflow?.changeHistory ?? [],
      'desc',
    ).map(mapWorkflowItem),
    documentFlowItems: sortWorkflowItems(
      workflow?.documentFlow ?? [],
      'asc',
    ).map(mapWorkflowItem),
  };
}

export const oneShiftThreeCheckDetailAdapter: OneShiftThreeCheckDetailAdapter = {
  createEditForm: createThreeCheckDetailEditForm,
  createUpdatePayload: createThreeCheckRecordUpdatePayloadFromDetail,
  isEditable: isThreeCheckDetailEditable,
  mapWorkflow: createOneShiftThreeCheckWorkflowPanelState,
};

export const createOneShiftThreeCheckDetailEditForm =
  oneShiftThreeCheckDetailAdapter.createEditForm;

export const createOneShiftThreeCheckRecordUpdatePayloadFromDetail =
  oneShiftThreeCheckDetailAdapter.createUpdatePayload;

export const isOneShiftThreeCheckDetailEditable =
  oneShiftThreeCheckDetailAdapter.isEditable;
