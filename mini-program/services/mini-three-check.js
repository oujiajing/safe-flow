const { request } = require("../utils/request")
const { uploadFile } = require("../utils/upload")

function recordsBase(moduleKey) {
  return `/api/mini/pingan/three-checks/${moduleKey}/records`
}

function listRecords(moduleKey, query) {
  return request({
    url: recordsBase(moduleKey),
    method: "GET",
    data: query
  })
}

function getRecord(moduleKey, id) {
  return request({
    url: `${recordsBase(moduleKey)}/${id}`,
    method: "GET"
  })
}

function createRecord(moduleKey, payload) {
  return request({
    url: recordsBase(moduleKey),
    method: "POST",
    data: payload
  })
}

function updateRecord(moduleKey, id, payload) {
  return request({
    url: `${recordsBase(moduleKey)}/${id}`,
    method: "PUT",
    data: payload
  })
}

function submitRecord(moduleKey, id) {
  return request({
    url: `${recordsBase(moduleKey)}/${id}/submit`,
    method: "POST"
  })
}

function withdrawRecord(moduleKey, id, reasonOrPayload) {
  const data = typeof reasonOrPayload === "string" ? { reason: reasonOrPayload } : reasonOrPayload
  return request({
    url: `${recordsBase(moduleKey)}/${id}/withdraw`,
    method: "POST",
    data
  })
}

function remindRecord(moduleKey, id) {
  return request({
    url: `${recordsBase(moduleKey)}/${id}/remind`,
    method: "POST"
  })
}

function workflowAction(moduleKey, id, payload) {
  return request({
    url: `${recordsBase(moduleKey)}/${id}/workflow-actions`,
    method: "POST",
    data: payload
  })
}

function getRecordWorkflow(moduleKey, id) {
  return request({
    url: `${recordsBase(moduleKey)}/${id}/workflow`,
    method: "GET"
  })
}

function openRectificationOrder(moduleKey, id) {
  return request({
    url: `${recordsBase(moduleKey)}/${id}/rectification-order`,
    method: "POST"
  })
}

function uploadAttachment(moduleKey, id, fileKind, filePath) {
  return uploadFile({
    url: `${recordsBase(moduleKey)}/${id}/attachments`,
    fileKind,
    filePath
  })
}

module.exports = {
  listRecords,
  getRecord,
  createRecord,
  updateRecord,
  submitRecord,
  withdrawRecord,
  remindRecord,
  workflowAction,
  getRecordWorkflow,
  openRectificationOrder,
  uploadAttachment
}
