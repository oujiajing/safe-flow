import type { RouteRecordRaw } from 'vue-router';

import { safetyLedgerSections } from '#/views/pingan/safety-ledger/safety-ledger.config';
import type {
  SafetyLedgerEntryConfig,
  SafetyLedgerSectionConfig,
} from '#/views/pingan/safety-ledger/safety-ledger.config';

const ComingSoon = () => import('#/views/_core/fallback/coming-soon.vue');
export const MonitorCenterPage = () =>
  import('#/views/pingan/monitor-center/index.vue');
export const HazardRectificationOrderPage = () =>
  import('#/views/pingan/hazard-rectification-order/index.vue');
export const OneShiftThreeChecksWorkbench = () =>
  import('#/views/pingan/one-shift-three-checks/workbench/index.vue');
export const OrganizationPage = () =>
  import('#/views/pingan/organization/index.vue');
export const PointsRankingPage = () =>
  import('#/views/pingan/points-ranking/index.vue');
export const RiskFourColorMapPage = () =>
  import('#/views/pingan/risk-four-color-map/index.vue');
export const RiskLevelControlPage = () =>
  import('#/views/pingan/risk-level-control/index.vue');
export const SpecialWorkPage = () =>
  import('#/views/pingan/special-work/index.vue');
export const SafetyLearningPage = () =>
  import('#/views/pingan/safety-learning/index.vue');
export const SafetyLedgerPage = () =>
  import('#/views/pingan/safety-ledger/index.vue');
export const GenericThreeCheckRecordWorkbench = () =>
  import('#/views/pingan/pre-shift-meeting/index.vue');
export const TrainingExamPage = () =>
  import('#/views/pingan/training-exam/index.vue');

const routeAuthorityCodes = {
  database: 'PINGAN_DATABASE_ENTRY',
  hazard: 'PINGAN_HAZARD_ENTRY',
  hazardCurtainWallPenalty: 'PINGAN_HAZARD_CURTAIN_WALL_PENALTY_ENTRY',
  hazardCurtainWallRoutineCheck: 'PINGAN_HAZARD_CURTAIN_WALL_ROUTINE_CHECK_ENTRY',
  hazardQuickShot: 'PINGAN_HAZARD_QUICK_SHOT_ENTRY',
  hazardRectification: 'PINGAN_HAZARD_RECTIFICATION_ENTRY',
  hazardSafetyCheck: 'PINGAN_HAZARD_SAFETY_CHECK_ENTRY',
  ledger: 'PINGAN_LEDGER_ENTRY',
  monitorCenter: 'PINGAN_MONITOR_CENTER_ENTRY',
  organization: 'PINGAN_ORGANIZATION_ENTRY',
  points: 'PINGAN_POINTS_ENTRY',
  pointsFlow: 'PINGAN_POINTS_FLOW_ENTRY',
  pointsRanking: 'PINGAN_POINTS_RANKING_ENTRY',
  preShiftMeeting: 'PINGAN_PRE_SHIFT_MEETING_ENTRY',
  preShiftSafetyActivity: 'PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_ENTRY',
  risk: 'PINGAN_RISK_ENTRY',
  specialWork: 'PINGAN_SPECIAL_WORK_ENTRY',
  keySites: 'PINGAN_KEY_SITES_ENTRY',
  oneShiftThreeChecks: 'PINGAN_ONE_SHIFT_THREE_CHECKS_ENTRY',
  teamDispatch: 'PINGAN_TEAM_DISPATCH_ENTRY',
  curtainWallTeamDispatch: 'PINGAN_CURTAIN_WALL_TEAM_DISPATCH_ENTRY',
  trainingExamResults: 'PINGAN_TRAINING_EXAM_RESULTS_ENTRY',
  trainingExamTasks: 'PINGAN_TRAINING_EXAM_TASKS_ENTRY',
  trainingSafetyLearning: 'PINGAN_TRAINING_SAFETY_LEARNING_ENTRY',
  threeChecks: 'PINGAN_THREE_CHECK_ENTRY',
  training: 'PINGAN_TRAINING_ENTRY',
} as const;

