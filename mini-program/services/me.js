const { request } = require("../utils/request")
const { uploadFile } = require("../utils/upload")

const BASE_PATH = "/api/mini/pingan/me"

function getMineOverview() {
  return request({ url: `${BASE_PATH}/overview`, method: "GET" })
}

function listMineRecords(type, query = {}) {
  return request({
    url: `${BASE_PATH}/records/${encodeURIComponent(type)}`,
    method: "GET",
    data: query
  })
}

function uploadAvatar(filePath) {
  return uploadFile({ url: `${BASE_PATH}/avatar`, filePath, name: "file", fileKind: "IMAGE" })
}

module.exports = {
  BASE_PATH,
  getMineOverview,
  listMineRecords,
  uploadAvatar
}
