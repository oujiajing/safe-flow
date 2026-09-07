import { describe, expect, it } from 'vitest';

import type { OrganizationNode } from '#/views/pingan/pre-shift-meeting/pre-shift-meeting.data';

import { organizationTree } from '#/views/pingan/pre-shift-meeting/pre-shift-meeting.data';

import { resolveOrganizationRelationship } from './organization.view-state';

const nodes: OrganizationNode[] = [
  {
    id: 'group-root',
    key: 'group-root',
    orgType: 'GROUP',
    title: '平安班组集团',
    children: [
      {
        id: 'group-nested',
        key: 'group-nested',
        orgType: 'GROUP',
        title: '矿业集团',
        children: [
          {
            id: 'company-nested',
            key: 'company-nested',
            orgType: 'COMPANY',
            title: '矿业一公司',
            children: [
              {
                id: 'subcompany-nested',
                key: 'subcompany-nested',
                orgType: 'COMPANY',
                title: '矿业一子公司',
              },
            ],
          },
        ],
      },
      {
        id: 'company-1',
        key: 'company-1',
        orgType: 'COMPANY',
        title: '广晟矿投',
        children: [
          {
            id: 'department-1',
            key: 'department-1',
            orgType: 'DEPARTMENT',
            title: '梅州嘉晟',
            children: [
              {
                id: 'team-1',
                key: 'team-1',
                orgType: 'TEAM',
                title: 'A班组',
              },
            ],
          },
        ],
      },
      {
        id: 'company-subonly',
        key: 'company-subonly',
        orgType: 'COMPANY',
        title: '只有子公司',
        children: [
          {
            id: 'subcompany-1',
            key: 'subcompany-1',
            orgType: 'COMPANY',
            title: '真实子公司',
          },
        ],
      },
      {
        id: 'company-2',
        key: 'company-2',
        orgType: 'COMPANY',
        title: '平安二公司',
      },
    ],
  },
];

const duplicateKeyNodes: OrganizationNode[] = [
  {
    id: 'group-with-duplicates',
    key: 'group-with-duplicates',
    orgType: 'GROUP',
    title: '重复编码集团',
    children: [
      {
        id: 'company-a-id',
        key: 'company-shared-key',
        orgType: 'COMPANY',
        title: '甲公司',
        children: [
          {
            id: 'department-a-id',
            key: 'department-shared-key',
            orgType: 'DEPARTMENT',
            title: '共享部门甲',
            children: [
              {
                id: 'team-a-id',
                key: 'team-shared-key',
                orgType: 'TEAM',
                title: '甲班组',
              },
            ],
          },
        ],
      },
      {
        id: 'company-b-id',
        key: 'company-shared-key',
        orgType: 'COMPANY',
        title: '乙公司',
        children: [
          {
            id: 'department-b-id',
            key: 'department-shared-key',
            orgType: 'DEPARTMENT',
            title: '共享部门乙',
            children: [
              {
                id: 'team-b-id',
                key: 'team-shared-key',
                orgType: 'TEAM',
                title: '乙班组',
              },
            ],
          },
        ],
      },
    ],
  },
];

