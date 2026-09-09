function randomPart() {
  return Math.random().toString(36).slice(2, 10)
}

function createSourceRecordId(moduleKey) {
  return `wx-local-${moduleKey}-${Date.now()}-${randomPart()}`
}

function createClientRequestId() {
  return `wx-req-${Date.now()}-${randomPart()}`
}

function createClientUpdatedAt(date = new Date()) {
  return date.toISOString().replace(/\.\d{3}Z$/, "")
}

module.exports = {
  createSourceRecordId,
  createClientRequestId,
  createClientUpdatedAt
}
