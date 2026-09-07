const assert = require("node:assert/strict")
const test = require("node:test")

const authPath = require.resolve("../utils/auth")
const authFlowPath = require.resolve("../utils/auth-flow")
const meServicePath = require.resolve("../services/me")

function loadAvatarPage(token = "token") {
  const redirects = []
  const sourceTypes = []
  const uploads = []
  const toasts = []

  require.cache[authPath] = {
    id: authPath,
    filename: authPath,
    loaded: true,
    exports: { getToken: () => token }
  }
  require.cache[authFlowPath] = {
    id: authFlowPath,
    filename: authFlowPath,
    loaded: true,
    exports: { LOGIN_PAGE: "/pages/login/index" }
  }
  require.cache[meServicePath] = {
    id: meServicePath,
    filename: meServicePath,
    loaded: true,
    exports: {
      getMineOverview: async () => ({ profile: { avatar: "/uploads/original.png" } }),
      uploadAvatar: async filePath => {
        uploads.push(filePath)
        return { url: "/uploads/avatar/new.png" }
      }
    }
  }
  global.wx = {
    redirectTo(options) { redirects.push(options) },
    navigateBack() {},
    chooseMedia(options) {
      sourceTypes.push(options.sourceType)
      options.success({ tempFiles: [{ tempFilePath: "wxfile://avatar.png" }] })
    },
    showLoading() {},
    hideLoading() {},
    showToast(options) { toasts.push(options) }
  }
  global.Page = config => {
    global.__avatarPage = {
      ...config,
      data: JSON.parse(JSON.stringify(config.data)),
      setData(patch) { this.data = { ...this.data, ...patch } }
    }
  }

  const pagePath = require.resolve("../pages/avatar/index")
  delete require.cache[pagePath]
  require("../pages/avatar/index")

  return {
    page: global.__avatarPage,
    redirects,
    sourceTypes,
    uploads,
    toasts
  }
}

test("avatar page loads the current avatar and uploads album and camera choices", async () => {
  const { page, sourceTypes, uploads } = loadAvatarPage()

  await page.onShow()
  assert.match(page.data.avatarUrl, /\/uploads\/original\.png$/)

  await page.chooseFromAlbum()
  await page.takePhoto()

  assert.deepEqual(sourceTypes, [["album"], ["camera"]])
  assert.deepEqual(uploads, ["wxfile://avatar.png", "wxfile://avatar.png"])
  assert.match(page.data.avatarUrl, /\/uploads\/avatar\/new\.png$/)
})

test("avatar page protects login and gives feedback for future avatar features", () => {
  const { page, redirects, toasts } = loadAvatarPage("")

  page.onLoad()
  page.openAvatarDecoration()
  page.openAiAvatar()

  assert.deepEqual(redirects, [{ url: "/pages/login/index" }])
  assert.deepEqual(toasts.map(item => item.title), ["头像装扮功能即将开放", "AI头像功能即将开放"])
})
