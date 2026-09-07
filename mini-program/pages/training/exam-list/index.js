const examService = require("../../../services/safety-exam")
const { getOrgTree } = require("../../../services/organization")
const { organizationOptionsFor } = require("../../../utils/organization-options")
const { normalizeExamRecord } = require("../exam-data")

function getWx() {
  return wx
}

const STATUS_TABS = [
  { label: "待考试", value: "pending" },
  { label: "已考试", value: "done" }
]
const ALL_DEPARTMENT_OPTION = { label: "全部部门", value: "" }
const ALL_TEAM_OPTION = { label: "全部班组", value: "" }

function filterRecords(records, keyword, activeStatus) {
  const search = String(keyword || "").trim()
  return records.filter(record => {
    const matchesStatus = record.statusKey === activeStatus
    if (!search) {
      return matchesStatus
    }
    return matchesStatus && [record.code, record.person, record.examDate, record.exam]
      .some(value => String(value || "").includes(search))
  })
}

function statusToBackend(activeStatus) {
  return activeStatus === "done" ? "EXAMED" : "PENDING_EXAM"
}

function oppositeStatus(activeStatus) {
  return activeStatus === "done" ? "pending" : "done"
}

function countKey(activeStatus) {
  return activeStatus === "done" ? "done" : "pending"
}

function formatDate(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, "0")
  const day = String(date.getDate()).padStart(2, "0")
  return `${year}-${month}-${day}`
}

function addDays(date, days) {
  const next = new Date(date)
  next.setDate(next.getDate() + days)
  return next
}

function defaultDateRange() {
  const end = new Date()
  return {
    startDate: formatDate(addDays(end, -7)),
    endDate: formatDate(end)
  }
}

const INITIAL_DATE_RANGE = defaultDateRange()

function withAllDepartment(options = []) {
  return [ALL_DEPARTMENT_OPTION, ...options]
}

function withAllTeam(options = []) {
  return [ALL_TEAM_OPTION, ...options]
}

function buildExamQuery(data, activeStatus, pageSize = 20) {
  const query = {
    page: 1,
    pageSize,
    status: statusToBackend(activeStatus)
  }
  if (data.filterApplied) {
    query.dateStart = data.filterStartDate
    query.dateEnd = data.filterEndDate
  }
  if (data.selectedDepartmentId) {
    query.departmentId = data.selectedDepartmentId
  }
  if (data.selectedTeamId) {
    query.teamId = data.selectedTeamId
  }
  return query
}

function hasExamPerson(record = {}) {
  if (record.examPersonName || record.person) {
    return true
  }
  return (record.results || []).some(item => item.examPersonName || item.person)
}

async function enrichExamPersons(records = []) {
  if (typeof examService.getExamTaskDetail !== "function") {
    return records
  }
  return Promise.all(records.map(async record => {
    if (hasExamPerson(record)) {
      return record
    }
    const id = record.id || record.code
    if (!id) {
      return record
    }
    try {
      const detail = await examService.getExamTaskDetail(id)
      return {
        ...detail,
        ...record,
        results: detail && detail.results ? detail.results : record.results
      }
    } catch (error) {
      return record
    }
  }))
}

