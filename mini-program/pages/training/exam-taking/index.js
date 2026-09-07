const examService = require("../../../services/safety-exam")
const { normalizeExamQuestions, normalizeExamRecord } = require("../exam-data")

function getWx() {
  return wx
}

function getStatusBarHeight() {
  try {
    const api = getWx()
    if (api && typeof api.getSystemInfoSync === "function") {
      const systemInfo = api.getSystemInfoSync()
      return systemInfo.statusBarHeight || 20
    }
  } catch (error) {
    return 20
  }
  return 20
}

function getCapsuleSafeRight() {
  try {
    const api = getWx()
    const systemInfo = api && typeof api.getSystemInfoSync === "function"
      ? api.getSystemInfoSync()
      : {}
    const windowWidth = Number(systemInfo.windowWidth || systemInfo.screenWidth)
    const capsule = api && typeof api.getMenuButtonBoundingClientRect === "function"
      ? api.getMenuButtonBoundingClientRect()
      : null
    if (windowWidth > 0 && capsule && Number(capsule.left) > 0) {
      return Math.max(17, Math.ceil(windowWidth - Number(capsule.left) + 8))
    }
  } catch (error) {
    return 17
  }
  return 17
}

function instructionFor(type) {
  const text = String(type || "")
  if (text === "CASE_ANALYSIS" || text.includes("案例")) {
    return "请阅读案例材料并完成以下子题"
  }
  if (text === "MULTIPLE_CHOICE" || text.includes("多")) {
    return "请选择两个或以上正确选项"
  }
  if (text === "SHORT_ANSWER" || text.includes("问答")) {
    return "请在下方填写答案，提交后由管理人员评分"
  }
  return "下列每题给出的选项中只有一个选项是正确的"
}

function splitOption(option, index) {
  const raw = String(option || "").trim()
  const match = raw.match(/^([A-Z])[\.\u3001\uff0e]\s*(.*)$/)
  if (match) {
    return {
      letter: match[1],
      text: match[2] || raw
    }
  }
  return {
    letter: String.fromCharCode(65 + index),
    text: raw
  }
}

function progressFor(index, total) {
  if (!total) {
    return 0
  }
  return Math.max(2, Math.round(((index + 1) / total) * 100))
}

function formatCountdown(totalSeconds) {
  const seconds = Math.max(0, Math.floor(Number(totalSeconds) || 0))
  const minutes = Math.floor(seconds / 60)
  const remainder = seconds % 60
  return `${String(minutes).padStart(2, "0")}:${String(remainder).padStart(2, "0")}`
}

function countdownSecondsFor(detail = {}) {
  const secondFields = ["remainingSeconds", "countdownSeconds", "durationSeconds", "timeLimitSeconds"]
  for (const field of secondFields) {
    const value = Number(detail[field])
    if (Number.isFinite(value) && value > 0) {
      return Math.floor(value)
    }
  }

  const minuteFields = ["durationMinutes", "examDuration", "timeLimit"]
  for (const field of minuteFields) {
    const raw = detail[field]
    const value = Number(raw)
    if (Number.isFinite(value) && value > 0) {
      return Math.floor(value * 60)
    }
    const text = String(raw || "").trim()
    const clock = text.match(/^(\d+):(\d{1,2})$/)
    if (clock) {
      return Number(clock[1]) * 60 + Number(clock[2])
    }
    const minutes = text.match(/(\d+(?:\.\d+)?)\s*分钟/)
    if (minutes) {
      return Math.floor(Number(minutes[1]) * 60)
    }
  }

  return 30 * 60
}

function isMultipleChoice(question) {
  const type = String(question && (question.questionType || question.type) || "")
  return type === "MULTIPLE_CHOICE" || type.includes("多")
}

function isShortAnswer(question) {
  const type = String(question && (question.questionType || question.type) || "")
  return type === "SHORT_ANSWER" || type.includes("问答")
}

function hasAnswer(answer) {
  return Array.isArray(answer)
    ? answer.length > 0
    : answer !== undefined && answer !== null && String(answer).trim() !== ""
}

