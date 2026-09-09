function pad(value) {
  return String(value).padStart(2, "0")
}

function formatDate(date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

function buildPeriodRange(period = "current-month", now = new Date()) {
  if (period === "all") {
    return {}
  }

  const offset = period === "previous-month" ? -1 : 0
  const year = now.getFullYear()
  const month = now.getMonth() + offset
  return {
    dateStart: formatDate(new Date(year, month, 1)),
    dateEnd: formatDate(new Date(year, month + 1, 0))
  }
}

function formatScore(value) {
  const score = Number(value)
  if (!Number.isFinite(score)) {
    return "0"
  }
  return String(Math.trunc(score)).replace(/\B(?=(\d{3})+(?!\d))/g, ",")
}

function toRank(value, index) {
  const rank = Number(value)
  return Number.isFinite(rank) && rank > 0 ? rank : index + 1
}

function normalizeIndividualRows(rows = []) {
  return rows
    .map((row, index) => ({
      key: String(row.userId || row.rankId || `${row.userName || "person"}-${index}`),
      rank: toRank(row.rank, index),
      name: row.userName || "未命名人员",
      subtitle: row.team || "未分配班组",
      score: Number(row.currentScore) || 0,
      scoreText: formatScore(row.currentScore),
      source: row
    }))
    .sort((left, right) => left.rank - right.rank)
}

function normalizeTeamRows(rows = []) {
  return rows
    .map((row, index) => ({
      key: String(row.rankId || `${row.rankName || "team"}-${index}`),
      rankId: row.rankId === undefined || row.rankId === null ? "" : String(row.rankId),
      rank: toRank(row.rank, index),
      name: row.rankName || "未命名班组",
      subtitle: `${Number(row.memberCount) || 0}人`,
      memberCount: Number(row.memberCount) || 0,
      score: Number(row.currentScore) || 0,
      scoreText: formatScore(row.currentScore),
      source: row
    }))
    .sort((left, right) => left.rank - right.rank)
}

function splitRanking(rows = []) {
  return {
    podium: rows.filter(item => item.rank <= 3),
    list: rows.filter(item => item.rank > 3)
  }
}

function firstMatch(rows, values, selector) {
  const candidates = values
    .filter(value => value !== undefined && value !== null && String(value).trim())
    .map(value => String(value).trim())
  return rows.find(row => candidates.includes(String(selector(row)).trim())) || null
}

function findCurrentIndividual(rows = [], user = {}) {
  return firstMatch(
    rows,
    [user.realName, user.name, user.username, user.displayName],
    row => row.name
  )
}

function findCurrentTeam(rows = [], user = {}) {
  return firstMatch(rows, [user.teamId, user.orgId, user.organizationId], row => row.rankId)
    || firstMatch(rows, [user.teamName, user.team, user.organizationName], row => row.name)
}

module.exports = {
  buildPeriodRange,
  findCurrentIndividual,
  findCurrentTeam,
  formatScore,
  normalizeIndividualRows,
  normalizeTeamRows,
  splitRanking
}
