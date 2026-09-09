const { request } = require("./request")

function safeTotal(result) {
  const total = Number(result && result.total)
  return Number.isFinite(total) && total > 0 ? total : 0
}

function loadPendingRectificationCount() {
  return request({
    url: "/api/pingan/hazard-rectification/orders",
    method: "GET",
    data: {
      status: "PENDING_RECTIFY",
      page: 1,
      pageSize: 1
    }
  }).then(safeTotal)
}

function loadPendingSourceCount(moduleKey) {
  return request({
    url: `/api/mini/pingan/three-checks/${moduleKey}/records`,
    method: "GET",
    data: {
      status: moduleKey === "quick-shot" ? "PENDING_REVIEW" : "DRAFT",
      page: 1,
      pageSize: 1
    }
  }).then(safeTotal)
}

module.exports = {
  loadPendingRectificationCount,
  loadPendingSourceCount,
  safeTotal
}
