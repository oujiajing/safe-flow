import { computed, reactive, ref } from 'vue';

import type {
  ThreeCheckCreateMode,
  ThreeCheckTableColumn,
} from './pre-shift-meeting.module';

export const dataMapTreeContinuousScrollConfig = {
  itemHeight: 28,
  virtual: false,
} as const;

export const dataMapResizeConfig = {
  defaultWidth: 280,
  minWidth: 220,
} as const;

export const threeCheckTableScrollConfig = {
  x: 1280,
  y: 'calc(100vh - 420px)',
} as const;

export const threeCheckTableColumnResizeConfig = {
  minWidth: 96,
  triggerEdgeWidth: 10,
} as const;

export const threeCheckPermissionCodes = {
  hazardClose: 'PINGAN_HAZARD_CLOSE',
  hazardQuickShotReport: 'PINGAN_HAZARD_QUICK_SHOT_REPORT',
  hazardReport: 'PINGAN_HAZARD_REPORT',
  hazardRectificationCreate: 'PINGAN_HAZARD_RECTIFICATION_CREATE',
} as const;

const oneShiftThreeChecksPermissions = {
  create: 'PINGAN_ONE_SHIFT_THREE_CHECKS_CREATE',
  delete: 'PINGAN_ONE_SHIFT_THREE_CHECKS_DELETE',
  export: 'PINGAN_ONE_SHIFT_THREE_CHECKS_EXPORT',
  remind: 'PINGAN_ONE_SHIFT_THREE_CHECKS_REMIND',
  submit: 'PINGAN_ONE_SHIFT_THREE_CHECKS_SUBMIT',
  update: 'PINGAN_ONE_SHIFT_THREE_CHECKS_UPDATE',
  view: 'PINGAN_ONE_SHIFT_THREE_CHECKS_VIEW',
  void: 'PINGAN_ONE_SHIFT_THREE_CHECKS_VOID',
} as const;

const preShiftMeetingPermissions = {
  create: 'PINGAN_PRE_SHIFT_MEETING_CREATE',
  delete: 'PINGAN_PRE_SHIFT_MEETING_DELETE',
  remind: 'PINGAN_PRE_SHIFT_MEETING_REMIND',
  submit: 'PINGAN_PRE_SHIFT_MEETING_SUBMIT',
  update: 'PINGAN_PRE_SHIFT_MEETING_UPDATE',
  view: 'PINGAN_PRE_SHIFT_MEETING_VIEW',
  void: 'PINGAN_PRE_SHIFT_MEETING_VOID',
} as const;

const threeCheckRoutePermissions = {
  PinganCurtainWallTeamDispatch: {
    create: 'PINGAN_CURTAIN_WALL_TEAM_DISPATCH_CREATE',
    delete: 'PINGAN_CURTAIN_WALL_TEAM_DISPATCH_DELETE',
    export: 'PINGAN_CURTAIN_WALL_TEAM_DISPATCH_EXPORT',
    remind: 'PINGAN_CURTAIN_WALL_TEAM_DISPATCH_REMIND',
    submit: 'PINGAN_CURTAIN_WALL_TEAM_DISPATCH_SUBMIT',
    update: 'PINGAN_CURTAIN_WALL_TEAM_DISPATCH_UPDATE',
    view: 'PINGAN_CURTAIN_WALL_TEAM_DISPATCH_VIEW',
    void: 'PINGAN_CURTAIN_WALL_TEAM_DISPATCH_VOID',
  },
  PinganKeySites: {
    create: 'PINGAN_KEY_SITES_CREATE',
    delete: 'PINGAN_KEY_SITES_DELETE',
    export: 'PINGAN_KEY_SITES_EXPORT',
    remind: 'PINGAN_KEY_SITES_REMIND',
    submit: 'PINGAN_KEY_SITES_SUBMIT',
    update: 'PINGAN_KEY_SITES_UPDATE',
    view: 'PINGAN_KEY_SITES_VIEW',
    void: 'PINGAN_KEY_SITES_VOID',
  },
  PinganMidShiftInspection: oneShiftThreeChecksPermissions,
  PinganPostShiftInspection: oneShiftThreeChecksPermissions,
  PinganPreShiftInspection: oneShiftThreeChecksPermissions,
  PinganPreShiftSafetyActivity: {
    create: 'PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_CREATE',
    delete: 'PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_DELETE',
    export: 'PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_EXPORT',
    remind: 'PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_REMIND',
    submit: 'PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_SUBMIT',
    update: 'PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_UPDATE',
    view: 'PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_VIEW',
    void: 'PINGAN_PRE_SHIFT_SAFETY_ACTIVITY_VOID',
  },
  PinganTeamDispatch: {
    create: 'PINGAN_TEAM_DISPATCH_CREATE',
    delete: 'PINGAN_TEAM_DISPATCH_DELETE',
    export: 'PINGAN_TEAM_DISPATCH_EXPORT',
    remind: 'PINGAN_TEAM_DISPATCH_REMIND',
    submit: 'PINGAN_TEAM_DISPATCH_SUBMIT',
    update: 'PINGAN_TEAM_DISPATCH_UPDATE',
    view: 'PINGAN_TEAM_DISPATCH_VIEW',
    void: 'PINGAN_TEAM_DISPATCH_VOID',
  },
  PreShiftMeeting: preShiftMeetingPermissions,
} as const;

