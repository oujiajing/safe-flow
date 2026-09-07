const assert = require("node:assert/strict")
const fs = require("node:fs")
const path = require("node:path")
const test = require("node:test")

function createWxMock() {
  const storage = new Map()
  const calls = {
    navigateTo: [],
    showToast: [],
    showModal: [],
    request: [],
    uploadFile: []
  }

  return {
    storage,
    calls,
    getStorageSync(key) {
      return storage.get(key)
    },
    setStorageSync(key, value) {
      storage.set(key, value)
    },
    removeStorageSync(key) {
      storage.delete(key)
    },
    navigateTo(options) {
      calls.navigateTo.push(options)
    },
    showToast(options) {
      calls.showToast.push(options)
    },
    showModal(options) {
      calls.showModal.push(options)
    },
    request(options) {
      calls.request.push(options)
    },
    uploadFile(options) {
      calls.uploadFile.push(options)
    }
  }
}

function reload(modulePath) {
  const resolved = require.resolve(modulePath)
  delete require.cache[resolved]
  return require(modulePath)
}

test("root project config disables url domain checks for local backend development", () => {
  const rootConfig = JSON.parse(fs.readFileSync(path.join(__dirname, "../../project.config.json"), "utf8"))

  assert.equal(rootConfig.miniprogramRoot, "mini-program/")
  assert.equal(rootConfig.setting.urlCheck, false)
  assert.equal(rootConfig.appid, "YOUR_WECHAT_APPID_HERE")
  assert.equal(fs.existsSync(path.join(__dirname, "../project.config.json")), false)
})

test("mini-program debug package keeps local UI assets while ignoring development files", () => {
  const rootConfig = JSON.parse(fs.readFileSync(path.join(__dirname, "../../project.config.json"), "utf8"))
  const rootIgnored = rootConfig.packOptions.ignore.map(item => item.value)

  assert.equal(rootIgnored.includes("mini-program/assets"), false)
  assert.ok(rootIgnored.includes("mini-program/node_modules"))
  assert.ok(rootIgnored.includes("mini-program/tests"))
  assert.equal(rootIgnored.includes("assets"), false)
  assert.ok(rootIgnored.includes("node_modules"))
  assert.ok(rootIgnored.includes("tests"))
})

test("portfolio candidate excludes internal static home assets", () => {
  const rootConfig = JSON.parse(fs.readFileSync(path.join(__dirname, "../../project.config.json"), "utf8"))
  const rootIgnored = rootConfig.packOptions.ignore.map(item => item.value)
  const staticHomeDir = path.join(__dirname, "../static/home")

  assert.equal(rootIgnored.includes("mini-program/static"), false)
  assert.equal(rootIgnored.includes("mini-program/static/home"), false)
  assert.equal(fs.existsSync(staticHomeDir), false)
})

test("auth stores token, permission codes, user info, and clears auth state", () => {
  global.wx = createWxMock()
  const auth = reload("../utils/auth")

  auth.setToken("abc-token")
  auth.setPermissionCodes(["PINGAN_THREE_CHECK_VIEW", "PINGAN_THREE_CHECK_EXECUTE"])
  auth.setCurrentUser({ id: 1, name: "管理员" })

  assert.equal(auth.getToken(), "abc-token")
  assert.deepEqual(auth.getPermissionCodes(), ["PINGAN_THREE_CHECK_VIEW", "PINGAN_THREE_CHECK_EXECUTE"])
  assert.equal(auth.hasPermission("PINGAN_THREE_CHECK_VIEW"), true)
  assert.equal(auth.hasPermission("MISSING_CODE"), false)
  assert.deepEqual(auth.getCurrentUser(), { id: 1, name: "管理员" })

  auth.clearAuth()

  assert.equal(auth.getToken(), "")
  assert.deepEqual(auth.getPermissionCodes(), [])
  assert.equal(auth.hasPermission("PINGAN_THREE_CHECK_VIEW"), false)
  assert.equal(auth.getCurrentUser(), null)
})

