import type { DataMapNode } from './data-map-state';

export type PinganOrganizationFilterKey = 'company' | 'department' | 'team';
export type PinganOrganizationId = number | string;

export interface PinganOrganizationDefaults {
  companyId?: PinganOrganizationId;
  departmentId?: PinganOrganizationId;
  teamId?: PinganOrganizationId;
}

const memberRoleCodes = new Set(['CURTAIN_WALL_MEMBER', 'TEAM_MEMBER']);
const departmentScopedRoleCodes = new Set([
  'DEPARTMENT_MANAGER',
  'WORKSHOP_DIRECTOR',
]);
const companyScopedRoleCodes = new Set(['COMPANY_LEADER', 'ENTERPRISE_LEADER']);

function hasAnyRole(roles: readonly string[] | undefined, roleCodes: Set<string>) {
  return (roles ?? []).some((role) => roleCodes.has(role));
}

export function isPinganMemberRole(roles: readonly string[] | undefined) {
  return hasAnyRole(roles, memberRoleCodes);
}

export function isPinganDepartmentScopedRole(
  roles: readonly string[] | undefined,
) {
  return hasAnyRole(roles, departmentScopedRoleCodes);
}

export function isPinganCompanyScopedRole(
  roles: readonly string[] | undefined,
) {
  return hasAnyRole(roles, companyScopedRoleCodes);
}

export function getVisiblePinganOrganizationFilterKeys(
  roles: readonly string[] | undefined,
): PinganOrganizationFilterKey[] {
  return isPinganMemberRole(roles) ? [] : ['company', 'department', 'team'];
}

export function isPinganOrganizationFilterLocked(
  roles: readonly string[] | undefined,
  field: PinganOrganizationFilterKey,
) {
  if (isPinganMemberRole(roles)) {
    return true;
  }
  if (isPinganDepartmentScopedRole(roles)) {
    return field === 'company' || field === 'department';
  }
  if (isPinganCompanyScopedRole(roles)) {
    return field === 'company';
  }
  return false;
}

export function resolveScopedPinganOrganizationDefaults(
  orgId: PinganOrganizationId | undefined,
  nodes: DataMapNode[],
): PinganOrganizationDefaults {
  if (orgId === undefined || orgId === null) {
    return {};
  }
  const path = findDataMapNodePath(nodes, orgId);
  const current = path.at(-1);
  const company =
    current?.orgType === 'COMPANY'
      ? current
      : nearestNodeByType(path, 'COMPANY');
  const department =
    current?.orgType === 'DEPARTMENT'
      ? current
      : nearestNodeByType(path, 'DEPARTMENT');
  const team = current?.orgType === 'TEAM' ? current : undefined;
  return {
    companyId: company?.id,
    departmentId: department?.id,
    teamId: team?.id,
  };
}

export function applyScopedPinganOrganizationDefaults<T extends PinganOrganizationDefaults>(
  target: T,
  defaults: PinganOrganizationDefaults,
  roles: readonly string[] | undefined,
) {
  const next = { ...target };
  if (
    isPinganOrganizationFilterLocked(roles, 'company') &&
    defaults.companyId !== undefined
  ) {
    next.companyId = defaults.companyId;
  }
  if (
    isPinganOrganizationFilterLocked(roles, 'department') &&
    defaults.departmentId !== undefined
  ) {
    next.departmentId = defaults.departmentId;
  }
  if (
    isPinganOrganizationFilterLocked(roles, 'team') &&
    defaults.teamId !== undefined
  ) {
    next.teamId = defaults.teamId;
  }
  return next;
}

function findDataMapNodePath(
  nodes: DataMapNode[],
  id: PinganOrganizationId,
  path: DataMapNode[] = [],
): DataMapNode[] {
  for (const node of nodes) {
    const nextPath = [...path, node];
    if (String(node.id) === String(id)) {
      return nextPath;
    }
    const childPath = findDataMapNodePath(node.children ?? [], id, nextPath);
    if (childPath.length > 0) {
      return childPath;
    }
  }
  return [];
}

function nearestNodeByType(path: DataMapNode[], orgType: string) {
  return [...path].reverse().find((node) => node.orgType === orgType);
}
