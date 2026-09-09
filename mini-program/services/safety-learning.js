const { request } = require("../utils/request")

const MINI_CONTENT_BASE = "/api/mini/pingan/training/safety-learning/contents"
const PC_CONTENT_BASE = "/api/pingan/training/safety-learning/contents"

function isNotFound(error) {
  return error && error.response && error.response.statusCode === 404
}

function compactQuery(query = {}) {
  return Object.keys(query).reduce((result, key) => {
    const value = query[key]
    if (value !== undefined && value !== null && value !== "") {
      result[key] = value
    }
    return result
  }, {})
}

async function requestWithPcFallback(miniOptions, pcOptions) {
  try {
    return await request(miniOptions)
  } catch (error) {
    if (isNotFound(error)) {
      return request(pcOptions)
    }
    throw error
  }
}

function listLearningContents(query = {}) {
  const data = compactQuery(query)
  return requestWithPcFallback(
    {
      url: MINI_CONTENT_BASE,
      method: "GET",
      data
    },
    {
      url: PC_CONTENT_BASE,
      method: "GET",
      data
    }
  )
}

function getLearningContent(id) {
  return requestWithPcFallback(
    {
      url: `${MINI_CONTENT_BASE}/${id}`,
      method: "GET"
    },
    {
      url: `${PC_CONTENT_BASE}/${id}`,
      method: "GET"
    }
  )
}

function checkInLearning(id, data = {}) {
  return request({
    url: `${MINI_CONTENT_BASE}/${id}/check-in`,
    method: "POST",
    data
  })
}

module.exports = {
  listLearningContents,
  getLearningContent,
  checkInLearning
}
