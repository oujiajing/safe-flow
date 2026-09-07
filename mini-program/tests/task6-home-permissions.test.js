const assert = require("node:assert/strict")
const test = require("node:test")

const servicePath = require.resolve("../services/mini-three-check")
const organizationServicePath = require.resolve("../services/organization")
const RealDate = global.Date

class FrozenDate extends RealDate {
  constructor(...args) {
    if (args.length === 0) {
      super("2026-07-03T08:00:00+08:00")
      return
    }
    super(...args)
  }

  static now() {
    return new RealDate("2026-07-03T08:00:00+08:00").getTime()
  }
}

global.Date = FrozenDate

function reload(modulePath) {
  const resolved = require.resolve(modulePath)
  delete require.cache[resolved]
  return require(modulePath)
}

const greenActions = [
  { title: "待开会", sub: "0", id: "meeting", type: "module", moduleKey: "pre-shift-meeting" },
  { title: "待检查", sub: "0", id: "before-check", type: "module", moduleKey: "pre-shift-inspection" },
  { title: "待检查", sub: "0", id: "during-check", type: "module", moduleKey: "mid-shift-inspection" },
  { title: "待检查", sub: "0", id: "after-check", type: "module", moduleKey: "post-shift-inspection" }
]

const infoCards = [
  { title: "班组派班", id: "dispatch", type: "module" },
  { title: "派班记录表", id: "dispatch-records", type: "module" }
]

test("home entry helper keeps formal entries when no real permission codes are loaded", () => {
  const helper = reload("../utils/home-entries")

  assert.deepEqual(helper.filterHomeEntries(greenActions, []), greenActions)
  assert.deepEqual(helper.filterHomeEntries(infoCards, null), infoCards)
})

test("home entry helper keeps first-batch entries visible for admin permission set", () => {
  const helper = reload("../utils/home-entries")
  const adminCodes = [
    "PINGAN_THREE_CHECK_ENTRY",
    "PINGAN_THREE_CHECK_VIEW",
    "PINGAN_THREE_CHECK_EXECUTE",
    "PINGAN_TEAM_DISPATCH_MANAGE"
  ]

  assert.deepEqual(helper.filterHomeEntries(greenActions, adminCodes).map(item => item.id), [
    "meeting",
    "before-check",
    "during-check",
    "after-check"
  ])
  assert.deepEqual(helper.filterHomeEntries(infoCards, adminCodes).map(item => item.id), [
    "dispatch",
    "dispatch-records"
  ])
})

test("home entry helper requires the new entry and view pair", () => {
  const helper = reload("../utils/home-entries")

  assert.deepEqual(helper.filterHomeEntries(greenActions, ["PINGAN_THREE_CHECK_ENTRY"]), [])
  assert.deepEqual(helper.filterHomeEntries(infoCards, ["PINGAN_THREE_CHECK_ENTRY", "PINGAN_THREE_CHECK_VIEW"]).map(item => item.id), [])
})

test("home entry helper uses team dispatch permissions for dispatch cards", () => {
  const helper = reload("../utils/home-entries")

  assert.deepEqual(helper.filterHomeEntries(infoCards, ["PINGAN_TEAM_DISPATCH_ENTRY", "PINGAN_TEAM_DISPATCH_VIEW"]).map(item => item.id), [
    "dispatch",
    "dispatch-records"
  ])
})

test("home entry helper filters formal key-sites entry and rejects unknown prototype entries", () => {
  const helper = reload("../utils/home-entries")
  const circleEntries = [
    { id: "hazard", title: "隐患整改" },
    { id: "key-place", title: "重点场所" },
    { id: "unknown-module", title: "未知模块" }
  ]

  assert.deepEqual(helper.filterHomeEntries(circleEntries, ["PINGAN_THREE_CHECK_ENTRY"]).map(item => item.id), [])
  assert.deepEqual(
    helper.filterHomeEntries(circleEntries, ["PINGAN_KEY_SITES_ENTRY", "PINGAN_KEY_SITES_VIEW"]).map(item => item.id),
    ["key-place"]
  )
})

test("home entry helper filters formal special-work entry by the shared PC permissions", () => {
  const helper = reload("../utils/home-entries")
  const entries = [{ id: "special-work", title: "特种作业" }, { id: "unknown-module", title: "未知模块" }]

  assert.deepEqual(helper.filterHomeEntries(entries, ["PINGAN_SPECIAL_WORK_ENTRY"]).map(item => item.id), [])
  assert.deepEqual(
    helper.filterHomeEntries(entries, ["PINGAN_SPECIAL_WORK_ENTRY", "PINGAN_SPECIAL_WORK_VIEW"]).map(item => item.id),
    ["special-work"]
  )
})

