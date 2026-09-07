const { getOrgTree } = require("../../../services/organization")
const { organizationOptionsFor } = require("../../../utils/organization-options")
const examService = require("../../../services/safety-exam")

const DRAFT_KEY = "safetyExamCreateDraft"
const ALL_DEPARTMENTS = { label: "全部部门（公司范围）", value: "" }
const ALL_TEAMS = { label: "全部班组（部门范围）", value: "" }

function getWx() {
  return wx
}

function todayText() {
  const date = new Date()
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, "0")
  const day = String(date.getDate()).padStart(2, "0")
  return `${year}-${month}-${day}`
}

function optionLabel(option, fallback = "点击选择") {
  return option && option.label ? option.label : fallback
}

function sameId(left, right) {
  return String(left || "") === String(right || "")
}

function selectedIndex(options, value) {
  return Math.max(0, options.findIndex(option => sameId(option.value, value)))
}

function trimText(value) {
  return String(value || "").trim()
}

function scopedOptions(options, allOption) {
  return [allOption, ...(Array.isArray(options) ? options : [])]
}

function paperAppliesToScope(paper, scope) {
  return (
    (!paper.companyId || sameId(paper.companyId, scope.companyId)) &&
    (!paper.departmentId || sameId(paper.departmentId, scope.departmentId)) &&
    (!paper.teamId || sameId(paper.teamId, scope.teamId))
  )
}

function questionPayload(question, index) {
  return {
    questionType: question.questionType,
    questionText: question.questionText,
    selectedOption: question.selectedOption || "",
    allOptions: question.allOptions || "",
    answer: question.answer || "",
    score: Number(question.score || 0),
    actualScore: 0,
    sortOrder: index + 1,
    options: Array.isArray(question.options) ? question.options : [],
    correctAnswers: Array.isArray(question.correctAnswers) ? question.correctAnswers : [],
    referenceAnswer: question.referenceAnswer || "",
    answerExplanation: question.answerExplanation || "",
    caseMaterial: question.caseMaterial || "",
    children: Array.isArray(question.children) ? question.children : []
  }
}

