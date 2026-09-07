const INSPECTION_MODULE_IDS = {
  "pre-shift-inspection": "before-check",
  "mid-shift-inspection": "during-check",
  "post-shift-inspection": "after-check"
}

function encode(value) {
  return encodeURIComponent(String(value == null ? "" : value))
}

function getNotificationBusinessRoute(message) {
  if (message && message.routeKey) {
    return getNotificationResolvedRoute(message.routeKey, message.routeParams || {})
  }
  if (!message || !message.bizId) return ""
  const id = encode(message.bizId)
  const moduleKey = message.moduleKey || ""

  if (moduleKey === "pre-shift-meeting") {
    return `/pages/three-check/meeting-detail/index?id=${id}`
  }
  if (INSPECTION_MODULE_IDS[moduleKey]) {
    return `/pages/three-check/inspection-detail/index?id=${id}&moduleId=${INSPECTION_MODULE_IDS[moduleKey]}&moduleKey=${encode(moduleKey)}`
  }
  if (moduleKey === "hazard-rectification") {
    return `/pages/hazard-rectification/order-detail/index?id=${id}`
  }
  if (moduleKey === "special-work") {
    return `/pages/special-work/detail/index?id=${id}`
  }
  return ""
}

function getNotificationResolvedRoute(routeKey, params = {}) {
  const id = encode(params.id)
  const routes = {
    THREE_CHECK_MEETING_DETAIL: `/pages/three-check/meeting-detail/index?id=${id}`,
    TEAM_DISPATCH_DETAIL: `/pages/dispatch/detail/index?id=${id}`,
    HAZARD_SOURCE_DETAIL: `/pages/hazard/source-detail/index?id=${id}`,
    HAZARD_RECTIFICATION_DETAIL: `/pages/hazard-rectification/order-detail/index?id=${id}`,
    SPECIAL_WORK_DETAIL: `/pages/special-work/detail/index?id=${id}`,
    SAFETY_EXAM_DETAIL: `/pages/training/exam-detail/index?id=${id}`,
    SAFETY_LEARNING_DETAIL: `/pages/training/learning-detail/index?id=${id}`,
    KEY_SITE_DETAIL: `/pages/key-sites/detail/index?id=${id}`,
    SAFETY_POINTS_FLOW: `/pages/points/flow/index?id=${id}`
  }
  if (routeKey === "THREE_CHECK_INSPECTION_DETAIL") {
    const moduleKey = params.moduleKey || ""
    return INSPECTION_MODULE_IDS[moduleKey]
      ? `/pages/three-check/inspection-detail/index?id=${id}&moduleId=${INSPECTION_MODULE_IDS[moduleKey]}&moduleKey=${encode(moduleKey)}`
      : ""
  }
  return routes[routeKey] || ""
}

module.exports = {
  getNotificationBusinessRoute,
  getNotificationResolvedRoute
}
