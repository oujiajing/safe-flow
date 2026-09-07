const assert = require("node:assert/strict")
const test = require("node:test")

const servicePath = require.resolve("../services/mini-three-check")

test("three-check stats counts today's pending records and backend-derived overdue records", async () => {
  const calls = []
  const RealDate = global.Date
  class FakeDate extends RealDate {
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
  global.Date = FakeDate
  require.cache[servicePath] = {
    id: servicePath,
    filename: servicePath,
    loaded: true,
    exports: {
      listRecords(moduleKey, query) {
        calls.push({ moduleKey, query })
        if (query.status === "DRAFT" && query.dateStart === "2026-07-03" && query.dateEnd === "2026-07-03") {
          return Promise.resolve({ total: 6 })
        }
        if (query.status === "WITHDRAWN" && query.dateStart === "2026-07-03" && query.dateEnd === "2026-07-03") {
          return Promise.resolve({ total: 1 })
        }
        if (query.overdue === true) {
          return Promise.resolve({ total: 6 })
        }
        if (query.status === "OPENED") {
          return Promise.resolve({ total: 3 })
        }
        if (query.status === "ARCHIVED") {
          return Promise.resolve({ total: 5 })
        }
        return Promise.resolve({ total: 99 })
      }
    }
  }

  const statsPath = require.resolve("../utils/three-check-stats")
  delete require.cache[statsPath]
  const { loadPendingCount, loadThreeCheckTabCounts } = require("../utils/three-check-stats")

  try {
    const counts = await loadThreeCheckTabCounts("pre-shift-meeting")
    const pending = await loadPendingCount("pre-shift-meeting")

    assert.deepEqual(counts, { pending: 7, done: 8, expired: 6 })
    assert.equal(pending, 7)
    assert.deepEqual(calls.slice(0, 5), [
      { moduleKey: "pre-shift-meeting", query: { status: "DRAFT", dateStart: "2026-07-03", dateEnd: "2026-07-03", page: 1, pageSize: 1 } },
      { moduleKey: "pre-shift-meeting", query: { status: "WITHDRAWN", dateStart: "2026-07-03", dateEnd: "2026-07-03", page: 1, pageSize: 1 } },
      { moduleKey: "pre-shift-meeting", query: { status: "OPENED", page: 1, pageSize: 1 } },
      { moduleKey: "pre-shift-meeting", query: { status: "ARCHIVED", page: 1, pageSize: 1 } },
      { moduleKey: "pre-shift-meeting", query: { overdue: true, page: 1, pageSize: 1 } }
    ])
  } finally {
    global.Date = RealDate
  }
})
