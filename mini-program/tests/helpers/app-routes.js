function registeredPages(appConfig) {
  const mainPages = Array.isArray(appConfig.pages) ? appConfig.pages : []
  const subpackagePages = (appConfig.subPackages || []).flatMap(subpackage =>
    (subpackage.pages || []).map(page => `${subpackage.root}/${page}`)
  )
  return [...mainPages, ...subpackagePages]
}

module.exports = {
  registeredPages
}
