import { beforeEach, describe, expect, it, vi } from 'vitest';

import { requestClient } from '#/api/request';

import {
  batchDeleteSpecialWorkRecordsApi,
  createSpecialWorkRecordApi,
  deleteSpecialWorkRecordApi,
  downloadSpecialWorkRecordsApi,
  executeSpecialWorkActionApi,
  getSpecialWorkRecordsApi,
  updateSpecialWorkRecordApi,
  uploadSpecialWorkImageApi,
} from './special-work';

vi.mock('#/api/request', () => ({
  requestClient: {
    delete: vi.fn(),
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    upload: vi.fn(),
  },
}));

describe('special-work api', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('maps list, create, update, delete, batch delete, image, and export endpoints', async () => {
    const payload = {
      applicationTime: '2026-05-22 10:00:00',
      companyId: 4,
      project: '动火审批',
      workType: '动火作业',
    };

    await getSpecialWorkRecordsApi({
      companyId: 4,
      dateEnd: '2026-05-22',
      dateStart: '2026-05-01',
      page: 1,
      pageSize: 20,
      status: 'PENDING_APPROVAL',
    });
    await createSpecialWorkRecordApi(payload);
    await updateSpecialWorkRecordApi(12, payload);
    await executeSpecialWorkActionApi(12, 'APPROVE_AND_START', {
      disclosureReceiver: '作业人员',
      guardian: '监护人',
      implementationStartTime: '2026-05-22 10:30:00',
      safetyDisclosurePerson: '安全员',
    });
    await deleteSpecialWorkRecordApi(12);
    await batchDeleteSpecialWorkRecordsApi([12, 13]);
    await downloadSpecialWorkRecordsApi({ companyId: 4, status: 'COMPLETED' });

    const file = new File(['image'], 'special-work.png', { type: 'image/png' });
    await uploadSpecialWorkImageApi(12, file);

    expect(requestClient.get).toHaveBeenNthCalledWith(
      1,
      '/pingan/special-work/records',
      {
        params: {
          companyId: 4,
          dateEnd: '2026-05-22',
          dateStart: '2026-05-01',
          page: 1,
          pageSize: 20,
          status: 'PENDING_APPROVAL',
        },
      },
    );
    expect(requestClient.post).toHaveBeenNthCalledWith(
      1,
      '/pingan/special-work/records',
      payload,
    );
    expect(requestClient.put).toHaveBeenCalledWith(
      '/pingan/special-work/records/12',
      payload,
    );
    expect(requestClient.delete).toHaveBeenCalledWith(
      '/pingan/special-work/records/12',
    );
    expect(requestClient.post).toHaveBeenNthCalledWith(
      2,
      '/pingan/special-work/records/12/actions/APPROVE_AND_START',
      {
        disclosureReceiver: '作业人员',
        guardian: '监护人',
        implementationStartTime: '2026-05-22 10:30:00',
        safetyDisclosurePerson: '安全员',
      },
    );
    expect(requestClient.post).toHaveBeenNthCalledWith(
      3,
      '/pingan/special-work/records/batch-delete',
      { ids: [12, 13] },
    );
    expect(requestClient.get).toHaveBeenNthCalledWith(
      2,
      '/pingan/special-work/records/export',
      {
        params: { companyId: 4, status: 'COMPLETED' },
        responseType: 'blob',
      },
    );
    expect(requestClient.upload).toHaveBeenCalledWith(
      '/pingan/special-work/records/12/image',
      { file },
    );
  });
});
