const { FORMAL_MODULES } = require("../../config/modules")
const env = require("../../config/env")
const { getCurrentUser, getPermissionCodes } = require("../../utils/auth")
const { filterHomeEntries } = require("../../utils/home-entries")
const { getModuleGridLayout, getTopAreaLayout } = require("../../utils/home-layout")
const { getEntryRoute } = require("../../utils/module-routes")
const { syncTabBarSelected } = require("../../utils/tab-bar")
const { loadPendingCount } = require("../../utils/three-check-stats")
const { findOrganizationNode, findOrganizationPath, flattenOrganizations } = require("../../utils/organization-options")
const organizationService = require("../../services/organization")
const notificationService = require("../../services/notification")
const {
  loadPendingRectificationCount,
  loadPendingSourceCount
} = require("../../utils/hazard-stats")

function assetUrl(path) {
  const normalizedPath = String(path || "").replace(/^\/+/, "")
  return `${env.assetBaseUrl.replace(/\/+$/, "")}/${normalizedPath}`
}

const companyProfiles = [
  {
    key: "gs",
    tab: "广晟幕墙",
    company: "深圳广晟幕墙科技有限公司",
    department: "幕墙项目部",
    user: "蔡燕艳",
    role: "安全环保部副部长",
    teams: ["幕墙组装2班", "玻璃铝板外委件班", "门窗组装2班", "门窗组装3班", "门窗组装4班", "门窗组装5班", "幕墙机加2班", "幕墙机加3班"]
  },
  {
    key: "yc",
    tab: "源成公司",
    company: "河源市广晟源成建筑节能系统技术有限公司",
    department: "源成生产部",
    user: "沈光明",
    role: "安全员",
    teams: ["幕墙机加班组", "幕墙组装班组", "幕墙机加1班", "幕墙机加4班", "湖州一车间", "广州鸣驹班组"]
  },
  {
    key: "zy",
    tab: "资源公司",
    company: "广东省广晟矿业集团有限公司",
    department: "资源安全环保部",
    user: "戴军权",
    role: "安全管理员",
    teams: ["检测中心班", "废水处理1班", "废水处理2班", "维修班", "分离车间-球磨班1", "分离车间-分解班1"]
  },
  {
    key: "xc",
    tab: "广晟新材",
    company: "广东广晟稀有金属光电新材料有限公司",
    department: "新材安全环保部",
    user: "陈志斌",
    role: "安全管理员",
    teams: ["制品二车间-铝饼干燥线班1", "制品二车间-铝沉淀测试组1", "制品一车间-氢氧铝熔铸1", "株洲高力厂房巡检班"]
  }
]

function firstText(...values) {
  const value = values.find(item => item !== undefined && item !== null && String(item).trim() !== "")
  return value === undefined ? "" : String(value).trim()
}

function isAdminUser(user) {
  const roles = Array.isArray(user && user.roles) ? user.roles : []
  return roles.some(role => String(role).toUpperCase() === "ADMIN")
}

function organizationNodeName(node) {
  return firstText(node && node.title, node && node.orgName, node && node.name, node && node.label)
}

function resolveHomeOrganizationNames(orgTree, user) {
  if (!user || isAdminUser(user)) return []
  const scopedOrgId = user.orgId || user.organizationId || user.teamId || user.departmentId
  const path = findOrganizationPath(orgTree, scopedOrgId)
  const current = path[path.length - 1]
  if (!current) return []

  if (isOrganizationType(current, "GROUP") || isOrganizationType(current, "COMPANY")) {
    return [organizationNodeName(current)].filter(Boolean)
  }

  const department = [...path].reverse().find(node => isOrganizationType(node, "DEPARTMENT"))
  const team = [...path].reverse().find(node => isOrganizationType(node, "TEAM"))
  return [organizationNodeName(department), organizationNodeName(team)].filter(Boolean)
}

function buildCurrentProfile(user, fallbackProfile, orgTree = []) {
  return {
    displayName: firstText(user && user.realName, user && user.name, user && user.username, fallbackProfile.user),
    organizationNames: resolveHomeOrganizationNames(orgTree, user)
  }
}

