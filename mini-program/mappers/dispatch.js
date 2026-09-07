function firstValue(...values) {
  return values.find(value => value !== undefined && value !== null && value !== "") || "-"
}

function statusLabel(status) {
  if (status === "DRAFT") return "待提交"
  if (status === "WITHDRAWN") return "已撤回"
  if (status === "OPENED" || status === "ARCHIVED") return "已提交"
  return firstValue(status)
}

function isSubmitted(row) {
  return row.status === "OPENED" || row.status === "ARCHIVED" || row.statusLabel === "已提交"
}

function isDraft(row) {
  return row.status === "DRAFT" || row.statusLabel === "待提交"
}

function mapDispatchRecordToRow(record) {
  const payload = record && record.payload ? record.payload : {}
  const status = firstValue(record.status, payload.status)
  return {
    id: firstValue(record.id, record.recordNo, payload.id),
    date: firstValue(record.businessDate, record.date, payload.recordDate, payload.dispatchDate),
    department: firstValue(record.department, record.departmentName, payload.department, payload.departmentName),
    team: firstValue(record.team, payload.team, payload.teamName),
    name: firstValue(record.owner, payload.owner, payload.ownerName, payload.managerName),
    work: firstValue(payload.workType, payload.jobType, payload.role, payload.position, "班组管理"),
    task: firstValue(payload.task, payload.taskName, payload.content, payload.dispatchTask, record.recordNo),
    status,
    statusLabel: statusLabel(firstValue(record.statusLabel, payload.statusLabel, status))
  }
}

function mapDispatchRecordsToRows(records) {
  return Array.isArray(records) ? records.map(mapDispatchRecordToRow) : []
}

function summarizeDispatchRows(rows) {
  const safeRows = Array.isArray(rows) ? rows : []
  const submitted = safeRows.filter(isSubmitted).length
  const draft = safeRows.filter(isDraft).length
  return {
    total: safeRows.length,
    submitted,
    draft,
    tasks: submitted * 4
  }
}

module.exports = {
  mapDispatchRecordToRow,
  mapDispatchRecordsToRows,
  summarizeDispatchRows
}
