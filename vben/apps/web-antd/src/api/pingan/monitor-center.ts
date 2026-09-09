import { requestClient } from '#/api/request';

export namespace PinganMonitorCenterApi {
  export type Id = number | string;
  export type NumericValue = number | string;

  export interface FlatScopeOption extends ScopeOption {
    depth: number;
  }

  export interface OverviewParams {
    dateEnd?: string;
    dateStart?: string;
    orgId?: Id;
  }

  export interface CompanyOption {
    id: number;
    name: string;
  }

  export interface ScopeOption {
    children: ScopeOption[];
    id: number;
    name: string;
    orgType: string;
  }

  export interface ThreeCheckRates {
    mid: string;
    post: string;
    pre: string;
    preInspection: string;
  }

  export interface Summary {
    dispatchFinished: NumericValue;
    dispatchTotal: NumericValue;
    hazardDone: NumericValue;
    hazardFixedRate: string;
    hazardOpen: NumericValue;
    hazardTotal: NumericValue;
    learningToday: NumericValue;
    learningTotal: NumericValue;
    riskTotal: NumericValue;
    teamCount: NumericValue;
    threeCheckRates: ThreeCheckRates;
  }

  export interface BarItem {
    name: string;
    value: NumericValue;
  }

  export interface DispatchTrendItem {
    finished: NumericValue;
    name: string;
    total: NumericValue;
  }

  export interface SeriesItem {
    companies: CompanyOption[];
    data: NumericValue[];
    name: string;
  }

  export interface RiskBarItem {
    low: NumericValue;
    major: NumericValue;
    name: string;
    normal: NumericValue;
    serious: NumericValue;
  }

  export interface PieItem {
    name: string;
    value: NumericValue;
  }

  export interface HazardRectificationOrderStats {
    activeOpen: NumericValue;
    cancelled: NumericValue;
    closed: NumericValue;
    total: NumericValue;
  }

  export interface LegacyHazardRectificationRecordStats {
    total: NumericValue;
  }

  export interface HazardBarItem {
    done: NumericValue;
    name: string;
    open: NumericValue;
  }

  export interface HazardRecordItem {
    company: string;
    date: string;
    detail: string;
    sourceModule: string;
    status: string;
    statusTone: string;
    team: string;
    type: string;
  }

  export interface TeamAnalysisSummary {
    abnormalTeams: NumericValue;
    averageCompletionRate: string;
    normalTeams: NumericValue;
    openHazards: NumericValue;
    teamCount: NumericValue;
    unfinishedThreeChecks: NumericValue;
  }

  export interface MetricRankingItem {
    name: string;
    value: NumericValue;
  }

  export interface MetricRankings {
    openHazardTopTeams: MetricRankingItem[];
    threeCheckTopTeams: MetricRankingItem[];
  }

  export interface HeatmapRow {
    name: string;
    values: NumericValue[];
  }

  export interface TeamDetailRow {
    abnormalItems: NumericValue;
    captain: string;
    company: string;
    companyLearningCount: NumericValue;
    dispatchCompletionRate: string;
    hazardTotal: NumericValue;
    mid: string;
    name: string;
    openHazards: NumericValue;
    post: string;
    preCheck: string;
    preMeeting: string;
    rank: NumericValue;
    safety: string;
    status: string;
    teamId: Id;
    threeCheckCompletionRate: string;
    unfinishedThreeChecks: NumericValue;
    workshop: string;
  }

  export interface TeamAnalysis {
    detailRows: TeamDetailRow[];
    heatmapRows: HeatmapRow[];
    metricRankings: MetricRankings;
    summary: TeamAnalysisSummary;
  }

  export interface Overview {
    companies: CompanyOption[];
    dateEnd: string;
    dateStart: string;
    dispatchBars: BarItem[];
    dispatchFinishedBars: BarItem[];
    dispatchTrend: DispatchTrendItem[];
    hazardBars: HazardBarItem[];
    hazardRecordRows: HazardRecordItem[];
    hazardRectificationPie: PieItem[];
    hazardRectificationOrders: HazardRectificationOrderStats;
    hazardStatusBars: BarItem[];
    learningCategoryBars: BarItem[];
    learningBars: BarItem[];
    learningTrend: BarItem[];
    legacyHazardRectificationRecords: LegacyHazardRectificationRecordStats;
    riskAccidentTypeBars: BarItem[];
    riskCauseBars: BarItem[];
    riskControlBars: RiskBarItem[];
    scopeOptions: ScopeOption[];
    specialWorkTypeBars: BarItem[];
    summary: Summary;
    teamAnalysis: TeamAnalysis;
    threeCheckSeries: SeriesItem[];
  }
}

