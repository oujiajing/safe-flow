const { request } = require("../utils/request")

const OVERVIEW_PATH = "/api/pingan/monitor-center/overview"

function compactQuery(query = {}) {
  return Object.keys(query).reduce((result, key) => {
    const value = query[key]
    if (value !== undefined && value !== null && value !== "") {
      result[key] = value
    }
    return result
  }, {})
}

function getMonitorCenterOverview(query = {}) {
  return request({ url: OVERVIEW_PATH, method: "GET", data: compactQuery(query) })
}

module.exports = {
  OVERVIEW_PATH,
  compactQuery,
  getMonitorCenterOverview
}