describe('organization relationship view state', () => {
  it('builds a group view that follows the group chain and stops company cards at collapsed child counts', () => {
    const relation = resolveOrganizationRelationship(nodes, 'group-root');

    expect(relation.graphView?.mode).toBe('GROUP');
    expect(relation.graphView?.chain.map((item) => item.node.title)).toEqual([
      '平安班组集团',
      '矿业集团',
    ]);
    expect(relation.graphView?.groups.map((group) => group.parent.node.title)).toEqual([
      '矿业一公司',
    ]);
    expect(relation.graphView?.groups[0]?.parent.collapsedChildCount).toBe(1);
    expect(relation.graphView?.groups[0]?.children).toEqual([]);
    expect(relation.graphView?.scopeLabel).toBe('集团视图：默认展开至公司');
  });

  it('renders the Guangsheng group graph with only third-level company cards', () => {
    const relation = resolveOrganizationRelationship(organizationTree, 'group-guangsheng');

    expect(relation.graphView?.chain.map((item) => item.node.title)).toEqual([
      '广晟控股集团',
      '广晟矿业集团',
    ]);
    expect(relation.graphView?.groups.map((group) => group.parent.node.title)).toEqual([
      '广晟幕墙',
      '广晟矿投',
      '南储仓储',
      '广晟冶金',
      '瑶岭矿业',
      '阳春金同',
      '黄金集团',
      '冶金技校',
      '广晟新材',
      '广晟仓储',
      '金粤幕墙',
      '泛澳公司',
    ]);
    expect(
      relation.graphView?.groups.flatMap((group) => group.children),
    ).toEqual([]);
    expect(
      relation.graphView?.groups.map((group) => group.parent.node.title),
    ).not.toContain('广晟源成');
  });

  it('expands a clicked third-level company under the group graph without switching roots', () => {
    const relation = resolveOrganizationRelationship(
      organizationTree,
      'group-guangsheng',
      'company-mining-invest',
    );

    expect(relation.graphView?.mode).toBe('GROUP');
    expect(relation.rootNode?.title).toBe('广晟控股集团');
    expect(relation.graphView?.chain.map((item) => item.node.title)).toEqual([
      '广晟控股集团',
      '广晟矿业集团',
    ]);
    expect(
      relation.graphView?.groups
        .find((group) => group.parent.node.title === '广晟矿投')
        ?.children.map((item) => item.node.title),
    ).toEqual([
      '梅州嘉晟',
      '河源古云',
      '博泰实业',
      '潮安立源',
      '广东省冶金工业总公司',
      '广晟禾尚田',
    ]);
    expect(
      relation.graphView?.groups
        .filter((group) => group.parent.node.title !== '广晟矿投')
        .flatMap((group) => group.children),
    ).toEqual([]);
  });

  it('expands all third-level companies in the group graph when multiple ids are provided', () => {
    const relation = resolveOrganizationRelationship(organizationTree, 'group-guangsheng', [
      'company-muqian',
      'company-mining-invest',
      'company-south-storage',
    ]);

    expect(
      relation.graphView?.groups
        .find((group) => group.parent.node.title === '广晟幕墙')
        ?.children.map((item) => item.node.title),
    ).toEqual(['广晟源成']);
    expect(
      relation.graphView?.groups
        .find((group) => group.parent.node.title === '广晟矿投')
        ?.children.map((item) => item.node.title),
    ).toEqual([
      '梅州嘉晟',
      '河源古云',
      '博泰实业',
      '潮安立源',
      '广东省冶金工业总公司',
      '广晟禾尚田',
    ]);
    expect(
      relation.graphView?.groups
        .find((group) => group.parent.node.title === '南储仓储')
        ?.children.map((item) => item.node.title),
    ).toEqual(['南储运输', '佛山南储', '常州南储']);
  });

  it('includes direct company children when the selected group has no lower group chain', () => {
    const relation = resolveOrganizationRelationship(nodes, 'group-nested');

    expect(relation.graphView?.chain.map((item) => item.node.title)).toEqual([
      '矿业集团',
    ]);
    expect(relation.graphView?.groups.map((group) => group.parent.node.title)).toEqual([
      '矿业一公司',
    ]);
  });

  it('builds a company view that expands departments to team children by default', () => {
    const relation = resolveOrganizationRelationship(nodes, 'company-1');

    expect(relation.graphView?.mode).toBe('COMPANY');
    expect(relation.graphView?.chain.map((item) => item.node.title)).toEqual([
      '广晟矿投',
    ]);
    expect(relation.graphView?.groups.map((group) => group.parent.node.title)).toEqual([
      '梅州嘉晟',
    ]);
    expect(relation.graphView?.groups[0]?.parent.kindLabel).toBe('班组');
    expect(relation.graphView?.groups[0]?.children.map((item) => item.node.title)).toEqual([
      'A班组',
    ]);
    expect(relation.graphView?.scopeLabel).toBe('公司视图：默认展开至班组');
  });

  it('can collapse all department team children in a company graph', () => {
    const relation = resolveOrganizationRelationship(nodes, 'company-1', []);

    expect(relation.graphView?.mode).toBe('COMPANY');
    expect(relation.graphView?.groups.map((group) => group.parent.node.title)).toEqual([
      '梅州嘉晟',
    ]);
    expect(relation.graphView?.groups[0]?.children).toEqual([]);
  });

  it('shows real child companies when a company has no department or team children', () => {
    const relation = resolveOrganizationRelationship(nodes, 'company-subonly');

    expect(relation.graphView?.mode).toBe('COMPANY');
    expect(relation.graphView?.groups.map((group) => group.parent.node.title)).toEqual([
      '真实子公司',
    ]);
    expect(relation.graphView?.groups[0]?.parent.kindLabel).toBe('子公司');
    expect(relation.graphView?.groups[0]?.children).toEqual([]);
  });

  it('resolves the relationship root and expanded department by id before duplicate keys', () => {
    const relation = resolveOrganizationRelationship(
      duplicateKeyNodes,
      'company-b-id',
    );

    expect(relation.rootNode?.title).toBe('乙公司');
    expect(relation.graphView?.groups.map((group) => group.parent.node.title)).toEqual([
      '共享部门乙',
    ]);
    expect(relation.graphView?.groups[0]?.children.map((item) => item.node.title)).toEqual([
      '乙班组',
    ]);
  });

  it('shows a selected team as a leaf graph when no deeper layer exists', () => {
    const relation = resolveOrganizationRelationship(nodes, 'team-1');

    expect(relation.rootNode?.title).toBe('A班组');
    expect(relation.graphView?.mode).toBe('LEAF');
    expect(relation.graphView?.groups).toEqual([]);
  });
});
