import { beforeEach, describe, expect, it, vi } from 'vitest';

import { requestClient } from '#/api/request';

import {
  createEmptyMonitorCenterOverview,
  flattenMonitorCenterScopeOptions,
  getMonitorCenterOverviewApi,
  normalizeMonitorCenterOverview,
  toMonitorCenterNumber,
} from './monitor-center';

vi.mock('#/api/request', () => ({
  requestClient: {
    get: vi.fn(),
  },
}));

describe('monitor-center api', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('loads the monitor overview from the backend aggregate endpoint', async () => {
    const params = {
      dateEnd: '2026-05-30',
      dateStart: '2026-05-30',
      orgId: 101_109,
    };

    await getMonitorCenterOverviewApi(params);

    expect(requestClient.get).toHaveBeenCalledWith(
      '/pingan/monitor-center/overview',
      { params },
    );
  });

  it('normalizes partial aggregate responses for monitor pages', () => {
    const overview = normalizeMonitorCenterOverview({
      companies: [{ id: 3, name: '广晟幕墙' }],
      learningBars: [{ name: '广晟幕墙', value: '2' }],
      learningTrend: [{ name: '2026-05-30', value: '2' }],
      riskAccidentTypeBars: [{ name: '机械伤害', value: '2' }],
      riskCauseBars: [{ name: '设备缺陷', value: '2' }],
      specialWorkTypeBars: [{ name: '动火作业', value: '1' }],
      teamAnalysis: {
        detailRows: [
          {
            abnormalItems: 4,
            captain: '余银',
            company: '广晟源成',
            companyLearningCount: 1,
            dispatchCompletionRate: '0.00%',
            hazardTotal: 1,
            mid: '0.00%',
            name: '幕墙组装1班',
            openHazards: 1,
            post: '0.00%',
            preCheck: '0.00%',
            preMeeting: '100.00%',
            safety: '余银',
            status: '异常',
            teamId: 101_1001,
            threeCheckCompletionRate: '25.00%',
            unfinishedThreeChecks: 3,
            workshop: '幕墙组装',
          },
        ],
        heatmapRows: [{ name: '幕墙组装1班', values: [100, 0, 100, 100, 100, 100, 0] }],
        metricRankings: {
          openHazardTopTeams: [{ name: '幕墙组装1班', value: 1 }],
          threeCheckTopTeams: [{ name: '幕墙组装1班', value: '25.00%' }],
        },
        summary: {
          abnormalTeams: 1,
          averageCompletionRate: '60.00%',
          normalTeams: 1,
          openHazards: 1,
          teamCount: 2,
          unfinishedThreeChecks: 3,
        },
      },
      hazardRecordRows: [
        {
          company: '广晟幕墙',
          date: '2026-05-30',
          detail: '临边防护缺失',
          sourceModule: '隐患整改',
          status: '待整改',
          statusTone: 'amber',
          team: '幕墙组装1班',
          type: '隐患整改',
        },
      ],
      summary: {
        hazardDone: '2',
        hazardOpen: '1',
        learningToday: '2',
        learningTotal: '3',
        threeCheckRates: { pre: '50.00%' },
      },
    } as any);

    expect(overview.companies).toEqual([{ id: 3, name: '广晟幕墙' }]);
    expect(overview.dispatchBars).toEqual([]);
    expect(overview.dispatchTrend).toEqual([]);
    expect(overview.hazardStatusBars).toEqual([]);
    expect(overview.learningCategoryBars).toEqual([]);
    expect(overview.learningBars).toEqual([{ name: '广晟幕墙', value: '2' }]);
    expect(overview.learningTrend).toEqual([
      { name: '2026-05-30', value: '2' },
    ]);
    expect(overview.riskAccidentTypeBars).toEqual([
      { name: '机械伤害', value: '2' },
    ]);
    expect(overview.riskCauseBars).toEqual([
      { name: '设备缺陷', value: '2' },
    ]);
    expect(overview.specialWorkTypeBars).toEqual([
      { name: '动火作业', value: '1' },
    ]);
    expect(overview.hazardRecordRows[0]?.detail).toBe('临边防护缺失');
    expect(overview.teamAnalysis.summary.teamCount).toBe(2);
    expect(overview.teamAnalysis.summary.averageCompletionRate).toBe('60.00%');
    expect(overview.teamAnalysis.metricRankings.threeCheckTopTeams[0]?.name).toBe(
      '幕墙组装1班',
    );
    expect(overview.teamAnalysis.detailRows[0]?.threeCheckCompletionRate).toBe(
      '25.00%',
    );
    expect(JSON.stringify(overview.teamAnalysis)).not.toContain('health');
    expect(overview.summary.hazardDone).toBe('2');
    expect(overview.summary.hazardOpen).toBe('1');
    expect(overview.summary.learningToday).toBe('2');
    expect(overview.summary.learningTotal).toBe('3');
    expect(overview.summary.threeCheckRates).toMatchObject({
      mid: '0.00%',
      post: '0.00%',
      pre: '50.00%',
      preInspection: '0.00%',
    });
    expect(createEmptyMonitorCenterOverview().summary.hazardFixedRate).toBe(
      '0.00%',
    );
    expect(createEmptyMonitorCenterOverview().summary.learningTotal).toBe(0);
    expect(createEmptyMonitorCenterOverview().riskAccidentTypeBars).toEqual([]);
    expect(createEmptyMonitorCenterOverview().hazardRecordRows).toEqual([]);
    expect(
      (createEmptyMonitorCenterOverview() as any).hazardRectificationOrders,
    ).toEqual({
      activeOpen: 0,
      cancelled: 0,
      closed: 0,
      total: 0,
    });
    expect(
      (normalizeMonitorCenterOverview({}) as any)
        .legacyHazardRectificationRecords,
    ).toEqual({ total: 0 });
    expect(createEmptyMonitorCenterOverview().teamAnalysis).toMatchObject({
      detailRows: [],
      heatmapRows: [],
      metricRankings: {
        openHazardTopTeams: [],
        threeCheckTopTeams: [],
      },
      summary: {
        abnormalTeams: 0,
        averageCompletionRate: '0.00%',
        normalTeams: 0,
        openHazards: 0,
        teamCount: 0,
        unfinishedThreeChecks: 0,
      },
    });
  });

  it('flattens scope options and safely converts backend numeric values', () => {
    expect(
      flattenMonitorCenterScopeOptions([
        {
          children: [
            { children: [], id: 101, name: '幕墙组装', orgType: 'DEPARTMENT' },
          ],
          id: 3,
          name: '广晟幕墙',
          orgType: 'COMPANY',
        },
      ]).map((item) => `${item.depth}:${item.name}`),
    ).toEqual(['0:广晟幕墙', '1:幕墙组装']);
    expect(toMonitorCenterNumber('12')).toBe(12);
    expect(toMonitorCenterNumber('bad')).toBe(0);
  });
});
