<script setup lang="ts">
import { nextTick, onBeforeUnmount, ref, watch } from 'vue';

import {
  Button,
  Drawer,
  Form,
  Input,
  InputNumber,
  Select,
  Space,
  Switch,
  Table,
  Tabs,
} from 'ant-design-vue';

import type { Sortable } from '@vben/hooks';
import { useSortable } from '@vben/hooks';

import type {
  SystemTableConfig,
  SystemTableConfigField,
  SystemTableConfigFilter,
  SystemTableRoleOption,
  SystemTableSortOrder,
  SystemTableWidthMode,
} from './systemTableConfig';

import {
  cloneSystemTableConfig,
  reorderSystemTableConfigFields,
} from './systemTableConfig';

const props = withDefaults(
  defineProps<{
    autoWidths?: Record<string, number>;
    config?: SystemTableConfig;
    moduleTitle: string;
    open: boolean;
    roleOptions?: SystemTableRoleOption[];
  }>(),
  {
    autoWidths: () => ({}),
    roleOptions: () => [],
  },
);

const emit = defineEmits<{
  delete: [];
  push: [config: SystemTableConfig];
  save: [config: SystemTableConfig];
  'update:open': [open: boolean];
}>();

const draft = ref<SystemTableConfig>();
const fieldTableRef = ref<HTMLElement>();
const sortable = ref<Sortable>();

const fieldConfigColumns = [
  { dataIndex: 'order', title: '排序', width: 72 },
  { dataIndex: 'title', title: '字段', width: 140 },
  { dataIndex: 'width', title: '宽度模式', width: 220 },
  { dataIndex: 'sort', title: '排序方式', width: 136 },
  { dataIndex: 'enabled', title: '启用', width: 92 },
];

const filterConfigColumns = [
  { dataIndex: 'title', title: '条件', width: 180 },
  { dataIndex: 'enabled', title: '启用', width: 92 },
];

const sortOptions = [
  { label: '不排序', value: 'none' },
  { label: '升序', value: 'ascend' },
  { label: '降序', value: 'descend' },
];

const widthModeOptions = [
  { label: '自适应', value: 'auto' },
  { label: '固定', value: 'fixed' },
];

watch(
  () => [props.config, props.open],
  () => {
    if (props.config) {
      draft.value = cloneSystemTableConfig(props.config);
    }
  },
  { immediate: true },
);

watch(
  () => [props.open, draft.value?.fields.length],
  () => {
    if (props.open) {
      void setupFieldSortable();
    } else {
      destroySortable();
    }
  },
  { flush: 'post' },
);

onBeforeUnmount(() => {
  destroySortable();
});

function closeDrawer() {
  emit('update:open', false);
}

function normalizedDraft() {
  if (!draft.value) return undefined;
  return {
    ...draft.value,
    fields: draft.value.fields.map((field, index) => ({
      ...field,
      order: index + 1,
    })),
  };
}

function saveDraft() {
  const config = normalizedDraft();
  if (!config) return;
  emit('save', config);
}

function pushDraft() {
  const config = normalizedDraft();
  if (!config) return;
  emit('push', config);
}

function destroySortable() {
  sortable.value?.destroy();
  sortable.value = undefined;
}

async function setupFieldSortable() {
  destroySortable();
  await nextTick();
  const tbody = fieldTableRef.value?.querySelector(
    '.ant-table-tbody',
  ) as HTMLElement | null;
  if (!tbody) return;

  const { initializeSortable } = useSortable(tbody, {
    draggable: 'tr.ant-table-row',
    handle: '.system-table-config__drag-handle',
    onEnd(event) {
      if (
        !draft.value ||
        event.oldIndex === undefined ||
        event.newIndex === undefined
      ) {
        return;
      }
      draft.value.fields = reorderSystemTableConfigFields(
        draft.value.fields,
        event.oldIndex,
        event.newIndex,
      );
    },
  });
  sortable.value = await initializeSortable();
}

function setFieldWidthMode(
  record: SystemTableConfigField,
  value: SystemTableWidthMode,
) {
  if (!draft.value) return;
  draft.value.fields = draft.value.fields.map((field) => ({
    ...field,
    widthMode: field.key === record.key ? value : field.widthMode,
  }));
}

function updateFilter(record: SystemTableConfigFilter, enabled: boolean) {
  record.enabled = enabled;
}

function updateFieldSort(
  record: SystemTableConfigField,
  value: SystemTableSortOrder,
) {
  if (!draft.value) return;
  draft.value.fields = draft.value.fields.map((field) => ({
    ...field,
    sort:
      field.key === record.key
        ? value
        : value === 'none'
          ? field.sort
          : 'none',
  }));
}
</script>