const homeIcons = {
  hazard: "/assets/icons/hazard_rectification.webp",
  "safety-check": "/assets/icons/safety_inspection.webp",
  snapshot: "/assets/icons/quick_photo_report.webp",
  learning: "/assets/icons/safety_learning.webp",
  exam: "/assets/icons/safety_exam.webp",
  "point-rank": "/assets/icons/points_ranking.webp",
  "key-place": "/assets/icons/key_places.webp",
  "special-work": "/assets/icons/special_operation.webp"
}

function getIconPathById(actions) {
  return actions.reduce((memo, item) => {
    memo[item.id] = item.iconPath
    return memo
  }, {})
}

const circleActions = [
  { title: "隐患整改", iconPath: homeIcons.hazard, id: "hazard", type: "module" },
  { title: "安全检查", iconPath: homeIcons["safety-check"], id: "safety-check", type: "module" },
  { title: "随手拍", iconPath: homeIcons.snapshot, id: "snapshot", type: "module" },
  { title: "安全学习", iconPath: homeIcons.learning, id: "learning", type: "module" },
  { title: "安全考试", iconPath: homeIcons.exam, id: "exam", type: "module" },
  { title: "积分榜单", iconPath: homeIcons["point-rank"], id: "point-rank", type: "module" },
  { title: "重点场所", iconPath: homeIcons["key-place"], id: "key-place", type: "module" },
  { title: "特种作业", iconPath: homeIcons["special-work"], id: "special-work", type: "module" }
]

const homeBanners = [
  { src: assetUrl("mini-program/banners/1.jpg"), alt: "宣传栏1" },
  { src: assetUrl("mini-program/banners/2.jpg"), alt: "宣传栏2" },
  { src: assetUrl("mini-program/banners/3.jpg"), alt: "宣传栏3" },
  { src: assetUrl("mini-program/banners/4.jpg"), alt: "宣传栏4" },
  { src: assetUrl("mini-program/banners/5.jpg"), alt: "宣传栏5" },
  { src: assetUrl("mini-program/banners/6.jpg"), alt: "宣传栏6" }
]

const primaryModuleIds = ["hazard", "safety-check", "snapshot", "special-work", "learning", "exam", "point-rank", "key-place"]
const secondaryModuleIds = []

function pickModuleActions(actions, ids) {
  return ids
    .map(id => actions.find(item => item.id === id))
    .filter(Boolean)
}

function buildModulePages(actions) {
  const primaryActions = pickModuleActions(actions, primaryModuleIds)
  const pages = [{ key: "primary", actions: primaryActions, ...getModuleGridLayout(primaryActions.length) }]
  const secondaryActions = pickModuleActions(actions, secondaryModuleIds)
  if (secondaryActions.length) {
    pages.push({ key: "secondary", actions: secondaryActions, ...getModuleGridLayout(secondaryActions.length) })
  }
  return pages
}

function buildHomeLayoutState(actions, infoCards) {
  const modulePages = buildModulePages(actions)
  const moduleSwiperHeightRpx = modulePages.reduce(
    (height, page) => Math.max(height, page.swiperHeightRpx),
    0
  )
  return {
    ...getTopAreaLayout(infoCards.length),
    hasModuleEntries: modulePages.some(page => page.actions.length > 0),
    modulePages,
    moduleSwiperHeightRpx
  }
}

function hasOrganizationId(value) {
  return value !== undefined && value !== null && value !== ""
}

function isOrganizationType(node, orgType) {
  return String(node && node.orgType ? node.orgType : "").toUpperCase() === orgType
}

function resolveDepartmentId(orgTree, user) {
  if (user && hasOrganizationId(user.departmentId)) {
    return user.departmentId
  }
  const scopedOrgId = user && (user.orgId || user.organizationId || user.teamId)
  const path = findOrganizationPath(orgTree, scopedOrgId)
  const department = [...path].reverse().find(node => isOrganizationType(node, "DEPARTMENT"))
  return department ? department.id : undefined
}

function countDepartmentTeams(orgTree, departmentId) {
  const department = findOrganizationNode(orgTree, departmentId)
  if (!department) {
    return null
  }
  return flattenOrganizations(department.children || [])
    .filter(node => isOrganizationType(node, "TEAM"))
    .length
}

