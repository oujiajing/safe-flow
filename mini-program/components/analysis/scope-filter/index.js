function pad(value) {
  return String(value).padStart(2, "0")
}

function formatDate(date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

function quickRange(days, label) {
  const end = new Date()
  const start = new Date(end.getTime())
  start.setDate(start.getDate() - days + 1)
  return { dateStart: formatDate(start), dateEnd: formatDate(end), rangeLabel: label }
}

const { buildScopeState, selectionContext } = require("../../../utils/analysis-scope")

Component({
  properties: {
    context: { type: Object, value: {} },
    choices: { type: Array, value: [] },
    scopeTree: { type: Array, value: [] },
    scopeAccess: { type: Object, value: {} },
    rangeText: { type: String, value: "" },
    updatedAt: { type: String, value: "--:--" },
    compact: { type: Boolean, value: false },
    tabBarInset: { type: Boolean, value: false }
  },

  data: {
    visible: false,
    draft: {},
    scopeState: {}
  },

  methods: {
    open() {
      const context = { ...this.properties.context }
      const scopeState = buildScopeState(this.properties.scopeTree, context, this.properties.scopeAccess)
      this.setData({ visible: true, draft: context, scopeState })
    },

    close() {
      this.setData({ visible: false })
    },

    stop() {},

    changeScope(event) {
      const level = event.currentTarget.dataset.level
      const target = selectionContext(this.properties.scopeTree, this.data.scopeState, level, event.detail.value)
      const draft = { ...this.data.draft, ...target }
      this.setData({
        draft,
        scopeState: buildScopeState(this.properties.scopeTree, draft, this.properties.scopeAccess)
      })
    },

    changeDate(event) {
      this.setData({
        [`draft.${event.currentTarget.dataset.field}`]: event.detail.value,
        "draft.rangeLabel": "自定义"
      })
    },

    chooseQuick(event) {
      const days = Number(event.currentTarget.dataset.days)
      const label = event.currentTarget.dataset.label
      this.setData({ draft: { ...this.data.draft, ...quickRange(days, label) } })
    },

    reset() {
      const initial = quickRange(7, "近7天")
      const scopeState = buildScopeState(this.properties.scopeTree, {}, this.properties.scopeAccess)
      const draft = { ...initial, ...scopeState.fallback }
      this.setData({
        draft,
        scopeState: buildScopeState(this.properties.scopeTree, draft, this.properties.scopeAccess)
      })
    },

    apply() {
      this.triggerEvent("apply", this.data.draft)
      this.close()
    }
  }
})
