const { request } = require("../utils/request")

const PC_EXAM_TASK_BASE = "/api/pingan/training/exam-tasks"
const PC_EXAM_PAPER_BASE = "/api/pingan/training/exam-papers"
const PC_EXAM_QUESTION_BANK_BASE = "/api/pingan/training/exam-question-bank"
const MINI_EXAM_BASE = "/api/mini/pingan/training/exams"

function compactQuery(query = {}) {
  return Object.keys(query).reduce((result, key) => {
    const value = query[key]
    if (value !== undefined && value !== null && value !== "") {
      result[key] = value
    }
    return result
  }, {})
}

function listExamTasks(query = {}) {
  return request({
    url: MINI_EXAM_BASE,
    method: "GET",
    data: compactQuery(query)
  })
}

function getExamTaskDetail(id) {
  return request({
    url: `${MINI_EXAM_BASE}/${id}`,
    method: "GET"
  })
}

function submitExam(id, data) {
  return request({
    url: `${MINI_EXAM_BASE}/${id}/submissions`,
    method: "POST",
    data
  })
}

function saveExamProgress(id, data) {
  return request({
    url: `${MINI_EXAM_BASE}/${id}/progress`,
    method: "PUT",
    data
  })
}

function createExamTask(data) {
  return request({
    url: PC_EXAM_TASK_BASE,
    method: "POST",
    data
  })
}

function listExamPapers(query = {}) {
  return request({
    url: PC_EXAM_PAPER_BASE,
    method: "GET",
    data: compactQuery(query)
  })
}

function listExamQuestionBank(query = {}) {
  return request({
    url: PC_EXAM_QUESTION_BANK_BASE,
    method: "GET",
    data: compactQuery(query)
  })
}

module.exports = {
  listExamTasks,
  getExamTaskDetail,
  saveExamProgress,
  submitExam,
  createExamTask,
  listExamPapers,
  listExamQuestionBank
}