const prototypeGreenActions = [
  { title: "待开会", sub: "0", id: "meeting", type: "module", moduleKey: "pre-shift-meeting" },
  { title: "待检查", sub: "0", id: "before-check", type: "module", moduleKey: "pre-shift-inspection" },
  { title: "待检查", sub: "0", id: "during-check", type: "module", moduleKey: "mid-shift-inspection" },
  { title: "待检查", sub: "0", id: "after-check", type: "module", moduleKey: "post-shift-inspection" }
]

function withPendingState(item, subValue) {
  const sub = String(subValue ?? item.sub ?? "0")
  return {
    ...item,
    sub,
    hasPending: Number(sub) > 0
  }
}

const noticeTabEntries = [
  { title: "班前会", id: "meeting", type: "module" },
  { title: "班前检查", id: "before-check", type: "module" },
  { title: "班中检查", id: "during-check", type: "module" },
  { title: "班后检查", id: "after-check", type: "module" }
]

const prototypeInfoCards = [
  { title: "班组派班", desc: "快速创建今日派班", icon: "assignment-user", color: "#ff6b63", id: "dispatch", type: "module" },
  { title: "派班记录表", desc: "查看班次与任务记录", icon: "table", color: "#4f8df7", id: "dispatch-records", type: "module" }
]

const initialHomeLayout = buildHomeLayoutState(circleActions, prototypeInfoCards)

