function firstValue(...values) {
  return values.find(value => value !== undefined && value !== null && value !== "") || "-"
}

function normalizeAttendees(value) {
  if (Array.isArray(value)) {
    return value.filter(item => item !== undefined && item !== null && item !== "")
  }
  if (typeof value === "string" && value.trim()) {
    return value.split(/[、,，]/).map(item => item.trim()).filter(Boolean)
  }
  return []
}

function normalizeAttachments(record, fileKind) {
  const attachments = Array.isArray(record && record.attachments) ? record.attachments : []
  return attachments.filter(item => item && item.fileKind === fileKind)
}

function normalizeSafetyConfirmItems(payload) {
  const items = Array.isArray(payload.safetyConfirmItems) ? payload.safetyConfirmItems : []
  return items.map(item => ({
    ...item,
    expanded: Boolean(item.expanded)
  }))
}

function mapMeetingStatus(status, overdue) {
  if (overdue) {
    return { text: "已逾期", className: "muted" }
  }
  if (status === "OPENED" || status === "ARCHIVED") {
    return { text: "已提交", className: "" }
  }
  if (status === "WITHDRAWN") {
    return { text: "待提交", className: "muted" }
  }
  return { text: "待提交", className: "muted" }
}

function mapMeetingRecordToRow(record) {
  const payload = record && record.payload ? record.payload : {}
  const mappedStatus = mapMeetingStatus(record && record.status, record && record.overdue)
  const attendees = normalizeAttendees(payload.attendees || record.attendees || payload.attendeesText || record.attendeesText || "")
  const imageAttachments = normalizeAttachments(record, "IMAGE")
  const videoAttachments = normalizeAttachments(record, "VIDEO")
  const capabilities = {}
  for (const key of ["canSubmit", "canWithdraw", "canRemind"]) {
    if (typeof record[key] === "boolean") capabilities[key] = record[key]
  }
  return {
    id: firstValue(record.id, record.meetingNo, record.recordNo, payload.id),
    company: firstValue(record.companyName, record.company, payload.companyName, payload.company),
    dept: firstValue(record.departmentName, record.department, payload.departmentName, payload.department),
    team: firstValue(record.teamName, record.team, payload.teamName, payload.team),
    owner: firstValue(record.ownerName, record.owner, payload.ownerName, payload.owner),
    attendees,
    attendeesText: attendees.join("、"),
    task: firstValue(record.meetingContent, record.content, payload.task, payload.meetingContent, payload.content, record.meetingNo, record.recordNo),
    date: firstValue(record.meetingDate, record.businessDate, record.date, payload.meetingDate, payload.recordDate),
    status: mappedStatus.text,
    statusClass: mappedStatus.className,
    imageCheck: firstValue(record.imageCheck, record.imageCheckStatus, payload.imageCheck),
    videoCheck: firstValue(record.videoCheck, record.videoCheckStatus, payload.videoCheck),
    imageAttachments,
    videoAttachments,
    safetyConfirmItems: normalizeSafetyConfirmItems(payload),
    ...capabilities
  }
}

function mapMeetingRecordsToRows(records) {
  return Array.isArray(records) ? records.map(mapMeetingRecordToRow) : []
}

module.exports = {
  mapMeetingRecordToRow,
  mapMeetingRecordsToRows
}
