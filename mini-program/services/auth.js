const { request } = require("../utils/request")

function login({ username, password }) {
  return request({
    url: "/api/auth/login",
    method: "POST",
    data: { username, password }
  })
}

function getPermissionCodes() {
  return request({
    url: "/api/auth/codes",
    method: "GET"
  })
}

module.exports = {
  login,
  getPermissionCodes
}