test("idempotency helpers create module scoped source ids and unique request ids", () => {
  const idempotency = reload("../utils/idempotency")

  const sourceRecordId = idempotency.createSourceRecordId("pre-shift-meeting")
  const firstRequestId = idempotency.createClientRequestId()
  const secondRequestId = idempotency.createClientRequestId()
  const clientUpdatedAt = idempotency.createClientUpdatedAt()

  assert.match(sourceRecordId, /^wx-local-pre-shift-meeting-\d+-[a-z0-9]+$/)
  assert.match(firstRequestId, /^wx-req-\d+-[a-z0-9]+$/)
  assert.match(secondRequestId, /^wx-req-\d+-[a-z0-9]+$/)
  assert.notEqual(firstRequestId, secondRequestId)
  assert.match(clientUpdatedAt, /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}/)
})

test("request adds bearer token and unwraps successful ApiResponse data", async () => {
  global.wx = createWxMock()
  const auth = reload("../utils/auth")
  auth.setToken("abc-token")
  const { request } = reload("../utils/request")

  const promise = request({ url: "/api/demo", method: "POST", data: { ok: true } })
  const requestOptions = global.wx.calls.request[0]

  assert.equal(requestOptions.url, "http://localhost:8080/api/demo")
  assert.equal(requestOptions.method, "POST")
  assert.equal(requestOptions.header.Authorization, "Bearer abc-token")

  requestOptions.success({ statusCode: 200, data: { code: 0, message: "ok", data: { id: 7 } } })

  await assert.deepEqual(await promise, { id: 7 })
})

test("request clears auth and navigates to login on HTTP 401", async () => {
  global.wx = createWxMock()
  const auth = reload("../utils/auth")
  auth.setToken("abc-token")
  const { request } = reload("../utils/request")

  const promise = request({ url: "/api/private" })
  global.wx.calls.request[0].success({ statusCode: 401, data: { message: "unauthorized" } })

  await assert.rejects(promise, /登录已失效/)
  assert.equal(auth.getToken(), "")
  assert.deepEqual(global.wx.calls.navigateTo, [{ url: "/pages/login/index" }])
})

test("request shows permission toast on HTTP 403", async () => {
  global.wx = createWxMock()
  const { request } = reload("../utils/request")

  const promise = request({ url: "/api/forbidden" })
  global.wx.calls.request[0].success({ statusCode: 403, data: { message: "forbidden" } })

  await assert.rejects(promise, /无权限操作/)
  assert.deepEqual(global.wx.calls.showToast, [{ title: "无权限操作", icon: "none" }])
})

test("request shows conflict modal on HTTP 409", async () => {
  global.wx = createWxMock()
  const { request } = reload("../utils/request")

  const promise = request({ url: "/api/conflict" })
  global.wx.calls.request[0].success({ statusCode: 409, data: { message: "version conflict" } })

  await assert.rejects(promise, /记录已被其他端更新，请刷新后重试/)
  assert.equal(global.wx.calls.showModal[0].title, "记录已更新")
  assert.equal(global.wx.calls.showModal[0].content, "记录已被其他端更新，请刷新后重试")
})

test("request explains mini-program domain whitelist failures in Chinese", async () => {
  global.wx = createWxMock()
  const { request } = reload("../utils/request")

  const promise = request({ url: "/api/pingan/org/tree" })
  global.wx.calls.request[0].fail({ errMsg: "request:fail url not in domain list" })

  await assert.rejects(
    promise,
    /本地后端域名被拦截，请在微信开发者工具关闭合法域名校验后重试/
  )
})