Page({
  data: {
    organizationNodes: [],
    companyOptions: [],
    departmentOptions: [ALL_DEPARTMENTS],
    teamOptions: [ALL_TEAMS],
    companyIndex: 0,
    departmentIndex: 0,
    teamIndex: 0,
    companyName: "点击选择",
    departmentName: ALL_DEPARTMENTS.label,
    teamName: ALL_TEAMS.label,
    companyId: "",
    departmentId: "",
    teamId: "",
    exam: "",
    examDate: "",
    durationMinutes: 30,
    remark: "",
    activeCreateTab: "basic",
    sourceMode: "paper",
    papers: [],
    questionBankRows: [],
    selectedPaperId: "",
    selectedQuestionIds: [],
    selectedCount: 0,
    sourceLoading: false,
    sourceLoaded: false,
    submitting: false
  },

  async onLoad() {
    const draft = getWx().getStorageSync(DRAFT_KEY) || {}
    this.setData({
      exam: draft.exam || "",
      examDate: draft.examDate || todayText(),
      durationMinutes: Number(draft.durationMinutes) || 30,
      remark: draft.remark || "",
      sourceMode: draft.sourceMode === "bank" ? "bank" : "paper",
      selectedPaperId: draft.selectedPaperId || "",
      selectedQuestionIds: Array.isArray(draft.selectedQuestionIds) ? draft.selectedQuestionIds : []
    })
    await this.loadOrganizations(draft)
  },

  async loadOrganizations(draft = {}) {
    const nodes = await getOrgTree()
    const companyOptions = organizationOptionsFor(nodes, undefined, ["COMPANY"])
    const companyId = draft.companyId || (companyOptions[0] && companyOptions[0].value) || ""
    this.applyOrganizationState(nodes, companyOptions, companyId, draft.departmentId, draft.teamId)
  },

  applyOrganizationState(nodes, companyOptions, companyId, departmentId, teamId) {
    const companyIndex = selectedIndex(companyOptions, companyId)
    const company = companyOptions[companyIndex]
    const resolvedCompanyId = company ? company.value : ""
    const departmentOptions = scopedOptions(
      resolvedCompanyId
        ? organizationOptionsFor(nodes, resolvedCompanyId, ["DEPARTMENT"])
        : [],
      ALL_DEPARTMENTS
    )
    const departmentIndex = selectedIndex(departmentOptions, departmentId)
    const department = departmentOptions[departmentIndex] || ALL_DEPARTMENTS
    const resolvedDepartmentId = department.value || ""
    const teamOptions = scopedOptions(
      resolvedDepartmentId
        ? organizationOptionsFor(nodes, resolvedDepartmentId, ["TEAM"])
        : [],
      ALL_TEAMS
    )
    const teamIndex = selectedIndex(teamOptions, resolvedDepartmentId ? teamId : "")
    const team = teamOptions[teamIndex] || ALL_TEAMS
    this.setData({
      organizationNodes: nodes,
      companyOptions,
      departmentOptions,
      teamOptions,
      companyIndex,
      departmentIndex,
      teamIndex,
      companyId: resolvedCompanyId,
      departmentId: resolvedDepartmentId,
      teamId: resolvedDepartmentId ? (team.value || "") : "",
      companyName: optionLabel(company),
      departmentName: optionLabel(department, ALL_DEPARTMENTS.label),
      teamName: optionLabel(team, ALL_TEAMS.label)
    })
  },

  resetQuestionSource() {
    this.setData({
      papers: [],
      questionBankRows: [],
      selectedPaperId: "",
      selectedQuestionIds: [],
      selectedCount: 0,
      sourceLoaded: false
    })
  },

  changeCompany(event = {}) {
    const index = Number(event.detail && event.detail.value ? event.detail.value : 0)
    const company = this.data.companyOptions[index] || {}
    this.applyOrganizationState(
      this.data.organizationNodes,
      this.data.companyOptions,
      company.value,
      "",
      ""
    )
    this.resetQuestionSource()
  },

  changeDepartment(event = {}) {
    const index = Number(event.detail && event.detail.value ? event.detail.value : 0)
    const department = this.data.departmentOptions[index] || ALL_DEPARTMENTS
    this.applyOrganizationState(
      this.data.organizationNodes,
      this.data.companyOptions,
      this.data.companyId,
      department.value,
      ""
    )
    this.resetQuestionSource()
  },

  changeTeam(event = {}) {
    const index = Number(event.detail && event.detail.value ? event.detail.value : 0)
    const team = this.data.teamOptions[index] || ALL_TEAMS
    this.setData({
      teamIndex: index,
      teamId: team.value || "",
      teamName: optionLabel(team, ALL_TEAMS.label)
    })
    this.resetQuestionSource()
  },

  changeExamName(event = {}) {
    this.setData({ exam: event.detail && event.detail.value ? event.detail.value : "" })
  },

  changeExamDate(event = {}) {
    this.setData({ examDate: event.detail && event.detail.value ? event.detail.value : this.data.examDate })
  },

  changeDuration(event = {}) {
    const value = Math.max(1, Math.min(480, Number(event.detail && event.detail.value) || 30))
    this.setData({ durationMinutes: Math.floor(value) })
  },

  changeRemark(event = {}) {
    this.setData({ remark: event.detail && event.detail.value ? event.detail.value : "" })
  },

  draftPayload() {
    return {
      companyId: this.data.companyId,
      departmentId: this.data.departmentId,
      teamId: this.data.teamId,
      exam: this.data.exam,
      examDate: this.data.examDate,
      durationMinutes: this.data.durationMinutes,
      remark: this.data.remark,
      sourceMode: this.data.sourceMode,
      selectedPaperId: this.data.selectedPaperId,
      selectedQuestionIds: this.data.selectedQuestionIds
    }
  },

  validateBasic() {
    if (!this.data.companyId || !trimText(this.data.exam) || !this.data.examDate) {
      getWx().showToast({ title: "请完善必填信息", icon: "none" })
      return false
    }
    return true
  },

  saveDraft() {
    getWx().setStorageSync(DRAFT_KEY, this.draftPayload())
    getWx().showToast({ title: "草稿已保存", icon: "none" })
  },

  async goNext() {
    if (!this.validateBasic()) {
      return
    }
    getWx().setStorageSync(DRAFT_KEY, this.draftPayload())
    this.setData({ activeCreateTab: "detail" })
    if (!this.data.sourceLoaded) {
      await this.loadQuestionSources()
    }
  },

  openBasic() {
    this.setData({ activeCreateTab: "basic" })
  },

  selectPaperMode() {
    this.setData({ sourceMode: "paper" })
    this.saveDraftSilently()
  },

  selectBankMode() {
    this.setData({ sourceMode: "bank" })
    this.saveDraftSilently()
  },

  async loadQuestionSources() {
    this.setData({ sourceLoading: true })
    const query = {
      companyId: this.data.companyId,
      departmentId: this.data.departmentId || undefined,
      teamId: this.data.teamId || undefined,
      page: 1,
      pageSize: 200
    }
    try {
      const [paperResult, bankResult] = await Promise.all([
        examService.listExamPapers(query),
        examService.listExamQuestionBank({ ...query, applicable: true })
      ])
      const selectedQuestionIds = this.data.selectedQuestionIds
      const papers = (paperResult.items || [])
        .filter(paper => paperAppliesToScope(paper, this.data))
        .map(paper => ({
          ...paper,
          selected: sameId(paper.id, this.data.selectedPaperId),
          scopeLabel: paper.team || paper.department || paper.company || "全局共享"
        }))
      const questionBankRows = (bankResult.items || []).map(question => ({
        ...question,
        selected: selectedQuestionIds.some(id => sameId(id, question.id))
      }))
      const selectedPaperId = papers.some(paper => paper.selected) ? this.data.selectedPaperId : ""
      const validQuestionIds = questionBankRows.filter(question => question.selected).map(question => question.id)
      this.setData({
        papers,
        questionBankRows,
        selectedPaperId,
        selectedQuestionIds: validQuestionIds,
        selectedCount: validQuestionIds.length,
        sourceLoaded: true
      })
    } catch (error) {
      getWx().showToast({ title: error.message || "试题资源加载失败", icon: "none" })
    } finally {
      this.setData({ sourceLoading: false })
    }
  },

  selectPaper(event = {}) {
    const id = event.currentTarget && event.currentTarget.dataset ? event.currentTarget.dataset.id : ""
    this.setData({
      selectedPaperId: id,
      papers: this.data.papers.map(paper => ({ ...paper, selected: sameId(paper.id, id) }))
    })
    this.saveDraftSilently()
  },

  toggleBankQuestion(event = {}) {
    const id = event.currentTarget && event.currentTarget.dataset ? event.currentTarget.dataset.id : ""
    const questionBankRows = this.data.questionBankRows.map(question =>
      sameId(question.id, id) ? { ...question, selected: !question.selected } : question
    )
    const selectedQuestionIds = questionBankRows.filter(question => question.selected).map(question => question.id)
    this.setData({
      questionBankRows,
      selectedQuestionIds,
      selectedCount: selectedQuestionIds.length
    })
    this.saveDraftSilently()
  },

  saveDraftSilently() {
    getWx().setStorageSync(DRAFT_KEY, this.draftPayload())
  },

  selectedQuestions() {
    if (this.data.sourceMode === "paper") {
      const paper = this.data.papers.find(item => sameId(item.id, this.data.selectedPaperId))
      return paper && Array.isArray(paper.questions) ? paper.questions : []
    }
    return this.data.questionBankRows.filter(question => question.selected)
  },

  buildPayload() {
    return {
      companyId: this.data.companyId,
      departmentId: this.data.departmentId || null,
      teamId: this.data.teamId || null,
      exam: trimText(this.data.exam),
      examDate: this.data.examDate,
      durationMinutes: this.data.durationMinutes,
      status: "ACTIVE",
      remark: trimText(this.data.remark),
      examPersonUserIds: [],
      questions: this.selectedQuestions().map(questionPayload)
    }
  },

  validatePayload(payload) {
    if (!this.validateBasic()) return false
    if (!payload.questions.length) {
      getWx().showToast({
        title: this.data.sourceMode === "paper" ? "请选择一份试卷" : "请至少选择一道题目",
        icon: "none"
      })
      return false
    }
    return true
  },

  async submitExamTask() {
    if (this.data.submitting) return
    const payload = this.buildPayload()
    if (!this.validatePayload(payload)) return
    this.setData({ submitting: true })
    try {
      await examService.createExamTask(payload)
      getWx().removeStorageSync(DRAFT_KEY)
      getWx().showToast({ title: "提交成功", icon: "success" })
      getWx().redirectTo({ url: "/pages/training/exam-list/index" })
    } finally {
      this.setData({ submitting: false })
    }
  },

  goBack() {
    getWx().navigateBack()
  }
})
