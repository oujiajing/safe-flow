function isOrganizationId(value) {
  return value !== undefined && value !== null && value !== ""
}

function organizationId(node) {
  if (!node) {
    return undefined
  }
  return node.id !== undefined && node.id !== null && node.id !== "" ? node.id : node.key
}

function organizationTitle(node) {
  if (!node) {
    return ""
  }
  return node.title || node.orgName || node.name || node.label || ""
}

function organizationType(node) {
  if (!node) {
    return ""
  }
  return node.orgType || node.type || node.organizationType || ""
}

function organizationChildren(node) {
  if (!node) {
    return []
  }
  return Array.isArray(node.children) ? node.children : Array.isArray(node.nodes) ? node.nodes : []
}

function isOrganizationType(node, orgType) {
  return String(organizationType(node)).toUpperCase() === orgType
}

function flattenOrganizations(nodes) {
  if (!Array.isArray(nodes)) {
    return []
  }
  return nodes.flatMap(node => [node, ...flattenOrganizations(organizationChildren(node))])
}

function findOrganizationNode(nodes, id) {
  if (!isOrganizationId(id)) {
    return null
  }
  const normalizedId = String(id)
  for (const node of Array.isArray(nodes) ? nodes : []) {
    if (String(organizationId(node)) === normalizedId) {
      return node
    }
    const child = findOrganizationNode(organizationChildren(node), id)
    if (child) {
      return child
    }
  }
  return null
}

function findOrganizationPath(nodes, id, path = []) {
  if (!isOrganizationId(id)) {
    return []
  }
  const normalizedId = String(id)
  for (const node of Array.isArray(nodes) ? nodes : []) {
    const nextPath = [...path, node]
    if (String(organizationId(node)) === normalizedId) {
      return nextPath
    }
    const childPath = findOrganizationPath(organizationChildren(node), id, nextPath)
    if (childPath.length > 0) {
      return childPath
    }
  }
  return []
}

function nearestNodeByType(path, orgType) {
  const normalizedType = String(orgType).toUpperCase()
  return [...path].reverse().find(node => isOrganizationType(node, normalizedType)) || null
}

function resolveScopedOrganizationDefaults(nodes, orgId) {
  const path = findOrganizationPath(nodes, orgId)
  const current = path[path.length - 1]
  const company = current && isOrganizationType(current, "COMPANY")
    ? current
    : nearestNodeByType(path, "COMPANY")
  const department = current && isOrganizationType(current, "DEPARTMENT")
    ? current
    : nearestNodeByType(path, "DEPARTMENT")
  const team = current && isOrganizationType(current, "TEAM") ? current : null
  return {
    companyId: company ? organizationId(company) : undefined,
    departmentId: department ? organizationId(department) : undefined,
    teamId: team ? organizationId(team) : undefined
  }
}

function organizationOptionsFor(nodes, parentId, orgTypes) {
  const normalizedTypes = orgTypes.map(type => String(type).toUpperCase())
  const parent = findOrganizationNode(nodes, parentId)
  const roots = parent ? organizationChildren(parent) : nodes
  return flattenOrganizations(roots)
    .filter(node => isOrganizationId(organizationId(node)) && normalizedTypes.includes(String(organizationType(node)).toUpperCase()))
    .map(node => ({
      label: organizationTitle(node),
      value: organizationId(node),
      orgType: organizationType(node)
    }))
}

function optionById(options, id) {
  if (!isOrganizationId(id)) {
    return null
  }
  return options.find(option => String(option.value) === String(id)) || null
}

function resolvePickerState(nodes, selected = {}) {
  const companyOptions = organizationOptionsFor(nodes, undefined, ["COMPANY"])
  const company = optionById(companyOptions, selected.companyId) || companyOptions[0]
  if (!company) {
    return null
  }

  const departmentOptions = organizationOptionsFor(nodes, company.value, ["DEPARTMENT"])
  const department = optionById(departmentOptions, selected.departmentId) || departmentOptions[0]
  const departmentId = department ? department.value : company.value
  const teamOptions = organizationOptionsFor(nodes, departmentId, ["TEAM"])
  const team = optionById(teamOptions, selected.teamId)
  const teamId = team ? team.value : ""

  return {
    companyOptions,
    departmentOptions,
    teamOptions,
    selectedCompanyId: company.value,
    selectedDepartmentId: departmentId,
    selectedTeamId: teamId,
    companyIndex: Math.max(0, companyOptions.findIndex(option => String(option.value) === String(company.value))),
    departmentIndex: Math.max(0, departmentOptions.findIndex(option => String(option.value) === String(departmentId))),
    teamIndex: Math.max(0, teamOptions.findIndex(option => String(option.value) === String(teamId))),
    currentCompany: {
      company: company.label,
      workshop: department ? department.label : company.label
    },
    currentTeam: team ? team.label : ""
  }
}

module.exports = {
  findOrganizationPath,
  findOrganizationNode,
  flattenOrganizations,
  organizationOptionsFor,
  resolveScopedOrganizationDefaults,
  resolvePickerState
}
