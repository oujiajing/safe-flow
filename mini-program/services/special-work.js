const { request } = require("../utils/request")
const { uploadFile } = require("../utils/upload")

const BASE_PATH = "/api/pingan/special-work/records"
const STATUS_OPTIONS = [
  { value: "PENDING_APPROVAL", label: "待审批" },
  { value: "IN_PROGRESS", label: "作业中" },
  { value: "PENDING_ACCEPTANCE", label: "待验收" },
  { value: "COMPLETED", label: "已完成" }
]

function listSpecialWorkRecords(query = {}) {
  return request({ url: BASE_PATH, method: "GET", data: query })
}

function getSpecialWorkRecord(id) {
  return request({ url: `${BASE_PATH}/${id}`, method: "GET" })
}

function createSpecialWorkRecord(payload) {
  return request({ url: BASE_PATH, method: "POST", data: payload })
}

function updateSpecialWorkRecord(id, payload) {
  return request({ url: `${BASE_PATH}/${id}`, method: "PUT", data: payload })
}

function executeSpecialWorkAction(id, action, payload = {}) {
  return request({ url: `${BASE_PATH}/${id}/actions/${action}`, method: "POST", data: payload })
}

function uploadSpecialWorkImage(id, filePath) {
  return uploadFile({ url: `${BASE_PATH}/${id}/image`, filePath, name: "file", fileKind: "IMAGE" })
}

function statusLabel(status) {
  const option = STATUS_OPTIONS.find(item => item.value === status)
  return option ? option.label : status || "-"
}

module.exports = {
  BASE_PATH,
  STATUS_OPTIONS,
  listSpecialWorkRecords,
  getSpecialWorkRecord,
  createSpecialWorkRecord,
  executeSpecialWorkAction,
  updateSpecialWorkRecord,
  uploadSpecialWorkImage,
  statusLabel
}
