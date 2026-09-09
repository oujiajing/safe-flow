<script setup lang="ts">
import { computed, onActivated, onBeforeUnmount, ref, watch } from 'vue';

import { VbenScrollbar } from '@vben/common-ui';

import { Button, Input, Tree } from 'ant-design-vue';

import type { DataMapNode } from './data-map-state';
import {
  createDataMapCollapseState,
  createDataMapResizeState,
  dataMapTreeContinuousScrollConfig,
} from './data-map-state';

const props = defineProps<{
  selectedKeys?: string[];
  treeData: DataMapNode[];
}>();

const emit = defineEmits<{
  select: [node?: DataMapNode];
  'update:selectedKeys': [keys: string[]];
}>();

const treeKeyword = ref('');
const expandedKeys = ref<string[]>([]);

const { dataMapToggleLabel, isDataMapCollapsed, toggleDataMap } =
  createDataMapCollapseState();
const {
  beginDataMapResize,
  dataMapWidth,
  endDataMapResize,
  isDataMapResizing,
  updateDataMapResize,
} = createDataMapResizeState();

const panelStyle = computed(() => ({
  width: isDataMapCollapsed.value ? '64px' : `${dataMapWidth.value}px`,
}));

const filteredTreeData = computed(() =>
  filterTree(props.treeData, treeKeyword.value),
);

function collectKeys(nodes: DataMapNode[]): string[] {
  return nodes.flatMap((node) => [
    node.key,
    ...collectKeys(node.children ?? []),
  ]);
}

function collectExpandableKeys(nodes: DataMapNode[]): string[] {
  return nodes.flatMap((node) => [
    ...(node.children?.length ? [node.key] : []),
    ...collectExpandableKeys(node.children ?? []),
  ]);
}

function openAllDataMap() {
  if (treeKeyword.value) {
    return;
  }
  expandedKeys.value = collectExpandableKeys(props.treeData);
}

function filterTree(nodes: DataMapNode[], keyword: string): DataMapNode[] {
  const normalizedKeyword = keyword.trim().toLowerCase();
  if (!normalizedKeyword) {
    return nodes;
  }

  return nodes
    .map((node) => {
      const children = filterTree(node.children ?? [], keyword);
      const selfMatched = node.title.toLowerCase().includes(normalizedKeyword);
      if (!selfMatched && children.length === 0) {
        return null;
      }
      return { ...node, children };
    })
    .filter(Boolean) as DataMapNode[];
}

function handleSearch(value: string) {
  treeKeyword.value = value;
  if (value) {
    expandedKeys.value = collectKeys(filteredTreeData.value);
    return;
  }
  openAllDataMap();
}

function handleSearchChange(event: Event) {
  handleSearch((event.target as HTMLInputElement).value);
}

function handleSelect(keys: unknown[], info: unknown) {
  const selectedKeys = keys.map(String);
  const eventInfo = info as {
    node?: DataMapNode & { dataRef?: DataMapNode };
  };
  const node = eventInfo.node?.dataRef ?? eventInfo.node;
  emit('update:selectedKeys', selectedKeys);
  emit('select', node);
}

function handleResizeMove(event: PointerEvent) {
  updateDataMapResize(event.clientX);
}

function cleanupResizeListeners() {
  window.removeEventListener('pointermove', handleResizeMove);
  window.removeEventListener('pointerup', handleResizeEnd);
  window.removeEventListener('pointercancel', handleResizeEnd);
}

function handleResizeEnd() {
  endDataMapResize();
  cleanupResizeListeners();
}

function handleResizeStart(event: PointerEvent) {
  event.preventDefault();
  beginDataMapResize(event.clientX);
  window.addEventListener('pointermove', handleResizeMove);
  window.addEventListener('pointerup', handleResizeEnd);
  window.addEventListener('pointercancel', handleResizeEnd);
}

onBeforeUnmount(cleanupResizeListeners);
onActivated(openAllDataMap);

