import type { SystemManagementApi } from '#/api/system-management/types';

function canShowInSystemDataMap(node: SystemManagementApi.OrganizationNode) {
  const orgType = String(node.orgType ?? '').toUpperCase();
  return orgType !== 'DEPARTMENT' && orgType !== 'TEAM';
}

function normalizeNodeId(value: unknown): SystemManagementApi.Id | undefined {
  return typeof value === 'number' || typeof value === 'string'
    ? value
    : undefined;
}

function normalizeSystemDataMapNodeList(
  nodes: unknown,
): SystemManagementApi.OrganizationNode[] {
  return Array.isArray(nodes)
    ? nodes
        .filter(
          (node): node is SystemManagementApi.OrganizationNode =>
            node !== null &&
            typeof node === 'object' &&
            'id' in node &&
            'key' in node &&
            'title' in node,
        )
        .map((node) => {
          const id = normalizeNodeId(node.id);
          return id === undefined
            ? undefined
            : {
                children: normalizeSystemDataMapNodeList(node.children),
                ...(typeof node.companyType === 'string'
                  ? { companyType: node.companyType }
                  : {}),
                id,
                key: String(node.key),
                orgType: String(node.orgType ?? ''),
                title: String(node.title),
              };
        })
        .filter(Boolean)
        .map((node) => node as SystemManagementApi.OrganizationNode)
        .filter(canShowInSystemDataMap)
    : [];
}

export function normalizeSystemDataMapNodes(
  nodes: unknown,
): SystemManagementApi.OrganizationNode[] {
  return normalizeSystemDataMapNodeList(nodes);
}

export function collectExpandableSystemDataMapKeys(
  nodes: SystemManagementApi.OrganizationNode[],
): string[] {
  return nodes.flatMap((node) => [
    ...(node.children?.length ? [node.key] : []),
    ...collectExpandableSystemDataMapKeys(node.children ?? []),
  ]);
}