const hazardRoutePermissions = {
  PinganCurtainWallPenalty: {
    create: 'PINGAN_HAZARD_CURTAIN_WALL_PENALTY_CREATE',
    delete: 'PINGAN_HAZARD_CURTAIN_WALL_PENALTY_DELETE',
    export: 'PINGAN_HAZARD_CURTAIN_WALL_PENALTY_EXPORT',
    view: 'PINGAN_HAZARD_CURTAIN_WALL_PENALTY_VIEW',
    void: 'PINGAN_HAZARD_CURTAIN_WALL_PENALTY_VOID',
  },
  PinganCurtainWallRoutineCheck: {
    create: 'PINGAN_HAZARD_CURTAIN_WALL_ROUTINE_CHECK_CREATE',
    delete: 'PINGAN_HAZARD_CURTAIN_WALL_ROUTINE_CHECK_DELETE',
    export: 'PINGAN_HAZARD_CURTAIN_WALL_ROUTINE_CHECK_EXPORT',
    view: 'PINGAN_HAZARD_CURTAIN_WALL_ROUTINE_CHECK_VIEW',
    void: 'PINGAN_HAZARD_CURTAIN_WALL_ROUTINE_CHECK_VOID',
  },
  PinganHazardRectification: {
    create: 'PINGAN_HAZARD_RECTIFICATION_CREATE',
    delete: 'PINGAN_HAZARD_RECTIFICATION_DELETE',
    export: 'PINGAN_HAZARD_RECTIFICATION_EXPORT',
    report: 'PINGAN_HAZARD_RECTIFICATION_REPORT',
    view: 'PINGAN_HAZARD_RECTIFICATION_VIEW',
    void: 'PINGAN_HAZARD_RECTIFICATION_VOID',
  },
  PinganQuickShot: {
    delete: 'PINGAN_HAZARD_QUICK_SHOT_DELETE',
    export: 'PINGAN_HAZARD_QUICK_SHOT_EXPORT',
    report: 'PINGAN_HAZARD_QUICK_SHOT_REPORT',
    review: 'PINGAN_HAZARD_QUICK_SHOT_REVIEW',
    view: 'PINGAN_HAZARD_QUICK_SHOT_VIEW',
    void: 'PINGAN_HAZARD_QUICK_SHOT_VOID',
  },
  PinganSafetyCheck: {
    create: 'PINGAN_HAZARD_SAFETY_CHECK_CREATE',
    delete: 'PINGAN_HAZARD_SAFETY_CHECK_DELETE',
    export: 'PINGAN_HAZARD_SAFETY_CHECK_EXPORT',
    view: 'PINGAN_HAZARD_SAFETY_CHECK_VIEW',
    void: 'PINGAN_HAZARD_SAFETY_CHECK_VOID',
  },
} as const;

const pointsFlowPermissions = {
  create: 'PINGAN_POINTS_FLOW_CREATE',
  delete: 'PINGAN_POINTS_FLOW_DELETE',
  legacyManage: 'PINGAN_POINTS_MANAGE',
} as const;

