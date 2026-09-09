<script setup lang="ts" generic="T extends string | undefined">
import { Button, Select, Space } from 'ant-design-vue';

interface StatusOption {
  label: string;
  value: T;
}

withDefaults(
  defineProps<{
    disabled?: boolean;
    hideReset?: boolean;
    modelValue?: T;
    options: StatusOption[];
    searchText?: string;
  }>(),
  {
    disabled: false,
    hideReset: false,
    searchText: '搜索',
  },
);

const emit = defineEmits<{
  reset: [];
  search: [];
  'update:modelValue': [value: T];
}>();
</script>

<template>
  <div class="status-filter-actions">
    <label class="status-filter-actions__field">
      <span>状态</span>
      <Select
        :disabled="disabled"
        :options="options"
        :value="modelValue"
        @update:value="emit('update:modelValue', $event as T)"
      />
    </label>
    <Space class="status-filter-actions__buttons" :size="10">
      <Button :disabled="disabled" type="primary" @click="emit('search')">
        {{ searchText }}
      </Button>
      <Button v-if="!hideReset" :disabled="disabled" @click="emit('reset')">
        重置
      </Button>
    </Space>
  </div>
</template>

<style scoped>
.status-filter-actions {
  display: flex;
  flex: 0 1 300px;
  gap: 10px;
  align-items: flex-end;
  min-width: 258px;
  max-width: 310px;
}

.status-filter-actions__field {
  display: grid;
  flex: 1 1 126px;
  gap: 5px;
  min-width: 0;
}

.status-filter-actions__field > span {
  color: #334155;
  font-size: 12px;
  font-weight: 700;
  line-height: 18px;
}

.status-filter-actions__field :deep(.ant-select) {
  width: 100%;
  height: 40px;
}

.status-filter-actions__field :deep(.ant-select-selector) {
  height: 40px !important;
  padding: 0 14px !important;
  border-radius: 8px !important;
}

.status-filter-actions__field :deep(.ant-select-selection-item),
.status-filter-actions__field :deep(.ant-select-selection-placeholder) {
  line-height: 38px !important;
}

.status-filter-actions__buttons :deep(.ant-btn) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 64px;
  height: 40px;
  padding: 0 14px;
  line-height: 1;
  border-radius: 8px;
}
</style>
