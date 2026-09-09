import { beforeEach, describe, expect, it, vi } from 'vitest';

import { requestClient } from '#/api/request';

import {
  actionHazardRectificationOrderApi,
  batchDeleteHazardRectificationOrdersApi,
  createHazardRectificationOrderApi,
  getHazardRectificationOrderDetailApi,
  getHazardRectificationOrdersApi,
  uploadHazardRectificationOrderAttachmentApi,
} from './hazard-rectification-order';

vi.mock('#/api/request', () => ({
  requestClient: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

describe('hazard-rectification-order api', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('maps order list, detail, and workflow action endpoints', async () => {
    const file = new File(['after'], 'after.jpg', { type: 'image/jpeg' });
    await getHazardRectificationOrdersApi({
      page: 1,
      pageSize: 20,
      sourceRecordId: '100000000025',
      sourceType: 'THREE_CHECK',
      status: 'PENDING_RECTIFY',
      teamId: 1_011_001,
    });
    await getHazardRectificationOrderDetailApi('1');
    await actionHazardRectificationOrderApi('1', {
      action: 'REJECT_ACCEPTANCE',
      payload: { rejectReason: '整改照片不清晰' },
      version: 3,
    });
    await uploadHazardRectificationOrderAttachmentApi(
      '1',
      'RECTIFICATION_AFTER_PHOTO',
      file,
    );
    await createHazardRectificationOrderApi({
      businessDate: '2026-10-01',
      companyId: 4,
      departmentId: 101_109,
      items: [
        {
          checkItem: '临边防护',
          hazardDescription: '防护栏杆缺失',
          riskType: '高处坠落',
        },
      ],
      teamId: 1_011_001,
    });
    await batchDeleteHazardRectificationOrdersApi(['1', '2']);

    expect(requestClient.get).toHaveBeenNthCalledWith(
      1,
      '/pingan/hazard-rectification/orders',
      {
        params: {
          page: 1,
          pageSize: 20,
          sourceRecordId: '100000000025',
          sourceType: 'THREE_CHECK',
          status: 'PENDING_RECTIFY',
          teamId: 1_011_001,
        },
      },
    );
    expect(requestClient.get).toHaveBeenNthCalledWith(
      2,
      '/pingan/hazard-rectification/orders/1',
    );
    expect(requestClient.post).toHaveBeenCalledWith(
      '/pingan/hazard-rectification/orders/1/actions',
      {
        action: 'REJECT_ACCEPTANCE',
        payload: { rejectReason: '整改照片不清晰' },
        version: 3,
      },
    );
    expect(requestClient.post).toHaveBeenCalledWith(
      '/pingan/hazard-rectification/orders/1/attachments',
      expect.any(FormData),
      {
        headers: { 'Content-Type': 'multipart/form-data' },
      },
    );
    expect(requestClient.post).toHaveBeenCalledWith(
      '/pingan/hazard-rectification/orders',
      {
        businessDate: '2026-10-01',
        companyId: 4,
        departmentId: 101_109,
        items: [
          {
            checkItem: '临边防护',
            hazardDescription: '防护栏杆缺失',
            riskType: '高处坠落',
          },
        ],
        teamId: 1_011_001,
      },
    );
    expect(requestClient.post).toHaveBeenCalledWith(
      '/pingan/hazard-rectification/orders/batch-delete',
      { ids: ['1', '2'] },
    );
  });
});
