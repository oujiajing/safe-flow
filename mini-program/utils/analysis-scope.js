function nodeId(node) {
  return node && node.id !== undefined && node.id !== null ? node.id : ""
}

function nodeName(node, fallback = "未命名组织") {
  return node && (node.name || node.title) || fallback
}

function nodeType(node) {
  return String(node && (node.orgType || node.type) || "").toUpperCase()
}

function childrenOf(node) {
  return Array.isArray(node && node.children) ? node.children : []
}

function walk(nodes, visitor, path = []) {
  for (const node of Array.isArray(nodes) ? nodes : []) {
    const nextPath = [...path, node]
    visitor(node, nextPath)
    walk(childrenOf(node), visitor, nextPath)
  }
}

function findPath(nodes, id) {
  if (id === "" || id === undefined || id === null) return []
  let result = []
  walk(nodes, (node, path) => {
    if (result.length === 0 && String(nodeId(node)) === String(id)) result = path
  })
  return result
}

function nodesByType(nodes, type) {
  const result = []
  walk(nodes, node => {
    if (nodeType(node) === type) result.push(node)
  })
  return result
}

function descendantsByType(node, type) {
  return nodesByType(childrenOf(node), type)
}

function option(node) {
  return { id: nodeId(node), name: nodeName(node), label: nodeName(node), orgType: nodeType(node) }
}

function optionIndex(options, id) {
  const index = options.findIndex(item => String(item.id) === String(id))
  return index < 0 ? 0 : index
}

function nodeFromOption(nodes, selected) {
  const path = findPath(nodes, selected && selected.id)
  return path[path.length - 1] || null
}

function nearest(path, type) {
  return [...path].reverse().find(node => nodeType(node) === type) || null
}

function scopeTarget(id, name) {
  return { orgId: id, orgName: name }
}

function buildScopeState(nodes, context = {}, access = {}) {
  const tree = Array.isArray(nodes) ? nodes : []
  const global = Boolean(access.global)
  const accessPath = findPath(tree, access.orgId)
  const accessNode = accessPath[accessPath.length - 1] || null
  const accessType = nodeType(accessNode)
  const fallbackId = global ? "" : nodeId(accessNode)
  const fallbackName = global ? "全部可见组织" : nodeName(accessNode, "当前组织")
  let targetPath = findPath(tree, context.orgId)

  if (!global && accessNode && !targetPath.some(node => String(nodeId(node)) === String(nodeId(accessNode)))) {
    targetPath = accessPath
  }

  const targetNode = targetPath[targetPath.length - 1] || null
  const targetCompany = nearest(targetPath, "COMPANY")
  const targetDepartment = nearest(targetPath, "DEPARTMENT")
  const targetTeam = nearest(targetPath, "TEAM")
  const accessCompany = nearest(accessPath, "COMPANY")
  const accessDepartment = nearest(accessPath, "DEPARTMENT")

  const showCompany = global
  const showDepartment = global || accessType === "COMPANY"
  const showTeam = global || accessType === "COMPANY" || accessType === "DEPARTMENT"

  const companyNodes = nodesByType(tree, "COMPANY")
  const companyOptions = global
    ? [{ id: "", name: "全部公司", label: "全部公司", orgType: "ALL" }, ...companyNodes.map(option)]
    : []
  const selectedCompany = global ? targetCompany : accessCompany
  const selectedCompanyId = selectedCompany ? nodeId(selectedCompany) : ""

  const departmentRoot = selectedCompany || (!global ? accessCompany : null)
  const departmentNodes = departmentRoot
    ? descendantsByType(departmentRoot, "DEPARTMENT")
    : nodesByType(tree, "DEPARTMENT")
  const departmentAllId = departmentRoot ? nodeId(departmentRoot) : ""
  const departmentOptions = showDepartment
    ? [{ id: departmentAllId, name: "全部部门", label: "全部部门", orgType: "ALL" }, ...departmentNodes.map(option)]
    : []
  const selectedDepartment = targetDepartment || (!global ? accessDepartment : null)
  const selectedDepartmentId = selectedDepartment ? nodeId(selectedDepartment) : departmentAllId

  const teamRoot = selectedDepartment || departmentRoot || (!global ? accessNode : null)
  const teamNodes = teamRoot
    ? descendantsByType(teamRoot, "TEAM")
    : nodesByType(tree, "TEAM")
  const teamAllId = teamRoot ? nodeId(teamRoot) : fallbackId
  const teamOptions = showTeam
    ? [{ id: teamAllId, name: "全部班组", label: "全部班组", orgType: "ALL" }, ...teamNodes.map(option)]
    : []
  const selectedTeamId = targetTeam ? nodeId(targetTeam) : teamAllId

  return {
    showCompany,
    showDepartment,
    showTeam,
    fixedOrgName: !showCompany && !showDepartment && !showTeam
      ? nodeName(accessNode, context.orgName || "当前班组")
      : "",
    companyOptions,
    departmentOptions,
    teamOptions,
    companyIndex: optionIndex(companyOptions, selectedCompanyId),
    departmentIndex: optionIndex(departmentOptions, selectedDepartmentId),
    teamIndex: optionIndex(teamOptions, selectedTeamId),
    fallback: scopeTarget(fallbackId, fallbackName),
    target: targetNode ? scopeTarget(nodeId(targetNode), nodeName(targetNode)) : scopeTarget(fallbackId, fallbackName)
  }
}

function selectionContext(nodes, state, level, index) {
  const options = state[`${level}Options`] || []
  const selected = options[Number(index) || 0] || options[0]
  if (!selected) return state.fallback
  if (selected.orgType === "ALL") {
    if (selected.id === "") return scopeTarget("", "全部可见组织")
    const parent = nodeFromOption(nodes, selected)
    return parent ? scopeTarget(nodeId(parent), nodeName(parent)) : state.fallback
  }
  return scopeTarget(selected.id, selected.name)
}

module.exports = {
  buildScopeState,
  findPath,
  nodeType,
  nodesByType,
  selectionContext
}
