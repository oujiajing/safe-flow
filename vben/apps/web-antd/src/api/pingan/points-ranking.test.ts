import { beforeEach, describe, expect, it, vi } from 'vitest';

import { requestClient } from '#/api/request';

import {
  getSafetyPointsRankingChartsApi,
  getSafetyPointsRankingIndividualApi,
  getSafetyPointsRankingOverviewApi,
  getSafetyPointsRankingTeamApi,
} from './points-ranking';

vi.mock('#/api/request', () => ({
  requestClient: {
    get: vi.fn(),
  },
}));

describe('points-ranking api', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('maps safety points ranking endpoints', async () => {
    const params = {
      companyId: 4,
      dateEnd: '2026-06-30',
      dateStart: '2026-06-01',
      keyword: '学习',
      page: 1,
      pageSize: 10,
      teamId: 1_011_001,
    };

    await getSafetyPointsRankingOverviewApi(params);
    await getSafetyPointsRankingChartsApi(params);
    await getSafetyPointsRankingIndividualApi(params);
    await getSafetyPointsRankingTeamApi({ ...params, rankBy: 'TEAM' });

    expect(requestClient.get).toHaveBeenNthCalledWith(
      1,
      '/pingan/safety-points/ranking/overview',
      { params },
    );
    expect(requestClient.get).toHaveBeenNthCalledWith(
      2,
      '/pingan/safety-points/ranking/charts',
      { params },
    );
    expect(requestClient.get).toHaveBeenNthCalledWith(
      3,
      '/pingan/safety-points/ranking/individual',
      { params },
    );
    expect(requestClient.get).toHaveBeenNthCalledWith(
      4,
      '/pingan/safety-points/ranking/team',
      { params: { ...params, rankBy: 'TEAM' } },
    );
  });
});