type HazardRouteName = keyof typeof hazardRoutePermissions;
type ThreeCheckPermissionRouteName = keyof typeof threeCheckRoutePermissions;

export function isThreeCheckDispatchRoute(routeName: string) {
  return (
    routeName === 'PinganTeamDispatch' ||
    routeName === 'PinganCurtainWallTeamDispatch'
  );
}

function isHazardPermissionRoute(routeName: string): routeName is HazardRouteName {
  return routeName in hazardRoutePermissions;
}

function isThreeCheckPermissionRoute(
  routeName: string,
): routeName is ThreeCheckPermissionRouteName {
  return routeName in threeCheckRoutePermissions;
}

function hasAnyAccessCode(accessCodes: string[], permissionCodes: Array<string | undefined>) {
  return permissionCodes.some((code) => !!code && accessCodes.includes(code));
}

export function canOpenThreeCheckCreateDialog(
  routeName: string,
  createMode: ThreeCheckCreateMode,
  accessCodes: string[],
) {
  return (
    createMode !== 'DISABLED' &&
    canCreateThreeCheckRecord(routeName, accessCodes)
  );
}

export function canCreateThreeCheckRecord(
  routeName: string,
  accessCodes: string[],
) {
  if (isHazardPermissionRoute(routeName)) {
    switch (routeName) {
      case 'PinganQuickShot':
        return hasAnyAccessCode(accessCodes, [
          hazardRoutePermissions.PinganQuickShot.report,
          threeCheckPermissionCodes.hazardReport,
        ]);
      case 'PinganHazardRectification':
        return hasAnyAccessCode(accessCodes, [
          hazardRoutePermissions.PinganHazardRectification.create,
          threeCheckPermissionCodes.hazardRectificationCreate,
          threeCheckPermissionCodes.hazardReport,
        ]);
      default:
        return hasAnyAccessCode(accessCodes, [
          hazardRoutePermissions[routeName].create,
        ]);
    }
  }
  if (isThreeCheckPermissionRoute(routeName)) {
    return accessCodes.includes(threeCheckRoutePermissions[routeName].create);
  }
  if (routeName === 'PinganPointsFlow') {
    return hasAnyAccessCode(accessCodes, [
      pointsFlowPermissions.create,
      pointsFlowPermissions.legacyManage,
    ]);
  }
  return false;
}

export function canViewThreeCheckRecord(
  routeName: string,
  accessCodes: string[],
) {
  if (isHazardPermissionRoute(routeName)) {
    switch (routeName) {
      case 'PinganQuickShot':
        return hasAnyAccessCode(accessCodes, [
          hazardRoutePermissions.PinganQuickShot.view,
          hazardRoutePermissions.PinganQuickShot.report,
          hazardRoutePermissions.PinganQuickShot.review,
          threeCheckPermissionCodes.hazardReport,
        ]);
      case 'PinganHazardRectification':
        return hasAnyAccessCode(accessCodes, [
          hazardRoutePermissions.PinganHazardRectification.view,
          hazardRoutePermissions.PinganHazardRectification.create,
          hazardRoutePermissions.PinganHazardRectification.report,
          threeCheckPermissionCodes.hazardRectificationCreate,
          threeCheckPermissionCodes.hazardReport,
        ]);
      default:
        return hasAnyAccessCode(accessCodes, [
          hazardRoutePermissions[routeName].view,
          hazardRoutePermissions[routeName].create,
        ]);
    }
  }
  if (isThreeCheckPermissionRoute(routeName)) {
    return accessCodes.includes(threeCheckRoutePermissions[routeName].view);
  }
  if (routeName === 'PinganPointsFlow') {
    return hasAnyAccessCode(accessCodes, [
      'PINGAN_POINTS_FLOW_VIEW',
      pointsFlowPermissions.create,
      pointsFlowPermissions.legacyManage,
    ]);
  }
  return false;
}