test("home page keeps one-shift three-check green actions visible after permissions load", async () => {
  const serviceCalls = []
  require.cache[servicePath] = {
    id: servicePath,
    filename: servicePath,
    loaded: true,
    exports: {
      listRecords(moduleKey, query) {
        serviceCalls.push({ moduleKey, query })
        const todayTotals = {
          "pre-shift-meeting": { DRAFT: 2, WITHDRAWN: 1 },
          "pre-shift-inspection": { DRAFT: 4, WITHDRAWN: 0 },
          "mid-shift-inspection": { DRAFT: 5, WITHDRAWN: 1 },
          "post-shift-inspection": { DRAFT: 7, WITHDRAWN: 2 }
        }
        if (query.dateStart === "2026-07-03" && query.dateEnd === "2026-07-03") {
          return Promise.resolve({ items: [], total: (todayTotals[moduleKey] && todayTotals[moduleKey][query.status]) || 0 })
        }
        if (query.overdue === true) {
          return Promise.resolve({ items: [], total: 0 })
        }
        return Promise.resolve({ items: [], total: 0 })
      }
    }
  }
  global.wx = {
    getStorageSync(key) {
      if (key === "auth.permissionCodes") {
        return ["PINGAN_THREE_CHECK_ENTRY", "PINGAN_THREE_CHECK_VIEW"]
      }
      return undefined
    }
  }
  let pageConfig = null
  global.Page = config => {
    pageConfig = {
      ...config,
      data: { ...config.data },
      setData(patch) {
        this.data = { ...this.data, ...patch }
      }
    }
  }

  const homePath = require.resolve("../pages/home/home")
  delete require.cache[homePath]
  require("../pages/home/home")

  assert.deepEqual(pageConfig.data.greenActions.map(item => item.id), [
    "meeting",
    "before-check",
    "during-check",
    "after-check"
  ])

  await pageConfig.onShow()

  assert.deepEqual(pageConfig.data.greenActions.map(item => item.sub), ["3", "4", "6", "9"])
  assert.equal(serviceCalls.every(call => call.query.pageSize === 1), true)
  assert.equal(serviceCalls.some(call => call.query.status === "DRAFT" && call.query.dateStart === "2026-07-03" && call.query.dateEnd === "2026-07-03"), true)
  assert.equal(serviceCalls.some(call => call.query.overdue === true), true)
  assert.deepEqual(pageConfig.data.infoCards.map(item => item.id), [])
})

test("home page keeps all three-check modules visible with entry-only permissions", () => {
  require.cache[servicePath] = {
    id: servicePath,
    filename: servicePath,
    loaded: true,
    exports: {
      listRecords() {
        return Promise.resolve({ items: [], total: 0 })
      }
    }
  }
  global.wx = {
    getStorageSync(key) {
      if (key === "auth.permissionCodes") {
        return ["PINGAN_THREE_CHECK_ENTRY"]
      }
      return undefined
    }
  }
  let pageConfig = null
  global.Page = config => {
    pageConfig = {
      ...config,
      data: { ...config.data },
      setData(patch) {
        this.data = { ...this.data, ...patch }
      }
    }
  }

  const homePath = require.resolve("../pages/home/home")
  delete require.cache[homePath]
  require("../pages/home/home")

  pageConfig.applyPermissionEntries()

  assert.deepEqual(pageConfig.data.greenActions.map(item => item.id), [
    "meeting",
    "before-check",
    "during-check",
    "after-check"
  ])
  assert.deepEqual(pageConfig.data.infoCards, [])
  assert.equal(pageConfig.data.hasInfoCards, false)
  assert.equal(pageConfig.data.profileAreaHeightRpx, 186)
})

test("home layout adapts columns, rows, and top area to visible entries", () => {
  const { getModuleGridLayout, getTopAreaLayout } = reload("../utils/home-layout")

  for (const count of [1, 4, 6, 8]) {
    assert.equal(getModuleGridLayout(count).columnCount, 4)
    assert.equal(getModuleGridLayout(count).layoutClass, "circle-grid--4")
  }
  for (const count of [9, 10, 11]) {
    assert.equal(getModuleGridLayout(count).columnCount, 5)
    assert.equal(getModuleGridLayout(count).layoutClass, "circle-grid--5")
  }

  assert.deepEqual(getModuleGridLayout(0), {
    columnCount: 4,
    layoutClass: "circle-grid--4",
    rowCount: 0,
    swiperHeightRpx: 0
  })
  assert.equal(getModuleGridLayout(4).rowCount, 1)
  assert.equal(getModuleGridLayout(6).rowCount, 2)
  assert.equal(getModuleGridLayout(10).rowCount, 2)
  assert.equal(getModuleGridLayout(6).swiperHeightRpx, 268)
  assert.deepEqual(getTopAreaLayout(0), { hasInfoCards: false, profileAreaHeightRpx: 186 })
  assert.deepEqual(getTopAreaLayout(1), { hasInfoCards: true, profileAreaHeightRpx: 334 })
  assert.deepEqual(getTopAreaLayout(2), { hasInfoCards: true, profileAreaHeightRpx: 334 })
})

