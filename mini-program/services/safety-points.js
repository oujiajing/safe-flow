const { request } = require("../utils/request")

function listPointFlow(query = {}) {
  return request({
    url: "/api/pingan/three-checks/points-flow/records",
    method: "GET",
    data: query
  })
}

function getRankingOverview(query = {}) {
  return request({
    url: "/api/pingan/safety-points/ranking/overview",
    method: "GET",
    data: query
  })
}

function listIndividualRanking(query = {}) {
  return request({
    url: "/api/pingan/safety-points/ranking/individual",
    method: "GET",
    data: query
  })
}

function listTeamRanking(query = {}) {
  return request({
    url: "/api/pingan/safety-points/ranking/team",
    method: "GET",
    data: query
  })
}

module.exports = {
  listPointFlow,
  getRankingOverview,
  listIndividualRanking,
  listTeamRanking
}
