import { describe, expect, it } from 'vitest';
import type { RouteRecordRaw } from 'vue-router';

import { generateRoutesByFrontend } from '@vben/utils';

import routes, {
  HazardRectificationOrderPage,
  MonitorCenterPage,
  OneShiftThreeChecksWorkbench,
  OrganizationPage,
  PointsRankingPage,
  RiskFourColorMapPage,
  RiskLevelControlPage,
  SafetyLedgerPage,
  SafetyLearningPage,
  SpecialWorkPage,
  GenericThreeCheckRecordWorkbench,
  TrainingExamPage,
} from '../modules/pingan';

describe('pingan routes', () => {
  function cloneRoutes(routeList: RouteRecordRaw[]): RouteRecordRaw[] {
    return routeList.map((route) => {
      const clonedRoute = {
        ...route,
        meta: route.meta ? { ...route.meta } : undefined,
      } as RouteRecordRaw;
      if (route.children) {
        clonedRoute.children = cloneRoutes(route.children);
      }
      return clonedRoute;
    });
  }

  function collectTitles(routeList = routes): string[] {
    return routeList.flatMap((route) => [
      ...(route.meta?.title && !route.meta.hideInMenu
        ? [String(route.meta.title)]
        : []),
      ...(route.meta?.hideChildrenInMenu
        ? []
        : collectTitles(route.children ?? [])),
    ]);
  }

  it('registers the screenshot menu tree in the left navigation', () => {
    expect(collectTitles()).toEqual([
      '平安班组监控中心',
      '组织架构',
      '风险管控',
      '风险分级管控',
      '风险四色图',
      '一班三查',
      '班组派班',
      '幕墙班组派班',
      '班前会',
      '班前安全活动',
      '班前检查',
      '班中检查',
      '班后检查',
      '重点场所',
      '隐患排查',
      '安全检查',
      '隐患整改',
      '随手拍',
      '幕墙处罚管理',
      '幕墙日周月检',
      '特殊作业',
      '宣教培训',
      '考试任务',
      '考试成绩',
      '安全学习',
      '安全积分',
      '积分流水',
      '积分榜单',
      '安全台账',
      '数据库',
      '数据库维护',
      '风险隐患库',
      '场所检查内容',
      '特殊作业措施',
    ]);
  });

  it('filters one-shift three-check child menus by their own module entry', async () => {
    const generatedRoutes = await generateRoutesByFrontend(cloneRoutes(routes), [
      'PINGAN_THREE_CHECK_ENTRY',
      'PINGAN_ONE_SHIFT_THREE_CHECKS_ENTRY',
    ]);
    const threeChecks = generatedRoutes.find(
      (route) => route.name === 'PinganThreeChecks',
    );

    expect(threeChecks?.children?.map((route) => route.name)).toEqual([
      'PinganPreShiftInspection',
      'PinganMidShiftInspection',
      'PinganPostShiftInspection',
    ]);

    const meetingRoutes = await generateRoutesByFrontend(cloneRoutes(routes), [
      'PINGAN_THREE_CHECK_ENTRY',
      'PINGAN_PRE_SHIFT_MEETING_ENTRY',
    ]);
    expect(
      meetingRoutes
        .find((route) => route.name === 'PinganThreeChecks')
        ?.children?.map((route) => route.name),
    ).toEqual(['PreShiftMeeting']);
  });

  it('filters hazard inspection child menus by their own module entry', async () => {
    const generatedRoutes = await generateRoutesByFrontend(cloneRoutes(routes), [
      'PINGAN_HAZARD_ENTRY',
      'PINGAN_HAZARD_RECTIFICATION_ENTRY',
    ]);
    const hazardInspection = generatedRoutes.find(
      (route) => route.name === 'PinganHazardInspection',
    );

    expect(hazardInspection?.children?.map((route) => route.name)).toEqual([
      'PinganHazardRectification',
    ]);
  });

  it('filters training child menus by their own module entry', async () => {
    const generatedRoutes = await generateRoutesByFrontend(cloneRoutes(routes), [
      'PINGAN_TRAINING_ENTRY',
      'PINGAN_TRAINING_SAFETY_LEARNING_ENTRY',
    ]);
    const training = generatedRoutes.find(
      (route) => route.name === 'PinganTraining',
    );

    expect(training?.children?.map((route) => route.name)).toEqual([
      'PinganSafetyLearning',
    ]);
  });

  it('lets generated training routes redirect to the first permitted child menu', () => {
    const training = routes.find((route) => route.name === 'PinganTraining');

    expect(training?.redirect).toBeUndefined();
  });

  it('filters safety points child menus by their own module entry', async () => {
    const generatedRoutes = await generateRoutesByFrontend(cloneRoutes(routes), [
      'PINGAN_POINTS_ENTRY',
      'PINGAN_POINTS_RANKING_ENTRY',
    ]);
    const safetyPoints = generatedRoutes.find(
      (route) => route.name === 'PinganSafetyPoints',
    );

    expect(safetyPoints?.children?.map((route) => route.name)).toEqual([
      'PinganPointsRanking',
    ]);
  });

  it('routes organization architecture to its implemented business overview page', () => {
    const organization = routes.find(
      (route) => route.name === 'PinganOrganization',
    );

    expect(organization).toMatchObject({
      component: OrganizationPage,
      meta: {
        title: '组织架构',
      },
      name: 'PinganOrganization',
      path: '/pingan/organization',
    });
  });

  it('routes monitor center to the implemented page inside the normal layout', () => {
    const monitorCenter = routes.find(
      (route) => route.name === 'PinganMonitorCenter',
    );

    expect(monitorCenter).toMatchObject({
      component: MonitorCenterPage,
      meta: {
        hideInTab: true,
        icon: 'lucide:gauge',
        order: -100,
        title: '平安班组监控中心',
      },
      name: 'PinganMonitorCenter',
      path: '/pingan/monitor-center',
    });
    expect(monitorCenter?.meta?.hideInMenu).toBeUndefined();
    expect(monitorCenter?.meta?.noBasicLayout).toBeUndefined();
  });

  it('declares module entry permissions for Pingan navigation routes', () => {
    const routeAuthorities = new Map(
      routes.map((route) => [route.name, route.meta?.authority]),
    );

    expect(routeAuthorities.get('PinganMonitorCenter')).toContain(
      'PINGAN_MONITOR_CENTER_ENTRY',
    );
    expect(routeAuthorities.get('PinganOrganization')).toContain(
      'PINGAN_ORGANIZATION_ENTRY',
    );
    expect(routeAuthorities.get('PinganRiskManagement')).toContain(
      'PINGAN_RISK_ENTRY',
    );
    expect(routeAuthorities.get('PinganThreeChecks')).toContain(
      'PINGAN_THREE_CHECK_ENTRY',
    );
    expect(routeAuthorities.get('PinganHazardInspection')).toContain(
      'PINGAN_HAZARD_ENTRY',
    );
    expect(routeAuthorities.get('PinganSpecialWork')).toContain(
      'PINGAN_SPECIAL_WORK_ENTRY',
    );
    expect(routeAuthorities.get('PinganTraining')).toContain(
      'PINGAN_TRAINING_ENTRY',
    );
    expect(routeAuthorities.get('PinganSafetyPoints')).toContain(
      'PINGAN_POINTS_ENTRY',
    );
    expect(routeAuthorities.get('PinganSafetyLedger')).toContain(
      'PINGAN_LEDGER_ENTRY',
    );
    expect(routeAuthorities.get('PinganDatabase')).toContain(
      'PINGAN_DATABASE_ENTRY',
    );

    const threeChecks = routes.find((route) => route.name === 'PinganThreeChecks');
    const threeCheckAuthorities = Object.fromEntries(
      threeChecks?.children?.map((route) => [
        route.name,
        route.meta?.authority,
      ]) ?? [],
    );
    expect(threeCheckAuthorities).toEqual({
      PinganCurtainWallTeamDispatch: [
        'PINGAN_CURTAIN_WALL_TEAM_DISPATCH_ENTRY',
      ],
      PinganKeySites: ['PINGAN_KEY_SITES_ENTRY'],
      PinganMidShiftInspection: ['PINGAN_ONE_SHIFT_THREE_CHECKS_ENTRY'],
      PinganPostShiftInspection: ['PINGAN_ONE_SHIFT_THREE_CHECKS_ENTRY'],
      PinganPreShiftInspection: ['PINGAN_ONE_SHIFT_THREE_CHECKS_ENTRY'],
      PinganPreShiftSafetyActivity: [
        'PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_ENTRY',
      ],
      PinganTeamDispatch: ['PINGAN_TEAM_DISPATCH_ENTRY'],
      PreShiftMeeting: ['PINGAN_PRE_SHIFT_MEETING_ENTRY'],
    });

    const hazardInspection = routes.find(
      (route) => route.name === 'PinganHazardInspection',
    );
    const hazardAuthorities = Object.fromEntries(
      hazardInspection?.children?.map((route) => [
        route.name,
        route.meta?.authority,
      ]) ?? [],
    );
    expect(hazardAuthorities).toEqual({
      PinganCurtainWallPenalty: [
        'PINGAN_HAZARD_CURTAIN_WALL_PENALTY_ENTRY',
      ],
      PinganCurtainWallRoutineCheck: [
        'PINGAN_HAZARD_CURTAIN_WALL_ROUTINE_CHECK_ENTRY',
      ],
      PinganHazardRectification: ['PINGAN_HAZARD_RECTIFICATION_ENTRY'],
      PinganQuickShot: ['PINGAN_HAZARD_QUICK_SHOT_ENTRY'],
      PinganSafetyCheck: ['PINGAN_HAZARD_SAFETY_CHECK_ENTRY'],
    });

    const training = routes.find((route) => route.name === 'PinganTraining');
    const trainingAuthorities = Object.fromEntries(
      training?.children?.map((route) => [
        route.name,
        route.meta?.authority,
      ]) ?? [],
    );
    expect(trainingAuthorities).toEqual({
      PinganExamResults: ['PINGAN_TRAINING_EXAM_RESULTS_ENTRY'],
      PinganExamTasks: ['PINGAN_TRAINING_EXAM_TASKS_ENTRY'],
      PinganSafetyLearning: ['PINGAN_TRAINING_SAFETY_LEARNING_ENTRY'],
    });

    const safetyPoints = routes.find(
      (route) => route.name === 'PinganSafetyPoints',
    );
    const safetyPointAuthorities = Object.fromEntries(
      safetyPoints?.children?.map((route) => [
        route.name,
        route.meta?.authority,
      ]) ?? [],
    );
    expect(safetyPointAuthorities).toEqual({
      PinganPointsFlow: ['PINGAN_POINTS_FLOW_ENTRY'],
      PinganPointsRanking: ['PINGAN_POINTS_RANKING_ENTRY'],
    });

    const safetyLedger = routes.find(
      (route) => route.name === 'PinganSafetyLedger',
    );
    const ledgerRoutes = safetyLedger?.children ?? [];
    expect(
      ledgerRoutes.every((route) =>
        route.meta?.authority?.includes('PINGAN_LEDGER_ENTRY'),
      ),
    ).toBe(true);
    expect(
      ledgerRoutes.flatMap((route) => route.children ?? []).every((route) =>
        route.meta?.authority?.includes('PINGAN_LEDGER_ENTRY'),
      ),
    ).toBe(true);

    const database = routes.find((route) => route.name === 'PinganDatabase');
    expect(
      database?.children?.every((route) =>
        route.meta?.authority?.includes('PINGAN_DATABASE_ENTRY'),
      ),
    ).toBe(true);
  });

  it('keeps the pre-shift meeting route under the generic three-check path', () => {
    const threeChecks = routes.find((route) => route.name === 'PinganThreeChecks');
    const preShiftMeeting = threeChecks?.children?.find(
      (route) => route.name === 'PreShiftMeeting',
    );

    expect(preShiftMeeting).toMatchObject({
      name: 'PreShiftMeeting',
      path: '/pingan/three-checks/pre-shift-meeting',
      meta: {
        title: '班前会',
      },
    });
    expect(threeChecks?.redirect).toBe('/pingan/three-checks/pre-shift-meeting');
  });

  it('keeps the old pre-shift meeting path as a compatibility redirect', () => {
    expect(routes).toContainEqual(
      expect.objectContaining({
        meta: {
          hideInMenu: true,
          title: '班前会旧路径重定向',
        },
        name: 'PreShiftMeetingLegacyRedirect',
        path: '/pingan/pre-shift-meeting',
        redirect: '/pingan/three-checks/pre-shift-meeting',
      }),
    );
  });

  it('keeps all one-shift-three-check child modules on the dedicated workbench', () => {
    const threeChecks = routes.find((route) => route.name === 'PinganThreeChecks');

    expect(
      threeChecks?.children
        ?.filter((route) => route.component === OneShiftThreeChecksWorkbench)
        .map((route) => route.name),
    ).toEqual([
      'PinganTeamDispatch',
      'PinganCurtainWallTeamDispatch',
      'PreShiftMeeting',
      'PinganPreShiftSafetyActivity',
      'PinganPreShiftInspection',
      'PinganMidShiftInspection',
      'PinganPostShiftInspection',
      'PinganKeySites',
    ]);
  });

  it('routes hazard rectification to the dedicated order workbench and keeps other hazard modules shared', () => {
    const hazardInspection = routes.find(
      (route) => route.name === 'PinganHazardInspection',
    );
    const hazardRectification = hazardInspection?.children?.find(
      (route) => route.name === 'PinganHazardRectification',
    );
    const quickShot = hazardInspection?.children?.find(
      (route) => route.name === 'PinganQuickShot',
    );

    expect(
      hazardInspection?.children
        ?.filter((route) => route.component === GenericThreeCheckRecordWorkbench)
        .map((route) => route.name),
    ).toEqual([
      'PinganSafetyCheck',
      'PinganQuickShot',
      'PinganCurtainWallPenalty',
      'PinganCurtainWallRoutineCheck',
    ]);
    expect(hazardRectification?.component).toBe(HazardRectificationOrderPage);
    expect(hazardRectification).toMatchObject({
      name: 'PinganHazardRectification',
      path: '/pingan/hazard-inspection/rectification',
      meta: {
        title: '隐患整改',
      },
    });
    expect(
      hazardInspection?.children?.some(
        (route) =>
          route.name === 'PinganHazardRectificationLegacy' ||
          route.path === '/pingan/hazard-inspection/rectification/legacy',
      ),
    ).toBe(false);
    expect(quickShot).toMatchObject({
      component: GenericThreeCheckRecordWorkbench,
      name: 'PinganQuickShot',
      path: '/pingan/hazard-inspection/quick-shot',
      meta: {
        title: '随手拍',
      },
    });
  });

  it('routes risk level control to its own module page', () => {
    const riskManagement = routes.find(
      (route) => route.name === 'PinganRiskManagement',
    );
    const riskLevelControl = riskManagement?.children?.find(
      (route) => route.name === 'PinganRiskLevelControl',
    );

    expect(riskLevelControl).toMatchObject({
      component: RiskLevelControlPage,
      meta: {
        title: '风险分级管控',
      },
      name: 'PinganRiskLevelControl',
      path: '/pingan/risk-management/risk-level-control',
    });
  });

  it('routes risk four-color map to its own implemented page', () => {
    const riskManagement = routes.find(
      (route) => route.name === 'PinganRiskManagement',
    );
    const riskFourColorMap = riskManagement?.children?.find(
      (route) => route.name === 'PinganRiskFourColorMap',
    );

    expect(riskFourColorMap).toMatchObject({
      component: RiskFourColorMapPage,
      meta: {
        title: '风险四色图',
      },
      name: 'PinganRiskFourColorMap',
      path: '/pingan/risk-management/risk-four-color-map',
    });
  });

  it('routes special work to its own implemented page', () => {
    const specialWork = routes.find((route) => route.name === 'PinganSpecialWork');

    expect(specialWork).toMatchObject({
      component: SpecialWorkPage,
      meta: {
        title: '特殊作业',
      },
      name: 'PinganSpecialWork',
      path: '/pingan/special-work',
    });
  });

  it('uses the implemented workbench for selected template-backed modules', () => {
    const routeNamesUsingWorkbench = [
      'PinganSafetyCheck',
      'PinganQuickShot',
      'PinganCurtainWallPenalty',
      'PinganCurtainWallRoutineCheck',
      'PinganPointsFlow',
    ];

    function flatten(routeList: RouteRecordRaw[] = routes): RouteRecordRaw[] {
      return routeList.flatMap((route) => [
        route,
        ...flatten(route.children ?? []),
      ]);
    }

    const allRoutes = flatten();
    const workbenchRoutes = allRoutes.filter(
      (route) => route.component === GenericThreeCheckRecordWorkbench,
    );

    expect(workbenchRoutes.map((route) => route.name)).toEqual(
      routeNamesUsingWorkbench,
    );
  });

  it('routes training exams to their own implemented workbench', () => {
    const training = routes.find((route) => route.name === 'PinganTraining');

    expect(
      training?.children
        ?.filter((route) => route.component === TrainingExamPage)
        .map((route) => route.name),
    ).toEqual(['PinganExamTasks', 'PinganExamResults']);
  });

  it('routes safety learning to its own implemented page', () => {
    const training = routes.find((route) => route.name === 'PinganTraining');
    const safetyLearning = training?.children?.find(
      (route) => route.name === 'PinganSafetyLearning',
    );

    expect(safetyLearning).toMatchObject({
      component: SafetyLearningPage,
      meta: {
        title: '安全学习',
      },
      name: 'PinganSafetyLearning',
      path: '/pingan/training/safety-learning',
    });
  });

  it('routes the safety points ranking menu to the implemented dashboard page', () => {
    const safetyPoints = routes.find((route) => route.name === 'PinganSafetyPoints');
    const ranking = safetyPoints?.children?.find(
      (route) => route.name === 'PinganPointsRanking',
    );

    expect(ranking).toMatchObject({
      component: PointsRankingPage,
      name: 'PinganPointsRanking',
      path: '/pingan/safety-points/ranking',
      meta: {
        title: '积分榜单',
      },
    });
  });

  it('routes safety ledger leaves to the implemented read-only page', () => {
    const safetyLedger = routes.find(
      (route) => route.name === 'PinganSafetyLedger',
    );

    function flatten(routeList: RouteRecordRaw[] = []): RouteRecordRaw[] {
      return routeList.flatMap((route) => [
        route,
        ...flatten(route.children ?? []),
      ]);
    }

    const ledgerRoutes = flatten(safetyLedger?.children ?? []).filter(
      (route) => route.component === SafetyLedgerPage,
    );

    expect(safetyLedger).toMatchObject({
      meta: expect.objectContaining({
        hideChildrenInMenu: true,
      }),
      name: 'PinganSafetyLedger',
      path: '/pingan/safety-ledger',
      redirect:
        '/pingan/safety-ledger/rules-responsibility/safety-rules',
    });
    expect(ledgerRoutes).toHaveLength(27);
    expect(new Set(ledgerRoutes.map((route) => route.path)).size).toBe(27);
    expect(ledgerRoutes.map((route) => route.path)).toContain(
      '/pingan/safety-ledger/hazard-governance/hazard-rectification/rectification',
    );
  });

  it('does not duplicate system management entries under the old database menu', () => {
    const database = routes.find((route) => route.name === 'PinganDatabase');
    const riskHazardLibrary = database?.children?.find(
      (route) => route.name === 'PinganRiskHazardLibrary',
    );

    expect(database?.children?.map((route) => route.meta?.title)).toEqual([
      '数据库维护',
      '风险隐患库',
      '场所检查内容',
      '特殊作业措施',
    ]);
    expect(riskHazardLibrary).toMatchObject({
      component: RiskLevelControlPage,
      path: '/pingan/database/risk-hazard-library',
      props: {
        pageTitle: '风险隐患库',
      },
    });
  });
});
