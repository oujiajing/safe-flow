import type {
  OrganizationId,
  OrganizationNode,
} from '#/views/pingan/pre-shift-meeting/pre-shift-meeting.data';

export type OrganizationKind = 'COMPANY' | 'DEPARTMENT' | 'GROUP' | 'TEAM' | string;

export type RelationGraphMode = 'COMPANY' | 'GROUP' | 'LEAF';

export type RelationExpandedChildIds =
  | Array<OrganizationId | string>
  | OrganizationId
  | string
  | undefined;

export interface RelationGraphNode {
  collapsedChildCount: number;
  kindLabel: string;
  node: OrganizationNode;
}

export interface RelationGraphGroup {
  children: RelationGraphNode[];
  parent: RelationGraphNode;
}

export interface RelationGraphViewModel {
  chain: RelationGraphNode[];
  footerHint: string;
  groups: RelationGraphGroup[];
  mode: RelationGraphMode;
  root: RelationGraphNode;
  scopeLabel: string;
}

export interface OrganizationPathMatch {
  node: OrganizationNode;
  path: OrganizationNode[];
}

export interface OrganizationRelationship {
  graphView?: RelationGraphViewModel;
  path: OrganizationNode[];
  primaryNodes: OrganizationNode[];
  rootNode?: OrganizationNode;
  secondaryNodes: OrganizationNode[];
  selectedNode?: OrganizationNode;
  treeNode?: OrganizationNode;
}

export function organizationType(node?: OrganizationNode): OrganizationKind {
  return String(node?.orgType ?? '').toUpperCase();
}

export function findOrganizationPathById(
  nodes: OrganizationNode[],
  id?: OrganizationId,
  path: OrganizationNode[] = [],
): OrganizationPathMatch | undefined {
  if (id === undefined) return undefined;
  for (const node of nodes) {
    const currentPath = [...path, node];
    if (String(node.id) === String(id)) {
      return { node, path: currentPath };
    }
    const child = findOrganizationPathById(node.children ?? [], id, currentPath);
    if (child) return child;
  }
  return undefined;
}

export function findOrganizationPathByKey(
  nodes: OrganizationNode[],
  key?: string,
  path: OrganizationNode[] = [],
): OrganizationPathMatch | undefined {
  if (!key) return undefined;
  for (const node of nodes) {
    const currentPath = [...path, node];
    if (node.key === key) {
      return { node, path: currentPath };
    }
    const child = findOrganizationPathByKey(node.children ?? [], key, currentPath);
    if (child) return child;
  }
  return undefined;
}

export function findFirstNodeByType(
  nodes: OrganizationNode[],
  type: OrganizationKind,
): OrganizationNode | undefined {
  for (const node of nodes) {
    if (organizationType(node) === type) return node;
    const child = findFirstNodeByType(node.children ?? [], type);
    if (child) return child;
  }
  return undefined;
}

export function collectDirectChildrenByType(
  node: OrganizationNode | undefined,
  type: OrganizationKind,
) {
  return (node?.children ?? []).filter((child) => organizationType(child) === type);
}

export function collectDescendantsByType(
  node: OrganizationNode | undefined,
  type: OrganizationKind,
): OrganizationNode[] {
  if (!node) return [];
  return (node.children ?? []).flatMap((child) => [
    ...(organizationType(child) === type ? [child] : []),
    ...collectDescendantsByType(child, type),
  ]);
}

export function countNodesByType(
  node: OrganizationNode,
  type: OrganizationKind,
): number {
  return (
    (organizationType(node) === type ? 1 : 0) +
    (node.children ?? []).reduce(
      (total, child) => total + countNodesByType(child, type),
      0,
    )
  );
}

function directVisibleChildren(node: OrganizationNode | undefined) {
  const children = node?.children ?? [];
  return children.filter((child) =>
    ['COMPANY', 'DEPARTMENT', 'GROUP', 'SCHOOL', 'TEAM'].includes(
      organizationType(child),
    ),
  );
}

