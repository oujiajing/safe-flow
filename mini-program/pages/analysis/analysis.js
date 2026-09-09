const { createAnalysisPage } = require("../../utils/analysis-page")
const { syncTabBarSelected } = require("../../utils/tab-bar")

const analysisPage = createAnalysisPage({ type: "overview" })
const sharedOnShow = analysisPage.onShow
analysisPage.onShow = function onShow() {
  syncTabBarSelected(this, "pages/analysis/analysis")
  return sharedOnShow.call(this)
}

Page(analysisPage)