test("uploadFile adds bearer token and unwraps JSON ApiResponse data", async () => {
  global.wx = createWxMock()
  const auth = reload("../utils/auth")
  auth.setToken("abc-token")
  const { uploadFile } = reload("../utils/upload")

  const promise = uploadFile({ url: "/api/files", filePath: "/tmp/a.png", fileKind: "IMAGE" })
  const uploadOptions = global.wx.calls.uploadFile[0]

  assert.equal(uploadOptions.url, "http://localhost:8080/api/files")
  assert.equal(uploadOptions.header.Authorization, "Bearer abc-token")
  assert.deepEqual(uploadOptions.formData, { fileKind: "IMAGE" })

  uploadOptions.success({ statusCode: 200, data: JSON.stringify({ code: 0, message: "ok", data: { id: 3 } }) })

  await assert.deepEqual(await promise, { id: 3 })
})

test("uploadFile clears auth and navigates to login on HTTP 401 without retrying", async () => {
  global.wx = createWxMock()
  const auth = reload("../utils/auth")
  auth.setToken("expired-token")
  const { uploadFile } = reload("../utils/upload")

  const promise = uploadFile({ url: "/api/files", filePath: "/tmp/a.png", fileKind: "IMAGE" })
  global.wx.calls.uploadFile[0].success({ statusCode: 401, data: JSON.stringify({ code: -1, message: "unauthorized" }) })

  await assert.rejects(promise, /登录已失效/)
  assert.equal(auth.getToken(), "")
  assert.deepEqual(global.wx.calls.navigateTo, [{ url: "/pages/login/index" }])
  assert.equal(global.wx.calls.uploadFile.length, 1)
})

test("uploadFile shows permission toast on HTTP 403 without retrying", async () => {
  global.wx = createWxMock()
  const { uploadFile } = reload("../utils/upload")

  const promise = uploadFile({ url: "/api/files", filePath: "/tmp/a.png", fileKind: "IMAGE" })
  global.wx.calls.uploadFile[0].success({ statusCode: 403, data: JSON.stringify({ code: -1, message: "forbidden" }) })

  await assert.rejects(promise, /无权限操作/)
  assert.deepEqual(global.wx.calls.showToast, [{ title: "无权限操作", icon: "none" }])
  assert.equal(global.wx.calls.uploadFile.length, 1)
})

test("uploadFile shows refresh guidance on HTTP 409 without retrying", async () => {
  global.wx = createWxMock()
  const { uploadFile } = reload("../utils/upload")

  const promise = uploadFile({ url: "/api/files", filePath: "/tmp/a.png", fileKind: "IMAGE" })
  global.wx.calls.uploadFile[0].success({ statusCode: 409, data: JSON.stringify({ code: -1, message: "conflict" }) })

  await assert.rejects(promise, /记录已被其他端更新，请刷新后重试/)
  assert.equal(global.wx.calls.showModal[0].title, "记录已更新")
  assert.equal(global.wx.calls.uploadFile.length, 1)
})

test("uploadFile treats business version conflicts like normal requests", async () => {
  global.wx = createWxMock()
  const { uploadFile } = reload("../utils/upload")

  const promise = uploadFile({ url: "/api/files", filePath: "/tmp/a.png", fileKind: "IMAGE" })
  global.wx.calls.uploadFile[0].success({
    statusCode: 200,
    data: JSON.stringify({ code: -1, message: "数据版本冲突，请刷新" })
  })

  await assert.rejects(promise, /记录已被其他端更新，请刷新后重试/)
  assert.equal(global.wx.calls.showModal.length, 1)
  assert.equal(global.wx.calls.uploadFile.length, 1)
})

test("uploadFile preserves ordinary business errors", async () => {
  global.wx = createWxMock()
  const { uploadFile } = reload("../utils/upload")

  const promise = uploadFile({ url: "/api/files", filePath: "/tmp/a.png", fileKind: "IMAGE" })
  global.wx.calls.uploadFile[0].success({
    statusCode: 200,
    data: JSON.stringify({ code: -1, message: "图片大小不能超过 10MB" })
  })

  await assert.rejects(promise, /图片大小不能超过 10MB/)
  assert.equal(global.wx.calls.uploadFile.length, 1)
})

