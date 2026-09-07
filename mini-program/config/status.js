const STATUS_LABELS = {
  DRAFT: "待提交",
  OPENED: "已提交",
  WITHDRAWN: "已撤回",
  ARCHIVED: "已完成"
}

const MEETING_STATUS_LABELS = {
  DRAFT: "待提交",
  OPENED: "已提交",
  WITHDRAWN: "待提交",
  ARCHIVED: "已提交"
}

const INSPECTION_STATUS_LABELS = {
  DRAFT: "待提交",
  OPENED: "已提交",
  WITHDRAWN: "待提交",
  ARCHIVED: "已提交"
}

function getStatusLabel(status) {
  return STATUS_LABELS[status] || status
}

function getMeetingStatusLabel(status) {
  return MEETING_STATUS_LABELS[status] || getStatusLabel(status)
}

function getInspectionStatusLabel(status) {
  return INSPECTION_STATUS_LABELS[status] || getStatusLabel(status)
}

module.exports = {
  STATUS_LABELS,
  MEETING_STATUS_LABELS,
  INSPECTION_STATUS_LABELS,
  getStatusLabel,
  getMeetingStatusLabel,
  getInspectionStatusLabel
}
