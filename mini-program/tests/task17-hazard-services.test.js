const assert = require("node:assert/strict")
const test = require("node:test")

const requestModulePath = require.resolve("../utils/request")
const uploadModulePath = require.resolve("../utils/upload")

function resetServiceModules() {
  for (const modulePath of [
    "../services/hazard-source-record",
    "../services/hazard-rectification-order",
    "../services/mini-three-check"
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
    sourceRecords: require("../services/hazard-source-record"),
    orders: require("../services/hazard-rectification-order")
  }
}

test("hazard source record service delegates supported module operations to mini three-check endpoints", async () => {
  const calls = withRequestStub()
  const uploadCalls = withUploadStub()
  const { sourceRecords } = loadServices()

  await sourceRecords.listHazardRecords("quick-shot", { status: "DRAFT", page: 1 })
  await sourceRecords.getHazardRecord("quick-shot", 11)
  await sourceRecords.createHazardRecord("quick-shot", { payload: { location: "通道" } })
  await sourceRecords.updateHazardRecord("quick-shot", 11, { payload: { location: "车间" } })
  await sourceRecords.submitHazardRecord("safety-check", 22)
  await sourceRecords.withdrawHazardRecord("safety-check", 22, "内容错误")
  await sourceRecords.remindHazardRecord("safety-check", 22)
  await sourceRecords.workflowHazardRecord("quick-shot", 11, { action: "APPROVE" })
  await sourceRecords.openSourceRectificationOrder("safety-check", 22)
  await sourceRecords.uploadHazardAttachment("quick-shot", 11, "IMAGE", "/tmp/hazard.jpg")

  assert.deepEqual(calls, [
    { url: "/api/mini/pingan/three-checks/quick-shot/records", method: "GET", data: { status: "DRAFT", page: 1 } },
    { url: "/api/mini/pingan/three-checks/quick-shot/records/11", method: "GET" },
    { url: "/api/mini/pingan/three-checks/quick-shot/records", method: "POST", data: { payload: { location: "通道" } } },
    { url: "/api/mini/pingan/three-checks/quick-shot/records/11", method: "PUT", data: { payload: { location: "车间" } } },
    { url: "/api/mini/pingan/three-checks/safety-check/records/22/submit", method: "POST" },
    { url: "/api/mini/pingan/three-checks/safety-check/records/22/withdraw", method: "POST", data: { reason: "内容错误" } },
    { url: "/api/mini/pingan/three-checks/safety-check/records/22/remind", method: "POST" },
    { url: "/api/mini/pingan/three-checks/quick-shot/records/11/workflow-actions", method: "POST", data: { action: "APPROVE" } },
    { url: "/api/mini/pingan/three-checks/safety-check/records/22/rectification-order", method: "POST" }
  ])
  assert.deepEqual(uploadCalls, [
    { url: "/api/mini/pingan/three-checks/quick-shot/records/11/attachments", fileKind: "IMAGE", filePath: "/tmp/hazard.jpg" }
  ])
})

test("hazard source record service rejects unsupported module keys before calling backend", async () => {
  const calls = withRequestStub()
  const { sourceRecords } = loadServices()

  assert.throws(
    () => sourceRecords.listHazardRecords("pre-shift-meeting", {}),
    /Unsupported hazard source moduleKey: pre-shift-meeting/
  )
  assert.deepEqual(calls, [])
})

test("hazard rectification order service maps list create action detail and upload endpoints", async () => {
  const calls = withRequestStub()
  const uploadCalls = withUploadStub()
  const { orders } = loadServices()

  await orders.listOrders({ status: "PENDING_RECTIFY", page: 1 })
  await orders.createOrder({ businessDate: "2026-07-01", items: [] })
  await orders.getOrder(31)
  await orders.actionOrder(31, { action: "MARK_RECTIFIED", version: 2, payload: {} })
  await orders.uploadOrderAttachment(31, "RECTIFICATION_AFTER_PHOTO", "/tmp/after.jpg")

  assert.deepEqual(calls, [
    { url: "/api/pingan/hazard-rectification/orders", method: "GET", data: { status: "PENDING_RECTIFY", page: 1 } },
    { url: "/api/pingan/hazard-rectification/orders", method: "POST", data: { businessDate: "2026-07-01", items: [] } },
    { url: "/api/pingan/hazard-rectification/orders/31", method: "GET" },
    { url: "/api/pingan/hazard-rectification/orders/31/actions", method: "POST", data: { action: "MARK_RECTIFIED", version: 2, payload: {} } }
  ])
  assert.deepEqual(uploadCalls, [
    { url: "/api/pingan/hazard-rectification/orders/31/attachments", fileKind: "RECTIFICATION_AFTER_PHOTO", filePath: "/tmp/after.jpg" }
  ])
})
