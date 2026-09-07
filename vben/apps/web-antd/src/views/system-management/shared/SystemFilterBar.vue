<script setup lang="ts">
import { Button, Input, Select, Space } from 'ant-design-vue';

import type {
  SystemCascadeField,
  SystemCrudFilters,
  SystemFilterOption,
} from './useSystemCrudPage';
import type { SystemTableFilterKey } from './systemTableConfig';

const props = withDefaults(defineProps<{
  cascadeFields?: SystemCascadeField[];
  companyOptions?: SystemFilterOption[];
  departmentOptions?: SystemFilterOption[];
  filters: SystemCrudFilters;
  showCascade?: boolean;
  showStatusFilter?: boolean;
  teamOptions?: SystemFilterOption[];
  visibleFilters?: SystemTableFilterKey[];
}>(), {
  cascadeFields: () => ['company', 'department', 'team'],
  companyOptions: () => [],
  departmentOptions: () => [],
  showCascade: true,
  showStatusFilter: true,
  teamOptions: () => [],
  visibleFilters: () => ['keyword', 'status', 'company', 'department', 'team'],
});

const emit = defineEmits<{
  apply: [];
  reset: [];
  setCompany: [value?: SystemFilterOption['value']];
  setDepartment: [value?: SystemFilterOption['value']];
  setTeam: [value?: SystemFilterOption['value']];
}>();

const statusOptions = [
  { label: '全部状态', value: 'all' },
  { label: '启用', value: 'ACTIVE' },
  { label: '草稿', value: 'DRAFT' },
  { label: '停用', value: 'INACTIVE' },
];

function hasCascadeField(field: SystemCascadeField) {
  return props.showCascade && props.cascadeFields.includes(field);
}

function hasFilter(field: SystemTableFilterKey) {
  return props.visibleFilters.includes(field);
}
</script>

<template>
  <div class="system-filter-bar">
    <Space :size="8" wrap>
      <Input
        v-if="hasFilter('keyword')"
        v-model:value="filters.keyword"
        allow-clear
        class="system-filter-bar__keyword"
        placeholder="编码 / 名称 / 手机号"
        @press-enter="emit('apply')"
      />
      <Select
        v-if="showStatusFilter && hasFilter('status')"
        v-model:value="filters.status"
        class="system-filter-bar__select"
        :options="statusOptions"
      />
      <Select
        v-if="hasCascadeField('company') && hasFilter('company')"
        allow-clear
        class="system-filter-bar__select"
        placeholder="公司"
        :options="companyOptions"
        :value="filters.companyId"
        @change="(value) => emit('setCompany', value as SystemFilterOption['value'] | undefined)"
      />
      <Select
        v-if="hasCascadeField('department') && hasFilter('department')"
        allow-clear
        class="system-filter-bar__select"
        placeholder="部门"
        :options="departmentOptions"
        :value="filters.departmentId"
        @change="(value) => emit('setDepartment', value as SystemFilterOption['value'] | undefined)"
      />
      <Select
        v-if="hasCascadeField('team') && hasFilter('team')"
        allow-clear
        class="system-filter-bar__select"
        placeholder="班组"
        :options="teamOptions"
        :value="filters.teamId"
        @change="(value) => emit('setTeam', value as SystemFilterOption['value'] | undefined)"
      />
      <Button type="primary" @click="emit('apply')">查询</Button>
      <Button @click="emit('reset')">重置</Button>
    </Space>
  </div>
</template>

<style scoped>
.system-filter-bar {
  border-bottom: 1px solid hsl(var(--border));
  padding: 12px 16px;
}

.system-filter-bar__keyword {
  width: 220px;
}

.system-filter-bar__select {
  width: 144px;
}
</style>
