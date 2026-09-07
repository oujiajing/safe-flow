const assert = require("node:assert/strict")
const test = require("node:test")

const authPath = require.resolve("../utils/auth")
const authFlowPath = require.resolve("../utils/auth-flow")
const meServicePath = require.resolve("../services/me")
const tabBarPath = require.resolve("../utils/tab-bar")

function loadMine({ token = "", currentUser = null, overview = {} } = {}) {
  const navigateCalls = []
  const chooseMediaCalls = []
  const downloadCalls = []
  const savedFiles = []
  const uploadCalls = []
  let logoutCalled = false
  require.cache[authPath] = {
    id: authPath, filename: authPath, loaded: true,
    exports: { getToken: () => token, getCurrentUser: () => currentUser }
  }
  require.cache[authFlowPath] = {
    id: authFlowPath, filename: authFlowPath, loaded: true,
    exports: {
      LOGIN_PAGE: "/pages/login/index",
      logout() { logoutCalled = true }
    }
  }
  require.cache[meServicePath] = {
    id: meServicePath, filename: meServicePath, loaded: true,
    exports: {
      getMineOverview: async () => overview,
      uploadAvatar: async filePath => {
        uploadCalls.push(filePath)
        return { url: "/uploads/mini/mine/avatar/1/image/avatar.png" }
      }
    }
  }
  require.cache[tabBarPath] = {
    id: tabBarPath, filename: tabBarPath, loaded: true,
    exports: { syncTabBarSelected() { return true } }
  }
  global.wx = {
    navigateTo(options) { navigateCalls.push(options) },
    showModal(options) { options.success({ confirm: true }) },
    chooseMedia(options) {
      chooseMediaCalls.push(options)
      options.success({ tempFiles: [{ tempFilePath: "wxfile://selected-avatar.png" }] })
    },
    downloadFile(options) {
      downloadCalls.push(options.url)
      options.success({ statusCode: 200, tempFilePath: "wxfile://downloaded-avatar.png" })
    },
    saveImageToPhotosAlbum(options) {
      savedFiles.push(options.filePath)
      options.success({})
    },
    showLoading() {},
    hideLoading() {},
    showToast() {}
  }
  global.Page = config => {
    global.__minePage = {
      ...config,
      data: JSON.parse(JSON.stringify(config.data)),
      setData(patch) { this.data = { ...this.data, ...patch } }
    }
  }
  const pagePath = require.resolve("../pages/mine/mine")
  delete require.cache[pagePath]
  require("../pages/mine/mine")
  return {
    page: global.__minePage,
    navigateCalls,
    chooseMediaCalls,
    downloadCalls,
    savedFiles,
    uploadCalls,
    logoutCalled: () => logoutCalled
  }
}

test("mine page provides a concise login state", async () => {
  const { page, navigateCalls } = loadMine()
  await page.onShow()

  assert.equal(page.data.loggedIn, false)
  assert.equal(page.data.organizationText, "登录后查看个人安全档案")
  page.openProfile()
  assert.deepEqual(navigateCalls, [{ url: "/pages/login/index" }])
})

test("mine page loads the personal safety overview and seven record entries", async () => {
  const { page, navigateCalls } = loadMine({
    token: "token",
    currentUser: { username: "worker" },
    overview: {
      profile: {
        realName: "张安全",
        positionName: "班组长",
        companyName: "示范公司",
        departmentName: "生产部",
        teamName: "一班"
      },
      safetySummary: { points: 128, completedLearningCount: 6, passedExamCount: 3 },
      capabilities: {}
    }
  })

  await page.onShow()
  assert.equal(page.data.profile.realName, "张安全")
  assert.equal(page.data.organizationText, "示范公司 · 生产部 · 一班")
  assert.deepEqual(page.data.summary, { points: 128, completedLearningCount: 6, passedExamCount: 3 })
  assert.equal(page.data.workEntries.length + page.data.growthEntries.length, 7)

  page.openRecordEntry({ currentTarget: { dataset: { type: "dispatch", label: "派班记录" } } })
  assert.deepEqual(navigateCalls, [{ url: "/pages/mine-records/index?type=dispatch&title=%E6%B4%BE%E7%8F%AD%E8%AE%B0%E5%BD%95" }])
})