export function canSubmitThreeCheckRecord(
  routeName: string,
  accessCodes: string[],
) {
  if (isHazardPermissionRoute(routeName)) {
    switch (routeName) {
      case 'PinganQuickShot':
        return hasAnyAccessCode(accessCodes, [
          hazardRoutePermissions.PinganQuickShot.report,
          threeCheckPermissionCodes.hazardReport,
        ]);
      case 'PinganHazardRectification':
        return hasAnyAccessCode(accessCodes, [
          hazardRoutePermissions.PinganHazardRectification.report,
          threeCheckPermissionCodes.hazardReport,
        ]);
      default:
        return hasAnyAccessCode(accessCodes, [
          hazardRoutePermissions[routeName].create,
        ]);
    }
  }
  if (isThreeCheckPermissionRoute(routeName)) {
    return accessCodes.includes(threeCheckRoutePermissions[routeName].submit);
  }
  if (routeName === 'PinganPointsFlow') {
    return hasAnyAccessCode(accessCodes, [
      pointsFlowPermissions.create,
      pointsFlowPermissions.legacyManage,
    ]);
  }
  return false;
}

export function canUploadThreeCheckAttachment(
  routeName: string,
  accessCodes: string[],
) {
  if (isHazardPermissionRoute(routeName)) {
    return canCreateThreeCheckRecord(routeName, accessCodes);
  }
  if (isThreeCheckPermissionRoute(routeName)) {
    return accessCodes.includes(threeCheckRoutePermissions[routeName].update);
  }
  if (routeName === 'PinganPointsFlow') {
    return hasAnyAccessCode(accessCodes, [
      pointsFlowPermissions.create,
      pointsFlowPermissions.legacyManage,
    ]);
  }
  return false;
}

export function canDeleteThreeCheckRecord(routeName: string, accessCodes: string[]) {
  if (isHazardPermissionRoute(routeName)) {
    return hasAnyAccessCode(accessCodes, [
      hazardRoutePermissions[routeName].delete,
    ]);
  }
  if (isThreeCheckPermissionRoute(routeName)) {
    return accessCodes.includes(threeCheckRoutePermissions[routeName].delete);
  }
  if (routeName === 'PinganPointsFlow') {
    return hasAnyAccessCode(accessCodes, [
      pointsFlowPermissions.delete,
      pointsFlowPermissions.legacyManage,
    ]);
  }
  return false;
}

export function canExportThreeCheckRecords(routeName: string, accessCodes: string[]) {
  if (isHazardPermissionRoute(routeName)) {
    return hasAnyAccessCode(accessCodes, [
      hazardRoutePermissions[routeName].export,
    ]);
  }
  if (isThreeCheckPermissionRoute(routeName)) {
    const permissions = threeCheckRoutePermissions[routeName];
    return 'export' in permissions && accessCodes.includes(permissions.export);
  }
  return false;
}

export function canReviewQuickShotRecord(accessCodes: string[]) {
  return hasAnyAccessCode(accessCodes, [
    hazardRoutePermissions.PinganQuickShot.review,
    'PINGAN_HAZARD_ACCEPT',
  ]);
}

export function canRectifyHazardRectificationOrder(accessCodes: string[]) {
  return hasAnyAccessCode(accessCodes, [
    'PINGAN_HAZARD_RECTIFICATION_RECTIFY',
    'PINGAN_HAZARD_RECTIFICATION',
  ]);
}

export function canAcceptHazardRectificationOrder(accessCodes: string[]) {
  return hasAnyAccessCode(accessCodes, [
    'PINGAN_HAZARD_RECTIFICATION_ACCEPT',
    'PINGAN_HAZARD_ACCEPT',
  ]);
}

export function canVoidHazardRectificationOrder(accessCodes: string[]) {
  return hasAnyAccessCode(accessCodes, [
    'PINGAN_HAZARD_RECTIFICATION_VOID',
    threeCheckPermissionCodes.hazardClose,
  ]);
}

export function canWithdrawThreeCheckRecord(
  accessCodes: string[],
  routeName?: string,
) {
  if (routeName && isHazardPermissionRoute(routeName)) {
    return hasAnyAccessCode(accessCodes, [
      hazardRoutePermissions[routeName].void,
      threeCheckPermissionCodes.hazardClose,
    ]);
  }
  if (routeName && isThreeCheckPermissionRoute(routeName)) {
    return accessCodes.includes(threeCheckRoutePermissions[routeName].void);
  }
  return false;
}

