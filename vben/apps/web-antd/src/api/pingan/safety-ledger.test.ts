import { beforeEach, describe, expect, it, vi } from 'vitest';

import { requestClient } from '#/api/request';

import { getSafetyLedgerRowsApi } from './safety-ledger';
import {
  batchDeleteSafetyLedgerDocumentsApi,
  createSafetyLedgerDocumentApi,
  deleteSafetyLedgerDocumentApi,
  updateSafetyLedgerDocumentApi,
} from './safety-ledger';

vi.mock('#/api/request', () => ({
  requestClient: {
    get: vi.fn(),
    delete: vi.fn(),
    post: vi.fn(),
  },
}));

describe('safety-ledger api adapter', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('routes self-maintained document ledgers to the safety ledger document source', async () => {
    vi.mocked(requestClient.get).mockResolvedValueOnce({
      items: [
        {
          attachment: {
            originalName: '制度.pdf',
            url: '/uploads/safety-ledger/制度.pdf',
          },
          company: 'Demo Works Company',
          createdAt: '2026-05-20T10:00:00.640438',
          id: 16,
          name: '安全生产制度',
          richText: '制度正文',
        },
      ],
      total: 1,
    });

    const result = await getSafetyLedgerRowsApi({
      ledgerKey: 'safety-rules',
      page: 1,
      pageSize: 20,
      sourceKind: 'DOCUMENT_LEDGER',
    });

    expect(requestClient.get).toHaveBeenCalledWith(
      '/pingan/safety-ledger/documents',
      {
        params: { ledgerKey: 'safety-rules', page: 1, pageSize: 20 },
      },
    );
    expect(result.items[0]).toMatchObject({
      enterprise: 'Demo Works Company',
      file: '制度.pdf',
      fileUrl: '/uploads/safety-ledger/制度.pdf',
      name: '安全生产制度',
      richText: '制度正文',
      uploadedAt: '2026-05-20 10:00:00',
    });
  });

  it('passes configured filter bar fields through to ledger sources', async () => {
    vi.mocked(requestClient.get).mockResolvedValueOnce({ items: [], total: 0 });

    await getSafetyLedgerRowsApi({
      checkUnit: '检查单位',
      companyId: 4,
      dateEnd: '2026-05-29',
      dateStart: '2026-05-01',
      departmentId: 101,
      inspectedUnit: '受检单位',
      ledgerKey: 'hidden-danger-checklist',
      moduleKey: 'hazard-rectification',
      page: 1,
      pageSize: 20,
      sourceKind: 'THREE_CHECK_RECORD',
      status: '待整改',
      teamId: 1001,
      uploadedEnd: '2026-05-29',
      uploadedStart: '2026-05-01',
      workshopId: 101,
    });

    expect(requestClient.get).toHaveBeenCalledWith(
      '/pingan/three-checks/hazard-rectification/records',
      {
        params: {
          checkUnit: '检查单位',
          companyId: 4,
          dateEnd: '2026-05-29',
          dateStart: '2026-05-01',
          departmentId: 101,
          inspectedUnit: '受检单位',
          ledgerKey: 'hidden-danger-checklist',
          page: 1,
          pageSize: 20,
          status: '待整改',
          teamId: 1001,
          uploadedEnd: '2026-05-29',
          uploadedStart: '2026-05-01',
          workshopId: 101,
        },
      },
    );
  });

  it('creates document ledgers with multipart upload and batch deletes selected rows', async () => {
    const file = new File(['doc'], '制度.docx', {
      type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    });

    await createSafetyLedgerDocumentApi({
      companyId: 4,
      file,
      ledgerKey: 'safety-rules',
      name: '安全制度',
      richText: '制度正文',
    });
    await batchDeleteSafetyLedgerDocumentsApi([16, 17]);

    const formData = vi.mocked(requestClient.post).mock.calls[0]?.[1] as FormData;
    expect(requestClient.post).toHaveBeenNthCalledWith(
      1,
      '/pingan/safety-ledger/documents',
      expect.any(FormData),
      { headers: { 'Content-Type': 'multipart/form-data' } },
    );
    expect(formData.get('ledgerKey')).toBe('safety-rules');
    expect(formData.get('companyId')).toBe('4');
    expect(formData.get('file')).toBe(file);
    expect(requestClient.post).toHaveBeenNthCalledWith(
      2,
      '/pingan/safety-ledger/documents/batch-delete',
      { ids: [16, 17] },
    );
  });

  it('updates and deletes a single document ledger row from operation actions', async () => {
    const file = new File(['pdf'], '更新.pdf', { type: 'application/pdf' });

    await updateSafetyLedgerDocumentApi(16, {
      companyId: 4,
      file,
      ledgerKey: 'safety-rules',
      name: '更新安全制度',
      richText: '更新正文',
    });
    await deleteSafetyLedgerDocumentApi(16);

    const formData = vi.mocked(requestClient.post).mock.calls[0]?.[1] as FormData;
    expect(requestClient.post).toHaveBeenCalledWith(
      '/pingan/safety-ledger/documents/16',
      expect.any(FormData),
      { headers: { 'Content-Type': 'multipart/form-data' } },
    );
    expect(formData.get('name')).toBe('更新安全制度');
    expect(formData.get('file')).toBe(file);
    expect(requestClient.delete).toHaveBeenCalledWith(
      '/pingan/safety-ledger/documents/16',
    );
  });

  it('normalizes one-shift three-check records', async () => {
    vi.mocked(requestClient.get).mockResolvedValueOnce({
      items: [
        {
          businessDate: '2026-05-15',
          company: 'Demo Works Company',
          department: '幕墙车间',
          id: '11',
          imageCheck: '现场照片',
          imagePreviewUrl: '/uploads/pre-shift-meeting/11/image/a.jpg',
          payload: { dispatchStatus: '已生效' },
          rootDispatchRecordId: '88',
          team: 'Demo Harbor班组',
          videoCheck: '视频已传',
          videoPreviewUrl: '/uploads/pre-shift-meeting/11/video/a.mp4',
        },
      ],
      total: 1,
    });

    const result = await getSafetyLedgerRowsApi({
      moduleKey: 'team-dispatch',
      page: 1,
      pageSize: 20,
      sourceKind: 'THREE_CHECK_RECORD',
    });

    expect(requestClient.get).toHaveBeenCalledWith(
      '/pingan/three-checks/team-dispatch/records',
      {
        params: { page: 1, pageSize: 20 },
      },
    );
    expect(result.items[0]).toMatchObject({
      date: '2026-05-15',
      department: '幕墙车间',
      enterprise: 'Demo Works Company',
      id: '11',
      imageCheck: '现场照片',
      imagePreviewUrl: '/uploads/pre-shift-meeting/11/image/a.jpg',
      rootDispatchRecordId: '88',
      videoCheck: '视频已传',
      videoPreviewUrl: '/uploads/pre-shift-meeting/11/video/a.mp4',
    });
  });

  it('normalizes safety points ranking and flow records', async () => {
    vi.mocked(requestClient.get)
      .mockResolvedValueOnce({
        items: [
          {
            company: 'Demo Works Company',
            currentScore: 88,
            department: '安全部',
            lastBusinessDate: '2026-05-18',
            userName: '张三',
          },
        ],
        total: 1,
      })
      .mockResolvedValueOnce({
        items: [
          {
            businessDate: '2026-05-19',
            company: 'Demo Works Company',
            department: '安全部',
            id: 'PF-1',
            payload: {
              pointsChange: '加分',
              pointsQuantity: 5,
              pointsReason: '安全学习',
              user: '张三',
            },
            team: '一班',
          },
        ],
        total: 1,
      });

    const ranking = await getSafetyLedgerRowsApi({
      page: 1,
      pageSize: 20,
      sourceKind: 'POINTS_INDIVIDUAL_RANKING',
    });
    const flow = await getSafetyLedgerRowsApi({
      moduleKey: 'points-flow',
      page: 1,
      pageSize: 20,
      sourceKind: 'POINTS_FLOW',
    });

    expect(requestClient.get).toHaveBeenNthCalledWith(
      1,
      '/pingan/safety-points/ranking/individual',
      {
        params: { page: 1, pageSize: 20 },
      },
    );
    expect(ranking.items[0]).toMatchObject({
      company: 'Demo Works Company',
      name: '张三',
      points: 88,
    });
    expect(flow.items[0]).toMatchObject({
      pointsChange: '加分',
      pointsQuantity: 5,
      pointsReason: '安全学习',
      user: '张三',
    });
  });

  it('normalizes special work records', async () => {
    vi.mocked(requestClient.get).mockResolvedValueOnce({
      items: [
        {
          applicationTime: '2026-05-20 10:00:00',
          company: 'Demo Works Company',
          id: 8,
          workType: '动火作业',
        },
      ],
      total: 1,
    });

    const result = await getSafetyLedgerRowsApi({
      page: 1,
      pageSize: 20,
      sourceKind: 'SPECIAL_WORK',
    });

    expect(requestClient.get).toHaveBeenCalledWith(
      '/pingan/special-work/records',
      {
        params: { page: 1, pageSize: 20 },
      },
    );
    expect(result.items[0]).toMatchObject({
      date: '2026-05-20 10:00:00',
      enterprise: 'Demo Works Company',
      operationType: '动火作业',
    });
  });
});
