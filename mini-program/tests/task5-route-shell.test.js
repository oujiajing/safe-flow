const assert = require("node:assert/strict")
const test = require("node:test")

const servicePath = require.resolve("../services/mini-three-check")

function reload(modulePath) {
  const resolved = require.resolve(modulePath)
  delete require.cache[resolved]
  return require(modulePath)
}

test("route shell helper resolves first-batch module ids to new routes", () => {
  const routes = reload("../utils/module-routes")

  assert.equal(routes.getEntryRoute("dispatch", "gs"), "/pages/dispatch/form/index?companyKey=gs")
  assert.equal(routes.getEntryRoute("meeting", "gs"), "/pages/three-check/meeting/index?companyKey=gs")
  assert.equal(routes.getEntryRoute("before-check", "gs"), "/pages/three-check/inspection-list/index?moduleId=before-check&moduleKey=pre-shift-inspection&companyKey=gs")
  assert.equal(routes.getEntryRoute("during-check", "gs"), "/pages/three-check/inspection-list/index?moduleId=during-check&moduleKey=mid-shift-inspection&companyKey=gs")
  assert.equal(routes.getEntryRoute("after-check", "gs"), "/pages/three-check/inspection-list/index?moduleId=after-check&moduleKey=post-shift-inspection&companyKey=gs")
})

test("route shell helper uses configured routes for enabled modules", () => {
  const routes = reload("../utils/module-routes")

  assert.equal(routes.getEntryRoute("hazard", "yc"), "/pages/hazard-rectification/order-list/index?companyKey=yc")
  assert.equal(routes.getEntryRoute("snapshot", "yc"), "/pages/hazard/source-list/index?moduleKey=quick-shot&title=%E9%9A%8F%E6%89%8B%E6%8B%8D&companyKey=yc")
  assert.equal(routes.getEntryRoute("safety-check", "yc"), "/pages/hazard/source-list/index?moduleKey=safety-check&title=%E5%AE%89%E5%85%A8%E6%A3%80%E6%9F%A5&companyKey=yc")
  assert.equal(routes.getEntryRoute("dispatch-records", "yc"), "/pages/dispatch/list/index?companyKey=yc")
  assert.equal(routes.getEntryRoute("special-work", "yc"), "/pages/special-work/list/index?companyKey=yc")
})

test("route shell helper rejects unknown prototype modules", () => {
  const routes = reload("../utils/module-routes")

  assert.equal(routes.getEntryRoute("unknown-module", "gs"), null)
})

test("home page uses formal module routes", () => {
  const navigateCalls = []
  const switchTabCalls = []
  const storageCalls = []
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
    getStorageSync() {
      return undefined
    },
    setStorageSync(key, value) {
      storageCalls.push({ key, value })
    },
    removeStorageSync() {},
    navigateTo(options) {
      navigateCalls.push(options)
    },
    switchTab(options) {
      switchTabCalls.push(options)
    },
    getSystemInfoSync() {
      return {}
    }
  }
  global.Page = config => {
    global.__homePage = {
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

  assert.deepEqual(global.__homePage.data.noticeTabs.map(item => ({ title: item.title, id: item.id, type: item.type })), [
    { title: "班前会", id: "meeting", type: "module" },
    { title: "班前检查", id: "before-check", type: "module" },
    { title: "班中检查", id: "during-check", type: "module" },
    { title: "班后检查", id: "after-check", type: "module" }
  ])
  assert.deepEqual(global.__homePage.data.greenActions, [
    { title: "待开会", sub: "0", id: "meeting", type: "module", moduleKey: "pre-shift-meeting" },
    { title: "待检查", sub: "0", id: "before-check", type: "module", moduleKey: "pre-shift-inspection" },
    { title: "待检查", sub: "0", id: "during-check", type: "module", moduleKey: "mid-shift-inspection" },
    { title: "待检查", sub: "0", id: "after-check", type: "module", moduleKey: "post-shift-inspection" }
  ])

  global.__homePage.openModule({ currentTarget: { dataset: { id: "meeting", type: "module" } } })
  global.__homePage.openModule({ currentTarget: { dataset: { id: "before-check", type: "module" } } })
  global.__homePage.openModule({ currentTarget: { dataset: { id: "during-check", type: "module" } } })
  global.__homePage.openModule({ currentTarget: { dataset: { id: "after-check", type: "module" } } })
  global.__homePage.openModule({ currentTarget: { dataset: { id: "dispatch", type: "module" } } })
  global.__homePage.openModule({ currentTarget: { dataset: { id: "dispatch-records", type: "module" } } })
  global.__homePage.openModule({ currentTarget: { dataset: { id: "hazard", type: "module" } } })
  global.__homePage.openMessage()

  assert.deepEqual(navigateCalls, [
    { url: "/pages/three-check/meeting/index?companyKey=gs" },
    { url: "/pages/three-check/inspection-list/index?moduleId=before-check&moduleKey=pre-shift-inspection&companyKey=gs" },
    { url: "/pages/three-check/inspection-list/index?moduleId=during-check&moduleKey=mid-shift-inspection&companyKey=gs" },
    { url: "/pages/three-check/inspection-list/index?moduleId=after-check&moduleKey=post-shift-inspection&companyKey=gs" },
    { url: "/pages/dispatch/form/index?companyKey=gs" },
    { url: "/pages/dispatch/list/index?companyKey=gs" },
    { url: "/pages/hazard-rectification/order-list/index?companyKey=gs" }
  ])
  assert.deepEqual(switchTabCalls, [{ url: "/pages/message/message" }])
  assert.deepEqual(storageCalls, [
    { key: "activeCompanyKey", value: "gs" },
    { key: "activeCompanyKey", value: "gs" },
    { key: "activeCompanyKey", value: "gs" },
    { key: "activeCompanyKey", value: "gs" },
    { key: "activeCompanyKey", value: "gs" },
    { key: "activeCompanyKey", value: "gs" },
    { key: "activeCompanyKey", value: "gs" }
  ])
})