export function canRemindThreeCheckRecord(accessCodes: string[], routeName?: string) {
  if (routeName && isThreeCheckPermissionRoute(routeName)) {
    return accessCodes.includes(threeCheckRoutePermissions[routeName].remind);
  }
  return false;
}

export function createThreeCheckWorkbenchText(title = '一班三查') {
  return {
    addButtonText: `新增${title}`,
    createSuccessText: `${title}已创建`,
    createTitle: `新增${title}`,
    dataLoadErrorText: `${title}数据加载失败`,
    detailTitle: `${title}详情`,
    emptyText: `暂无${title}数据`,
    formContentPlaceholder: `填写${title}内容`,
    recordButtonText: `${title}记录表`,
    submitSuccessText: `${title}已提交`,
    title,
    withdrawSuccessText: `${title}已撤回`,
  };
}

export function clampDataMapWidth(width: number) {
  return Math.max(dataMapResizeConfig.minWidth, Math.round(width));
}

export function createDataMapCollapseState() {
  const isDataMapCollapsed = ref(false);
  const dataMapToggleLabel = computed(() =>
    isDataMapCollapsed.value ? '展开' : '收起',
  );

  function toggleDataMap() {
    isDataMapCollapsed.value = !isDataMapCollapsed.value;
  }

  return {
    dataMapToggleLabel,
    isDataMapCollapsed,
    toggleDataMap,
  };
}

export function createDataMapResizeState(
  initialWidth = dataMapResizeConfig.defaultWidth,
) {
  const dataMapWidth = ref(clampDataMapWidth(initialWidth));
  const isDataMapResizing = ref(false);
  let startClientX = 0;
  let startWidth = dataMapWidth.value;

  function beginDataMapResize(clientX: number) {
    startClientX = clientX;
    startWidth = dataMapWidth.value;
    isDataMapResizing.value = true;
  }

  function updateDataMapResize(clientX: number) {
    if (!isDataMapResizing.value) {
      return;
    }
    dataMapWidth.value = clampDataMapWidth(
      startWidth + clientX - startClientX,
    );
  }

  function endDataMapResize() {
    isDataMapResizing.value = false;
  }

  return {
    beginDataMapResize,
    dataMapWidth,
    endDataMapResize,
    isDataMapResizing,
    updateDataMapResize,
  };
}

export function clampThreeCheckTableColumnWidth(width: number) {
  return Math.max(
    threeCheckTableColumnResizeConfig.minWidth,
    Math.round(width),
  );
}

export function createThreeCheckTableColumnResizeState(
  initialWidths: Record<string, number> = {},
) {
  const columnWidths = reactive<Record<string, number>>({
    ...initialWidths,
  });
  const isColumnResizing = ref(false);
  let activeColumnKey = '';
  let startClientX = 0;
  let startWidth = 0;

  function beginColumnResize(
    columnKey: string,
    clientX: number,
    currentWidth: number,
  ) {
    activeColumnKey = columnKey;
    startClientX = clientX;
    startWidth = currentWidth;
    isColumnResizing.value = true;
  }

  function updateColumnResize(clientX: number) {
    if (!isColumnResizing.value || !activeColumnKey) {
      return;
    }
    columnWidths[activeColumnKey] = clampThreeCheckTableColumnWidth(
      startWidth + clientX - startClientX,
    );
  }

  function endColumnResize() {
    isColumnResizing.value = false;
    activeColumnKey = '';
  }

  return {
    beginColumnResize,
    columnWidths,
    endColumnResize,
    isColumnResizing,
    updateColumnResize,
  };
}

export function createThreeCheckTableHeaderCellClass(
  column: ThreeCheckTableColumn,
) {
  return {
    'meeting-table__center-cell': true,
    'meeting-table__fixed-left-resizable-header':
      column.fixed === 'left' && column.dataIndex !== 'actions',
    'meeting-table__resizable-header': column.dataIndex !== 'actions',
  };
}

export function createThreeCheckTableBodyCellClass(
  _column: ThreeCheckTableColumn,
) {
  return {
    'meeting-table__center-cell': true,
  };
}
