import { beforeEach, describe, expect, it, vi } from 'vitest';
import { readFileSync } from 'node:fs';
import { join } from 'node:path';

import { requestClient } from '#/api/request';

import {
  batchThreeCheckRecordsApi,
  createThreeCheckRecordApi,
  deleteThreeCheckRecordAttachmentApi,
  deleteThreeCheckRecordApi,
  getHazardInspectionChangeHistoryApi,
  getHazardInspectionDocumentFlowApi,
  getThreeCheckRecordDetailApi,
  getThreeCheckRecordFlowApi,
  getThreeCheckRecordsApi,
  getThreeCheckRecordStatisticsApi,
  getThreeCheckRecordWorkflowApi,
  openThreeCheckRectificationOrderApi,
  remindThreeCheckRecordApi,
  submitThreeCheckRecordApi,
  updateThreeCheckRecordApi,
  uploadThreeCheckRecordAttachmentApi,
  withdrawThreeCheckRecordApi,
} from './three-check-record';

vi.mock('#/api/request', () => ({
  requestClient: {
    delete: vi.fn(),
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
  },
}));

describe('three-check-record api', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('maps generic one-shift-three-check list and detail endpoints', async () => {
    await getThreeCheckRecordsApi('pre-shift-inspection', {
      company: 'Demo Works Company',
      companyId: 4,
      dateStart: '2026-05-01',
      departmentId: 101_109,
      overdue: true,
      page: 1,
      pageSize: 20,
      sourceChannel: 'WECHAT_MINI_PROGRAM',
      status: 'OPENED',
      teamId: 1_011_001,
    });
    await getThreeCheckRecordDetailApi('pre-shift-inspection', '3001');

    expect(requestClient.get).toHaveBeenNthCalledWith(
      1,
      '/pingan/three-checks/pre-shift-inspection/records',
      {
        params: {
          company: 'Demo Works Company',
          companyId: 4,
          dateStart: '2026-05-01',
          departmentId: 101_109,
          overdue: true,
          page: 1,
          pageSize: 20,
          sourceChannel: 'WECHAT_MINI_PROGRAM',
          status: 'OPENED',
          teamId: 1_011_001,
        },
      },
    );
    expect(requestClient.get).toHaveBeenNthCalledWith(
      2,
      '/pingan/three-checks/pre-shift-inspection/records/3001',
    );
  });

  it('passes points ledger reason filter to the generic records endpoint', async () => {
    await getThreeCheckRecordsApi('points-flow', {
      companyId: 4,
      dateEnd: '2026-05-22',
      dateStart: '2026-05-22',
      page: 1,
      pageSize: 20,
      pointsReason: '兑换',
    });

    expect(requestClient.get).toHaveBeenCalledWith(
      '/pingan/three-checks/points-flow/records',
      {
        params: {
          companyId: 4,
          dateEnd: '2026-05-22',
          dateStart: '2026-05-22',
          page: 1,
          pageSize: 20,
          pointsReason: '兑换',
        },
      },
    );
  });

  it('maps generic one-shift-three-check statistics endpoint', async () => {
    await getThreeCheckRecordStatisticsApi('pre-shift-inspection', {
      companyId: 4,
      dateEnd: '2026-06-30',
      dateStart: '2026-06-01',
      organizationId: 4,
      sourceChannel: 'PC',
      status: 'all',
    });

    expect(requestClient.get).toHaveBeenCalledWith(
      '/pingan/three-checks/pre-shift-inspection/records/statistics',
      {
        params: {
          companyId: 4,
          dateEnd: '2026-06-30',
          dateStart: '2026-06-01',
          organizationId: 4,
          sourceChannel: 'PC',
          status: 'all',
        },
      },
    );
  });

  it('maps one-shift full-chain flow endpoint by root dispatch record id', async () => {
    await getThreeCheckRecordFlowApi('100000000003');

    expect(requestClient.get).toHaveBeenCalledWith(
      '/pingan/three-checks/flows/100000000003',
    );
  });

  it('maps hazard inspection document flow endpoint', async () => {
    await getHazardInspectionDocumentFlowApi('safety-check', '1001');

    expect(requestClient.get).toHaveBeenCalledWith(
      '/pingan/hazard-inspection/safety-check/records/1001/document-flow',
    );
  });

  it('keeps quick-shot workflow type contract aligned with unified rectification orders', () => {
    const source = readFileSync(
      join(process.cwd(), 'apps/web-antd/src/api/pingan/three-check-record.ts'),
      'utf8',
    );
    const quickShotActionType = source.slice(
      source.indexOf('export type QuickShotWorkflowAction'),
      source.indexOf('export type RecordStatus'),
    );

    expect(quickShotActionType).toContain("'APPROVE'");
    expect(quickShotActionType).toContain("'REJECT'");
    expect(quickShotActionType).not.toContain("'ISSUE_RECTIFICATION'");
    expect(quickShotActionType).not.toContain("'MARK_RECTIFIED'");
    expect(quickShotActionType).not.toContain("'REQUEST_ACCEPTANCE'");
    expect(quickShotActionType).not.toContain("'ACCEPT'");
    expect(source).toContain(
      'linkedRectificationOrder?: PinganHazardRectificationOrderApi.OrderDetail',
    );
  });

  it('maps hazard inspection change history endpoint', async () => {
    await getHazardInspectionChangeHistoryApi('safety-check', '1001');

    expect(requestClient.get).toHaveBeenCalledWith(
      '/pingan/hazard-inspection/safety-check/records/1001/change-history',
    );
  });

  it('maps generic create, update, workflow, and attachment endpoints', async () => {
    const payload = {
      businessDate: '2026-05-15',
      companyId: 4,
      departmentId: 101_109,
      payload: { owner: '湖贝班长', statusLabel: '待检查' },
      teamId: 1_011_001,
    };

    await createThreeCheckRecordApi('pre-shift-inspection', payload);
    await updateThreeCheckRecordApi('pre-shift-inspection', '3001', {
      ...payload,
      version: 1,
    });
    await submitThreeCheckRecordApi('pre-shift-inspection', '3001');
    await withdrawThreeCheckRecordApi(
      'pre-shift-inspection',
      '3001',
      '内容填写错误',
    );
    await remindThreeCheckRecordApi('pre-shift-inspection', '3001');
    await openThreeCheckRectificationOrderApi(
      'pre-shift-inspection',
      '3001',
    );
    await deleteThreeCheckRecordApi('pre-shift-inspection', '3001');
    await batchThreeCheckRecordsApi('pre-shift-inspection', {
      action: 'DELETE',
      ids: ['3001', '3002'],
      reason: '批量清理',
    });
    await getThreeCheckRecordWorkflowApi('pre-shift-inspection', '3001');

    expect(requestClient.post).toHaveBeenNthCalledWith(
      1,
      '/pingan/three-checks/pre-shift-inspection/records',
      payload,
    );
    expect(requestClient.put).toHaveBeenCalledWith(
      '/pingan/three-checks/pre-shift-inspection/records/3001',
      {
        ...payload,
        version: 1,
      },
    );
    expect(requestClient.post).toHaveBeenNthCalledWith(
      2,
      '/pingan/three-checks/pre-shift-inspection/records/3001/submit',
    );
    expect(requestClient.post).toHaveBeenNthCalledWith(
      3,
      '/pingan/three-checks/pre-shift-inspection/records/3001/withdraw',
      { reason: '内容填写错误' },
    );
    expect(requestClient.post).toHaveBeenNthCalledWith(
      4,
      '/pingan/three-checks/pre-shift-inspection/records/3001/remind',
    );
    expect(requestClient.post).toHaveBeenNthCalledWith(
      5,
      '/pingan/three-checks/pre-shift-inspection/records/3001/rectification-order',
    );
    expect(requestClient.delete).toHaveBeenCalledWith(
      '/pingan/three-checks/pre-shift-inspection/records/3001',
    );
    expect(requestClient.post).toHaveBeenNthCalledWith(
      6,
      '/pingan/three-checks/pre-shift-inspection/records/batch',
      {
        action: 'DELETE',
        ids: ['3001', '3002'],
        reason: '批量清理',
      },
    );
    expect(requestClient.get).toHaveBeenCalledWith(
      '/pingan/three-checks/pre-shift-inspection/records/3001/workflow',
    );

    const file = new File(['image'], 'inspection.jpg', { type: 'image/jpeg' });
    await uploadThreeCheckRecordAttachmentApi(
      'pre-shift-inspection',
      '3001',
      'IMAGE',
      file,
    );
    await deleteThreeCheckRecordAttachmentApi(
      'pre-shift-inspection',
      '3001',
      '9001',
    );

    expect(requestClient.post).toHaveBeenNthCalledWith(
      7,
      '/pingan/three-checks/pre-shift-inspection/records/3001/attachments',
      expect.any(FormData),
      {
        headers: { 'Content-Type': 'multipart/form-data' },
      },
    );
    expect(requestClient.delete).toHaveBeenCalledWith(
      '/pingan/three-checks/pre-shift-inspection/records/3001/attachments/9001',
    );
  });
});

