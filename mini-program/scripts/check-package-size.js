const fs = require("node:fs")
const path = require("node:path")

const projectRoot = path.resolve(__dirname, "..")
const appConfig = readJson(path.join(projectRoot, "app.json"))
const MAX_MAIN_PACKAGE_BYTES = 2 * 1024 * 1024
const MAX_SUBPACKAGE_BYTES = 2 * 1024 * 1024
const MAX_LOCAL_IMAGE_BYTES = 300 * 1024
const MAX_SUBPACKAGE_COUNT = 20
const PAGE_EXTENSIONS = [".js", ".json", ".wxml", ".wxss"]
const IMAGE_EXTENSIONS = new Set([".jpg", ".jpeg", ".png", ".webp", ".gif", ".svg"])

function readJson(file) {
  return JSON.parse(fs.readFileSync(file, "utf8"))
}

function fileSize(target) {
  if (!fs.existsSync(target)) return 0
  const stat = fs.statSync(target)
  if (stat.isFile()) return stat.size
  return fs.readdirSync(target).reduce((total, item) => total + fileSize(path.join(target, item)), 0)
}

function walkFiles(target) {
  if (!fs.existsSync(target)) return []
  const stat = fs.statSync(target)
  if (stat.isFile()) return [target]
  return fs.readdirSync(target).flatMap(item => walkFiles(path.join(target, item)))
}

function routeList() {
  const mainPages = appConfig.pages || []
  const subpackagePages = (appConfig.subPackages || []).flatMap(subpackage =>
    (subpackage.pages || []).map(page => `${subpackage.root}/${page}`)
  )
  return [...mainPages, ...subpackagePages]
}

function formatBytes(bytes) {
  return `${(bytes / 1024 / 1024).toFixed(2)}MB`
}

const errors = []
const routes = routeList()
const duplicateRoutes = routes.filter((route, index) => routes.indexOf(route) !== index)
if (duplicateRoutes.length) {
  errors.push(`发现重复页面路由：${[...new Set(duplicateRoutes)].join(", ")}`)
}

for (const route of routes) {
  for (const extension of PAGE_EXTENSIONS) {
    const file = path.join(projectRoot, `${route}${extension}`)
    if (!fs.existsSync(file)) errors.push(`页面文件不存在：${path.relative(projectRoot, file)}`)
  }
}

const subpackages = appConfig.subPackages || []
if (subpackages.length > MAX_SUBPACKAGE_COUNT) {
  errors.push(`分包数量 ${subpackages.length} 超过限制 ${MAX_SUBPACKAGE_COUNT}`)
}

const mainPageDirectories = (appConfig.pages || []).map(route =>
  path.join(projectRoot, "pages", route.split("/")[1])
)
const conservativeMainTargets = [
  "app.js",
  "app.json",
  "app.wxss",
  "sitemap.json",
  "assets",
  "static",
  "components",
  "custom-tab-bar",
  "config",
  "mappers",
  "services",
  "styles",
  "utils",
  "miniprogram_npm"
].map(item => path.join(projectRoot, item))

const conservativeMainBytes = [...new Set([...conservativeMainTargets, ...mainPageDirectories])]
  .reduce((total, target) => total + fileSize(target), 0)
if (conservativeMainBytes > MAX_MAIN_PACKAGE_BYTES) {
  errors.push(`主包保守估算 ${formatBytes(conservativeMainBytes)} 超过 ${formatBytes(MAX_MAIN_PACKAGE_BYTES)}`)
}

for (const subpackage of subpackages) {
  const bytes = fileSize(path.join(projectRoot, subpackage.root))
  if (bytes > MAX_SUBPACKAGE_BYTES) {
    errors.push(`分包 ${subpackage.root} 为 ${formatBytes(bytes)}，超过 ${formatBytes(MAX_SUBPACKAGE_BYTES)}`)
  }
}

for (const file of walkFiles(path.join(projectRoot, "assets")).concat(walkFiles(path.join(projectRoot, "static")))) {
  if (!IMAGE_EXTENSIONS.has(path.extname(file).toLowerCase())) continue
  const bytes = fs.statSync(file).size
  if (bytes > MAX_LOCAL_IMAGE_BYTES) {
    errors.push(`本地图片超过 300KB：${path.relative(projectRoot, file)} (${formatBytes(bytes)})`)
  }
}

for (const tab of (appConfig.tabBar && appConfig.tabBar.list) || []) {
  if (!(appConfig.pages || []).includes(tab.pagePath)) {
    errors.push(`TabBar 页面必须位于主包：${tab.pagePath}`)
  }
}

console.log(`主包保守估算：${formatBytes(conservativeMainBytes)} / ${formatBytes(MAX_MAIN_PACKAGE_BYTES)}`)
console.log(`主包页面：${(appConfig.pages || []).length}，分包：${subpackages.length}，分包页面：${routes.length - (appConfig.pages || []).length}`)

if (errors.length) {
  console.error(errors.map(error => `- ${error}`).join("\n"))
  process.exitCode = 1
} else {
  console.log("小程序包体积与路由检查通过")
}
