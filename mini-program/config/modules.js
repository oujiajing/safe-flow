const { MODULE_PERMISSION_CODES } = require("./permissions")

const FIRST_BATCH_MODULE_IDS = [
  "dispatch",
  "meeting",
  "before-check",
  "during-check",
  "after-check"
]

const MODULE_KEY_BY_ID = {
  dispatch: "team-dispatch",
  meeting: "pre-shift-meeting",
  "before-check": "pre-shift-inspection",
  "during-check": "mid-shift-inspection",
  "after-check": "post-shift-inspection"
}

const ROUTE_BY_ID = {
  dispatch: "/pages/dispatch/list/index",
  "dispatch-records": "/pages/dispatch/list/index",
  meeting: "/pages/three-check/meeting/index",
  "before-check": "/pages/three-check/inspection-list/index?moduleKey=pre-shift-inspection",
  "during-check": "/pages/three-check/inspection-list/index?moduleKey=mid-shift-inspection",
  "after-check": "/pages/three-check/inspection-list/index?moduleKey=post-shift-inspection",
  "key-place": "/pages/key-sites/list/index",
  "special-work": "/pages/special-work/list/index",
  hazard: "/pages/hazard-rectification/order-list/index",
  "safety-check": "/pages/hazard/source-list/index?moduleKey=safety-check&title=%E5%AE%89%E5%85%A8%E6%A3%80%E6%9F%A5",
  snapshot: "/pages/hazard/source-list/index?moduleKey=quick-shot&title=%E9%9A%8F%E6%89%8B%E6%8B%8D",
  learning: "/pages/training/learning-list/index",
  exam: "/pages/training/exam-list/index",
  "point-flow": "/pages/points/flow/index",
  "point-rank": "/pages/points/rank/index"
}

const FORMAL_MODULES = [
  {
    id: "dispatch",
    name: "班组派班",
    moduleKey: "team-dispatch",
    enabled: true,
    mockOnly: false,
    route: ROUTE_BY_ID.dispatch,
    permissionCodes: MODULE_PERMISSION_CODES.dispatch
  },
  {
    id: "dispatch-records",
    name: "派班记录表",
    moduleKey: "team-dispatch",
    enabled: true,
    mockOnly: false,
    route: ROUTE_BY_ID["dispatch-records"],
    permissionCodes: MODULE_PERMISSION_CODES.dispatch
  },
  {
    id: "meeting",
    name: "班前会",
    moduleKey: "pre-shift-meeting",
    enabled: true,
    mockOnly: false,
    route: ROUTE_BY_ID.meeting,
    permissionCodes: MODULE_PERMISSION_CODES.meeting
  },
  {
    id: "before-check",
    name: "班前检查",
    moduleKey: "pre-shift-inspection",
    enabled: true,
    mockOnly: false,
    route: ROUTE_BY_ID["before-check"],
    permissionCodes: MODULE_PERMISSION_CODES["before-check"]
  },
  {
    id: "during-check",
    name: "班中检查",
    moduleKey: "mid-shift-inspection",
    enabled: true,
    mockOnly: false,
    route: ROUTE_BY_ID["during-check"],
    permissionCodes: MODULE_PERMISSION_CODES["during-check"]
  },
  {
    id: "after-check",
    name: "班后检查",
    moduleKey: "post-shift-inspection",
    enabled: true,
    mockOnly: false,
    route: ROUTE_BY_ID["after-check"],
    permissionCodes: MODULE_PERMISSION_CODES["after-check"]
  },
  {
    id: "key-place",
    name: "重点场所",
    moduleKey: "key-sites",
    enabled: true,
    mockOnly: false,
    route: ROUTE_BY_ID["key-place"],
    permissionCodes: MODULE_PERMISSION_CODES["key-place"]
  },
  {
    id: "special-work",
    name: "特种作业",
    moduleKey: "special-work",
    enabled: true,
    mockOnly: false,
    route: ROUTE_BY_ID["special-work"],
    permissionCodes: MODULE_PERMISSION_CODES["special-work"]
  },
  {
    id: "learning",
    name: "安全学习",
    moduleKey: "safety-learning",
    enabled: true,
    mockOnly: false,
    route: ROUTE_BY_ID.learning,
    permissionCodes: MODULE_PERMISSION_CODES.learning
  },
  {
    id: "exam",
    name: "安全考试",
    moduleKey: "safety-exam",
    enabled: true,
    mockOnly: false,
    route: ROUTE_BY_ID.exam,
    permissionCodes: MODULE_PERMISSION_CODES.exam
  },
  {
    id: "snapshot",
    name: "随手拍",
    moduleKey: "quick-shot",
    enabled: true,
    mockOnly: false,
    route: ROUTE_BY_ID.snapshot,
    permissionCodes: MODULE_PERMISSION_CODES.snapshot
  },
  {
    id: "hazard",
    name: "隐患排查",
    moduleKey: "hazard-rectification",
    enabled: true,
    mockOnly: false,
    route: ROUTE_BY_ID.hazard,
    permissionCodes: MODULE_PERMISSION_CODES.hazard
  },
  {
    id: "safety-check",
    name: "安全检查",
    moduleKey: "safety-check",
    enabled: true,
    mockOnly: false,
    route: ROUTE_BY_ID["safety-check"],
    permissionCodes: MODULE_PERMISSION_CODES["safety-check"]
  },
  {
    id: "point-flow",
    name: "积分流水",
    moduleKey: "points-flow",
    enabled: true,
    mockOnly: false,
    route: ROUTE_BY_ID["point-flow"],
    permissionCodes: MODULE_PERMISSION_CODES["point-flow"]
  },
  {
    id: "point-rank",
    name: "积分榜单",
    moduleKey: "points-ranking",
    enabled: true,
    mockOnly: false,
    route: ROUTE_BY_ID["point-rank"],
    permissionCodes: MODULE_PERMISSION_CODES["point-rank"]
  }
]

function getModuleById(id) {
  return FORMAL_MODULES.find(item => item.id === id) || null
}

function getRouteForModule(id) {
  const moduleConfig = getModuleById(id)
  return moduleConfig && moduleConfig.enabled && moduleConfig.route
    ? moduleConfig.route
    : null
}

function isFirstBatchModule(id) {
  return FIRST_BATCH_MODULE_IDS.includes(id)
}

module.exports = {
  FIRST_BATCH_MODULE_IDS,
  MODULE_KEY_BY_ID,
  ROUTE_BY_ID,
  FORMAL_MODULES,
  getModuleById,
  getRouteForModule,
  isFirstBatchModule
}
