const assert = require("node:assert/strict")
const test = require("node:test")

function reload(modulePath) {
  const resolved = require.resolve(modulePath)
  delete require.cache[resolved]
  return require(modulePath)
}

test("module config maps first-batch prototype ids to backend module keys", () => {
  const modules = reload("../config/modules")

  assert.deepEqual(modules.FIRST_BATCH_MODULE_IDS, [
    "dispatch",
    "meeting",
    "before-check",
    "during-check",
    "after-check"
  ])

  assert.equal(modules.getModuleById("dispatch").moduleKey, "team-dispatch")
  assert.equal(modules.getModuleById("meeting").moduleKey, "pre-shift-meeting")
  assert.equal(modules.getModuleById("before-check").moduleKey, "pre-shift-inspection")
  assert.equal(modules.getModuleById("during-check").moduleKey, "mid-shift-inspection")
  assert.equal(modules.getModuleById("after-check").moduleKey, "post-shift-inspection")
})

test("module config promotes key sites, special work, learning, exam, and points modules", () => {
  const modules = reload("../config/modules")

  const keySites = modules.getModuleById("key-place")
  const specialWork = modules.getModuleById("special-work")
  const learning = modules.getModuleById("learning")
  const exam = modules.getModuleById("exam")
  const pointFlow = modules.getModuleById("point-flow")
  const pointRank = modules.getModuleById("point-rank")

  assert.equal(keySites.enabled, true)
  assert.equal(keySites.mockOnly, false)
  assert.equal(keySites.route, "/pages/key-sites/list/index")
  assert.deepEqual(keySites.permissionCodes, ["PINGAN_KEY_SITES_ENTRY", "PINGAN_KEY_SITES_VIEW"])
  assert.equal(specialWork.enabled, true)
  assert.equal(specialWork.mockOnly, false)
  assert.equal(specialWork.route, "/pages/special-work/list/index")
  assert.deepEqual(specialWork.permissionCodes, ["PINGAN_SPECIAL_WORK_ENTRY", "PINGAN_SPECIAL_WORK_VIEW"])
  assert.equal(learning.enabled, true)
  assert.equal(learning.mockOnly, false)
  assert.equal(learning.route, "/pages/training/learning-list/index")
  assert.deepEqual(learning.permissionCodes, ["PINGAN_TRAINING_SAFETY_LEARNING_ENTRY", "PINGAN_TRAINING_SAFETY_LEARNING_VIEW"])
  assert.equal(exam.enabled, true)
  assert.equal(exam.mockOnly, false)
  assert.equal(exam.route, "/pages/training/exam-list/index")
  assert.deepEqual(exam.permissionCodes, ["PINGAN_TRAINING_EXAM_TASKS_ENTRY", "PINGAN_TRAINING_EXAM_TASKS_VIEW"])
  assert.equal(pointFlow.enabled, true)
  assert.equal(pointFlow.route, "/pages/points/flow/index")
  assert.deepEqual(pointFlow.permissionCodes, ["PINGAN_POINTS_ENTRY", "PINGAN_POINTS_VIEW"])
  assert.equal(pointRank.enabled, true)
  assert.equal(pointRank.route, "/pages/points/rank/index")
  assert.deepEqual(pointRank.permissionCodes, ["PINGAN_POINTS_RANKING_ENTRY", "PINGAN_POINTS_RANKING_VIEW"])
})

test("module config exposes only formal route mappings", () => {
  const modules = reload("../config/modules")

  assert.equal(modules.getRouteForModule("dispatch"), "/pages/dispatch/list/index")
  assert.equal(modules.getRouteForModule("meeting"), "/pages/three-check/meeting/index")
  assert.equal(modules.getRouteForModule("before-check"), "/pages/three-check/inspection-list/index?moduleKey=pre-shift-inspection")
  assert.equal(modules.getRouteForModule("special-work"), "/pages/special-work/list/index")
  assert.equal(modules.getRouteForModule("learning"), "/pages/training/learning-list/index")
  assert.equal(modules.getRouteForModule("exam"), "/pages/training/exam-list/index")
  assert.equal(modules.getRouteForModule("point-flow"), "/pages/points/flow/index")
  assert.equal(modules.getRouteForModule("point-rank"), "/pages/points/rank/index")
  assert.equal(modules.getRouteForModule("unknown-module"), null)
})

