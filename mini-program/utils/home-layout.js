const EXPANDED_PROFILE_AREA_HEIGHT_RPX = 334
const COMPACT_PROFILE_AREA_HEIGHT_RPX = 186
const FIRST_MODULE_ROW_HEIGHT_RPX = 140
const ADDITIONAL_MODULE_ROW_HEIGHT_RPX = 128

function resolveModuleColumnCount(visibleCount) {
  return Number(visibleCount) >= 9 ? 5 : 4
}

function getModuleGridLayout(visibleCount) {
  const count = Math.max(0, Number(visibleCount) || 0)
  const columnCount = resolveModuleColumnCount(count)
  const rowCount = count > 0 ? Math.ceil(count / columnCount) : 0
  const swiperHeightRpx = rowCount > 0
    ? FIRST_MODULE_ROW_HEIGHT_RPX + (rowCount - 1) * ADDITIONAL_MODULE_ROW_HEIGHT_RPX
    : 0

  return {
    columnCount,
    layoutClass: `circle-grid--${columnCount}`,
    rowCount,
    swiperHeightRpx
  }
}

function getTopAreaLayout(infoCardCount) {
  const hasInfoCards = Number(infoCardCount) > 0
  return {
    hasInfoCards,
    profileAreaHeightRpx: hasInfoCards ? EXPANDED_PROFILE_AREA_HEIGHT_RPX : COMPACT_PROFILE_AREA_HEIGHT_RPX
  }
}

module.exports = {
  getModuleGridLayout,
  getTopAreaLayout,
  resolveModuleColumnCount
}
