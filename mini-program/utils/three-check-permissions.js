const {
  PRE_SHIFT_MEETING_ACTION_PERMISSION_CODES,
  THREE_CHECK_ACTION_PERMISSION_CODES
} = require("../config/permissions")
const { refreshPermissionCodes } = require("./auth-flow")

function permissionCodes() {
  if (typeof wx === "undefined" || typeof wx.getStorageSync !== "function") {
    return null
  }
  const codes = wx.getStorageSync("auth.permissionCodes")
  return Array.isArray(codes) ? codes : []
}

function canThreeCheckAction(action, moduleId) {
  const codes = permissionCodes()
  if (codes === null) {
    return true
  }
  const permissionMap = moduleId === "meeting"
    ? PRE_SHIFT_MEETING_ACTION_PERMISSION_CODES
    : THREE_CHECK_ACTION_PERMISSION_CODES
  const acceptedCodes = permissionMap[action] || []
  return acceptedCodes.some(code => codes.includes(code))
}

function threeCheckActionState(moduleId) {
  return {
    canCreateAction: canThreeCheckAction("CREATE", moduleId),
    canUpdateAction: canThreeCheckAction("UPDATE", moduleId),
    canSubmitAction: canThreeCheckAction("SUBMIT", moduleId),
    canWithdrawAction: canThreeCheckAction("VOID", moduleId),
    canRemindAction: canThreeCheckAction("REMIND", moduleId),
    canCreateRectificationOrderAction: canThreeCheckAction("RECTIFICATION_ORDER_CREATE", moduleId)
  }
}

async function refreshThreeCheckActionState(moduleId) {
  if (typeof wx !== "undefined"
      && typeof wx.getStorageSync === "function"
      && typeof wx.setStorageSync === "function") {
    await refreshPermissionCodes()
  }
  return threeCheckActionState(moduleId)
}

module.exports = {
  canThreeCheckAction,
  refreshThreeCheckActionState,
  threeCheckActionState
}
