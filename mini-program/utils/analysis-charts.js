function windowWidth() {
  return typeof wx !== "undefined" && wx.getSystemInfoSync ? wx.getSystemInfoSync().windowWidth : 375
}

function compactLabelIndexes(length) {
  if (length <= 3) return Array.from({ length }, (_, index) => index)
  return [0, Math.floor((length - 1) / 2), length - 1]
}

function drawLineChart(canvasId, rows, options = {}) {
  if (typeof wx === "undefined" || !wx.createCanvasContext || !Array.isArray(rows) || rows.length === 0) return
  const viewport = windowWidth()
  const width = options.compact ? (viewport - 33) / 2 - 24 : viewport - 48
  const heightRpx = options.heightRpx || (options.compact ? 130 : 240)
  const height = viewport * heightRpx / 750
  const padding = options.compact
    ? { left: 18, right: 8, top: 8, bottom: 20 }
    : { left: 28, right: 10, top: 14, bottom: 28 }
  const values = rows.flatMap(item => [Number(item.value ?? item.finished ?? 0), Number(item.pending ?? 0)])
  const max = Math.max(1, ...values)
  const ctx = wx.createCanvasContext(canvasId)
  if (!options.compact && options.showYAxisTicks) {
    ctx.setFontSize(9)
    for (let index = 0; index <= 4; index += 1) {
      const ratio = index / 4
      const y = padding.top + ratio * (height - padding.top - padding.bottom)
      const label = Math.round(max * (1 - ratio))
      ctx.setStrokeStyle("#edf0f2")
      ctx.setLineWidth(1)
      ctx.beginPath()
      ctx.moveTo(padding.left, y)
      ctx.lineTo(width - padding.right, y)
      ctx.stroke()
      ctx.setFillStyle("#697386")
      ctx.fillText(String(label), 3, y + 3)
    }
  }
  ctx.setStrokeStyle("#dfe5ea")
  ctx.setLineWidth(1)
  ctx.beginPath()
  ctx.moveTo(padding.left, padding.top)
  ctx.lineTo(padding.left, height - padding.bottom)
  ctx.lineTo(width - padding.right, height - padding.bottom)
  ctx.stroke()

  const compactDateIndexes = new Set(compactLabelIndexes(rows.length))
  const drawSeries = (key, color, drawDateLabels) => {
    if (options.fillArea && drawDateLabels && rows.length > 0) {
      ctx.setFillStyle(options.fillColor || "#fff0f0")
      ctx.beginPath()
      rows.forEach((item, index) => {
        const x = padding.left + index * (width - padding.left - padding.right) / Math.max(1, rows.length - 1)
        const y = height - padding.bottom - Number(item[key] || 0) * (height - padding.top - padding.bottom) / max
        if (index === 0) ctx.moveTo(x, height - padding.bottom)
        ctx.lineTo(x, y)
      })
      ctx.lineTo(width - padding.right, height - padding.bottom)
      ctx.closePath()
      ctx.fill()
    }
    ctx.setStrokeStyle(color)
    ctx.setFillStyle(color)
    ctx.setLineWidth(options.compact ? 1.5 : 2)
    ctx.beginPath()
    rows.forEach((item, index) => {
      const x = padding.left + index * (width - padding.left - padding.right) / Math.max(1, rows.length - 1)
      const y = height - padding.bottom - Number(item[key] || 0) * (height - padding.top - padding.bottom) / max
      if (index === 0) ctx.moveTo(x, y)
      else ctx.lineTo(x, y)
    })
    ctx.stroke()
    rows.forEach((item, index) => {
      const x = padding.left + index * (width - padding.left - padding.right) / Math.max(1, rows.length - 1)
      const y = height - padding.bottom - Number(item[key] || 0) * (height - padding.top - padding.bottom) / max
      ctx.beginPath()
      ctx.arc(x, y, options.compact ? 2 : 3, 0, Math.PI * 2)
      if (options.compact) {
        ctx.fill()
      } else {
        ctx.setFillStyle("#ffffff")
        ctx.fill()
        ctx.setStrokeStyle(color)
        ctx.setLineWidth(2)
        ctx.stroke()
      }
      ctx.setFillStyle("#475569")
      ctx.setFontSize(options.compact ? 7 : 10)
      if (!options.compact) ctx.fillText(String(item[key] || 0), x - 4, y - 7)
      if (drawDateLabels && options.compact && compactDateIndexes.has(index)) {
        if (ctx.setTextAlign) ctx.setTextAlign(index === 0 ? "left" : index === rows.length - 1 ? "right" : "center")
        ctx.fillText(String(item.date || ""), x, height - 5)
      } else if (drawDateLabels && !options.compact) {
        ctx.fillText(String(item.date || ""), x - 12, height - 8)
      }
      ctx.setFillStyle(color)
    })
  }
  if (options.secondaryKey) drawSeries(options.secondaryKey, options.secondaryColor || "#ff7800", false)
  drawSeries(options.primaryKey || "value", options.primaryColor || "#1677ff", true)
  ctx.draw()
}

