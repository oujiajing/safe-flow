function draftKey(moduleKey, sourceRecordId) {
  return `draft:${moduleKey}:${sourceRecordId}`
}

function saveDraft(moduleKey, sourceRecordId, draft) {
  wx.setStorageSync(draftKey(moduleKey, sourceRecordId), draft)
}

function getDraft(moduleKey, sourceRecordId) {
  return wx.getStorageSync(draftKey(moduleKey, sourceRecordId))
}

function removeDraft(moduleKey, sourceRecordId) {
  wx.removeStorageSync(draftKey(moduleKey, sourceRecordId))
}

module.exports = {
  draftKey,
  saveDraft,
  getDraft,
  removeDraft
}