<template>
  <Drawer
    :open="open"
    width="760"
    :title="`${moduleTitle}表格配置`"
    @close="closeDrawer"
  >
    <div v-if="draft" class="system-table-config">
      <Form class="system-table-config__form" layout="inline">
        <Form.Item label="编码">
          <Input v-model:value="draft.code" class="system-table-config__code" />
        </Form.Item>
        <Form.Item label="名称">
          <Input v-model:value="draft.name" class="system-table-config__name" />
        </Form.Item>
        <Form.Item label="置顶">
          <Switch v-model:checked="draft.pinned" />
        </Form.Item>
        <Form.Item label="推送角色">
          <Select
            v-model:value="draft.roleIds"
            allow-clear
            class="system-table-config__roles"
            mode="multiple"
            :options="roleOptions"
            placeholder="请选择推送角色"
          />
        </Form.Item>
      </Form>

      <Tabs>
        <Tabs.TabPane key="fields" tab="字段配置">
          <div ref="fieldTableRef">
            <Table
              bordered
              :columns="fieldConfigColumns"
              :data-source="draft.fields"
              :pagination="false"
              row-key="key"
              size="small"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.dataIndex === 'order'">
                  <span
                    aria-label="拖动排序"
                    class="system-table-config__drag-handle"
                    role="button"
                    tabindex="0"
                    title="拖动排序"
                  >
                    ::
                  </span>
                </template>
                <template v-else-if="column.dataIndex === 'width'">
                  <Space :size="6">
                    <Select
                      class="system-table-config__width-mode"
                      :options="widthModeOptions"
                      :value="record.widthMode"
                      @change="(value) => setFieldWidthMode(record as SystemTableConfigField, value as SystemTableWidthMode)"
                    />
                    <InputNumber
                      v-if="record.widthMode === 'fixed'"
                      v-model:value="record.width"
                      :min="80"
                      :step="10"
                    />
                    <span v-else class="system-table-config__auto-width">
                      自适应 {{ autoWidths[record.key] ?? record.width }}px
                    </span>
                  </Space>
                </template>
                <template v-else-if="column.dataIndex === 'sort'">
                  <Select
                    class="system-table-config__sort"
                    :options="sortOptions"
                    :value="record.sort"
                    @change="(value) => updateFieldSort(record as SystemTableConfigField, value as SystemTableSortOrder)"
                  />
                </template>
                <template v-else-if="column.dataIndex === 'enabled'">
                  <Switch v-model:checked="record.enabled" />
                </template>
              </template>
            </Table>
          </div>
        </Tabs.TabPane>
        <Tabs.TabPane key="filters" tab="过滤条件">
          <Table
            bordered
            :columns="filterConfigColumns"
            :data-source="draft.filters"
            :pagination="false"
            row-key="key"
            size="small"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'enabled'">
                <Switch
                  :checked="record.enabled"
                  @change="(checked) => updateFilter(record as SystemTableConfigFilter, checked as boolean)"
                />
              </template>
            </template>
          </Table>
        </Tabs.TabPane>
      </Tabs>
    </div>

    <template #footer>
      <Space>
        <Button
          :disabled="!draft?.roleIds.length"
          @click="pushDraft"
        >
          推送
        </Button>
        <Button type="primary" @click="saveDraft">保存</Button>
        <Button @click="closeDrawer">关闭</Button>
        <Button danger @click="emit('delete')">删除</Button>
      </Space>
    </template>
  </Drawer>
</template>

<style scoped>
.system-table-config {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.system-table-config__form {
  border-bottom: 1px solid hsl(var(--border));
  padding-bottom: 12px;
}

.system-table-config__code {
  width: 180px;
}

.system-table-config__name {
  width: 200px;
}

.system-table-config__roles {
  width: 220px;
}

.system-table-config__sort {
  width: 112px;
}

.system-table-config__width-mode {
  width: 92px;
}

.system-table-config__drag-handle {
  align-items: center;
  color: hsl(var(--muted-foreground));
  cursor: grab;
  display: inline-flex;
  font-weight: 700;
  height: 24px;
  justify-content: center;
  letter-spacing: 1px;
  line-height: 1;
  user-select: none;
  width: 32px;
}

.system-table-config__drag-handle:active {
  cursor: grabbing;
}

.system-table-config__auto-width {
  color: hsl(var(--muted-foreground));
  display: inline-block;
  min-width: 88px;
}

.system-table-config :deep(.sortable-ghost) {
  opacity: 0.45;
}

.system-table-config :deep(.sortable-chosen) {
  background: hsl(var(--accent));
}
</style>