function isSafetyLedgerEntry(
  item: SafetyLedgerEntryConfig | SafetyLedgerSectionConfig,
): item is SafetyLedgerEntryConfig {
  return 'columns' in item;
}

function safetyLedgerRoutePath(parentPath: string, path: string) {
  return `${parentPath}/${path}`;
}

function firstSafetyLedgerLeafPath(
  items: Array<SafetyLedgerEntryConfig | SafetyLedgerSectionConfig>,
  parentPath: string,
): string {
  const first = items[0];
  if (!first) {
    return parentPath;
  }
  const path = safetyLedgerRoutePath(parentPath, first.path);
  return isSafetyLedgerEntry(first)
    ? path
    : firstSafetyLedgerLeafPath(first.children, path);
}

function createSafetyLedgerRoutes(
  items: Array<SafetyLedgerEntryConfig | SafetyLedgerSectionConfig>,
  parentPath = '/pingan/safety-ledger',
): RouteRecordRaw[] {
  return items.map((item) => {
    const path = safetyLedgerRoutePath(parentPath, item.path);
    if (isSafetyLedgerEntry(item)) {
      return {
        component: SafetyLedgerPage,
        meta: {
          authority: [routeAuthorityCodes.ledger],
          title: item.title,
        },
        name: item.routeName,
        path,
      };
    }

    return {
      meta: {
        authority: [routeAuthorityCodes.ledger],
        title: item.title,
      },
      name: item.routeName,
      path,
      redirect: firstSafetyLedgerLeafPath(item.children, path),
      children: createSafetyLedgerRoutes(item.children, path),
    };
  });
}