function formatElapsed(seconds) {
  const value = Math.max(0, Math.floor(Number(seconds) || 0))
  return `${Math.floor(value / 60)}分${String(value % 60).padStart(2, "0")}秒`
}

function selectedIndexes(question, selectedOption) {
  if (isShortAnswer(question)) return selectedOption
  return String(selectedOption || "")
    .split(",")
    .map(item => item.trim().charCodeAt(0) - 65)
    .filter(index => index >= 0 && index < (question.options || []).length)
}

function normalizeTakingQuestions(rawQuestions = []) {
  const questions = normalizeExamQuestions(rawQuestions)
  const result = []
  const cases = new Map()

  questions.forEach(question => {
    if (!question.caseMaterial) {
      result.push(question)
      return
    }
    const caseId = `case-${question.questionId}`
    let caseQuestion = cases.get(caseId)
    if (!caseQuestion) {
      caseQuestion = {
        id: caseId,
        questionId: question.questionId,
        questionKey: caseId,
        questionType: "CASE_ANALYSIS",
        type: "案例题",
        title: "案例题",
        question: question.caseTitle || "案例题",
        caseTitle: question.caseTitle || "案例材料",
        caseMaterial: question.caseMaterial,
        isCase: true,
        children: []
      }
      cases.set(caseId, caseQuestion)
      result.push(caseQuestion)
    }
    caseQuestion.children.push({
      ...question,
      subNo: caseQuestion.children.length + 1
    })
  })

  return result.map((question, index) => ({
    ...question,
    no: index + 1
  }))
}

function answerQuestions(questions = []) {
  return questions.flatMap(question => question.isCase ? (question.children || []) : [question])
}

function isQuestionAnswered(question, answers = {}) {
  if (!question) return false
  if (question.isCase) {
    return Boolean(question.children && question.children.length)
      && question.children.every(child => hasAnswer(answers[child.id]))
  }
  return hasAnswer(answers[question.id])
}

function resultStateFor(question, resultMap = {}) {
  const states = (question.isCase ? question.children || [] : [question])
    .map(item => resultMap[item.questionKey || item.id])
    .filter(Boolean)
  if (!states.length) return ""
  if (states.includes("incorrect")) return "incorrect"
  if (states.every(state => state === "correct")) return "correct"
  if (states.includes("pending")) return "pending"
  return ""
}

function buildAnswerCardGroups(questions = [], answers = {}, currentIndex = 0, resultMap = {}) {
  const groups = [
    { key: "single", title: "单项选择题", questions: [] },
    { key: "multiple", title: "多项选择题", questions: [] },
    { key: "short", title: "问答题", questions: [] },
    { key: "case", title: "案例题", questions: [] }
  ]

  questions.forEach((question, index) => {
    const target = question.isCase
      ? groups[3]
      : isShortAnswer(question)
        ? groups[2]
        : isMultipleChoice(question)
          ? groups[1]
          : groups[0]
    target.questions.push({
      id: question.id,
      index,
      no: index + 1,
      answered: isQuestionAnswered(question, answers),
      current: index === currentIndex,
      resultState: resultStateFor(question, resultMap)
    })
  })

  return groups
    .filter(group => group.questions.length > 0)
    .sort((left, right) => left.questions[0].no - right.questions[0].no)
}

function decorateQuestion(question, selectedAnswer) {
  if (!question) {
    return {
      id: "",
      type: "",
      instruction: "",
      question: "",
      options: []
    }
  }
  if (question.isCase) {
    return {
      ...question,
      type: "案例题",
      instruction: instructionFor("CASE_ANALYSIS"),
      isCase: true,
      isShortAnswer: false,
      children: (question.children || []).map(child => {
        return decorateQuestion(child, selectedAnswer && selectedAnswer[child.id])
      })
    }
  }
  const selectedIndexes = Array.isArray(selectedAnswer)
    ? selectedAnswer
    : selectedAnswer === undefined || selectedAnswer === null
      ? []
      : [selectedAnswer]
  return {
    ...question,
    instruction: instructionFor(question.questionType || question.type),
    isShortAnswer: isShortAnswer(question),
    answerText: isShortAnswer(question) ? String(selectedAnswer || "") : "",
    options: (question.options || []).map((option, index) => {
      const item = splitOption(option, index)
      return {
        id: `${question.id}-${index}`,
        letter: item.letter,
        text: item.text,
        selected: selectedIndexes.includes(index)
      }
    })
  }
}

