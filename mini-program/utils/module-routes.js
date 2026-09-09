const { MODULE_KEY_BY_ID, getModuleById, isFirstBatchModule } = require("../config/modules")

const SHELL_ROUTES = {
  dispatch: "/pages/dispatch/form/index",
  meeting: "/pages/three-check/meeting/index",
  "before-check": "/pages/three-check/inspection-list/index",
  "during-check": "/pages/three-check/inspection-list/index",
  "after-check": "/pages/three-check/inspection-list/index"
}

function encodeParams(params) {
  return Object.keys(params)
    .filter(key => params[key] !== undefined && params[key] !== null && params[key] !== "")
    .map(key => `${encodeURIComponent(key)}=${encodeURIComponent(params[key])}`)
    .join("&")
}

function appendQuery(path, params) {
  const query = encodeParams(params)
  if (!query) {
    return path
  }
  return `${path}${path.includes("?") ? "&" : "?"}${query}`
}

function getEntryRoute(moduleId, companyKey) {
  if (!isFirstBatchModule(moduleId)) {
    const moduleConfig = getModuleById(moduleId)
    if (moduleConfig && moduleConfig.enabled && moduleConfig.route) {
      return appendQuery(moduleConfig.route, { companyKey })
    }
    return null
  }

  if (moduleId === "dispatch" || moduleId === "meeting") {
    return appendQuery(SHELL_ROUTES[moduleId], { companyKey })
  }

  return appendQuery(SHELL_ROUTES[moduleId], {
    moduleId,
    moduleKey: MODULE_KEY_BY_ID[moduleId],
    companyKey
  })
}

module.exports = {
  SHELL_ROUTES,
  appendQuery,
  getEntryRoute
}