const routes: RouteRecordRaw[] = [
  {
    component: MonitorCenterPage,
    meta: {
      authority: [routeAuthorityCodes.monitorCenter],
      hideInTab: true,
      icon: 'lucide:gauge',
      order: -100,
      title: '平安班组监控中心',
    },
    name: 'PinganMonitorCenter',
    path: '/pingan/monitor-center',
  },
  {
    component: OrganizationPage,
    meta: {
      authority: [routeAuthorityCodes.organization],
      icon: 'lucide:network',
      order: -99,
      title: '组织架构',
    },
    name: 'PinganOrganization',
    path: '/pingan/organization',
  },
  {
    meta: {
      authority: [routeAuthorityCodes.risk],
      icon: 'lucide:shield-alert',
      order: -98,
      title: '风险管控',
    },
    name: 'PinganRiskManagement',
    path: '/pingan/risk-management',
    redirect: '/pingan/risk-management/risk-level-control',
    children: [
      {
        component: RiskLevelControlPage,
        meta: {
          authority: [routeAuthorityCodes.risk],
          icon: 'lucide:signpost',
          title: '风险分级管控',
        },
        name: 'PinganRiskLevelControl',
        path: '/pingan/risk-management/risk-level-control',
      },
      {
        component: RiskFourColorMapPage,
        meta: {
          authority: [routeAuthorityCodes.risk],
          icon: 'lucide:map',
          title: '风险四色图',
        },
        name: 'PinganRiskFourColorMap',
        path: '/pingan/risk-management/risk-four-color-map',
      },
    ],
  },
  {
    meta: {
      authority: [routeAuthorityCodes.threeChecks],
      icon: 'lucide:list-checks',
      order: -97,
      title: '一班三查',
    },
    name: 'PinganThreeChecks',
    path: '/pingan/three-checks',
    redirect: '/pingan/three-checks/pre-shift-meeting',
    children: [
      {
        component: OneShiftThreeChecksWorkbench,
        meta: {
          authority: [routeAuthorityCodes.teamDispatch],
          title: '班组派班',
        },
        name: 'PinganTeamDispatch',
        path: '/pingan/three-checks/team-dispatch',
      },
      {
        component: OneShiftThreeChecksWorkbench,
        meta: {
          authority: [routeAuthorityCodes.curtainWallTeamDispatch],
          title: '幕墙班组派班',
        },
        name: 'PinganCurtainWallTeamDispatch',
        path: '/pingan/three-checks/curtain-wall-team-dispatch',
      },
      {
        component: OneShiftThreeChecksWorkbench,
        meta: {
          affixTab: true,
          authority: [routeAuthorityCodes.preShiftMeeting],
          title: '班前会',
        },
        name: 'PreShiftMeeting',
        path: '/pingan/three-checks/pre-shift-meeting',
      },
      {
        component: OneShiftThreeChecksWorkbench,
        meta: {
          authority: [routeAuthorityCodes.preShiftSafetyActivity],
          title: '班前安全活动',
        },
        name: 'PinganPreShiftSafetyActivity',
        path: '/pingan/three-checks/pre-shift-safety-activity',
      },
      {
        component: OneShiftThreeChecksWorkbench,
        meta: {
          authority: [routeAuthorityCodes.oneShiftThreeChecks],
          title: '班前检查',
        },
        name: 'PinganPreShiftInspection',
        path: '/pingan/three-checks/pre-shift-inspection',
      },
      {
        component: OneShiftThreeChecksWorkbench,
        meta: {
          authority: [routeAuthorityCodes.oneShiftThreeChecks],
          title: '班中检查',
        },
        name: 'PinganMidShiftInspection',
        path: '/pingan/three-checks/mid-shift-inspection',
      },
      {
        component: OneShiftThreeChecksWorkbench,
        meta: {
          authority: [routeAuthorityCodes.oneShiftThreeChecks],
          title: '班后检查',
        },
        name: 'PinganPostShiftInspection',
        path: '/pingan/three-checks/post-shift-inspection',
      },
      {
        component: OneShiftThreeChecksWorkbench,
        meta: {
          authority: [routeAuthorityCodes.keySites],
          title: '重点场所',
        },
        name: 'PinganKeySites',
        path: '/pingan/three-checks/key-sites',
      },
    ],
  },
  {
    meta: {
      hideInMenu: true,
      title: '班前会旧路径重定向',
    },
    name: 'PreShiftMeetingLegacyRedirect',
    path: '/pingan/pre-shift-meeting',
    redirect: '/pingan/three-checks/pre-shift-meeting',
  },
  {
    meta: {
      authority: [routeAuthorityCodes.hazard],
      icon: 'lucide:search-check',
      order: -96,
      title: '隐患排查',
    },
    name: 'PinganHazardInspection',
    path: '/pingan/hazard-inspection',
    redirect: '/pingan/hazard-inspection/safety-check',
    children: [
      {
        component: GenericThreeCheckRecordWorkbench,
        meta: {
          authority: [routeAuthorityCodes.hazardSafetyCheck],
          title: '安全检查',
        },
        name: 'PinganSafetyCheck',
        path: '/pingan/hazard-inspection/safety-check',
      },
      {
        component: HazardRectificationOrderPage,
        meta: {
          authority: [routeAuthorityCodes.hazardRectification],
          title: '隐患整改',
        },
        name: 'PinganHazardRectification',
        path: '/pingan/hazard-inspection/rectification',
      },
      {
        component: GenericThreeCheckRecordWorkbench,
        meta: {
          authority: [routeAuthorityCodes.hazardQuickShot],
          title: '随手拍',
        },
        name: 'PinganQuickShot',
        path: '/pingan/hazard-inspection/quick-shot',
      },
      {
        component: GenericThreeCheckRecordWorkbench,
        meta: {
          authority: [routeAuthorityCodes.hazardCurtainWallPenalty],
          title: '幕墙处罚管理',
        },
        name: 'PinganCurtainWallPenalty',
        path: '/pingan/hazard-inspection/curtain-wall-penalty',
      },
      {
        component: GenericThreeCheckRecordWorkbench,
        meta: {
          authority: [routeAuthorityCodes.hazardCurtainWallRoutineCheck],
          title: '幕墙日周月检',
        },
        name: 'PinganCurtainWallRoutineCheck',
        path: '/pingan/hazard-inspection/curtain-wall-routine-check',
      },
    ],
  },
  {
    component: SpecialWorkPage,
    meta: {
      authority: [routeAuthorityCodes.specialWork],
      icon: 'lucide:grid-2x2-check',
      order: -95,
      title: '特殊作业',
    },
    name: 'PinganSpecialWork',
    path: '/pingan/special-work',
  },
  {
    meta: {
      authority: [routeAuthorityCodes.training],
      icon: 'lucide:graduation-cap',
      order: -94,
      title: '宣教培训',
    },
    name: 'PinganTraining',
    path: '/pingan/training',
    children: [
      {
        component: TrainingExamPage,
        meta: {
          authority: [routeAuthorityCodes.trainingExamTasks],
          title: '考试任务',
        },
        name: 'PinganExamTasks',
        path: '/pingan/training/exam-tasks',
      },
      {
        component: TrainingExamPage,
        meta: {
          authority: [routeAuthorityCodes.trainingExamResults],
          title: '考试成绩',
        },
        name: 'PinganExamResults',
        path: '/pingan/training/exam-results',
      },
      {
        component: SafetyLearningPage,
        meta: {
          authority: [routeAuthorityCodes.trainingSafetyLearning],
          title: '安全学习',
        },
        name: 'PinganSafetyLearning',
        path: '/pingan/training/safety-learning',
      },
    ],
  },
  {
    meta: {
      authority: [routeAuthorityCodes.points],
      icon: 'lucide:bar-chart-3',
      order: -93,
      title: '安全积分',
    },
    name: 'PinganSafetyPoints',
    path: '/pingan/safety-points',
    redirect: '/pingan/safety-points/flow',
    children: [
      {
        component: GenericThreeCheckRecordWorkbench,
        meta: {
          authority: [routeAuthorityCodes.pointsFlow],
          icon: 'lucide:bar-chart-3',
          title: '积分流水',
        },
        name: 'PinganPointsFlow',
        path: '/pingan/safety-points/flow',
      },
      {
        component: PointsRankingPage,
        meta: {
          authority: [routeAuthorityCodes.pointsRanking],
          icon: 'lucide:bar-chart-3',
          title: '积分榜单',
        },
        name: 'PinganPointsRanking',
        path: '/pingan/safety-points/ranking',
      },
    ],
  },
  {
    meta: {
      authority: [routeAuthorityCodes.ledger],
      hideChildrenInMenu: true,
      icon: 'lucide:book-open-check',
      order: -92,
      title: '安全台账',
    },
    name: 'PinganSafetyLedger',
    path: '/pingan/safety-ledger',
    redirect: '/pingan/safety-ledger/rules-responsibility/safety-rules',
    children: createSafetyLedgerRoutes(safetyLedgerSections),
  },
  {
    meta: {
      authority: [routeAuthorityCodes.database],
      icon: 'lucide:database',
      order: -91,
      title: '数据库',
    },
    name: 'PinganDatabase',
    path: '/pingan/database',
    redirect: '/pingan/database/maintenance',
    children: [
      {
        component: ComingSoon,
        meta: {
          authority: [routeAuthorityCodes.database],
          title: '数据库维护',
        },
        name: 'PinganDatabaseMaintenance',
        path: '/pingan/database/maintenance',
      },
      {
        component: RiskLevelControlPage,
        meta: {
          authority: [routeAuthorityCodes.database],
          title: '风险隐患库',
        },
        name: 'PinganRiskHazardLibrary',
        path: '/pingan/database/risk-hazard-library',
        props: {
          pageTitle: '风险隐患库',
        },
      },
      {
        component: ComingSoon,
        meta: {
          authority: [routeAuthorityCodes.database],
          title: '场所检查内容',
        },
        name: 'PinganSiteInspectionContent',
        path: '/pingan/database/site-inspection-content',
      },
      {
        component: ComingSoon,
        meta: {
          authority: [routeAuthorityCodes.database],
          title: '特殊作业措施',
        },
        name: 'PinganSpecialWorkMeasures',
        path: '/pingan/database/special-work-measures',
      },
    ],
  },
];

export default routes;
