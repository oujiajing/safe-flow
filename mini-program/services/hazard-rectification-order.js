const { request } = require("../utils/request")
const { uploadFile } = require("../utils/upload")

const BASE_URL = "/api/pingan/hazard-rectification/orders"

function listOrders(query) {
  return request({
    url: BASE_URL,
    method: "GET",
    data: query
  })
}

function createOrder(payload) {
  return request({
    url: BASE_URL,
    method: "POST",
    data: payload
  })
}

function getOrder(id) {
  return request({
    url: `${BASE_URL}/${id}`,
    method: "GET"
  })
}

function actionOrder(id, payload) {
  return request({
    url: `${BASE_URL}/${id}/actions`,
    method: "POST",
    data: payload
  })
}

function uploadOrderAttachment(id, fileKind, filePath) {
  return uploadFile({
    url: `${BASE_URL}/${id}/attachments`,
    fileKind,
    filePath
  })
}

module.exports = {
  listOrders,
  createOrder,
  getOrder,
  actionOrder,
  uploadOrderAttachment
}