Page({
  data: {
    recordId: "",
    record: null,
    questions: [],
    currentQuestionIndex: 0,
    currentQuestion: decorateQuestion(null),
    questionTotal: 0,
    progressPercent: 0,
    remainingSeconds: 0,
    remainingText: "30:00",
    statusBarHeight: 20,
    capsuleSafeRight: 17,
    answers: {},
    answerCardVisible: false,
    exitConfirmVisible: false,
    answerCardGroups: [],
    answeredCount: 0,
    currentQuestionAnswered: false,
    resultMode: false,
    resultSummary: { correctCount: 0, incorrectCount: 0, elapsedText: "0分00秒" },
    annotationMode: false,
    hasCurrentAnnotation: false,
    annotationCanvasWidth: 375,
    annotationCanvasHeight: 700,
    submitting: false
  },

  async onLoad(options = {}) {
    this.annotationPaths = {}
    this.currentAnnotationPath = null
    const id = options.id
    this.submissionRequestId = `mini-exam-${id || "unknown"}-${Date.now()}-${Math.random().toString(16).slice(2)}`
    this.setData({
      statusBarHeight: getStatusBarHeight(),
      capsuleSafeRight: getCapsuleSafeRight()
    })
    if (!id) {
      getWx().showToast({ title: "缺少考试记录", icon: "none" })
      return
    }
    const detail = await examService.getExamTaskDetail(id)
    const questions = normalizeTakingQuestions(detail.questions)
    const answers = {}
    answerQuestions(questions).forEach(question => {
      const selected = question.selectedOption
      if (hasAnswer(selected)) {
        answers[question.id] = selectedIndexes(question, selected)
      }
    })
    const record = normalizeExamRecord(detail)
    this.setData({
      recordId: String(id),
      record,
      questions,
      currentQuestionIndex: Math.min(
        Math.max(0, Number(detail.currentQuestionIndex || 0)),
        Math.max(0, questions.length - 1)
      ),
      questionTotal: questions.length,
      answers,
      answerCardVisible: false
    })
    this.syncCurrentQuestion()
    this.startCountdown(Number(detail.remainingSeconds) || countdownSecondsFor(detail))
  },

  onShow() {
    if (this.countdownEndAt && !this.countdownTimer) {
      this.resumeCountdown()
    }
  },

  onReady() {
    this.measureAnnotationCanvas()
  },

  onHide() {
    this.saveProgressSilently()
    this.clearCountdownTimer()
  },

  onUnload() {
    this.clearCountdownTimer()
  },

  syncCurrentQuestion() {
    const index = this.data.currentQuestionIndex
    const total = this.data.questions.length
    const question = this.data.questions[index]
    const selectedAnswer = question && question.isCase
      ? this.data.answers
      : question
        ? this.data.answers[question.id]
        : undefined
    const answerCardGroups = buildAnswerCardGroups(this.data.questions, this.data.answers, index, this.resultMap)
    this.setData({
      currentQuestion: decorateQuestion(question, selectedAnswer),
      questionTotal: total,
      progressPercent: progressFor(index, total),
      answerCardGroups,
      currentQuestionAnswered: isQuestionAnswered(question, this.data.answers),
      answeredCount: answerCardGroups.reduce((count, group) => {
        return count + group.questions.filter(item => item.answered).length
      }, 0)
    })
    this.syncAnnotationState(question)
  },

  startCountdown(totalSeconds) {
    this.clearCountdownTimer()
    const seconds = Math.max(0, Math.floor(Number(totalSeconds) || 0))
    this.countdownEndAt = Date.now() + seconds * 1000
    this.countdownExpiredNotified = false
    this.updateCountdown()
    if (seconds <= 0) {
      return
    }
    this.countdownTimer = setInterval(() => this.updateCountdown(), 1000)
    if (this.countdownTimer && typeof this.countdownTimer.unref === "function") {
      this.countdownTimer.unref()
    }
  },

  resumeCountdown() {
    this.clearCountdownTimer()
    this.updateCountdown()
    if (this.countdownEndAt > Date.now()) {
      this.countdownTimer = setInterval(() => this.updateCountdown(), 1000)
      if (this.countdownTimer && typeof this.countdownTimer.unref === "function") {
        this.countdownTimer.unref()
      }
    }
  },

  updateCountdown(now = Date.now()) {
    const remainingSeconds = Math.max(0, Math.ceil((Number(this.countdownEndAt) - Number(now)) / 1000))
    this.setData({
      remainingSeconds,
      remainingText: formatCountdown(remainingSeconds)
    })
    if (remainingSeconds === 0) {
      this.clearCountdownTimer()
      if (!this.countdownExpiredNotified) {
        this.countdownExpiredNotified = true
        getWx().showToast({ title: "考试时间已到", icon: "none" })
      }
    }
  },

  clearCountdownTimer() {
    if (this.countdownTimer) {
      clearInterval(this.countdownTimer)
      this.countdownTimer = null
    }
  },

  currentAnnotationId(question = this.data.currentQuestion) {
    return question && question.id ? String(question.id) : ""
  },

  syncAnnotationState(question) {
    const questionId = this.currentAnnotationId(question)
    const paths = questionId && this.annotationPaths ? this.annotationPaths[questionId] : null
    this.setData({ hasCurrentAnnotation: Boolean(paths && paths.length) })
    this.measureAnnotationCanvas(questionId)
  },

  measureAnnotationCanvas(questionId = this.currentAnnotationId()) {
    const api = getWx()
    if (!api || typeof api.createSelectorQuery !== "function") {
      this.redrawAnnotations(questionId)
      return
    }
    const measure = () => {
      const query = api.createSelectorQuery()
      const scopedQuery = query && typeof query.in === "function" ? query.in(this) : query
      if (!scopedQuery || typeof scopedQuery.select !== "function") {
        this.redrawAnnotations(questionId)
        return
      }
      scopedQuery.select(".exam-taking-body").boundingClientRect(rect => {
        if (rect && rect.width > 0 && rect.height > 0) {
          this.setData({
            annotationCanvasWidth: Math.round(rect.width),
            annotationCanvasHeight: Math.round(rect.height)
          })
        }
        this.redrawAnnotations(questionId)
      }).exec()
    }
    if (typeof api.nextTick === "function") {
      api.nextTick(measure)
    } else {
      measure()
    }
  },

  annotationContext() {
    const api = getWx()
    if (!api || typeof api.createCanvasContext !== "function") {
      return null
    }
    return api.createCanvasContext("examAnnotationCanvas", this)
  },

  redrawAnnotations(questionId = this.currentAnnotationId()) {
    const context = this.annotationContext()
    if (!context) {
      return
    }
    context.clearRect(0, 0, 1200, 2400)
    const paths = this.annotationPaths && this.annotationPaths[questionId] || []
    paths.forEach(path => this.drawAnnotationPath(context, path))
    context.draw()
  },

  drawAnnotationPath(context, path) {
    if (!context || !path || !path.length) {
      return
    }
    context.beginPath()
    context.setStrokeStyle("#ff5b00")
    context.setLineWidth(3)
    context.setLineCap("round")
    context.setLineJoin("round")
    context.moveTo(path[0].x, path[0].y)
    path.slice(1).forEach(point => context.lineTo(point.x, point.y))
    context.stroke()
  },

  annotationPoint(event) {
    const touch = event && event.touches && event.touches[0]
      || event && event.changedTouches && event.changedTouches[0]
    if (!touch) {
      return null
    }
    const x = Number(touch.x !== undefined ? touch.x : touch.clientX)
    const y = Number(touch.y !== undefined ? touch.y : touch.clientY)
    return Number.isFinite(x) && Number.isFinite(y) ? { x, y } : null
  },

  toggleAnnotation() {
    const annotationMode = !this.data.annotationMode
    this.setData({ annotationMode })
    this.redrawAnnotations()
  },

  startAnnotation(event) {
    if (!this.data.annotationMode) {
      return
    }
    const point = this.annotationPoint(event)
    if (point) {
      this.currentAnnotationPath = [point]
    }
  },

  moveAnnotation(event) {
    if (!this.data.annotationMode || !this.currentAnnotationPath) {
      return
    }
    const point = this.annotationPoint(event)
    if (!point) {
      return
    }
    const previous = this.currentAnnotationPath[this.currentAnnotationPath.length - 1]
    this.currentAnnotationPath.push(point)
    const context = this.annotationContext()
    if (context) {
      this.drawAnnotationPath(context, [previous, point])
      context.draw(true)
    }
  },

  endAnnotation(event) {
    if (!this.data.annotationMode || !this.currentAnnotationPath) {
      return
    }
    const point = this.annotationPoint(event)
    const lastPoint = this.currentAnnotationPath[this.currentAnnotationPath.length - 1]
    if (point && (!lastPoint || lastPoint.x !== point.x || lastPoint.y !== point.y)) {
      this.currentAnnotationPath.push(point)
    }
    const questionId = this.currentAnnotationId()
    if (questionId && this.currentAnnotationPath.length > 1) {
      const paths = this.annotationPaths[questionId] || []
      this.annotationPaths[questionId] = [...paths, this.currentAnnotationPath]
      this.setData({ hasCurrentAnnotation: true })
    }
    this.currentAnnotationPath = null
  },

  clearCurrentAnnotation() {
    const questionId = this.currentAnnotationId()
    if (questionId && this.annotationPaths) {
      delete this.annotationPaths[questionId]
    }
    this.currentAnnotationPath = null
    this.setData({ hasCurrentAnnotation: false })
    this.redrawAnnotations(questionId)
  },

  selectOption(event) {
    const index = Number(event.currentTarget.dataset.index)
    const currentQuestion = this.data.questions[this.data.currentQuestionIndex]
    const childId = event.currentTarget.dataset.questionId
    const question = currentQuestion && currentQuestion.isCase
      ? currentQuestion.children.find(item => String(item.id) === String(childId))
      : currentQuestion
    if (!question || Number.isNaN(index)) {
      return
    }
    const currentAnswer = this.data.answers[question.id]
    let nextAnswer = index
    if (isMultipleChoice(question)) {
      const selectedIndexes = Array.isArray(currentAnswer) ? currentAnswer : []
      nextAnswer = selectedIndexes.includes(index)
        ? selectedIndexes.filter(item => item !== index)
        : [...selectedIndexes, index].sort((left, right) => left - right)
    }
    this.setData({ answers: { ...this.data.answers, [question.id]: nextAnswer } })
    this.syncCurrentQuestion()
    this.saveProgressSilently()
  },

  inputShortAnswer(event) {
    const currentQuestion = this.data.questions[this.data.currentQuestionIndex]
    const childId = event.currentTarget.dataset.questionId
    const question = currentQuestion && currentQuestion.isCase
      ? currentQuestion.children.find(item => String(item.id) === String(childId))
      : currentQuestion
    if (!question || !isShortAnswer(question)) {
      return
    }
    const value = event && event.detail && event.detail.value || ""
    this.setData({ answers: { ...this.data.answers, [question.id]: value } })
    this.syncCurrentQuestion()
    this.saveProgressSilently()
  },

  openAnswerCard() {
    this.syncCurrentQuestion()
    this.setData({ answerCardVisible: true })
  },

  closeAnswerCard() {
    this.setData({ answerCardVisible: false })
  },

  jumpToQuestion(event) {
    const index = Number(event.currentTarget.dataset.index)
    if (Number.isNaN(index) || index < 0 || index >= this.data.questionTotal) {
      return
    }
    this.setData({
      answerCardVisible: false,
      currentQuestionIndex: index
    })
    this.syncCurrentQuestion()
  },

  async submitAnswers() {
    const unansweredCount = this.data.questionTotal - this.data.answeredCount
    if (unansweredCount > 0) {
      getWx().showToast({ title: `还有${unansweredCount}题未作答`, icon: "none" })
      return
    }
    this.closeAnswerCard()
    if (this.data.submitting) {
      return
    }
    const answers = answerQuestions(this.data.questions).map(question => {
      const selected = this.data.answers[question.id]
      const selectedOption = isShortAnswer(question)
        ? String(selected).trim()
        : (Array.isArray(selected) ? selected : [selected])
          .map(index => String.fromCharCode(65 + Number(index)))
          .join(",")
      return {
        questionId: Number(question.questionId),
        questionKey: question.questionKey,
        selectedOption
      }
    })
    this.setData({ submitting: true })
    try {
      const result = await examService.submitExam(this.data.recordId, {
        requestId: this.submissionRequestId,
        answers,
        currentQuestionIndex: this.data.currentQuestionIndex,
        remainingSeconds: this.data.remainingSeconds
      })
      this.clearCountdownTimer()
      this.examCompleted = true
      this.resultMap = {}
      ;(result.questions || []).forEach(question => {
        if (question.actualScore === null || question.actualScore === undefined) {
          this.resultMap[question.questionKey || question.id] = "pending"
        } else {
          this.resultMap[question.questionKey || question.id] = Number(question.actualScore) >= Number(question.score) ? "correct" : "incorrect"
        }
      })
      this.syncCurrentQuestion()
      this.setData({
        resultMode: true,
        answerCardVisible: true,
        resultSummary: {
          correctCount: Number(result.correctCount || 0),
          incorrectCount: Number(result.incorrectCount || 0),
          elapsedText: formatElapsed(result.elapsedSeconds)
        }
      })
      return result
    } catch (error) {
      getWx().showToast({ title: error && error.message || "交卷失败，请重试", icon: "none" })
      throw error
    } finally {
      this.setData({ submitting: false })
    }
  },

  preventTouchMove() {},

  stopPropagation() {},

  goPrev() {
    if (this.data.currentQuestionIndex <= 0) {
      return
    }
    this.setData({ currentQuestionIndex: this.data.currentQuestionIndex - 1 })
    this.syncCurrentQuestion()
  },

  goNext() {
    if (this.data.currentQuestionIndex >= this.data.questionTotal - 1) {
      this.openAnswerCard()
      return
    }
    this.setData({ currentQuestionIndex: this.data.currentQuestionIndex + 1 })
    this.syncCurrentQuestion()
  },

  goBack() {
    if (this.examCompleted) {
      getWx().navigateBack()
      return
    }
    this.setData({ exitConfirmVisible: true })
  },

  cancelExitExam() {
    this.setData({ exitConfirmVisible: false })
  },

  async confirmExitExam() {
    this.setData({ exitConfirmVisible: false })
    await this.saveProgressSilently()
    getWx().navigateBack()
  },

  buildProgressPayload() {
    return {
      currentQuestionIndex: this.data.currentQuestionIndex,
      remainingSeconds: this.data.remainingSeconds,
      answers: answerQuestions(this.data.questions)
        .filter(question => hasAnswer(this.data.answers[question.id]))
        .map(question => ({
          questionId: Number(question.questionId),
          questionKey: question.questionKey,
          selectedOption: isShortAnswer(question)
            ? String(this.data.answers[question.id])
            : (Array.isArray(this.data.answers[question.id]) ? this.data.answers[question.id] : [this.data.answers[question.id]])
              .map(index => String.fromCharCode(65 + Number(index)))
              .join(",")
        }))
    }
  },

  async saveProgressSilently() {
    if (!this.data.recordId || this.examCompleted || this.data.submitting) return
    try {
      await examService.saveExamProgress(this.data.recordId, this.buildProgressPayload())
    } catch (error) {
      // Progress is best-effort while leaving or changing a question.
    }
  }
})