function buildVisibleTree(node: OrganizationNode): OrganizationNode {
  return {
    ...node,
    children: directVisibleChildren(node).map((child) => buildVisibleTree(child)),
  };
}

function findOrganizationPathByIdOrKey(
  nodes: OrganizationNode[],
  idOrKey?: OrganizationId | string,
) {
  return (
    findOrganizationPathById(nodes, idOrKey) ??
    findOrganizationPathByKey(nodes, idOrKey === undefined ? undefined : String(idOrKey))
  );
}

function relationKindLabel(node: OrganizationNode, companyAsChild = false) {
  const type = organizationType(node);
  if (type === 'GROUP') return '集团';
  if (type === 'DEPARTMENT') return '部门';
  if (type === 'TEAM') return '班组';
  if (type === 'SCHOOL') return '学校';
  if (type === 'COMPANY') return companyAsChild ? '子公司' : '公司';
  return '组织';
}

function createRelationGraphNode(
  node: OrganizationNode,
  options: {
    collapsedChildCount?: number;
    companyAsChild?: boolean;
    kindLabel?: string;
  } = {},
): RelationGraphNode {
  return {
    collapsedChildCount:
      options.collapsedChildCount ?? directVisibleChildren(node).length,
    kindLabel: options.kindLabel ?? relationKindLabel(node, options.companyAsChild),
    node,
  };
}

function collectDefaultGroupChain(root: OrganizationNode) {
  const chain = [root];
  let current = root;

  while (true) {
    const nextGroup = directVisibleChildren(current).find(
      (child) => organizationType(child) === 'GROUP',
    );
    if (!nextGroup) break;
    chain.push(nextGroup);
    current = nextGroup;
  }

  return chain;
}

function isSameOrganizationIdOrKey(
  node: OrganizationNode,
  idOrKey?: OrganizationId | string,
) {
  if (idOrKey === undefined) return false;
  return String(node.id) === String(idOrKey) || node.key === String(idOrKey);
}

function isExpandedNode(
  node: OrganizationNode,
  expandedChildIdOrKey: RelationExpandedChildIds,
  defaultExpanded: boolean,
) {
  if (expandedChildIdOrKey === undefined) return defaultExpanded;
  const expandedIds = Array.isArray(expandedChildIdOrKey)
    ? expandedChildIdOrKey
    : [expandedChildIdOrKey];
  return expandedIds.some((idOrKey) => isSameOrganizationIdOrKey(node, idOrKey));
}

function buildGroupGraph(
  root: OrganizationNode,
  expandedChildIdOrKey?: RelationExpandedChildIds,
): RelationGraphViewModel {
  const chainNodes = collectDefaultGroupChain(root);
  const displayRoot = chainNodes.at(-1) ?? root;
  const companyCards = directVisibleChildren(displayRoot).filter((child) =>
    ['COMPANY', 'SCHOOL'].includes(organizationType(child)),
  );
  const groups = companyCards.map((node) => ({
    children: isExpandedNode(node, expandedChildIdOrKey, false)
      ? directVisibleChildren(node)
          .filter((child) =>
            ['COMPANY', 'SCHOOL'].includes(organizationType(child)),
          )
          .map((child) =>
            createRelationGraphNode(child, {
              companyAsChild: organizationType(child) === 'COMPANY',
            }),
          )
      : [],
    parent: createRelationGraphNode(node),
  }));
  const rootGraphNode = createRelationGraphNode(root);

  return {
    chain: chainNodes.map((node) => createRelationGraphNode(node)),
    footerHint: '当前视图仅展示公司层级，点击公司节点可展开查看下级组织',
    groups,
    mode: 'GROUP',
    root: rootGraphNode,
    scopeLabel: '集团视图：默认展开至公司',
  };
}

