const threeCheck = require("./mini-three-check")

const SUPPORTED_MODULE_KEYS = ["quick-shot", "safety-check"]

function assertSupportedModuleKey(moduleKey) {
  if (!SUPPORTED_MODULE_KEYS.includes(moduleKey)) {
    throw new Error(`Unsupported hazard source moduleKey: ${moduleKey}`)
  }
}

function withSupportedModule(moduleKey, callback) {
  assertSupportedModuleKey(moduleKey)
  return callback()
}

function listHazardRecords(moduleKey, query) {
  return withSupportedModule(moduleKey, () => threeCheck.listRecords(moduleKey, query))
}

function getHazardRecord(moduleKey, id) {
  return withSupportedModule(moduleKey, () => threeCheck.getRecord(moduleKey, id))
}

function createHazardRecord(moduleKey, payload) {
  return withSupportedModule(moduleKey, () => threeCheck.createRecord(moduleKey, payload))
}

function updateHazardRecord(moduleKey, id, payload) {
  return withSupportedModule(moduleKey, () => threeCheck.updateRecord(moduleKey, id, payload))
}

function submitHazardRecord(moduleKey, id) {
  return withSupportedModule(moduleKey, () => threeCheck.submitRecord(moduleKey, id))
}

function withdrawHazardRecord(moduleKey, id, reasonOrPayload) {
  return withSupportedModule(moduleKey, () => threeCheck.withdrawRecord(moduleKey, id, reasonOrPayload))
}

function remindHazardRecord(moduleKey, id) {
  return withSupportedModule(moduleKey, () => threeCheck.remindRecord(moduleKey, id))
}

function workflowHazardRecord(moduleKey, id, payload) {
  return withSupportedModule(moduleKey, () => threeCheck.workflowAction(moduleKey, id, payload))
}

function openSourceRectificationOrder(moduleKey, id) {
  return withSupportedModule(moduleKey, () => threeCheck.openRectificationOrder(moduleKey, id))
}

function uploadHazardAttachment(moduleKey, id, fileKind, filePath) {
  return withSupportedModule(moduleKey, () => threeCheck.uploadAttachment(moduleKey, id, fileKind, filePath))
}

module.exports = {
  SUPPORTED_MODULE_KEYS,
  listHazardRecords,
  getHazardRecord,
  createHazardRecord,
  updateHazardRecord,
  submitHazardRecord,
  withdrawHazardRecord,
  remindHazardRecord,
  workflowHazardRecord,
  openSourceRectificationOrder,
  uploadHazardAttachment
}
