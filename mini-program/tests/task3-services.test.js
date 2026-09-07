const assert = require("node:assert/strict")
const test = require("node:test")

const requestModulePath = require.resolve("../utils/request")
const uploadModulePath = require.resolve("../utils/upload")

function resetServiceModules() {
  for (const modulePath of [
    "../services/auth",
    "../services/user",
    "../services/organization",
    "../services/mini-three-check",
    "../services/me",
    "../services/flow"
  ]) {
    try {
      delete require.cache[require.resolve(modulePath)]
    } catch (error) {
      // Module may not exist yet during RED.
    }
  }
}

function withRequestStub() {
  const calls = []
  require.cache[requestModulePath] = {
    id: requestModulePath,
    filename: requestModulePath,
    loaded: true,
    exports: {
      request(options) {
        calls.push(options)
        return Promise.resolve({ ok: true, options })
      }
    }
  }
  return calls
}

function withUploadStub() {
  const calls = []
  require.cache[uploadModulePath] = {
    id: uploadModulePath,
    filename: uploadModulePath,
    loaded: true,
    exports: {
      uploadFile(options) {
        calls.push(options)
        return Promise.resolve({ uploaded: true, options })
      }
    }
  }
  return calls
}

function loadServices() {
  resetServiceModules()
  return {
    auth: require("../services/auth"),
    user: require("../services/user"),
    organization: require("../services/organization"),
    threeCheck: require("../services/mini-three-check"),
    flow: require("../services/flow"),
    me: require("../services/me")
  }
}

test("auth service calls login and permission endpoints", async () => {
  const calls = withRequestStub()
  withUploadStub()
  const { auth } = loadServices()

  await auth.login({ username: "admin", password: "123456" })
  await auth.getPermissionCodes()

  assert.deepEqual(calls, [
    { url: "/api/auth/login", method: "POST", data: { username: "admin", password: "123456" } },
    { url: "/api/auth/codes", method: "GET" }
  ])
})

test("mine service calls personal overview and isolated record endpoints", async () => {
  const calls = withRequestStub()
  const uploadCalls = withUploadStub()
  const { me } = loadServices()

  await me.getMineOverview()
  await me.listMineRecords("three-check", { page: 2, pageSize: 20 })
  await me.uploadAvatar("wxfile://avatar.png")

  assert.deepEqual(calls, [
    { url: "/api/mini/pingan/me/overview", method: "GET" },
    { url: "/api/mini/pingan/me/records/three-check", method: "GET", data: { page: 2, pageSize: 20 } }
  ])
  assert.deepEqual(uploadCalls, [{
    url: "/api/mini/pingan/me/avatar",
    filePath: "wxfile://avatar.png",
    name: "file",
    fileKind: "IMAGE"
  }])
})

test("user and organization services call current user and org tree endpoints", async () => {
  const calls = withRequestStub()
  withUploadStub()
  const { user, organization } = loadServices()

  await user.getCurrentUser()
  await organization.getOrgTree()
  await organization.getCompanyTree()

  assert.deepEqual(calls, [
    { url: "/api/user/info", method: "GET" },
    { url: "/api/pingan/org/tree", method: "GET" },
    { url: "/api/pingan/org/company-tree", method: "GET" }
  ])
})

test("mini three-check service maps record operations to mini endpoints", async () => {
  const calls = withRequestStub()
  const uploadCalls = withUploadStub()
  const { threeCheck } = loadServices()

  await threeCheck.listRecords("team-dispatch", { status: "DRAFT", page: 1 })
  await threeCheck.getRecord("team-dispatch", 11)
  await threeCheck.createRecord("team-dispatch", { content: "new" })
  await threeCheck.updateRecord("team-dispatch", 11, { content: "edit" })
  await threeCheck.submitRecord("team-dispatch", 11)
  await threeCheck.withdrawRecord("team-dispatch", 11, "填错了")
  await threeCheck.remindRecord("team-dispatch", 11)
  await threeCheck.workflowAction("team-dispatch", 11, { action: "APPROVE" })
  await threeCheck.getRecordWorkflow("pre-shift-meeting", "m1")
  await threeCheck.openRectificationOrder("pre-shift-inspection", 11)
  await threeCheck.uploadAttachment("team-dispatch", 11, "IMAGE", "/tmp/a.png")

  assert.deepEqual(calls, [
    { url: "/api/mini/pingan/three-checks/team-dispatch/records", method: "GET", data: { status: "DRAFT", page: 1 } },
    { url: "/api/mini/pingan/three-checks/team-dispatch/records/11", method: "GET" },
    { url: "/api/mini/pingan/three-checks/team-dispatch/records", method: "POST", data: { content: "new" } },
    { url: "/api/mini/pingan/three-checks/team-dispatch/records/11", method: "PUT", data: { content: "edit" } },
    { url: "/api/mini/pingan/three-checks/team-dispatch/records/11/submit", method: "POST" },
    { url: "/api/mini/pingan/three-checks/team-dispatch/records/11/withdraw", method: "POST", data: { reason: "填错了" } },
    { url: "/api/mini/pingan/three-checks/team-dispatch/records/11/remind", method: "POST" },
    { url: "/api/mini/pingan/three-checks/team-dispatch/records/11/workflow-actions", method: "POST", data: { action: "APPROVE" } },
    { url: "/api/mini/pingan/three-checks/pre-shift-meeting/records/m1/workflow", method: "GET" },
    { url: "/api/mini/pingan/three-checks/pre-shift-inspection/records/11/rectification-order", method: "POST" }
  ])
  assert.deepEqual(uploadCalls, [
    { url: "/api/mini/pingan/three-checks/team-dispatch/records/11/attachments", fileKind: "IMAGE", filePath: "/tmp/a.png" }
  ])
})

test("flow service calls dispatch flow endpoint", async () => {
  const calls = withRequestStub()
  withUploadStub()
  const { flow } = loadServices()

  await flow.getDispatchFlow(33)

  assert.deepEqual(calls, [
    { url: "/api/pingan/three-checks/flows/33", method: "GET" }
  ])
})
