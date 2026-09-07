const { listOrders } = require("../../../services/hazard-rectification-order")
const { getOrgTree } = require("../../../services/organization")
const { organizationOptionsFor } = require("../../../utils/organization-options")

const ALL_DEPARTMENT_OPTION = { label: "全部部门", value: "" }
const ALL_TEAM_OPTION = { label: "全部班组", value: "" }

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

Page({
  data: {
    status: "PENDING_RECTIFY",
    orders: [],
    total: 0,
    page: 1,
    pageSize: 20,
    filterApplied: false,
    startDate: INITIAL_DATE_RANGE.startDate,
    endDate: INITIAL_DATE_RANGE.endDate,
    organizationNodes: [],
    departmentOptions: [ALL_DEPARTMENT_OPTION],
    teamOptions: [ALL_TEAM_OPTION],
    departmentIndex: 0,
    teamIndex: 0,
    selectedDepartmentId: "",
    selectedTeamId: "",
    currentDepartmentLabel: ALL_DEPARTMENT_OPTION.label,
    currentTeamLabel: ALL_TEAM_OPTION.label,
    loading: false,
    errorText: "",
    navStatusBarHeight: 20,
    navBarHeight: 44,
    navHeight: 64,
    navTitleLeft: 112,
    navTitleRight: 112
  },

  async onLoad() {
    this.initCustomNavigation()
    await this.loadOrganizationFilters()
    await this.loadOrders()
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

  initCustomNavigation() {
    if (typeof wx === "undefined") return
    const systemInfo = wx.getSystemInfoSync ? wx.getSystemInfoSync() : {}
    const statusBarHeight = systemInfo.statusBarHeight || 20
    const windowWidth = systemInfo.windowWidth || 375
    let menuButton = null
    if (wx.getMenuButtonBoundingClientRect) {
      menuButton = wx.getMenuButtonBoundingClientRect()
    }
    const menuTop = menuButton && menuButton.top ? menuButton.top : statusBarHeight + 4
    const menuHeight = menuButton && menuButton.height ? menuButton.height : 32
    const navBarHeight = (menuTop - statusBarHeight) * 2 + menuHeight
    const menuLeft = menuButton && menuButton.left ? menuButton.left : windowWidth - 96
    const titleRight = Math.max(windowWidth - menuLeft + 10, 104)
    this.setData({
      navStatusBarHeight: statusBarHeight,
      navBarHeight,
      navHeight: statusBarHeight + navBarHeight,
      navTitleLeft: titleRight,
      navTitleRight: titleRight
    })
  },

  async loadOrders() {
    const query = {
      page: this.data.page,
      pageSize: this.data.pageSize
    }
    if (this.data.status !== "all") {
      query.status = this.data.status
    }
    if (this.data.filterApplied) {
      query.dateStart = this.data.startDate
      query.dateEnd = this.data.endDate
    }
    if (this.data.selectedDepartmentId) {
      query.departmentId = this.data.selectedDepartmentId
    }
    if (this.data.selectedTeamId) {
      query.teamId = this.data.selectedTeamId
    }
    this.setData({ loading: true, errorText: "" })
    try {
      const result = await listOrders(query)
      this.setData({
        orders: result.items || [],
        total: result.total || 0,
        loading: false
      })
    } catch (error) {
      this.setData({
        loading: false,
        errorText: error && error.message ? error.message : "整改单加载失败"
      })
    }
  },

  async changeStatus(event = {}) {
    const dataset = event.currentTarget && event.currentTarget.dataset ? event.currentTarget.dataset : {}
    const detail = event.detail || {}
    this.setData({ status: detail.value || dataset.status || "all", page: 1 })
    await this.loadOrders()
  },

  async changeStartDate(event = {}) {
    const startDate = event.detail && event.detail.value ? event.detail.value : this.data.startDate
    const endDate = startDate > this.data.endDate ? startDate : this.data.endDate
    this.setData({ startDate, endDate, filterApplied: true, page: 1 })
    await this.loadOrders()
  },

  async changeEndDate(event = {}) {
    const endDate = event.detail && event.detail.value ? event.detail.value : this.data.endDate
    const startDate = endDate < this.data.startDate ? endDate : this.data.startDate
    this.setData({ startDate, endDate, filterApplied: true, page: 1 })
    await this.loadOrders()
  },

  async changeDepartment(event = {}) {
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
      teamOptions,
      page: 1
    })
    await this.loadOrders()
  },

  async changeTeam(event = {}) {
    const index = Number(event.detail && event.detail.value ? event.detail.value : 0)
    const team = this.data.teamOptions[index] || ALL_TEAM_OPTION
    this.setData({
      teamIndex: index,
      selectedTeamId: team.value || "",
      currentTeamLabel: team.label || ALL_TEAM_OPTION.label,
      page: 1
    })
    await this.loadOrders()
  },

  openDetail(event = {}) {
    const dataset = event.currentTarget && event.currentTarget.dataset ? event.currentTarget.dataset : {}
    if (!dataset.id) return
    wx.navigateTo({ url: `/pages/hazard-rectification/order-detail/index?id=${dataset.id}` })
  },

  goBack() {
    const pages = typeof getCurrentPages === "function" ? getCurrentPages() : []
    if (pages.length > 1 && wx.navigateBack) {
      wx.navigateBack()
      return
    }
    if (wx.switchTab) {
      wx.switchTab({ url: "/pages/home/home" })
    }
  }
})
