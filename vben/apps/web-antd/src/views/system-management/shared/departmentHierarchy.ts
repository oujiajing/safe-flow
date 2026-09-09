import type { SystemManagementApi } from '#/api/system-management/types';

import type { SystemFieldConfig } from './useSystemCrudPage';

type DepartmentPayload = Record<string, unknown>;

interface NormalizeDepartmentPayloadOptions {
  selectedPath?: SystemManagementApi.OrganizationNode[];
}

interface NormalizeDepartmentPayloadResult {
  data?: DepartmentPayload;
  error?: string;
}

const DEPARTMENT_CREATE_HIDDEN_FIELD_KEYS = new Set([
  'companyOrgId',
  'topLevelName',
  'groupName',
  'level1Unit',
  'level2Unit',
  'companySortOrder',
]);

function text(value: unknown) {
  return typeof value === 'string' ? value.trim() : '';
}

function compactPayload(payload: DepartmentPayload) {
  return Object.fromEntries(
    Object.entries(payload).filter(
      ([, value]) => value !== '' && value !== null && value !== undefined,
    ),
  );
}

function requireText(payload: DepartmentPayload, key: string, label: string) {
  return text(payload[key]) ? undefined : `${label}不能为空`;
}

function pathIdSet(orgPath?: string) {
  return new Set(
    String(orgPath ?? '')
      .split('/')
      .map((item) => item.trim())
      .filter(Boolean),
  );
}

export function filterDepartmentDrawerFields(
  fields: SystemFieldConfig[],
  options: { departmentHierarchyMode: boolean; editing: boolean },
) {
  if (!options.departmentHierarchyMode || options.editing) {
    return fields;
  }
  return fields.filter(
    (field) => !DEPARTMENT_CREATE_HIDDEN_FIELD_KEYS.has(field.valueKey),
  );
}

export function findDefaultDepartmentCompanyPath(
  nodes: SystemManagementApi.OrganizationNode[],
  orgPath?: string,
) {
  const ids = pathIdSet(orgPath);
  let bestPath: SystemManagementApi.OrganizationNode[] = [];

  function visit(
    currentNodes: SystemManagementApi.OrganizationNode[],
    currentPath: SystemManagementApi.OrganizationNode[],
  ) {
    for (const node of currentNodes) {
      const nextPath = [...currentPath, node];
      if (ids.has(String(node.id)) && nextPath.length >= bestPath.length) {
        bestPath = nextPath;
      }
      visit(node.children ?? [], nextPath);
    }
  }

  visit(nodes, []);
  return bestPath;
}

function departmentHierarchyFromPath(
  selectedPath: SystemManagementApi.OrganizationNode[],
): NormalizeDepartmentPayloadResult {
  const company = selectedPath.at(-1);
  if (!company) {
    return { error: '未找到当前登录组织对应公司' };
  }
  return {
    data: {
      companyOrgId: company.id,
      groupName: selectedPath[1]?.title,
      level1Unit: selectedPath[2]?.title,
      level2Unit: selectedPath[3]?.title,
      topLevelName: selectedPath[0]?.title,
    },
  };
}

function validateDepartmentPayload(payload: DepartmentPayload) {
  return (
    requireText(payload, 'code', '编码') ??
    requireText(payload, 'name', '名称') ??
    requireText(payload, 'status', '状态') ??
    (payload.companyOrgId === undefined ? '所属公司不能为空' : undefined)
  );
}

export function normalizeDepartmentPayloadForSubmit(
  rawPayload: DepartmentPayload,
  options: NormalizeDepartmentPayloadOptions,
): NormalizeDepartmentPayloadResult {
  const hierarchy = departmentHierarchyFromPath(options.selectedPath ?? []);
  if (hierarchy.error) {
    return { error: hierarchy.error };
  }
  const data = compactPayload({
    ...rawPayload,
    ...(hierarchy.data ?? {}),
  });
  const error = validateDepartmentPayload(data);
  return error ? { error } : { data };
}
