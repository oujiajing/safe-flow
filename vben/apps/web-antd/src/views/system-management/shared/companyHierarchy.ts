import type { SystemManagementApi } from '#/api/system-management/types';

import type { SystemFieldConfig } from './useSystemCrudPage';

type CompanyPayload = Record<string, unknown>;

interface NormalizeCompanyPayloadOptions {
  mode: 'create' | 'edit';
  selectedPath?: SystemManagementApi.OrganizationNode[];
}

interface NormalizeCompanyPayloadResult {
  data?: CompanyPayload;
  error?: string;
}

const COMPANY_CREATE_DISABLED_MESSAGE = '四级子公司下不能继续新增公司';
const COMPANY_CREATE_HIDDEN_FIELD_KEYS = new Set([
  'companyType',
  'level1Name',
  'level2Name',
  'level3Name',
  'level4Name',
]);

function text(value: unknown) {
  return typeof value === 'string' ? value.trim() : '';
}

function compactPayload(payload: CompanyPayload) {
  return Object.fromEntries(
    Object.entries(payload).filter(
      ([, value]) => value !== '' && value !== null && value !== undefined,
    ),
  );
}

function requireText(payload: CompanyPayload, key: string, label: string) {
  return text(payload[key]) ? undefined : `${label}不能为空`;
}

function selectedDepth(path: SystemManagementApi.OrganizationNode[]) {
  return path.length;
}

export function findCompanyNodePathByKey(
  nodes: SystemManagementApi.OrganizationNode[],
  key?: string,
): SystemManagementApi.OrganizationNode[] {
  if (!key) return [];
  for (const node of nodes) {
    if (node.key === key) {
      return [node];
    }
    const childPath = findCompanyNodePathByKey(node.children ?? [], key);
    if (childPath.length > 0) {
      return [node, ...childPath];
    }
  }
  return [];
}

export function shouldDisableCompanyCreateForPath(
  selectedPath: SystemManagementApi.OrganizationNode[] = [],
) {
  const selected = selectedPath.at(-1);
  return selectedDepth(selectedPath) >= 4 || selected?.companyType === '子公司';
}

export function filterCompanyDrawerFields(
  fields: SystemFieldConfig[],
  options: { companyHierarchyMode: boolean; editing: boolean },
) {
  if (!options.companyHierarchyMode || options.editing) {
    return fields;
  }
  return fields.filter(
    (field) => !COMPANY_CREATE_HIDDEN_FIELD_KEYS.has(field.valueKey),
  );
}

function deriveCreateHierarchy(
  name: string,
  selectedPath: SystemManagementApi.OrganizationNode[],
): NormalizeCompanyPayloadResult {
  if (shouldDisableCompanyCreateForPath(selectedPath)) {
    return { error: COMPANY_CREATE_DISABLED_MESSAGE };
  }
  if (selectedPath.length === 0) {
    return {
      data: {
        companyType: '集团',
        level1Name: name,
      },
    };
  }
  if (selectedPath.length === 1) {
    return {
      data: {
        companyType: '集团',
        level1Name: selectedPath[0]?.title,
        level2Name: name,
      },
    };
  }
  if (selectedPath.length === 2) {
    return {
      data: {
        companyType: '分公司',
        level1Name: selectedPath[0]?.title,
        level2Name: selectedPath[1]?.title,
        level3Name: name,
      },
    };
  }
  return {
    data: {
      companyType: '子公司',
      level1Name: selectedPath[0]?.title,
      level2Name: selectedPath[1]?.title,
      level3Name: selectedPath[2]?.title,
      level4Name: name,
    },
  };
}

function validateCompanyPayload(payload: CompanyPayload) {
  return (
    requireText(payload, 'code', '编码') ??
    requireText(payload, 'name', '名称') ??
    requireText(payload, 'status', '状态') ??
    requireText(payload, 'companyType', '公司类型') ??
    validateCompanyHierarchy(payload)
  );
}

function validateCompanyHierarchy(payload: CompanyPayload) {
  const companyType = text(payload.companyType);
  if (companyType === '集团') {
    return text(payload.level2Name)
      ? requireText(payload, 'level2Name', '二级')
      : requireText(payload, 'level1Name', '一级');
  }
  if (companyType === '分公司') {
    return (
      requireText(payload, 'level1Name', '一级') ??
      requireText(payload, 'level2Name', '二级') ??
      requireText(payload, 'level3Name', '三级')
    );
  }
  if (companyType === '子公司') {
    return (
      requireText(payload, 'level1Name', '一级') ??
      requireText(payload, 'level2Name', '二级') ??
      requireText(payload, 'level3Name', '三级') ??
      requireText(payload, 'level4Name', '四级')
    );
  }
  return '公司类型必须是集团、分公司或子公司';
}

export function normalizeCompanyPayloadForSubmit(
  rawPayload: CompanyPayload,
  options: NormalizeCompanyPayloadOptions,
): NormalizeCompanyPayloadResult {
  const name = text(rawPayload.name);
  if (!name) {
    return { error: '名称不能为空' };
  }

  const hierarchy: NormalizeCompanyPayloadResult =
    options.mode === 'create'
      ? deriveCreateHierarchy(name, options.selectedPath ?? [])
      : { data: {} };
  if (hierarchy.error) {
    return { error: hierarchy.error };
  }

  const data = compactPayload({
    ...rawPayload,
    ...(hierarchy.data ?? {}),
  });
  const error = validateCompanyPayload(data);
  return error ? { error } : { data };
}
