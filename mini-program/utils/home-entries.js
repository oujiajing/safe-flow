const { getModuleById } = require("../config/modules")
const { MODULE_LEGACY_PERMISSION_CODES } = require("../config/permissions")

function hasAllPermissions(permissionCodes, requiredCodes) {
  const codes = Array.isArray(permissionCodes) ? permissionCodes : []
  return requiredCodes.every(code => codes.includes(code))
}

function shouldShowEntry(entry, permissionCodes) {
  const moduleConfig = getModuleById(entry.id)
  if (!moduleConfig || !moduleConfig.enabled || !moduleConfig.route) {
    return false
  }

  const codes = Array.isArray(permissionCodes) ? permissionCodes : []
  if (codes.length === 0) {
    return true
  }

  const legacyCodes = MODULE_LEGACY_PERMISSION_CODES[entry.id] || []
  return hasAllPermissions(codes, moduleConfig.permissionCodes || []) ||
    (legacyCodes.length > 0 && hasAllPermissions(codes, legacyCodes))
}

function filterHomeEntries(entries, permissionCodes) {
  if (!Array.isArray(entries)) {
    return []
  }
  return entries.filter(entry => shouldShowEntry(entry, permissionCodes))
}

module.exports = {
  filterHomeEntries,
  shouldShowEntry,
  hasAllPermissions
}
