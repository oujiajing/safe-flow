const assert = require("node:assert/strict")
const fs = require("node:fs")
const path = require("node:path")
const test = require("node:test")

const threeCheckServicePath = require.resolve("../services/mini-three-check")
const flowServicePath = require.resolve("../services/flow")
const organizationServicePath = require.resolve("../services/organization")

function installStorage() {
  const store = {}
  global.wx = {
    getStorageSync(key) {
      return store[key]
    },
    setStorageSync(key, value) {
      store[key] = value
    },
    removeStorageSync(key) {
      delete store[key]
    },
    showToast() {},
    navigateBack() {}
  }
  return store
}

test("draft utility stores only module scoped local drafts", () => {
  const store = installStorage()
  const draft = require("../utils/draft")

  draft.saveDraft("team-dispatch", "source-1", { content: "安全交底" })

  assert.deepEqual(store["draft:team-dispatch:source-1"], { content: "安全交底" })
  assert.deepEqual(draft.getDraft("team-dispatch", "source-1"), { content: "安全交底" })

  draft.removeDraft("team-dispatch", "source-1")

  assert.equal(store["draft:team-dispatch:source-1"], undefined)
})

test("dispatch form saves draft locally and removes it after successful submit", async () => {
  const store = installStorage()
  require.cache[organizationServicePath] = {
    id: organizationServicePath,
    filename: organizationServicePath,
    loaded: true,
    exports: {
      getOrgTree() {
        return Promise.resolve([
          {
            id: 4,
            title: "Demo Works Company",
            orgType: "COMPANY",
            children: [
              {
                id: 101109,
                title: "幕墙组装",
                orgType: "DEPARTMENT",
                children: [
                  { id: 1011001, title: "幕墙组装1班", orgType: "TEAM", children: [] }
                ]
              }
            ]
          }
        ])
      }
    }
  }
  require.cache[threeCheckServicePath] = {
    id: threeCheckServicePath,
    filename: threeCheckServicePath,
    loaded: true,
    exports: {
      createRecord() {
        return Promise.resolve({ id: "d500" })
      },
      submitRecord() {
        return Promise.resolve({ id: "d500" })
      }
    }
  }
  require.cache[flowServicePath] = {
    id: flowServicePath,
    filename: flowServicePath,
    loaded: true,
    exports: {
      getDispatchFlow() {
        return Promise.resolve({ children: [] })
      }
    }
  }

  global.Page = config => {
    global.__dispatchDraftPage = {
      ...config,
      data: { ...config.data },
      setData(patch) {
        this.data = { ...this.data, ...patch }
      }
    }
  }

  const pagePath = require.resolve("../pages/dispatch/form/index")
  delete require.cache[pagePath]
  delete require.cache[require.resolve("../utils/draft")]
  require("../pages/dispatch/form/index")

  await global.__dispatchDraftPage.onLoad({ companyKey: "gs" })
  global.__dispatchDraftPage.changeDispatchTeam({ detail: { value: "0" } })
  global.__dispatchDraftPage.updateDispatchRemark({ detail: { value: "夜班前确认设备状态" } })
  global.__dispatchDraftPage.saveDispatchDraft()

  const sourceRecordId = global.__dispatchDraftPage.data.sourceRecordId
  assert.deepEqual(store[`draft:team-dispatch:${sourceRecordId}`], {
    companyId: 4,
    departmentId: 101109,
    teamId: 1011001,
    dispatchDate: global.__dispatchDraftPage.data.dispatchDate,
    dispatchTaskText: "班组派班",
    dispatchType: "今日",
    managementCount: "0",
    dispatchRemark: "夜班前确认设备状态",
    dispatchDraftName: "班组派班草稿"
  })

  await global.__dispatchDraftPage.submitDispatch()

  assert.equal(store[`draft:team-dispatch:${sourceRecordId}`], undefined)
})

test("formal first-batch pages do not depend on legacy prototype mock storage", () => {
  const root = path.resolve(__dirname, "..")
  const formalDirs = [
    "pages/dispatch",
    "pages/three-check/meeting",
    "pages/three-check/meeting-detail",
    "pages/three-check/inspection-list",
    "pages/three-check/inspection-detail"
  ]
  const forbidden = /companyProfiles|dispatchThreeCheckTasks|reminderMessages|createDispatchThreeCheckTasks/

  const hits = []
  for (const dir of formalDirs) {
    const absDir = path.join(root, dir)
    const stack = [absDir]
    while (stack.length) {
      const current = stack.pop()
      for (const entry of fs.readdirSync(current, { withFileTypes: true })) {
        const fullPath = path.join(current, entry.name)
        if (entry.isDirectory()) {
          stack.push(fullPath)
        } else if (/\.(js|wxml|wxss|json)$/.test(entry.name)) {
          const content = fs.readFileSync(fullPath, "utf8")
          if (forbidden.test(content)) hits.push(path.relative(root, fullPath))
        }
      }
    }
  }

  assert.deepEqual(hits, [])
})

test("retired one-shift-three-check form drafts are absent", () => {
  const appJson = JSON.parse(fs.readFileSync(path.join(__dirname, "../app.json"), "utf8"))
  const { registeredPages } = require("./helpers/app-routes")
  const pages = registeredPages(appJson)

  assert.equal(pages.includes("pages/three-check/meeting-form/index"), false)
  assert.equal(pages.includes("pages/three-check/inspection-form/index"), false)
  assert.equal(fs.existsSync(path.join(__dirname, "../pages/three-check/meeting-form")), false)
  assert.equal(fs.existsSync(path.join(__dirname, "../pages/three-check/inspection-form")), false)
  assert.equal(pages.includes("pages/three-check/meeting-detail/index"), true)
  assert.equal(pages.includes("pages/three-check/inspection-detail/index"), true)
})

test("removed prototype shells are absent from routes and source files", () => {
  const appJson = JSON.parse(fs.readFileSync(path.join(__dirname, "../app.json"), "utf8"))
  const { registeredPages } = require("./helpers/app-routes")
  const pages = registeredPages(appJson)

  assert.equal(pages.includes("pages/module/module"), false)
  assert.equal(pages.includes("pages/todo/todo"), false)
  assert.equal(fs.existsSync(path.join(__dirname, "../pages/module")), false)
  assert.equal(fs.existsSync(path.join(__dirname, "../pages/todo")), false)
  assert.equal(fs.existsSync(path.join(__dirname, "../utils/modules.js")), false)
})

