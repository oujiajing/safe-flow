const EXAM_RECORDS = [
  {
    id: "KSPC00000072-190",
    code: "KSPC00000072-190",
    company: "Demo Materials Company",
    department: "火法厂",
    person: "霍永清",
    exam: "2026年防灾减灾知识考试",
    examDate: "2026-05-14",
    status: "待考试",
    statusKey: "pending",
    createdAt: "2026-05-14 15:35",
    remark: "—"
  },
  {
    id: "KSPC00000072-191",
    code: "KSPC00000072-191",
    company: "Demo Materials Company",
    department: "火法厂",
    person: "张神红",
    exam: "2026年防灾减灾知识考试",
    examDate: "2026-05-14",
    status: "待考试",
    statusKey: "pending",
    createdAt: "2026-05-14 15:36",
    remark: "—"
  },
  {
    id: "KSPC00000072-192",
    code: "KSPC00000072-192",
    company: "Demo Materials Company",
    department: "火法厂",
    person: "李奕伦",
    exam: "2026年防灾减灾知识考试",
    examDate: "2026-05-14",
    status: "待考试",
    statusKey: "pending",
    createdAt: "2026-05-14 15:37",
    remark: "—"
  },
  {
    id: "KSPC00000072-193",
    code: "KSPC00000072-193",
    company: "Demo Materials Company",
    department: "火法厂",
    person: "张方严",
    exam: "2026年防灾减灾知识考试",
    examDate: "2026-05-14",
    status: "待考试",
    statusKey: "pending",
    createdAt: "2026-05-14 15:38",
    remark: "—"
  },
  {
    id: "KSPC00000072-194",
    code: "KSPC00000072-194",
    company: "Demo Materials Company",
    department: "火法厂",
    person: "黄慧勤",
    exam: "2026年防灾减灾知识考试",
    examDate: "2026-05-14",
    status: "待考试",
    statusKey: "pending",
    createdAt: "2026-05-14 15:39",
    remark: "—"
  }
]

const EXAM_QUESTIONS = [
  {
    id: "q1",
    no: 1,
    title: "单选题",
    type: "单选题",
    question: "2026年5月12日是我国第18个全国防灾减灾日，主题是（ ）",
    options: [
      "A. 人人讲安全、个个会应急",
      "B. 防范灾害风险、护航高质量发展",
      "C. 减轻灾害风险、守护美好家园",
      "D. 提升基层应急能力、筑牢防灾减灾救灾人民防线"
    ],
    expanded: true
  },
  { id: "q2", no: 2, title: "单选题", type: "单选题", question: "发现安全隐患后应首先（ ）", options: ["A. 立即上报并采取临时防护", "B. 等待下班后处理", "C. 自行忽略", "D. 继续作业"], expanded: false },
  { id: "q3", no: 3, title: "单选题", type: "单选题", question: "班前安全活动应由谁组织开展（ ）", options: ["A. 班组长", "B. 外来人员", "C. 临时访客", "D. 无需组织"], expanded: false },
  { id: "q4", no: 4, title: "单选题", type: "单选题", question: "动火作业前应确认（ ）", options: ["A. 作业票和现场防护", "B. 只确认工具", "C. 只确认人员", "D. 不用确认"], expanded: false },
  { id: "q5", no: 5, title: "单选题", type: "单选题", question: "设备运行异常时应（ ）", options: ["A. 停机报告", "B. 强行运行", "C. 隐瞒情况", "D. 离开现场"], expanded: false },
  { id: "q6", no: 6, title: "单选题", type: "单选题", question: "高处作业应正确佩戴（ ）", options: ["A. 安全带", "B. 普通帽子", "C. 装饰手环", "D. 非防护鞋"], expanded: false },
  { id: "q7", no: 7, title: "单选题", type: "单选题", question: "应急通道应保持（ ）", options: ["A. 畅通", "B. 堆放物料", "C. 临时封堵", "D. 无需管理"], expanded: false }
]

function cloneList(items) {
  return items.map(item => ({ ...item }))
}

function getExamRecords() {
  return cloneList(EXAM_RECORDS)
}

function getExamRecord(id) {
  return EXAM_RECORDS.find(item => item.id === id) || EXAM_RECORDS[0]
}

function pickFirst(record, keys, fallback = "") {
  for (const key of keys) {
    if (record && record[key] !== undefined && record[key] !== null && record[key] !== "") {
      return record[key]
    }
  }
  return fallback
}

function normalizeStatus(record) {
  const rawStatus = String(pickFirst(record, ["status"], "")).toUpperCase()
  const label = pickFirst(record, ["statusLabel"], rawStatus === "EXAMED" ? "已考试" : "待考试")
  if (rawStatus === "ACTIVE") {
    return { label, key: "pending" }
  }
  if (rawStatus === "INACTIVE") {
    return { label, key: "done" }
  }
  return {
    label,
    key: rawStatus === "EXAMED" || rawStatus === "PENDING_REVIEW" || label.indexOf("已") >= 0
      ? "done"
      : "pending"
  }
}

