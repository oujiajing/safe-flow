const assert = require("node:assert/strict")
const test = require("node:test")

const requestPath = require.resolve("../utils/request")

function reload(modulePath) {
  const resolved = require.resolve(modulePath)
  delete require.cache[resolved]
  return require(modulePath)
}

test("hazard stats loads pending rectification count safely", async () => {
  const calls = []
  require.cache[requestPath] = {
    id: requestPath,
    filename: requestPath,
    loaded: true,
    exports: {
      request(options) {
        calls.push(options)
        return Promise.resolve({ items: [], total: "12" })
      }
    }
  }

  const { loadPendingRectificationCount } = reload("../utils/hazard-stats")
  const count = await loadPendingRectificationCount()

  assert.equal(count, 12)
  assert.deepEqual(calls, [
    {
      url: "/api/pingan/hazard-rectification/orders",
      method: "GET",
      data: { status: "PENDING_RECTIFY", page: 1, pageSize: 1 }
    }
  ])
})

test("hazard stats loads pending source counts by module status and normalizes bad totals", async () => {
  const calls = []
  require.cache[requestPath] = {
    id: requestPath,
    filename: requestPath,
    loaded: true,
    exports: {
      request(options) {
        calls.push(options)
        return Promise.resolve({ items: [], total: options.data.status === "PENDING_REVIEW" ? 4 : null })
      }
    }
  }

  const { loadPendingSourceCount } = reload("../utils/hazard-stats")

  assert.equal(await loadPendingSourceCount("quick-shot"), 4)
  assert.equal(await loadPendingSourceCount("safety-check"), 0)
  assert.deepEqual(calls, [
    {
      url: "/api/mini/pingan/three-checks/quick-shot/records",
      method: "GET",
      data: { status: "PENDING_REVIEW", page: 1, pageSize: 1 }
    },
    {
      url: "/api/mini/pingan/three-checks/safety-check/records",
      method: "GET",
      data: { status: "DRAFT", page: 1, pageSize: 1 }
    }
  ])
})