Page({
  data: {
    keyword: "",
    activeStatus: "pending",
    statusTabs: STATUS_TABS,
    statusCounts: {
      pending: 0,
      done: 0
    },
    allRecords: [],
    records: [],
    filterVisible: false,
    filterApplied: false,
    filterStartDate: INITIAL_DATE_RANGE.startDate,
    filterEndDate: INITIAL_DATE_RANGE.endDate,
    organizationNodes: [],
    departmentOptions: [ALL_DEPARTMENT_OPTION],
    teamOptions: [ALL_TEAM_OPTION],
    departmentIndex: 0,
    teamIndex: 0,
    selectedDepartmentId: "",
    selectedTeamId: "",
    currentDepartmentLabel: ALL_DEPARTMENT_OPTION.label,
    currentTeamLabel: ALL_TEAM_OPTION.label,
    loading: false
  },

  async onLoad(options = {}) {
    const activeStatus = options.status === "done" ? "done" : "pending"
    this.setData({ activeStatus })
    await this.loadOrganizationFilters()
    return this.loadRecords()
  },

  async loadOrganizationFilters() {
    try {
      const nodes = await getOrgTree()
      const departmentOptions = withAllDepartment(organizationOptionsFor(nodes, undefined, ["DEPARTMENT"]))
      const teamOptions = withAllTeam(organizationOptionsFor(nodes, undefined, ["TEAM"]))
      this.setData({
        organizationNodes: nodes,
        departmentOptions,
        teamOptions,
        departmentIndex: 0,
        teamIndex: 0,
        selectedDepartmentId: "",
        selectedTeamId: "",
        currentDepartmentLabel: ALL_DEPARTMENT_OPTION.label,
        currentTeamLabel: ALL_TEAM_OPTION.label
      })
    } catch (error) {
      this.setData({
        departmentOptions: [ALL_DEPARTMENT_OPTION],
        teamOptions: [ALL_TEAM_OPTION],
        currentDepartmentLabel: ALL_DEPARTMENT_OPTION.label,
        currentTeamLabel: ALL_TEAM_OPTION.label
      })
    }
  },

  async loadRecords() {
    const activeStatus = this.data.activeStatus
    const otherStatus = oppositeStatus(activeStatus)
    this.setData({ loading: true })
    try {
      const [activeResult, otherResult] = await Promise.all([
        examService.listExamTasks(buildExamQuery(this.data, activeStatus)),
        examService.listExamTasks(buildExamQuery(this.data, otherStatus, 1))
      ])
      const enrichedItems = await enrichExamPersons(activeResult.items || [])
      const allRecords = enrichedItems.map(normalizeExamRecord)
      this.setData({
        allRecords,
        records: filterRecords(allRecords, this.data.keyword, activeStatus),
        statusCounts: {
          ...this.data.statusCounts,
          [countKey(activeStatus)]: activeResult.total || allRecords.length,
          [countKey(otherStatus)]: otherResult.total || 0
        }
      })
    } finally {
      this.setData({ loading: false })
    }
  },

  updateKeyword(event) {
    const keyword = event.detail.value || ""
    this.setData({
      keyword,
      records: filterRecords(this.data.allRecords, keyword, this.data.activeStatus)
    })
  },

  search() {
    this.setData({
      records: filterRecords(this.data.allRecords, this.data.keyword, this.data.activeStatus)
    })
  },

  changeStatus(event) {
    const activeStatus = event.currentTarget.dataset.status
    this.setData({ activeStatus })
    return this.loadRecords()
  },

  openCreate() {
    getWx().navigateTo({ url: "/pages/training/exam-create-basic/index" })
  },

  openFilter() {
    this.setData({ filterVisible: !this.data.filterVisible })
  },

  closeFilter() {
    this.setData({ filterVisible: false })
  },

  changeFilterStartDate(event = {}) {
    const startDate = event.detail && event.detail.value ? event.detail.value : this.data.filterStartDate
    const endDate = startDate > this.data.filterEndDate ? startDate : this.data.filterEndDate
    this.setData({ filterStartDate: startDate, filterEndDate: endDate })
  },

  changeFilterEndDate(event = {}) {
    const endDate = event.detail && event.detail.value ? event.detail.value : this.data.filterEndDate
    const startDate = endDate < this.data.filterStartDate ? endDate : this.data.filterStartDate
    this.setData({ filterStartDate: startDate, filterEndDate: endDate })
  },

  changeDepartmentFilter(event = {}) {
    const index = Number(event.detail && event.detail.value ? event.detail.value : 0)
    const department = this.data.departmentOptions[index] || ALL_DEPARTMENT_OPTION
    const teamOptions = withAllTeam(
      department.value
        ? organizationOptionsFor(this.data.organizationNodes, department.value, ["TEAM"])
        : organizationOptionsFor(this.data.organizationNodes, undefined, ["TEAM"])
    )
    this.setData({
      departmentIndex: index,
      teamIndex: 0,
      selectedDepartmentId: department.value || "",
      selectedTeamId: "",
      currentDepartmentLabel: department.label || ALL_DEPARTMENT_OPTION.label,
      currentTeamLabel: ALL_TEAM_OPTION.label,
      teamOptions
    })
  },

  changeTeamFilter(event = {}) {
    const index = Number(event.detail && event.detail.value ? event.detail.value : 0)
    const team = this.data.teamOptions[index] || ALL_TEAM_OPTION
    this.setData({
      teamIndex: index,
      selectedTeamId: team.value || "",
      currentTeamLabel: team.label || ALL_TEAM_OPTION.label
    })
  },

  async applyFilter() {
    this.setData({ filterApplied: true, filterVisible: false })
    await this.loadRecords()
  },

  async resetFilter() {
    const teamOptions = withAllTeam(organizationOptionsFor(this.data.organizationNodes, undefined, ["TEAM"]))
    this.setData({
      filterApplied: false,
      filterStartDate: INITIAL_DATE_RANGE.startDate,
      filterEndDate: INITIAL_DATE_RANGE.endDate,
      departmentIndex: 0,
      teamIndex: 0,
      selectedDepartmentId: "",
      selectedTeamId: "",
      currentDepartmentLabel: ALL_DEPARTMENT_OPTION.label,
      currentTeamLabel: ALL_TEAM_OPTION.label,
      teamOptions,
      filterVisible: false
    })
    await this.loadRecords()
  },

  openBasic(event) {
    const id = event.currentTarget.dataset.id
    if (!id) return
    getWx().navigateTo({ url: `/pages/training/exam-basic/index?id=${id}` })
  },

  startExam(event) {
    const id = event.currentTarget.dataset.id
    if (!id) return
    getWx().navigateTo({ url: `/pages/training/exam-taking/index?id=${id}` })
  },

  goBack() {
    getWx().navigateBack()
  }
})