Page({
  data: {
    statusBarHeight: 20,
    navigationBarHeight: 44,
    companies: companyProfiles,
    activeCompanyKey: "gs",
    company: companyProfiles[0],
    currentProfile: buildCurrentProfile(getCurrentUser(), companyProfiles[0]),
    organizationTree: [],
    avatarText: companyProfiles[0].user.slice(0, 1),
    teamCount: companyProfiles[0].teams.length,
    noticeTabs: noticeTabEntries,
    greenActions: prototypeGreenActions,
    circleActions,
    ...initialHomeLayout,
    moduleSwiperCurrent: 0,
    homeBanners,
    infoCards: prototypeInfoCards,
    moduleCountText: `${FORMAL_MODULES.length} 项`,
    messageUnreadCount: 0
  },

  onLoad() {
    this.setupStatusBar()
  },

  onShow() {
    syncTabBarSelected(this, "pages/home/home")
    this.refreshCurrentProfile()
    this.applyPermissionEntries()
    return Promise.all([
      this.loadDepartmentTeamCount(),
      this.loadHomeThreeCheckCounts(),
      this.loadHomeHazardCounts(),
      this.loadMessageUnreadCount()
    ])
  },

  async loadMessageUnreadCount() {
    try {
      const counts = await notificationService.getUnreadCounts()
      this.setData({ messageUnreadCount: Number(counts && counts.unread ? counts.unread : 0) })
    } catch (error) {
      this.setData({ messageUnreadCount: 0 })
    }
  },

  refreshCurrentProfile() {
    this.setData({
      currentProfile: buildCurrentProfile(getCurrentUser(), this.data.company, this.data.organizationTree)
    })
  },

  async loadDepartmentTeamCount() {
    try {
      const orgTree = await organizationService.getOrgTree()
      const departmentId = resolveDepartmentId(orgTree, getCurrentUser())
      const teamCount = countDepartmentTeams(orgTree, departmentId)
      this.setData({
        organizationTree: orgTree,
        currentProfile: buildCurrentProfile(getCurrentUser(), this.data.company, orgTree),
        ...(teamCount !== null ? { teamCount } : {})
      })
    } catch (error) {
      // Keep the prototype fallback when organization data is unavailable.
    }
  },

  applyPermissionEntries() {
    const permissionCodes = getPermissionCodes()
    const countById = this.data.greenActions.reduce((memo, item) => {
      memo[item.id] = item.sub
      return memo
    }, {})
    const greenActions = prototypeGreenActions.map(item => withPendingState(item, countById[item.id] || item.sub))
    const badgeById = this.data.circleActions.reduce((memo, item) => {
      memo[item.id] = item.badge
      return memo
    }, {})
    const iconPathById = getIconPathById(this.data.circleActions)
    const nextCircleActions = circleActions.map(item => ({
      ...item,
      ...(iconPathById[item.id] ? { iconPath: iconPathById[item.id] } : {}),
      ...(badgeById[item.id] ? { badge: badgeById[item.id] } : {})
    }))
    const filteredCircleActions = filterHomeEntries(nextCircleActions, permissionCodes)
    const infoCards = filterHomeEntries(prototypeInfoCards, permissionCodes)
    this.setData({
      greenActions,
      circleActions: filteredCircleActions,
      infoCards,
      ...buildHomeLayoutState(filteredCircleActions, infoCards)
    })
  },

  async loadHomeThreeCheckCounts() {
    const actions = this.data.greenActions
    const updated = await Promise.all(actions.map(async item => {
      if (!item.moduleKey) return item
      try {
        const count = await loadPendingCount(item.moduleKey)
        return withPendingState(item, count)
      } catch (error) {
        return withPendingState(item)
      }
    }))
    this.setData({ greenActions: updated })
  },

  async loadHomeHazardCounts() {
    const loaders = {
      hazard: () => loadPendingRectificationCount(),
      "safety-check": () => loadPendingSourceCount("safety-check"),
      snapshot: () => loadPendingSourceCount("quick-shot")
    }
    const updated = await Promise.all(this.data.circleActions.map(async item => {
      const loadCount = loaders[item.id]
      if (!loadCount) return item
      try {
        const count = await loadCount()
        return { ...item, badge: String(count) }
      } catch (error) {
        return item
      }
    }))
    const badgeById = updated.reduce((memo, item) => {
      memo[item.id] = item.badge
      return memo
    }, {})
    const iconPathById = getIconPathById(this.data.circleActions)
    const nextCircleActions = circleActions.map(item => ({
      ...item,
      ...(iconPathById[item.id] ? { iconPath: iconPathById[item.id] } : {}),
      ...(badgeById[item.id] ? { badge: badgeById[item.id] } : {})
    }))
    const filteredCircleActions = filterHomeEntries(nextCircleActions, getPermissionCodes())
    this.setData({
      circleActions: filteredCircleActions,
      ...buildHomeLayoutState(filteredCircleActions, this.data.infoCards)
    })
  },

  setupStatusBar() {
    const systemInfo = wx.getSystemInfoSync ? wx.getSystemInfoSync() : {}
    const menuButton = wx.getMenuButtonBoundingClientRect ? wx.getMenuButtonBoundingClientRect() : null
    const statusBarHeight = systemInfo.statusBarHeight || 20
    const navigationBarHeight = menuButton
      && Number.isFinite(menuButton.top)
      && Number.isFinite(menuButton.height)
      ? Math.max(menuButton.height, (menuButton.top - statusBarHeight) * 2 + menuButton.height)
      : 44
    this.setData({ statusBarHeight, navigationBarHeight })
  },

  switchCompany(event) {
    const key = event.currentTarget.dataset.key
    const company = companyProfiles.find(item => item.key === key) || companyProfiles[0]
    this.setData({
      activeCompanyKey: key,
      company,
      currentProfile: buildCurrentProfile(getCurrentUser(), company, this.data.organizationTree),
      avatarText: company.user.slice(0, 1)
    })
    wx.setStorageSync("activeCompanyKey", key)
    wx.showToast({ title: `已切换至${company.tab}`, icon: "none" })
  },

  openMessage() {
    wx.switchTab({ url: "/pages/message/message" })
  },

  handleModuleSwiperChange(event) {
    this.setData({
      moduleSwiperCurrent: event.detail.current
    })
  },

  openModule(event) {
    const { id, type } = event.currentTarget.dataset
    const formalId = id === "study" ? "learning" : (id === "points" ? "point-rank" : id)
    wx.setStorageSync("activeCompanyKey", this.data.activeCompanyKey)
    const route = getEntryRoute(formalId, this.data.activeCompanyKey)
    if (!route) {
      wx.showToast({ title: "该功能暂不可用", icon: "none" })
      return
    }
    wx.navigateTo({ url: route })
  }
})

module.exports = {
  buildCurrentProfile,
  isAdminUser,
  resolveHomeOrganizationNames
}
