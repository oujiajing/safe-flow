function formatLocalDate(date) {
  const month = String(date.getMonth() + 1).padStart(2, "0")
  const day = String(date.getDate()).padStart(2, "0")
  return `${date.getFullYear()}-${month}-${day}`
}

function offsetDateString(offsetDays) {
  const date = new Date()
  date.setDate(date.getDate() + offsetDays)
  return formatLocalDate(date)
}

const TODAY = offsetDateString(0)
const YESTERDAY = offsetDateString(-1)

function getService() {
  return require("../services/mini-three-check")
}

function sumTotals(results) {
  return results.reduce((sum, result) => sum + (Number(result && result.total) || 0), 0)
}

function countRecords(moduleKey, query) {
  const { listRecords } = getService()
  return listRecords(moduleKey, {
    ...query,
    page: 1,
    pageSize: 1
  })
}

async function loadThreeCheckTabCounts(moduleKey) {
  const [draft, withdrawn, opened, archived, overdue] = await Promise.all([
    countRecords(moduleKey, { status: "DRAFT", dateStart: TODAY, dateEnd: TODAY }),
    countRecords(moduleKey, { status: "WITHDRAWN", dateStart: TODAY, dateEnd: TODAY }),
    countRecords(moduleKey, { status: "OPENED" }),
    countRecords(moduleKey, { status: "ARCHIVED" }),
    countRecords(moduleKey, { overdue: true })
  ])

  return {
    pending: sumTotals([draft, withdrawn]),
    done: sumTotals([opened, archived]),
    expired: sumTotals([overdue])
  }
}

async function loadPendingCount(moduleKey) {
  const counts = await loadThreeCheckTabCounts(moduleKey)
  return counts.pending
}

module.exports = {
  TODAY,
  YESTERDAY,
  loadPendingCount,
  loadThreeCheckTabCounts
}
