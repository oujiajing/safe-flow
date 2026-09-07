const STATUS_FLOW = ["PENDING_APPROVAL", "IN_PROGRESS", "PENDING_ACCEPTANCE", "COMPLETED"]
const WORKFLOW_ACTION_BY_STATUS = {
  PENDING_APPROVAL: {
    action: "APPROVE_AND_START",
    label: "审批",
    permission: "PINGAN_SPECIAL_WORK_APPROVE"
  },
  IN_PROGRESS: {
    action: "SUBMIT_ACCEPTANCE",
    label: "提交验收",
    permission: "PINGAN_SPECIAL_WORK_APPLY"
  },
  PENDING_ACCEPTANCE: {
    action: "COMPLETE_ACCEPTANCE",
    label: "验收",
    permission: "PINGAN_SPECIAL_WORK_REVIEW"
  }
}

function normalizeDateTime(value) {
  if (!value) return ""
  return String(value).replace("T", " ").replace(/\.\d+$/, "").slice(0, 19)
}

function formatDateTime(value) {
  return normalizeDateTime(value) || "-"
}

function dateTimeParts(value) {
  const normalized = normalizeDateTime(value)
  const [date = "", time = ""] = normalized.split(" ")
  return { date, time: time.slice(0, 5) }
}

function mergeDateTimePart(value, part, nextValue) {
  const current = dateTimeParts(value)
  const date = part === "date" ? nextValue : current.date
  const time = part === "time" ? nextValue : current.time
  return date ? `${date} ${time || "00:00"}:00` : ""
}

function nowDateTime() {
  const date = new Date()
  const pad = value => String(value).padStart(2, "0")
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:00`
}

function recordPayload(record = {}) {
  return {
    companyId: record.companyId,
    project: record.project || "",
    workType: record.workType || "",
    applicationTime: normalizeDateTime(record.applicationTime),
    workContent: record.workContent || "",
    workLocation: record.workLocation || "",
    riskIdentificationResult: record.riskIdentificationResult || "",
    implementationStartTime: normalizeDateTime(record.implementationStartTime),
    implementationEndTime: normalizeDateTime(record.implementationEndTime),
    safetyDisclosurePerson: record.safetyDisclosurePerson || "",
    guardian: record.guardian || "",
    disclosureReceiver: record.disclosureReceiver || "",
    completionAcceptor: record.completionAcceptor || "",
    completionAcceptanceTime: normalizeDateTime(record.completionAcceptanceTime),
    status: record.status || "PENDING_APPROVAL"
  }
}

function nextStatus(status) {
  const index = STATUS_FLOW.indexOf(status)
  return index >= 0 && index < STATUS_FLOW.length - 1 ? STATUS_FLOW[index + 1] : ""
}

function workflowAction(status) {
  return WORKFLOW_ACTION_BY_STATUS[status]
}

function statusClass(status) {
  return {
    PENDING_APPROVAL: "pending",
    IN_PROGRESS: "working",
    PENDING_ACCEPTANCE: "accepting",
    COMPLETED: "completed"
  }[status] || "pending"
}

module.exports = {
  STATUS_FLOW,
  WORKFLOW_ACTION_BY_STATUS,
  dateTimeParts,
  formatDateTime,
  mergeDateTimePart,
  nextStatus,
  normalizeDateTime,
  nowDateTime,
  recordPayload,
  statusClass,
  workflowAction
}