export function getMonitorCenterOverviewApi(
  params?: PinganMonitorCenterApi.OverviewParams,
) {
  return requestClient.get<PinganMonitorCenterApi.Overview>(
    '/pingan/monitor-center/overview',
    { params },
  );
}

export function createEmptyMonitorCenterOverview(): PinganMonitorCenterApi.Overview {
  return {
    companies: [],
    dateEnd: '',
    dateStart: '',
    dispatchBars: [],
    dispatchFinishedBars: [],
    dispatchTrend: [],
    hazardBars: [],
    hazardRecordRows: [],
    hazardRectificationPie: [],
    hazardRectificationOrders: {
      activeOpen: 0,
      cancelled: 0,
      closed: 0,
      total: 0,
    },
    hazardStatusBars: [],
    learningCategoryBars: [],
    learningBars: [],
    learningTrend: [],
    legacyHazardRectificationRecords: {
      total: 0,
    },
    riskAccidentTypeBars: [],
    riskCauseBars: [],
    riskControlBars: [],
    scopeOptions: [],
    specialWorkTypeBars: [],
    summary: {
      dispatchFinished: 0,
      dispatchTotal: 0,
      hazardDone: 0,
      hazardFixedRate: '0.00%',
      hazardOpen: 0,
      hazardTotal: 0,
      learningToday: 0,
      learningTotal: 0,
      riskTotal: 0,
      teamCount: 0,
      threeCheckRates: {
        mid: '0.00%',
        post: '0.00%',
        pre: '0.00%',
        preInspection: '0.00%',
      },
    },
    teamAnalysis: {
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
    },
    threeCheckSeries: [],
  };
}

export function normalizeMonitorCenterOverview(
  source?: Partial<PinganMonitorCenterApi.Overview>,
): PinganMonitorCenterApi.Overview {
  const empty = createEmptyMonitorCenterOverview();
  return {
    ...empty,
    ...source,
    companies: source?.companies ?? [],
    dispatchBars: source?.dispatchBars ?? [],
    dispatchFinishedBars: source?.dispatchFinishedBars ?? [],
    dispatchTrend: source?.dispatchTrend ?? [],
    hazardBars: source?.hazardBars ?? [],
    hazardRecordRows: source?.hazardRecordRows ?? [],
    hazardRectificationPie: source?.hazardRectificationPie ?? [],
    hazardRectificationOrders: {
      ...empty.hazardRectificationOrders,
      ...(source?.hazardRectificationOrders ?? {}),
    },
    hazardStatusBars: source?.hazardStatusBars ?? [],
    learningCategoryBars: source?.learningCategoryBars ?? [],
    learningBars: source?.learningBars ?? [],
    learningTrend: source?.learningTrend ?? [],
    legacyHazardRectificationRecords: {
      ...empty.legacyHazardRectificationRecords,
      ...(source?.legacyHazardRectificationRecords ?? {}),
    },
    riskAccidentTypeBars: source?.riskAccidentTypeBars ?? [],
    riskCauseBars: source?.riskCauseBars ?? [],
    riskControlBars: source?.riskControlBars ?? [],
    scopeOptions: source?.scopeOptions ?? [],
    specialWorkTypeBars: source?.specialWorkTypeBars ?? [],
    summary: {
      ...empty.summary,
      ...(source?.summary ?? {}),
      threeCheckRates: {
        ...empty.summary.threeCheckRates,
        ...(source?.summary?.threeCheckRates ?? {}),
      },
    },
    teamAnalysis: {
      ...empty.teamAnalysis,
      ...(source?.teamAnalysis ?? {}),
      detailRows: source?.teamAnalysis?.detailRows ?? [],
      heatmapRows: source?.teamAnalysis?.heatmapRows ?? [],
      metricRankings: {
        ...empty.teamAnalysis.metricRankings,
        ...(source?.teamAnalysis?.metricRankings ?? {}),
        openHazardTopTeams:
          source?.teamAnalysis?.metricRankings?.openHazardTopTeams ?? [],
        threeCheckTopTeams:
          source?.teamAnalysis?.metricRankings?.threeCheckTopTeams ?? [],
      },
      summary: {
        ...empty.teamAnalysis.summary,
        ...(source?.teamAnalysis?.summary ?? {}),
      },
    },
    threeCheckSeries: source?.threeCheckSeries ?? [],
  };
}

export function flattenMonitorCenterScopeOptions(
  options: PinganMonitorCenterApi.ScopeOption[] = [],
  depth = 0,
): PinganMonitorCenterApi.FlatScopeOption[] {
  return options.flatMap((option) => [
    { ...option, children: option.children ?? [], depth },
    ...flattenMonitorCenterScopeOptions(option.children ?? [], depth + 1),
  ]);
}

export function toMonitorCenterNumber(value: unknown) {
  const numberValue = Number(value ?? 0);
  return Number.isFinite(numberValue) ? numberValue : 0;
}