function drawDonut(canvasId, segments, options = {}) {
  if (typeof wx === "undefined" || !wx.createCanvasContext || !Array.isArray(segments)) return
  const ctx = wx.createCanvasContext(canvasId)
  const sizeRpx = options.sizeRpx || (options.compact ? 116 : 188)
  const size = windowWidth() * sizeRpx / 750
  const center = size / 2
  const radius = size * 0.36
  const total = Math.max(1, segments.reduce((sum, item) => sum + Number(item.value || 0), 0))
  let angle = -Math.PI / 2
  segments.forEach(item => {
    const next = angle + Number(item.value || 0) * Math.PI * 2 / total
    ctx.beginPath()
    ctx.setStrokeStyle(item.color)
    ctx.setLineWidth(size * 0.18)
    ctx.arc(center, center, radius, angle, next)
    ctx.stroke()
    angle = next
  })
  ctx.draw()
}

function drawFunnel(canvasId, rows, options = {}) {
  if (typeof wx === "undefined" || !wx.createCanvasContext || !Array.isArray(rows) || rows.length === 0) return
  const viewport = windowWidth()
  const width = options.compact ? (viewport - 33) / 2 - 24 : viewport - 48
  const heightRpx = options.heightRpx || 336
  const height = viewport * heightRpx / 750
  const centerX = width / 2
  const chartTop = 5
  const chartBottom = height - 5
  const gap = 2
  const segmentHeight = (chartBottom - chartTop - gap * (rows.length - 1)) / rows.length
  const maxWidth = width * 0.9
  const minBottomWidth = width * 0.28
  const widthStep = (maxWidth - minBottomWidth) / rows.length
  const colors = options.colors || ["#347df0", "#18b9ad", "#ffad24", "#58c92e"]
  const ctx = wx.createCanvasContext(canvasId)

  rows.forEach((item, index) => {
    const topWidth = maxWidth - widthStep * index
    const bottomWidth = maxWidth - widthStep * (index + 1)
    const top = chartTop + index * (segmentHeight + gap)
    const bottom = top + segmentHeight
    ctx.beginPath()
    ctx.moveTo(centerX - topWidth / 2, top)
    ctx.lineTo(centerX + topWidth / 2, top)
    ctx.lineTo(centerX + bottomWidth / 2, bottom)
    ctx.lineTo(centerX - bottomWidth / 2, bottom)
    ctx.closePath()
    ctx.setFillStyle(colors[index % colors.length])
    ctx.fill()

    if (ctx.setTextAlign) ctx.setTextAlign("center")
    if (ctx.setTextBaseline) ctx.setTextBaseline("middle")
    ctx.setFillStyle("#ffffff")
    ctx.setFontSize(9)
    ctx.fillText(String(item.label || ""), centerX, top + segmentHeight * 0.34)
    ctx.setFontSize(12)
    ctx.fillText(String(Number(item.value || 0)), centerX, top + segmentHeight * 0.7)
  })
  ctx.draw()
}

module.exports = { compactLabelIndexes, drawDonut, drawFunnel, drawLineChart }
