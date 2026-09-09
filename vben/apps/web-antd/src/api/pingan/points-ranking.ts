import { requestClient } from '#/api/request';

export namespace PinganSafetyPointsRankingApi {
  export type Id = number | string;
  export type RankBy = 'COMPANY' | 'DEPARTMENT' | 'TEAM';

  export interface RankingParams {
    companyId?: Id;
    dateEnd?: string;
    dateStart?: string;
    departmentId?: Id;
    keyword?: string;
    page?: number;
    pageSize?: number;
    teamId?: Id;
  }

  export interface ChartParams extends RankingParams {
    rankBy?: RankBy;
  }

  export interface TeamRankingParams extends RankingParams {
    rankBy?: RankBy;
  }

  export interface PageResult<T> {
    items: T[];
    total: number;
  }

  export interface Overview {
    currentTotalScore: number;
    monthlyAddScore: number;
    monthlyRedeemScore: number;
    participantCount: number;
  }

  export interface TrendPoint {
    date: string;
    score: number;
  }

  export interface SourceSlice {
    name: string;
    value: number;
  }

  export interface TeamTopItem {
    name: string;
    score: number;
  }

  export interface Charts {
    sources: SourceSlice[];
    teamTop5: TeamTopItem[];
    trend: TrendPoint[];
  }

  export interface IndividualRow {
    addScore: number;
    company: string;
    currentScore: number;
    deductScore: number;
    department: string;
    lastBusinessDate: string;
    rank: number;
    recordCount: number;
    redeemScore: number;
    team: string;
    userName: string;
  }

  export interface TeamRow {
    addScore: number;
    currentScore: number;
    deductScore: number;
    lastBusinessDate: string;
    memberCount: number;
    rank: number;
    rankBy: RankBy;
    rankId: string;
    rankName: string;
    recordCount: number;
    redeemScore: number;
  }
}

const rankingPath = '/pingan/safety-points/ranking';

export function getSafetyPointsRankingOverviewApi(
  params: PinganSafetyPointsRankingApi.RankingParams,
) {
  return requestClient.get<PinganSafetyPointsRankingApi.Overview>(
    `${rankingPath}/overview`,
    { params },
  );
}

export function getSafetyPointsRankingChartsApi(
  params: PinganSafetyPointsRankingApi.ChartParams,
) {
  return requestClient.get<PinganSafetyPointsRankingApi.Charts>(
    `${rankingPath}/charts`,
    { params },
  );
}

export function getSafetyPointsRankingIndividualApi(
  params: PinganSafetyPointsRankingApi.RankingParams,
) {
  return requestClient.get<
    PinganSafetyPointsRankingApi.PageResult<PinganSafetyPointsRankingApi.IndividualRow>
  >(`${rankingPath}/individual`, { params });
}

export function getSafetyPointsRankingTeamApi(
  params: PinganSafetyPointsRankingApi.TeamRankingParams,
) {
  return requestClient.get<
    PinganSafetyPointsRankingApi.PageResult<PinganSafetyPointsRankingApi.TeamRow>
  >(`${rankingPath}/team`, { params });
}
