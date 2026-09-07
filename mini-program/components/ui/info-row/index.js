Component({
  options: {
    multipleSlots: true
  },

  properties: {
    label: {
      type: String,
      value: ""
    },
    value: {
      type: String,
      value: ""
    },
    required: {
      type: Boolean,
      value: false
    },
    arrow: {
      type: Boolean,
      value: false
    },
    placeholder: {
      type: String,
      value: "-"
    }
  },

  data: {
    displayValue: "-"
  },

  observers: {
    "value, placeholder": function () {
      this.setData({ displayValue: this.data.value || this.data.placeholder || "-" })
    }
  },

  lifetimes: {
    attached() {
      this.setData({ displayValue: this.data.value || this.data.placeholder || "-" })
    }
  }
})