test("permissions config declares all first-batch permission codes", () => {
  const permissions = reload("../config/permissions")

  assert.equal(permissions.PERMISSION_CODES.THREE_CHECK_ENTRY, "PINGAN_THREE_CHECK_ENTRY")
  assert.equal(permissions.PERMISSION_CODES.THREE_CHECK_VIEW, "PINGAN_THREE_CHECK_VIEW")
  assert.equal(permissions.PERMISSION_CODES.THREE_CHECK_EXECUTE, "PINGAN_THREE_CHECK_EXECUTE")
  assert.equal(permissions.PERMISSION_CODES.TEAM_DISPATCH_MANAGE, "PINGAN_TEAM_DISPATCH_MANAGE")
  assert.equal(permissions.PERMISSION_CODES.THREE_CHECK_WITHDRAW, "PINGAN_THREE_CHECK_WITHDRAW")
  assert.equal(permissions.PERMISSION_CODES.THREE_CHECK_REMIND, "PINGAN_THREE_CHECK_REMIND")
  assert.equal(permissions.PERMISSION_CODES.TRAINING_ENTRY, "PINGAN_TRAINING_ENTRY")
  assert.equal(permissions.PERMISSION_CODES.TRAINING_VIEW, "PINGAN_TRAINING_VIEW")
  assert.equal(permissions.PERMISSION_CODES.TRAINING_STUDY_EXAM, "PINGAN_TRAINING_STUDY_EXAM")
  assert.equal(permissions.PERMISSION_CODES.POINTS_ENTRY, "PINGAN_POINTS_ENTRY")
  assert.equal(permissions.PERMISSION_CODES.POINTS_VIEW, "PINGAN_POINTS_VIEW")
  assert.equal(permissions.PERMISSION_CODES.SPECIAL_WORK_ENTRY, "PINGAN_SPECIAL_WORK_ENTRY")
  assert.equal(permissions.PERMISSION_CODES.SPECIAL_WORK_VIEW, "PINGAN_SPECIAL_WORK_VIEW")
  assert.equal(permissions.PERMISSION_CODES.SPECIAL_WORK_APPLY, "PINGAN_SPECIAL_WORK_APPLY")
  assert.equal(permissions.PERMISSION_CODES.SPECIAL_WORK_APPROVE, "PINGAN_SPECIAL_WORK_APPROVE")
  assert.equal(permissions.PERMISSION_CODES.SPECIAL_WORK_REVIEW, "PINGAN_SPECIAL_WORK_REVIEW")
  assert.equal(permissions.PERMISSION_CODES.ONE_SHIFT_THREE_CHECKS_ENTRY, "PINGAN_ONE_SHIFT_THREE_CHECKS_ENTRY")
  assert.equal(permissions.PERMISSION_CODES.ONE_SHIFT_THREE_CHECKS_VIEW, "PINGAN_ONE_SHIFT_THREE_CHECKS_VIEW")
  assert.equal(permissions.PERMISSION_CODES.PRE_SHIFT_MEETING_ENTRY, "PINGAN_PRE_SHIFT_MEETING_ENTRY")
  assert.equal(permissions.PERMISSION_CODES.PRE_SHIFT_MEETING_VIEW, "PINGAN_PRE_SHIFT_MEETING_VIEW")
  assert.equal(
    permissions.PERMISSION_CODES.ONE_SHIFT_THREE_CHECKS_RECTIFICATION_ORDER_CREATE,
    "PINGAN_ONE_SHIFT_THREE_CHECKS_RECTIFICATION_ORDER_CREATE"
  )
  assert.equal(permissions.PERMISSION_CODES.SAFETY_LEARNING_ENTRY, "PINGAN_TRAINING_SAFETY_LEARNING_ENTRY")
  assert.equal(permissions.PERMISSION_CODES.EXAM_TASKS_ENTRY, "PINGAN_TRAINING_EXAM_TASKS_ENTRY")
  assert.equal(permissions.PERMISSION_CODES.POINTS_RANKING_ENTRY, "PINGAN_POINTS_RANKING_ENTRY")

  assert.deepEqual(permissions.MODULE_PERMISSION_CODES.dispatch, [
    "PINGAN_TEAM_DISPATCH_ENTRY",
    "PINGAN_TEAM_DISPATCH_VIEW"
  ])
  assert.deepEqual(permissions.MODULE_PERMISSION_CODES.learning, [
    "PINGAN_TRAINING_SAFETY_LEARNING_ENTRY",
    "PINGAN_TRAINING_SAFETY_LEARNING_VIEW"
  ])
  assert.deepEqual(permissions.MODULE_PERMISSION_CODES.exam, [
    "PINGAN_TRAINING_EXAM_TASKS_ENTRY",
    "PINGAN_TRAINING_EXAM_TASKS_VIEW"
  ])
  assert.deepEqual(permissions.MODULE_PERMISSION_CODES["point-flow"], [
    "PINGAN_POINTS_ENTRY",
    "PINGAN_POINTS_VIEW"
  ])
  assert.deepEqual(permissions.MODULE_PERMISSION_CODES["point-rank"], [
    "PINGAN_POINTS_RANKING_ENTRY",
    "PINGAN_POINTS_RANKING_VIEW"
  ])
})

test("status config maps backend statuses to prototype-facing labels", () => {
  const status = reload("../config/status")

  assert.equal(status.getStatusLabel("DRAFT"), "待提交")
  assert.equal(status.getStatusLabel("OPENED"), "已提交")
  assert.equal(status.getStatusLabel("WITHDRAWN"), "已撤回")
  assert.equal(status.getStatusLabel("ARCHIVED"), "已完成")
  assert.equal(status.getInspectionStatusLabel("DRAFT"), "待提交")
  assert.equal(status.getMeetingStatusLabel("DRAFT"), "待提交")
  assert.equal(status.getStatusLabel("UNKNOWN"), "UNKNOWN")
})