function buildCompanyGroups(
  root: OrganizationNode,
  expandedChildIdOrKey?: RelationExpandedChildIds,
): RelationGraphGroup[] {
  const children = directVisibleChildren(root);
  const departments = children.filter((child) => organizationType(child) === 'DEPARTMENT');
  if (departments.length > 0) {
    return departments.map((department) => {
      const teams = directVisibleChildren(department).filter(
        (child) => organizationType(child) === 'TEAM',
      );
      return {
        children: isExpandedNode(department, expandedChildIdOrKey, true)
          ? teams.map((team) => createRelationGraphNode(team))
          : [],
        parent: createRelationGraphNode(department, {
          collapsedChildCount: teams.length,
          kindLabel: '班组',
        }),
      };
    });
  }

  const teams = children.filter((child) => organizationType(child) === 'TEAM');
  if (teams.length > 0) {
    return teams.map((team) => ({
      children: [],
      parent: createRelationGraphNode(team),
    }));
  }

  return children.map((child) => ({
    children: isExpandedNode(child, expandedChildIdOrKey, true)
      ? directVisibleChildren(child)
          .filter((grandchild) => organizationType(grandchild) === 'TEAM')
          .map((team) => createRelationGraphNode(team))
      : [],
    parent: createRelationGraphNode(child, {
      companyAsChild: organizationType(child) === 'COMPANY',
    }),
  }));
}

function buildCompanyGraph(
  root: OrganizationNode,
  expandedChildIdOrKey?: RelationExpandedChildIds,
): RelationGraphViewModel {
  const rootGraphNode = createRelationGraphNode(root);

  return {
    chain: [rootGraphNode],
    footerHint: '公司视图按真实组织层级展开；暂无班组时展示现有下级组织',
    groups: buildCompanyGroups(root, expandedChildIdOrKey),
    mode: 'COMPANY',
    root: rootGraphNode,
    scopeLabel: '公司视图：默认展开至班组',
  };
}

function buildLeafGraph(root: OrganizationNode): RelationGraphViewModel {
  const rootGraphNode = createRelationGraphNode(root);

  return {
    chain: [rootGraphNode],
    footerHint: '当前组织暂无可展开的下级关系',
    groups: [],
    mode: 'LEAF',
    root: rootGraphNode,
    scopeLabel: `${rootGraphNode.kindLabel}视图`,
  };
}

function buildRelationGraph(
  root: OrganizationNode,
  expandedChildIdOrKey?: RelationExpandedChildIds,
): RelationGraphViewModel {
  const type = organizationType(root);
  if (type === 'GROUP') return buildGroupGraph(root, expandedChildIdOrKey);
  if (type === 'COMPANY') return buildCompanyGraph(root, expandedChildIdOrKey);
  if (type === 'SCHOOL') return buildCompanyGraph(root, expandedChildIdOrKey);
  return buildLeafGraph(root);
}

export function resolveOrganizationRelationship(
  nodes: OrganizationNode[],
  selectedIdOrKey?: OrganizationId | string,
  expandedChildIdOrKey?: RelationExpandedChildIds,
): OrganizationRelationship {
  const match = findOrganizationPathByIdOrKey(nodes, selectedIdOrKey);
  const selectedNode = match?.node ?? nodes[0] ?? findFirstNodeByType(nodes, 'COMPANY');
  const path = match?.path ?? (selectedNode ? [selectedNode] : []);

  if (!selectedNode) {
    return { path: [], primaryNodes: [], secondaryNodes: [] };
  }

  const primaryNodes = directVisibleChildren(selectedNode);
  const expandedChild = primaryNodes.find(
    (node) =>
      !Array.isArray(expandedChildIdOrKey) &&
      (String(node.id) === String(expandedChildIdOrKey) ||
        node.key === String(expandedChildIdOrKey)),
  );

  return {
    graphView: buildRelationGraph(selectedNode, expandedChildIdOrKey),
    path,
    primaryNodes,
    rootNode: selectedNode,
    secondaryNodes: directVisibleChildren(expandedChild),
    selectedNode,
    treeNode: buildVisibleTree(selectedNode),
  };
}
