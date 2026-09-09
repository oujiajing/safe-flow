import type { SystemManagementApi } from '#/api/system-management/types';

import type {
  SystemCrudColumn,
  SystemFieldConfig,
  SystemFieldOption,
} from '../shared/useSystemCrudPage';

const statusOptions: SystemFieldOption[] = [
  { label: '启用', value: 'ACTIVE' },
  { label: '停用', value: 'INACTIVE' },
];

export const accountColumns: SystemCrudColumn[] = [
  { dataIndex: 'username', fixed: 'left', title: '用户名', width: 140 },
  { dataIndex: 'realName', title: '姓名', width: 120 },
  { dataIndex: 'mobile', title: '手机号', width: 140 },
  { dataIndex: 'orgName', title: '组织', width: 180 },
  { dataIndex: 'roles', title: '角色', width: 220 },
  { dataIndex: 'status', title: '状态', width: 100 },
  { dataIndex: 'lastLoginAt', title: '最近登录', width: 170 },
];

export function buildAccountFields(options: {
  organizations: SystemFieldOption[];
  roles: SystemFieldOption[];
}): SystemFieldConfig[] {
  return [
    { label: '用户名', required: true, valueKey: 'username' },
    { label: '初始密码', valueKey: 'password' },
    { label: '姓名', required: true, valueKey: 'realName' },
    { label: '手机号', valueKey: 'mobile' },
    {
      component: 'tree-select',
      dropdownMatchSelectWidth: 520,
      label: '组织',
      listHeight: 520,
      options: options.organizations,
      placeholder: '请选择组织',
      required: true,
      showSearch: true,
      treeDefaultExpandAll: true,
      valueKey: 'orgId',
    },
    {
      component: 'select',
      label: '角色',
      mode: 'multiple',
      options: options.roles,
      valueKey: 'roleIds',
    },
    {
      component: 'select',
      label: '状态',
      options: statusOptions,
      required: true,
      valueKey: 'status',
    },
  ];
}

export function normalizeRoleOptions(roles: unknown): SystemFieldOption[] {
  const rows = Array.isArray(roles)
    ? roles
    : roles && typeof roles === 'object' && 'items' in roles
      ? (roles as SystemManagementApi.PageResult<SystemManagementApi.Role>).items
      : [];

  return rows
    .filter(
      (role): role is SystemManagementApi.Role =>
        role !== null && typeof role === 'object' && 'id' in role,
    )
    .map((role) => ({
      label: role.roleName || role.name || role.roleCode || String(role.id),
      value: role.id,
    }));
}

export function flattenOrganizationOptions(
  nodes: SystemManagementApi.OrganizationNode[],
  parents: string[] = [],
): SystemFieldOption[] {
  return nodes.flatMap((node) => {
    const labelParts = [...parents, node.title].filter(Boolean);
    return [
      { label: labelParts.join(' / '), value: node.id },
      ...flattenOrganizationOptions(node.children ?? [], labelParts),
    ];
  });
}

export function normalizeOrganizationTreeOptions(
  nodes: SystemManagementApi.OrganizationNode[],
): SystemFieldOption[] {
  return nodes.map((node) => ({
    children:
      node.children && node.children.length > 0
        ? normalizeOrganizationTreeOptions(node.children)
        : undefined,
    label: node.title,
    value: node.id,
  }));
}

export function scopeOrganizationTreeToCompany(
  nodes: SystemManagementApi.OrganizationNode[],
  companyId?: SystemManagementApi.Id,
): SystemManagementApi.OrganizationNode[] {
  if (companyId === undefined || companyId === null || companyId === '') {
    return nodes;
  }
  const targetId = String(companyId);
  for (const node of nodes) {
    if (String(node.id) === targetId) {
      return [node];
    }
    const scopedChildren = scopeOrganizationTreeToCompany(
      node.children ?? [],
      companyId,
    );
    if (scopedChildren.length > 0) {
      return scopedChildren;
    }
  }
  return nodes;
}

export function formatAccountRoleNames(
  account: Pick<SystemManagementApi.Account, 'roles'>,
) {
  const roles = Array.isArray(account.roles) ? account.roles : [];
  return roles
    .map((role) => {
      if (typeof role === 'string') return role;
      return role.roleName || role.name || role.roleCode || String(role.id);
    })
    .filter(Boolean);
}
