import { beforeEach, describe, expect, it, vi } from 'vitest';

import { requestClient } from '#/api/request';

import {
  createRiskFourColorMapApi,
  deleteRiskFourColorMapApi,
  getRiskFourColorMapsApi,
  updateRiskFourColorMapApi,
  uploadRiskFourColorMapBackgroundApi,
} from './risk-four-color-map';

vi.mock('#/api/request', () => ({
  requestClient: {
    delete: vi.fn(),
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
  },
}));

describe('risk-four-color-map api', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('maps four-color map CRUD and background upload endpoints', async () => {
    const payload = { companyId: 4, name: 'Demo Works厂区四色图', remark: '厂区总图' };
    const file = new File(['png'], 'four-color.png', { type: 'image/png' });

    await getRiskFourColorMapsApi({ companyId: 4, keyword: 'Demo Works', page: 1, pageSize: 20 });
    await createRiskFourColorMapApi(payload);
    await updateRiskFourColorMapApi(12, { ...payload, name: 'Demo Works车间四色图' });
    await deleteRiskFourColorMapApi(12);
    await uploadRiskFourColorMapBackgroundApi(12, file);

    expect(requestClient.get).toHaveBeenCalledWith('/pingan/risk-four-color-maps', {
      params: { companyId: 4, keyword: 'Demo Works', page: 1, pageSize: 20 },
    });
    expect(requestClient.post).toHaveBeenNthCalledWith(
      1,
      '/pingan/risk-four-color-maps',
      payload,
    );
    expect(requestClient.put).toHaveBeenCalledWith('/pingan/risk-four-color-maps/12', {
      ...payload,
      name: 'Demo Works车间四色图',
    });
    expect(requestClient.delete).toHaveBeenCalledWith('/pingan/risk-four-color-maps/12');
    expect(requestClient.post).toHaveBeenNthCalledWith(
      2,
      '/pingan/risk-four-color-maps/12/background',
      expect.any(FormData),
      {
        headers: { 'Content-Type': 'multipart/form-data' },
      },
    );
  });
});

