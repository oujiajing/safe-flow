const pointsService = require("../../../services/safety-points")
const { getCurrentUser } = require("../../../utils/auth")
const {
  buildPeriodRange,
  findCurrentIndividual,
  findCurrentTeam,
  normalizeIndividualRows,
  normalizeTeamRows,
  splitRanking
} = require("./ranking-data")

const PERIOD_OPTIONS = [
  { label: "本月", value: "current-month" },
  { label: "上月", value: "previous-month" },
  { label: "累计", value: "all" }
]

function buildTeamFilterLabels(rows) {
  return ["全部班组", ...new Set(rows.map(item => item.subtitle).filter(Boolean))]
}

function buildIndividualView(rows, teamLabel, currentUser) {
  const filteredRows = teamLabel === "全部班组"
    ? rows
    : rows.filter(item => item.subtitle === teamLabel)
  const ranking = splitRanking(filteredRows)
  return {
    individualPodium: ranking.podium,
    individualList: ranking.list,
    currentIndividual: findCurrentIndividual(filteredRows, currentUser)
  }
}

Page({
  data: {
    activeTab: "individual",
    loading: false,
    loadError: "",
    overview: {},
    periodOptions: PERIOD_OPTIONS,
    periodLabels: PERIOD_OPTIONS.map(item => item.label),
    periodIndex: 0,
    periodLabel: PERIOD_OPTIONS[0].label,
    teamFilterLabels: ["全部班组"],
    teamFilterIndex: 0,
    teamFilterLabel: "全部班组",
    individualRows: [],
    individualPodium: [],
    individualList: [],
    teamPodium: [],
    teamList: [],
    currentIndividual: null,
    currentTeam: null,
    individualTotal: 0,
    teamTotal: 0
  },

  onLoad() {
    return this.loadRanking()
  },

  async loadRanking() {
    const period = PERIOD_OPTIONS[this.data.periodIndex] || PERIOD_OPTIONS[0]
    const range = buildPeriodRange(period.value)
    const pageQuery = { ...range, page: 1, pageSize: 100 }
    this.setData({ loading: true, loadError: "" })

    try {
      const [overview, individualResult, teamResult] = await Promise.all([
        pointsService.getRankingOverview(range),
        pointsService.listIndividualRanking(pageQuery),
        pointsService.listTeamRanking({ ...pageQuery, rankBy: "TEAM" })
      ])
      const individuals = normalizeIndividualRows(individualResult && individualResult.items)
      const teams = normalizeTeamRows(teamResult && teamResult.items)
      const teamRanking = splitRanking(teams)
      const currentUser = getCurrentUser() || {}
      const teamFilterLabels = buildTeamFilterLabels(individuals)
      const selectedTeamLabel = teamFilterLabels.includes(this.data.teamFilterLabel)
        ? this.data.teamFilterLabel
        : teamFilterLabels[0]
      const individualView = buildIndividualView(individuals, selectedTeamLabel, currentUser)

      this.setData({
        overview: overview || {},
        ...individualView,
        individualRows: individuals,
        teamFilterLabels,
        teamFilterIndex: teamFilterLabels.indexOf(selectedTeamLabel),
        teamFilterLabel: selectedTeamLabel,
        teamPodium: teamRanking.podium,
        teamList: teamRanking.list,
        currentTeam: findCurrentTeam(teams, currentUser),
        individualTotal: Number(individualResult && individualResult.total) || individuals.length,
        teamTotal: Number(teamResult && teamResult.total) || teams.length
      })
    } catch (error) {
      this.setData({ loadError: "榜单加载失败，请稍后重试" })
    } finally {
      this.setData({ loading: false })
    }
  },

  switchTab(event) {
    const tab = event.currentTarget.dataset.tab
    if (tab !== "individual" && tab !== "team") {
      return
    }
    this.setData({ activeTab: tab })
  },

  changePeriod(event) {
    const periodIndex = Number(event.detail.value) || 0
    const period = PERIOD_OPTIONS[periodIndex] || PERIOD_OPTIONS[0]
    this.setData({
      periodIndex,
      periodLabel: period.label
    })
    return this.loadRanking()
  },

  changeTeamFilter(event) {
    const teamFilterIndex = Number(event.detail.value) || 0
    const teamFilterLabel = this.data.teamFilterLabels[teamFilterIndex] || "全部班组"
    this.setData({
      teamFilterIndex,
      teamFilterLabel,
      ...buildIndividualView(this.data.individualRows, teamFilterLabel, getCurrentUser() || {})
    })
  },

  retryLoad() {
    return this.loadRanking()
  },

  goBack() {
    wx.navigateBack()
  }
})
