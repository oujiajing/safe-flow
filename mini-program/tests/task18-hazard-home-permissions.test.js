const assert = require("node:assert/strict")
const test = require("node:test")

function reload(modulePath) {
  const resolved = require.resolve(modulePath)
  delete require.cache[resolved]
  return require(modulePath)
}

test("hazard permission constants and grouped module permissions are exported", () => {
  const { PERMISSION_CODES, MODULE_PERMISSION_CODES } = reload("../config/permissions")

  assert.equal(PERMISSION_CODES.HAZARD_ENTRY, "PINGAN_HAZARD_ENTRY")
  assert.equal(PERMISSION_CODES.HAZARD_QUICK_SHOT_VIEW, "PINGAN_HAZARD_QUICK_SHOT_VIEW")
  assert.equal(PERMISSION_CODES.HAZARD_RECTIFICATION_RECTIFY, "PINGAN_HAZARD_RECTIFICATION_RECTIFY")
  assert.deepEqual(MODULE_PERMISSION_CODES.hazard, [
    "PINGAN_HAZARD_ENTRY",
    "PINGAN_HAZARD_VIEW",
    "PINGAN_HAZARD_RECTIFICATION_ENTRY",
    "PINGAN_HAZARD_RECTIFICATION_VIEW"
  ])
  assert.deepEqual(MODULE_PERMISSION_CODES.snapshot, [
    "PINGAN_HAZARD_ENTRY",
    "PINGAN_HAZARD_QUICK_SHOT_ENTRY",
    "PINGAN_HAZARD_QUICK_SHOT_VIEW"
  ])
  assert.deepEqual(MODULE_PERMISSION_CODES["safety-check"], [
    "PINGAN_HAZARD_ENTRY",
    "PINGAN_HAZARD_SAFETY_CHECK_ENTRY",
    "PINGAN_HAZARD_SAFETY_CHECK_VIEW"
  ])
})

test("formal hazard modules are enabled with source routes and permission codes", () => {
  const { getModuleById } = reload("../config/modules")

  assert.deepEqual(
    ["hazard", "safety-check", "snapshot"].map(id => {
      const moduleConfig = getModuleById(id)
      return {
        id,
        moduleKey: moduleConfig.moduleKey,
        enabled: moduleConfig.enabled,
        mockOnly: moduleConfig.mockOnly,
        route: moduleConfig.route,
        permissionCodes: moduleConfig.permissionCodes
      }
    }),
    [
      {
        id: "hazard",
        moduleKey: "hazard-rectification",
        enabled: true,
        mockOnly: false,
        route: "/pages/hazard-rectification/order-list/index",
        permissionCodes: [
          "PINGAN_HAZARD_ENTRY",
          "PINGAN_HAZARD_VIEW",
          "PINGAN_HAZARD_RECTIFICATION_ENTRY",
          "PINGAN_HAZARD_RECTIFICATION_VIEW"
        ]
      },
      {
        id: "safety-check",
        moduleKey: "safety-check",
        enabled: true,
        mockOnly: false,
        route: "/pages/hazard/source-list/index?moduleKey=safety-check&title=%E5%AE%89%E5%85%A8%E6%A3%80%E6%9F%A5",
        permissionCodes: [
          "PINGAN_HAZARD_ENTRY",
          "PINGAN_HAZARD_SAFETY_CHECK_ENTRY",
          "PINGAN_HAZARD_SAFETY_CHECK_VIEW"
        ]
      },
      {
        id: "snapshot",
        moduleKey: "quick-shot",
        enabled: true,
        mockOnly: false,
        route: "/pages/hazard/source-list/index?moduleKey=quick-shot&title=%E9%9A%8F%E6%89%8B%E6%8B%8D",
        permissionCodes: [
          "PINGAN_HAZARD_ENTRY",
          "PINGAN_HAZARD_QUICK_SHOT_ENTRY",
          "PINGAN_HAZARD_QUICK_SHOT_VIEW"
        ]
      }
    ]
  )
})

test("module routes point hazard and source modules to registered pages", () => {
  const routes = reload("../utils/module-routes")

  assert.equal(routes.getEntryRoute("hazard", "gs"), "/pages/hazard-rectification/order-list/index?companyKey=gs")
  assert.equal(routes.getEntryRoute("safety-check", "gs"), "/pages/hazard/source-list/index?moduleKey=safety-check&title=%E5%AE%89%E5%85%A8%E6%A3%80%E6%9F%A5&companyKey=gs")
  assert.equal(routes.getEntryRoute("snapshot", "gs"), "/pages/hazard/source-list/index?moduleKey=quick-shot&title=%E9%9A%8F%E6%89%8B%E6%8B%8D&companyKey=gs")
  assert.equal(routes.getEntryRoute("key-place", "gs"), "/pages/key-sites/list/index?companyKey=gs")
})