test("home page marks three-check actions with pending count for orange highlight", async () => {
  require.cache[servicePath] = {
    id: servicePath,
    filename: servicePath,
    loaded: true,
    exports: {
      listRecords(moduleKey, query) {
        if (moduleKey === "pre-shift-meeting" && query.status === "DRAFT") {
          return Promise.resolve({ items: [], total: 1 })
        }
        return Promise.resolve({ items: [], total: 0 })
      }
    }
  }
  global.wx = {
    getStorageSync(key) {
      if (key === "auth.permissionCodes") {
        return ["PINGAN_THREE_CHECK_ENTRY", "PINGAN_THREE_CHECK_VIEW"]
      }
      return undefined
    }
  }
  let pageConfig = null
  global.Page = config => {
    pageConfig = {
      ...config,
      data: { ...config.data },
      setData(patch) {
        this.data = { ...this.data, ...patch }
      }
    }
  }

  const homePath = require.resolve("../pages/home/home")
  delete require.cache[homePath]
  require("../pages/home/home")

  await pageConfig.onShow()

  assert.deepEqual(
    pageConfig.data.greenActions.map(item => ({ id: item.id, sub: item.sub, hasPending: item.hasPending })),
    [
      { id: "meeting", sub: "1", hasPending: true },
      { id: "before-check", sub: "0", hasPending: false },
      { id: "during-check", sub: "0", hasPending: false },
      { id: "after-check", sub: "0", hasPending: false }
    ]
  )
})

test("home page counts teams from the current user's department", async () => {
  require.cache[organizationServicePath] = {
    id: organizationServicePath,
    filename: organizationServicePath,
    loaded: true,
    exports: {
      getOrgTree() {
        return Promise.resolve([
          {
            id: 1,
            title: "Demo集团",
            orgType: "GROUP",
            children: [
              {
                id: 4,
                title: "Demo Works Company",
                orgType: "COMPANY",
                children: [
                  {
                    id: 101109,
                    title: "安全环保部",
                    orgType: "DEPARTMENT",
                    children: [
                      { id: 1011001, title: "安全一班", orgType: "TEAM", children: [] },
                      { id: 1011002, title: "安全二班", orgType: "TEAM", children: [] },
                      { id: 1011003, title: "安全三班", orgType: "TEAM", children: [] }
                    ]
                  },
                  {
                    id: 101110,
                    title: "生产部",
                    orgType: "DEPARTMENT",
                    children: [
                      { id: 1012001, title: "生产一班", orgType: "TEAM", children: [] }
                    ]
                  }
                ]
              }
            ]
          }
        ])
      }
    }
  }
  global.wx = {
    getStorageSync(key) {
      if (key === "auth.currentUser") {
        return { id: 7, realName: "安全员", departmentId: 101109 }
      }
      return undefined
    }
  }
  let pageConfig = null
  global.Page = config => {
    pageConfig = {
      ...config,
      data: { ...config.data },
      setData(patch) {
        this.data = { ...this.data, ...patch }
      }
    }
  }

  const homePath = require.resolve("../pages/home/home")
  delete require.cache[homePath]
  require("../pages/home/home")

  assert.equal(pageConfig.data.teamCount, 8)

  await pageConfig.loadDepartmentTeamCount()

  assert.equal(pageConfig.data.teamCount, 3)
  assert.deepEqual(pageConfig.data.currentProfile.organizationNames, ["安全环保部"])
})

test("home profile shows real organization path by account level and hides it for admins", () => {
  const previousPage = global.Page
  global.Page = () => {}
  const homePath = require.resolve("../pages/home/home")
  delete require.cache[homePath]
  const { resolveHomeOrganizationNames } = require("../pages/home/home")
  global.Page = previousPage

  const tree = [{
    id: 1,
    title: "Demo集团",
    orgType: "GROUP",
    children: [{
      id: 4,
      title: "Demo Works Company",
      orgType: "COMPANY",
      children: [{
        id: 20,
        title: "生产部",
        orgType: "DEPARTMENT",
        children: [{ id: 30, title: "生产一班", orgType: "TEAM", children: [] }]
      }]
    }]
  }]

  assert.deepEqual(resolveHomeOrganizationNames(tree, { orgId: 1, roles: ["GROUP_MANAGER"] }), ["Demo集团"])
  assert.deepEqual(resolveHomeOrganizationNames(tree, { orgId: 4, roles: ["COMPANY_MANAGER"] }), ["Demo Works Company"])
  assert.deepEqual(resolveHomeOrganizationNames(tree, { orgId: 20, roles: ["DEPARTMENT_MANAGER"] }), ["生产部"])
  assert.deepEqual(resolveHomeOrganizationNames(tree, { orgId: 30, roles: ["TEAM_LEADER"] }), ["生产部", "生产一班"])
  assert.deepEqual(resolveHomeOrganizationNames(tree, { orgId: 1, roles: ["ADMIN"] }), [])
})

