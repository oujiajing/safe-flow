const examService = require("../../../services/safety-exam")
const { getExamInfoRows, normalizeExamQuestions, normalizeExamRecord } = require("../exam-data")

function getWx() {
  return wx
}

Page({
  data: {
    recordId: "",
    record: null,
    infoRows: [],
    questions: [],
    activeDetailTab: "basic"
  },

  async onLoad(options = {}) {
    if (!options.id) {
      getWx().showToast({ title: "缺少考试记录", icon: "none" })
      return
    }
    const detail = await examService.getExamTaskDetail(options.id)
    const record = normalizeExamRecord(detail)
    this.setData({
      activeDetailTab: "basic",
      recordId: record.id,
      record,
      infoRows: getExamInfoRows(record),
      questions: normalizeExamQuestions(detail.questions)
    })
  },

  openDetail() {
    this.setData({ activeDetailTab: "detail" })
  },

  openBasic() {
    this.setData({ activeDetailTab: "basic" })
  },

  toggleQuestion(event) {
    const id = event.currentTarget.dataset.id
    const current = this.data.questions.find(item => item.id === id)
    this.setData({
      questions: normalizeExamQuestions(this.data.questions, current && current.expanded ? "" : id)
    })
  },

  saveDraft() {
    getWx().showToast({ title: "草稿已保留", icon: "none" })
  },

  submitExam() {
    getWx().navigateTo({ url: `/pages/training/exam-taking/index?id=${this.data.recordId}` })
  },

  goBack() {
    getWx().navigateBack()
  }
})