test("home entry helper filters enabled formal hazard modules by permission codes", () => {
  const helper = reload("../utils/home-entries")
  const entries = [
    { id: "hazard", title: "隐患整改" },
    { id: "safety-check", title: "安全检查" },
    { id: "snapshot", title: "随手拍" },
    { id: "key-place", title: "重点场所" }
  ]

  assert.deepEqual(helper.filterHomeEntries(entries, []).map(item => item.id), [
    "hazard",
    "safety-check",
    "snapshot",
    "key-place"
  ])
  assert.deepEqual(helper.filterHomeEntries(entries, ["PINGAN_HAZARD_ENTRY"]).map(item => item.id), [
  ])
  assert.deepEqual(
    helper.filterHomeEntries(entries, [
      "PINGAN_HAZARD_ENTRY",
      "PINGAN_HAZARD_QUICK_SHOT_ENTRY",
      "PINGAN_HAZARD_QUICK_SHOT_VIEW"
    ]).map(item => item.id),
    ["snapshot"]
  )
  assert.deepEqual(
    helper.filterHomeEntries(entries, ["PINGAN_KEY_SITES_ENTRY", "PINGAN_KEY_SITES_VIEW"]).map(item => item.id),
    ["key-place"]
  )
})

test("home page filters hazard circle actions by permission codes and loads badges", async () => {
  const threeCheckPath = require.resolve("../services/mini-three-check")
  require.cache[threeCheckPath] = {
    id: threeCheckPath,
    filename: threeCheckPath,
    loaded: true,
    exports: {
      listRecords() {
        return Promise.resolve({ items: [], total: 0 })
      }
    }
  }

  const hazardStatsPath = require.resolve("../utils/hazard-stats")
  const hazardCalls = []
  require.cache[hazardStatsPath] = {
    id: hazardStatsPath,
    filename: hazardStatsPath,
    loaded: true,
    exports: {
      loadPendingRectificationCount() {
        hazardCalls.push({ type: "rectification" })
        return Promise.resolve(8)
      },
      loadPendingSourceCount(moduleKey) {
        hazardCalls.push({ type: "source", moduleKey })
        return Promise.resolve(moduleKey === "safety-check" ? 3 : 5)
      }
    }
  }

  global.wx = {
    getStorageSync(key) {
      if (key === "auth.permissionCodes") {
        return [
          "PINGAN_HAZARD_ENTRY",
          "PINGAN_HAZARD_QUICK_SHOT_ENTRY",
          "PINGAN_HAZARD_QUICK_SHOT_VIEW",
          "PINGAN_HAZARD_SAFETY_CHECK_ENTRY",
          "PINGAN_HAZARD_SAFETY_CHECK_VIEW"
        ]
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

  assert.equal(pageConfig.data.circleActions.some(item => item.id === "hazard"), false)
  assert.deepEqual(
    pageConfig.data.circleActions
      .filter(item => ["safety-check", "snapshot"].includes(item.id))
      .map(item => ({ id: item.id, badge: item.badge })),
    [
      { id: "safety-check", badge: "3" },
      { id: "snapshot", badge: "5" }
    ]
  )
  assert.deepEqual(hazardCalls, [
    { type: "source", moduleKey: "safety-check" },
    { type: "source", moduleKey: "quick-shot" }
  ])
})

test("home hazard count completion respects permissions narrowed while requests are in flight", async () => {
  const threeCheckPath = require.resolve("../services/mini-three-check")
  require.cache[threeCheckPath] = {
    id: threeCheckPath,
    filename: threeCheckPath,
    loaded: true,
    exports: {
      listRecords() {
        return Promise.resolve({ items: [], total: 0 })
      }
    }
  }

  const pendingCounts = []
  function pendingCount(value) {
    return new Promise(resolve => {
      pendingCounts.push(() => resolve(value))
    })
  }

  const hazardStatsPath = require.resolve("../utils/hazard-stats")
  require.cache[hazardStatsPath] = {
    id: hazardStatsPath,
    filename: hazardStatsPath,
    loaded: true,
    exports: {
      loadPendingRectificationCount() {
        return pendingCount(8)
      },
      loadPendingSourceCount(moduleKey) {
        return pendingCount(moduleKey === "safety-check" ? 3 : 5)
      }
    }
  }

  let permissionCodes = [
    "PINGAN_HAZARD_ENTRY",
    "PINGAN_HAZARD_VIEW",
    "PINGAN_HAZARD_RECTIFICATION_ENTRY",
    "PINGAN_HAZARD_RECTIFICATION_VIEW",
    "PINGAN_HAZARD_QUICK_SHOT_ENTRY",
    "PINGAN_HAZARD_QUICK_SHOT_VIEW",
    "PINGAN_HAZARD_SAFETY_CHECK_ENTRY",
    "PINGAN_HAZARD_SAFETY_CHECK_VIEW"
  ]
  global.wx = {
    getStorageSync(key) {
      if (key === "auth.permissionCodes") {
        return permissionCodes
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
  const showPromise = (require("../pages/home/home"), pageConfig.onShow())

  assert.equal(pendingCounts.length, 3)
  assert.deepEqual(
    pageConfig.data.circleActions
      .filter(item => ["hazard", "safety-check", "snapshot"].includes(item.id))
      .map(item => item.id),
    ["hazard", "safety-check", "snapshot"]
  )

  permissionCodes = ["PINGAN_HAZARD_ENTRY"]
  pageConfig.applyPermissionEntries()
  assert.deepEqual(
    pageConfig.data.circleActions
      .filter(item => ["hazard", "safety-check", "snapshot"].includes(item.id))
      .map(item => item.id),
    []
  )

  pendingCounts.forEach(resolve => resolve())
  await showPromise

  assert.deepEqual(
    pageConfig.data.circleActions
      .filter(item => ["hazard", "safety-check", "snapshot"].includes(item.id))
      .map(item => item.id),
    []
  )
})
