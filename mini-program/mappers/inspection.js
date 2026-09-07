function firstValue(...values) {
  return values.find(value => value !== undefined && value !== null && value !== "") || "-"
}

function rawFirstValue(...values) {
  const value = values.find(item => item !== undefined && item !== null && item !== "")
  return value === undefined ? "" : value
}

function mapInspectionStatus(status, overdue) {
  if (overdue) {
    return { text: "已逾期", className: "pending" }
  }
  if (status === "OPENED" || status === "ARCHIVED") {
    return { text: "已提交", className: "done" }
  }
  if (status === "WITHDRAWN") {
    return { text: "待提交", className: "pending" }
  }
  return { text: "待提交", className: "pending" }
}

function normalizeCheckItems(record, payload) {
  const items = Array.isArray(payload.checkItems) ? payload.checkItems : []
  const isPostShiftInspection = record && record.moduleKey === "post-shift-inspection"

  return items.map(item => ({
    ...item,
    riskType: rawFirstValue(item.riskType),
    checkItem: rawFirstValue(item.checkItem),
    checkResult: rawFirstValue(item.checkResult),
    defaultCheckResult: rawFirstValue(item.defaultCheckResult),
    expanded: true
  }))
}

function normalizeAttachments(record, fileKind) {
  const attachments = Array.isArray(record && record.attachments) ? record.attachments : []
  return attachments.filter(item => item && item.fileKind === fileKind)
}

function mapInspectionRecordToRow(record) {
  const payload = record && record.payload ? record.payload : {}
  const mappedStatus = mapInspectionStatus(record && record.status, record && record.overdue)
  const moduleKey = firstValue(record && record.moduleKey, payload.moduleKey)
  const capabilities = {}
  for (const key of ["canSubmit", "canWithdraw", "canRemind", "canCreateRectificationOrder"]) {
    if (typeof record[key] === "boolean") capabilities[key] = record[key]
  }
  return {
    id: firstValue(record.id, record.recordNo, payload.id),
    code: firstValue(record.recordNo, record.id, payload.recordNo, payload.id),
    company: firstValue(record.companyName, record.company, payload.companyName, payload.company),
    dept: firstValue(record.departmentName, record.department, payload.departmentName, payload.department),
    team: firstValue(record.teamName, record.team, payload.teamName, payload.team),
    owner: firstValue(record.ownerName, record.owner, payload.ownerName, payload.owner),
    task: firstValue(record.content, record.task, payload.task, payload.content, record.recordNo),
    date: firstValue(record.businessDate, record.date, payload.recordDate, payload.date),
    imageCheck: firstValue(record.imageCheck, payload.imageCheck),
    imageAttachments: normalizeAttachments(record, "IMAGE"),
    videoCheck: firstValue(record.videoCheck, payload.videoCheck),
    handoverStatus: firstValue(record.handoverStatus, payload.handoverStatus, payload["交班状态"]),
    status: mappedStatus.text,
    statusClass: mappedStatus.className,
    remark: firstValue(record.remark, record.remarks, payload.remark, payload.remarks),
    checkItems: normalizeCheckItems(record, payload),
    isPostShiftInspection: moduleKey === "post-shift-inspection",
    rawPayload: payload,
    version: Number(record && record.version ? record.version : 0),
    ...capabilities
  }
}

function mapInspectionRecordsToRows(records) {
  return Array.isArray(records)
    ? records.map(record => {
        const row = mapInspectionRecordToRow(record)
        return {
          id: row.id,
          dept: row.dept,
          team: row.team,
          owner: row.owner,
          task: row.task,
          date: row.date,
          status: row.status,
          statusClass: row.statusClass,
          ...(typeof row.canSubmit === "boolean" ? { canSubmit: row.canSubmit } : {}),
          ...(typeof row.canWithdraw === "boolean" ? { canWithdraw: row.canWithdraw } : {}),
          ...(typeof row.canRemind === "boolean" ? { canRemind: row.canRemind } : {}),
          ...(typeof row.canCreateRectificationOrder === "boolean" ? { canCreateRectificationOrder: row.canCreateRectificationOrder } : {})
        }
      })
    : []
}

module.exports = {
  mapInspectionRecordToRow,
  mapInspectionRecordsToRows
}
