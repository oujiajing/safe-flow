const { request } = require("../utils/request")

function getDispatchFlow(rootDispatchRecordId) {
  return request({
    url: `/api/pingan/three-checks/flows/${rootDispatchRecordId}`,
    method: "GET"
  })
}

module.exports = {
  getDispatchFlow
}