function examPersons(record = {}) {
  if (record.examPersonName || record.person) {
    return pickFirst(record, ["examPersonName", "person"])
  }
  const names = (record.results || [])
    .map(item => pickFirst(item, ["examPersonName", "person"]))
    .filter(Boolean)
  return names.length ? names.join("、") : "考试任务"
}

function normalizeExamRecord(record = {}) {
  const status = normalizeStatus(record)
  const hasProgress = status.key === "pending" && Number(record.answeredCount || 0) > 0
  return {
    ...record,
    id: pickFirst(record, ["taskId", "id", "code"]),
    code: pickFirst(record, ["code", "id"]),
    company: pickFirst(record, ["company"], "—"),
    department: pickFirst(record, ["department"], "—"),
    person: examPersons(record),
    exam: pickFirst(record, ["exam"], "—"),
    examDate: pickFirst(record, ["examDate"], "—"),
    status: hasProgress ? "继续考试" : status.label,
    statusLabel: hasProgress ? "继续考试" : status.label,
    statusKey: status.key,
    hasProgress,
    createdAt: formatDateTime(pickFirst(record, ["createdAt"], "—")),
    remark: pickFirst(record, ["remark"], "—")
  }
}

function formatDateTime(value) {
  if (!value || value === "—") {
    return "—"
  }
  return String(value).replace("T", " ").replace(/\.\d+$/, "").slice(0, 16)
}

function splitOptions(options) {
  if (Array.isArray(options)) {
    return options
      .filter(Boolean)
      .map((item, index) => {
        if (typeof item === "object") {
          const key = String(item.key || String.fromCharCode(65 + index)).toUpperCase()
          return `${key}. ${item.content || ""}`.trim()
        }
        return item
      })
  }
  const text = String(options || "").trim()
  if (!text) {
    return []
  }
  if (text.includes("\n")) {
    return text.split(/\r?\n/).map(item => item.trim()).filter(Boolean)
  }
  const marked = text.match(/[A-Z][.、．]\s*[^A-Z]+(?=\s*[A-Z][.、．]|$)/g)
  if (marked && marked.length > 1) {
    return marked.map(item => item.trim())
  }
  return text.split(/[;；|]/).map(item => item.trim()).filter(Boolean)
}

function normalizeExamQuestions(questions = [], expandedId) {
  const rows = Array.isArray(questions) ? questions : []
  const firstId = rows[0] ? String(pickFirst(rows[0], ["id"], "q1")) : ""
  const activeId = expandedId === undefined ? firstId : String(expandedId || "")
  return rows.map((item, index) => {
    const id = String(pickFirst(item, ["questionKey", "id"], `q${index + 1}`))
    const questionType = pickFirst(item, ["questionType"], "SINGLE_CHOICE")
    const type = pickFirst(item, ["questionTypeLabel", "type", "title"], questionType)
    return {
      ...item,
      id,
      questionId: pickFirst(item, ["id"], id),
      questionKey: id,
      no: index + 1,
      title: type,
      type,
      questionType,
      question: pickFirst(item, ["questionText", "question"], "—"),
      options: splitOptions(pickFirst(item, ["options", "allOptions"], [])),
      caseTitle: pickFirst(item, ["caseTitle"], ""),
      caseMaterial: pickFirst(item, ["caseMaterial"], ""),
      expanded: id === activeId
    }
  })
}

function getExamQuestions(expandedId = "q1") {
  return EXAM_QUESTIONS.map(item => ({
    ...item,
    options: [...item.options],
    expanded: item.id === expandedId
  }))
}

function getExamInfoRows(record) {
  return [
    { label: "编码", value: record.code, required: true, icon: "file-paste" },
    { label: "公司", value: record.company, required: true, icon: "building" },
    { label: "部门", value: record.department, required: true, icon: "usergroup" },
    { label: "考试人员", value: record.person, required: true, icon: "user" },
    { label: "考试", value: record.exam, required: true, icon: "book" },
    { label: "考试日期", value: record.examDate, required: true, icon: "calendar" },
    { label: "状态", value: record.status, required: true, icon: "tag", type: "status" },
    { label: "创建时间", value: record.createdAt, required: true, icon: "time" },
    { label: "备注", value: record.remark, required: false, icon: "edit" }
  ]
}

module.exports = {
  getExamRecords,
  getExamRecord,
  getExamQuestions,
  getExamInfoRows,
  normalizeExamRecord,
  normalizeExamQuestions
}

