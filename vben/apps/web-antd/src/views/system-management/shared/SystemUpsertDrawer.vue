<script setup lang="ts">
import { computed, reactive, watch } from 'vue';

import { Button, Drawer, Form, Input, Select, Space, TreeSelect } from 'ant-design-vue';
import type { TreeSelectProps } from 'ant-design-vue';

import type { SystemFieldConfig, SystemFieldOption } from './useSystemCrudPage';

const props = withDefaults(
  defineProps<{
    fields: SystemFieldConfig[];
    modelValue: boolean;
    record?: Record<string, unknown>;
    title: string;
  }>(),
  {
    record: undefined,
  },
);

const emit = defineEmits<{
  fieldChange: [key: string, value: unknown, formState: Record<string, unknown>];
  'update:modelValue': [value: boolean];
  submit: [value: Record<string, unknown>];
}>();

const formState = reactive<Record<string, any>>({});

const drawerTitle = computed(() =>
  props.record ? `编辑${props.title}` : `新建${props.title}`,
);

function resolveDefaultValue(field: SystemFieldConfig) {
  return typeof field.defaultValue === 'function'
    ? field.defaultValue()
    : field.defaultValue;
}

watch(
  () => [props.modelValue, props.record] as const,
  () => {
    if (!props.modelValue) return;
    for (const field of props.fields) {
      const value = props.record?.[field.valueKey];
      formState[field.valueKey] = field.formatValue
        ? field.formatValue(value)
        : value === undefined && !props.record
          ? resolveDefaultValue(field)
          : value === undefined
            ? undefined
          : value;
    }
  },
  { immediate: true },
);

function close() {
  emit('update:modelValue', false);
}

function submit() {
  const payload: Record<string, unknown> = {};
  for (const field of props.fields) {
    const value = formState[field.valueKey];
    payload[field.valueKey] = field.normalizeValue
      ? field.normalizeValue(value)
      : value;
  }
  emit('submit', payload);
}

function handleFieldChange(key: string, value: unknown) {
  emit('fieldChange', key, value, formState);
}

function fieldOptionsRenderKey(field: SystemFieldConfig) {
  function optionSignature(options = field.options ?? []): string {
    return options
      .map((option) =>
        [
          String(option.value ?? option.label),
          option.children ? optionSignature(option.children) : '',
        ].join(':'),
      )
      .join('|');
  }
  const optionKey = optionSignature();
  return `${field.valueKey}:${optionKey}`;
}

function treeSelectData(
  options: SystemFieldOption[] = [],
): TreeSelectProps['treeData'] {
  return options
    .filter((option) => option.value !== null && option.value !== undefined)
    .map((option) => ({
      children: treeSelectData(option.children ?? []),
      label: option.label,
      value: option.value as number | string,
    }));
}
</script>

<template>
  <Drawer
    :open="modelValue"
    :title="drawerTitle"
    width="560"
    @close="close"
    @update:open="emit('update:modelValue', $event)"
  >
    <Form :label-col="{ span: 6 }" :model="formState">
      <Form.Item
        v-for="field in fields"
        :key="field.valueKey"
        :label="field.label"
        :required="field.required"
      >
        <Select
          v-if="field.component === 'select'"
          :key="fieldOptionsRenderKey(field)"
          v-model:value="formState[field.valueKey]"
          allow-clear
          :disabled="field.readonly"
          :mode="field.mode"
          :options="field.options"
          :placeholder="field.placeholder"
          @change="(value) => handleFieldChange(field.valueKey, value)"
        />
        <TreeSelect
          v-else-if="field.component === 'tree-select'"
          :key="fieldOptionsRenderKey(field)"
          v-model:value="formState[field.valueKey]"
          allow-clear
          class="system-upsert-drawer__tree-select"
          :disabled="field.readonly"
          :dropdown-match-select-width="field.dropdownMatchSelectWidth"
          :list-height="field.listHeight"
          :placeholder="field.placeholder"
          :show-search="field.showSearch"
          tree-node-filter-prop="label"
          :tree-data="treeSelectData(field.options)"
          :tree-default-expand-all="field.treeDefaultExpandAll"
          @change="(value) => handleFieldChange(field.valueKey, value)"
        />
        <Input.TextArea
          v-else-if="field.component === 'textarea'"
          v-model:value="formState[field.valueKey]"
          :auto-size="{ minRows: 3, maxRows: 5 }"
          :disabled="field.readonly"
          :placeholder="field.placeholder"
        />
        <Input
          v-else
          v-model:value="formState[field.valueKey]"
          :disabled="field.readonly"
          :placeholder="field.placeholder"
          :type="field.type || 'text'"
        />
      </Form.Item>
    </Form>
    <template #footer>
      <Space>
        <Button @click="close">取消</Button>
        <Button type="primary" @click="submit">保存</Button>
      </Space>
    </template>
  </Drawer>
</template>

<style scoped>
.system-upsert-drawer__tree-select {
  width: 100%;
}
</style>
