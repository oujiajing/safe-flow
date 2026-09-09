<script setup lang="ts">
import { computed, ref } from 'vue';

import { VbenScrollbar } from '@vben/common-ui';

import { Button, Input, Tree } from 'ant-design-vue';

import type { SystemManagementApi } from '#/api/system-management/types';

const props = withDefaults(defineProps<{
  collapsed?: boolean;
  expandedKeys: string[];
  nodes: SystemManagementApi.OrganizationNode[];
  selectedKeys: string[];
}>(), {
  collapsed: false,
});

const emit = defineEmits<{
  selectOrganization: [id?: SystemManagementApi.Id, keys?: string[]];
  toggle: [];
  updateExpandedKeys: [keys: string[]];
}>();

const treeKeyword = ref('');

const filteredTree = computed(() =>
  filterOrganizationTree(props.nodes, treeKeyword.value),
);

const toggleLabel = computed(() => (props.collapsed ? '展开' : '收起'));

function filterOrganizationTree(
  nodes: SystemManagementApi.OrganizationNode[],
  keyword: string,
): SystemManagementApi.OrganizationNode[] {
  const normalized = keyword.trim().toLowerCase();
  if (!normalized) {
    return nodes;
  }

  return nodes
    .map((node) => {
      const children = filterOrganizationTree(node.children ?? [], keyword);
      const matched =
        node.title.toLowerCase().includes(normalized) ||
        node.orgType.toLowerCase().includes(normalized);
      return matched || children.length ? { ...node, children } : undefined;
    })
    .filter(Boolean) as SystemManagementApi.OrganizationNode[];
}

function findNode(
  nodes: SystemManagementApi.OrganizationNode[],
  key: string,
): SystemManagementApi.OrganizationNode | undefined {
  for (const node of nodes) {
    if (node.key === key) {
      return node;
    }
    const child = findNode(node.children ?? [], key);
    if (child) {
      return child;
    }
  }
  return undefined;
}

function handleSelect(keys: unknown[]) {
  const key = keys[0];
  const normalizedKey =
    typeof key === 'string' || typeof key === 'number' ? String(key) : '';
  const node = normalizedKey ? findNode(props.nodes, normalizedKey) : undefined;
  emit(
    'selectOrganization',
    typeof node?.id === 'number' || typeof node?.id === 'string'
      ? node.id
      : undefined,
    normalizedKey ? [normalizedKey] : [],
  );
}

function handleExpand(keys: unknown[]) {
  emit('updateExpandedKeys', keys.map(String));
}
</script>

<template>
  <aside
    :class="[
      'system-data-map',
      { 'system-data-map--collapsed': collapsed },
    ]"
  >
    <div class="system-data-map__header">
      <div>
        <div class="system-data-map__title">数据地图</div>
        <div class="system-data-map__subtitle">组织层级快速定位</div>
      </div>
      <Button
        :aria-expanded="!collapsed"
        :aria-label="toggleLabel"
        size="small"
        type="text"
        @click="emit('toggle')"
      >
        {{ toggleLabel }}
      </Button>
    </div>

    <div v-if="!collapsed" class="system-data-map__body">
      <Input
        v-model:value="treeKeyword"
        allow-clear
        placeholder="输入关键字搜索"
      />
      <VbenScrollbar class="system-data-map__tree-scroll" shadow shadow-border>
        <Tree
          :expanded-keys="expandedKeys"
          :selected-keys="selectedKeys"
          :tree-data="filteredTree"
          block-node
          class="system-data-map__tree"
          :field-names="{ children: 'children', key: 'key', title: 'title' }"
          :virtual="false"
          @expand="handleExpand"
          @select="handleSelect"
        />
      </VbenScrollbar>
    </div>
  </aside>
</template>

<style scoped>
.system-data-map {
  background: hsl(var(--background));
  border: 1px solid hsl(var(--border));
  border-radius: 6px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 100%;
  min-width: 0;
  overflow: hidden;
  padding: 14px;
}

.system-data-map__header {
  align-items: center;
  display: flex;
  gap: 12px;
  justify-content: space-between;
}

.system-data-map__header > div {
  min-width: 0;
}

.system-data-map__title {
  color: hsl(var(--foreground));
  font-size: 15px;
  font-weight: 700;
}

.system-data-map__subtitle {
  color: hsl(var(--muted-foreground));
  font-size: 12px;
  margin-top: 3px;
}

.system-data-map__body {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 12px;
  min-height: 0;
}

.system-data-map__tree-scroll {
  flex: 1;
  min-height: 0;
}

.system-data-map__tree {
  padding-right: 6px;
}

.system-data-map--collapsed {
  align-items: center;
  padding: 12px 8px;
}

.system-data-map--collapsed .system-data-map__header {
  flex: 1;
  flex-direction: column;
  justify-content: flex-start;
}

.system-data-map--collapsed .system-data-map__header > div {
  display: flex;
  justify-content: center;
}

.system-data-map--collapsed .system-data-map__title {
  line-height: 1.3;
  letter-spacing: 4px;
  writing-mode: vertical-rl;
}

.system-data-map--collapsed .system-data-map__subtitle {
  display: none;
}
</style>
