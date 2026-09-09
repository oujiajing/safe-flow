<script setup lang="ts">
import type { OrganizationNode } from '#/views/pingan/pre-shift-meeting/pre-shift-meeting.data';

import { organizationType } from './organization.view-state';

const props = defineProps<{
  node: OrganizationNode;
  selectedNode?: OrganizationNode;
}>();

const emit = defineEmits<{
  select: [node: OrganizationNode];
}>();

function isSameOrganizationNode(left?: OrganizationNode, right?: OrganizationNode) {
  return Boolean(
    left &&
      right &&
      (String(left.id) === String(right.id) || left.key === right.key),
  );
}

function relationNodeClass(node: OrganizationNode) {
  const type = organizationType(node).toLowerCase();
  return [
    'relation-node',
    type && `relation-node--${type}`,
    isSameOrganizationNode(props.selectedNode, node) && 'relation-node--active',
  ];
}
</script>

<template>
  <ul class="relation-tree">
    <li class="relation-tree__item">
      <button
        :aria-pressed="isSameOrganizationNode(selectedNode, node)"
        :class="relationNodeClass(node)"
        type="button"
        @click="emit('select', node)"
      >
        {{ node.title }}
      </button>
      <ul v-if="node.children?.length" class="relation-tree__children">
        <li
          v-for="child in node.children"
          :key="child.key"
          class="relation-tree__item"
        >
          <OrganizationRelationTree
            :node="child"
            :selected-node="selectedNode"
            @select="emit('select', $event)"
          />
        </li>
      </ul>
    </li>
  </ul>
</template>

<style scoped>
.relation-tree,
.relation-tree__children {
  position: relative;
  display: flex;
  justify-content: center;
  margin: 0;
  padding: 0;
  list-style: none;
}

.relation-tree {
  min-width: max-content;
}

.relation-tree__item {
  position: relative;
  display: flex;
  align-items: center;
  flex-direction: column;
  padding: 0 10px;
}

.relation-tree__children {
  gap: 12px;
  padding-top: 28px;
}

.relation-tree__children::before {
  position: absolute;
  top: 14px;
  right: 38px;
  left: 38px;
  height: 1px;
  background: #cbd5e1;
  content: '';
}

.relation-tree__children > .relation-tree__item::before {
  position: absolute;
  top: 0;
  left: 50%;
  width: 1px;
  height: 28px;
  background: #cbd5e1;
  content: '';
  transform: translateX(-50%);
}

.relation-tree__children > .relation-tree__item:only-child::before {
  height: 28px;
}

.relation-node {
  appearance: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 118px;
  min-height: 44px;
  padding: 0 14px;
  color: #0f172a;
  border: 1px solid #dce5f3;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 8px 20px rgb(15 23 42 / 7%);
  cursor: pointer;
  font-family: inherit;
  font-size: 14px;
  font-weight: 700;
  transition:
    border-color 0.16s ease,
    box-shadow 0.16s ease,
    transform 0.16s ease;
}

.relation-node:hover {
  border-color: #1264f0;
  box-shadow: 0 10px 24px rgb(18 100 240 / 13%);
  transform: translateY(-1px);
}

.relation-node:focus-visible {
  outline: 2px solid #1264f0;
  outline-offset: 2px;
}

.relation-node--active {
  border-color: #1264f0;
  box-shadow: 0 0 0 2px rgb(18 100 240 / 12%);
}
</style>
