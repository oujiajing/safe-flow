import { beforeEach, describe, expect, it, vi } from 'vitest';

import { requestClient } from '#/api/request';

import {
  createRiskControlHazardApi,
  createRiskControlLibraryApi,
  downloadRiskControlDataApi,
  downloadRiskControlTemplateApi,
  getRiskControlHazardsApi,
  getRiskControlLibrariesApi,
  updateRiskControlHazardApi,
  uploadRiskControlDataApi,
} from './risk-level-control';

vi.mock('#/api/request', () => ({
  requestClient: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
  },
}));

describe('risk-level-control api', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('maps risk library and hazard endpoints', async () => {
    const hazard = {
      accidentType: '高处坠落',
      companyId: 4,
      dangerSource: '吊篮作业',
      riskLevel: '一般风险',
      riskPoint: '外立面施工',
    };

    await getRiskControlLibrariesApi({ keyword: '事故隐患', page: 1, pageSize: 20 });
    await createRiskControlLibraryApi({ hazard, name: '源成风险库' });
    await getRiskControlHazardsApi(12, { page: 2, pageSize: 50 });
    await createRiskControlHazardApi(12, hazard);
    await updateRiskControlHazardApi(12, 99, { ...hazard, riskLevel: '重大风险' });

    expect(requestClient.get).toHaveBeenNthCalledWith(
      1,
      '/pingan/risk-level-control/libraries',
      { params: { keyword: '事故隐患', page: 1, pageSize: 20 } },
    );
    expect(requestClient.post).toHaveBeenNthCalledWith(
      1,
      '/pingan/risk-level-control/libraries',
      { hazard, name: '源成风险库' },
    );
    expect(requestClient.get).toHaveBeenNthCalledWith(
      2,
      '/pingan/risk-level-control/libraries/12/hazards',
      { params: { page: 2, pageSize: 50 } },
    );
    expect(requestClient.post).toHaveBeenNthCalledWith(
      2,
      '/pingan/risk-level-control/libraries/12/hazards',
      hazard,
    );
    expect(requestClient.put).toHaveBeenCalledWith(
      '/pingan/risk-level-control/libraries/12/hazards/99',
      { ...hazard, riskLevel: '重大风险' },
    );
  });

  it('maps template, export, and import endpoints', async () => {
    vi.mocked(requestClient.get).mockResolvedValue(new Blob());
    const file = new File(['xlsx'], 'risk.xlsx');

    await downloadRiskControlTemplateApi();
    await downloadRiskControlDataApi(12);
    await uploadRiskControlDataApi(file);

    expect(requestClient.get).toHaveBeenNthCalledWith(
      1,
      '/pingan/risk-level-control/template',
      { responseType: 'blob' },
    );
    expect(requestClient.get).toHaveBeenNthCalledWith(
      2,
      '/pingan/risk-level-control/libraries/12/export',
      { responseType: 'blob' },
    );
    expect(requestClient.post).toHaveBeenCalledWith(
      '/pingan/risk-level-control/import',
      expect.any(FormData),
      {
        headers: { 'Content-Type': 'multipart/form-data' },
      },
    );
  });
});