watch(() => props.treeData, openAllDataMap, { immediate: true });
</script>

<template>
  <aside
    :class="[
      'data-map',
      {
        'data-map--collapsed': isDataMapCollapsed,
        'data-map--resizing': isDataMapResizing,
      },
    ]"
    :style="panelStyle"
  >
    <div class="data-map__header">
      <div>
        <div class="data-map__title">数据地图</div>
        <div class="data-map__subtitle">组织层级快速定位</div>
      </div>
      <Button
        :aria-expanded="!isDataMapCollapsed"
        class="data-map__collapse"
        size="small"
        type="text"
        @click="toggleDataMap"
      >
        {{ dataMapToggleLabel }}
      </Button>
    </div>

    <div v-if="!isDataMapCollapsed" class="data-map__body">
      <Input.Search
        :value="treeKeyword"
        allow-clear
        placeholder="搜索组织"
        size="small"
        @change="handleSearchChange"
        @search="handleSearch"
      />
      <VbenScrollbar class="data-map__tree-scroll">
        <Tree
          v-model:expanded-keys="expandedKeys"
          :selected-keys="selectedKeys"
          :tree-data="filteredTreeData"
          v-bind="dataMapTreeContinuousScrollConfig"
          block-node
          class="data-map__tree"
          @select="handleSelect"
        />
      </VbenScrollbar>
    </div>

    <button
      v-if="!isDataMapCollapsed"
      aria-label="拖拽调整数据地图宽度"
      class="data-map__resize-handle"
      type="button"
      @pointerdown="handleResizeStart"
    />
  </aside>
</template>

<style scoped>
.data-map {
  position: relative;
  display: flex;
  flex: 0 0 auto;
  flex-direction: column;
  min-width: 0;
  height: 100%;
  overflow: hidden;
  background: #fff;
  border: 1px solid #e6eaf2;
  border-radius: 8px;
  transition: width 0.16s ease;
}

.data-map__header {
  display: flex;
  gap: 8px;
  align-items: center;
  justify-content: space-between;
  min-height: 56px;
  padding: 12px;
  background: #f8fafc;
  border-bottom: 1px solid #e6eaf2;
}

.data-map__header > div {
  min-width: 0;
}

.data-map__title {
  color: #0f172a;
  font-size: 15px;
  font-weight: 800;
  line-height: 1.2;
}

.data-map__subtitle {
  margin-top: 3px;
  overflow: hidden;
  color: #64748b;
  font-size: 12px;
  line-height: 1.2;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.data-map__collapse {
  flex: 0 0 auto;
}

.data-map__body {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 10px;
  min-height: 0;
  padding: 12px;
}

.data-map__tree-scroll {
  flex: 1;
  min-height: 0;
}

.data-map__tree {
  min-width: 220px;
  color: #334155;
}

.data-map__resize-handle {
  position: absolute;
  top: 0;
  right: -5px;
  bottom: 0;
  z-index: 2;
  width: 10px;
  padding: 0;
  cursor: col-resize;
  background: transparent;
  border: 0;
}

.data-map__resize-handle::after {
  position: absolute;
  top: 50%;
  right: 4px;
  width: 2px;
  height: 36px;
  content: '';
  background: #cbd5e1;
  border-radius: 999px;
  transform: translateY(-50%);
}

.data-map__resize-handle:focus-visible::after,
.data-map__resize-handle:hover::after,
.data-map--resizing .data-map__resize-handle::after {
  background: #2563eb;
}

.data-map--collapsed .data-map__header {
  flex-direction: column;
  justify-content: flex-start;
  height: 100%;
  padding: 18px 6px 10px;
}

.data-map--collapsed .data-map__header > div {
  width: 100%;
}

.data-map--collapsed .data-map__title {
  margin: 0 auto;
  line-height: 1.25;
  text-align: center;
  writing-mode: vertical-rl;
}

.data-map--collapsed .data-map__subtitle {
  display: none;
}
</style>
