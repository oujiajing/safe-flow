const { request } = require("../utils/request")

function getOrgTree() {
  return request({
    url: "/api/pingan/org/tree",
    method: "GET"
  })
}

function getCompanyTree() {
  return request({
    url: "/api/pingan/org/company-tree",
    method: "GET"
  })
}

module.exports = {
  getOrgTree,
  getCompanyTree
}
