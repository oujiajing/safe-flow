const { request } = require("../utils/request")

function getCurrentUser() {
  return request({
    url: "/api/user/info",
    method: "GET"
  })
}

function listPinganUsers() {
  return request({
    url: "/api/pingan/users",
    method: "GET"
  })
}

module.exports = {
  getCurrentUser,
  listPinganUsers
}
