<script setup lang="ts">
import type { TableColumnsType } from 'ant-design-vue';

import { computed, onMounted, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { useUserStore } from '@vben/stores';

import { Button, message, Modal, Table, Tag } from 'ant-design-vue';

import { getRoleListApi } from '#/api/system-management/security';
import {
  getCompanyOptionsApi,
  getDepartmentOptionsApi,
  getSystemOrganizationTreeApi,
  getTeamOptionsApi,
} from '#/api/system-management/options';
import type { SystemManagementApi } from '#/api/system-management/types';

import type {
  SystemCascadeField,
  SystemCrudAction,
  SystemCrudColumn,
  SystemCrudStateOptions,
  SystemFieldConfig,
  SystemFilterOption,
  SystemRowAction,
} from './useSystemCrudPage';

import SystemDataMap from './SystemDataMap.vue';
import SystemFilterBar from './SystemFilterBar.vue';
import SystemTableConfigDrawer from './SystemTableConfigDrawer.vue';
import SystemToolbar from './SystemToolbar.vue';
import SystemUpsertDrawer from './SystemUpsertDrawer.vue';
import {
  filterCompanyDrawerFields,
  findCompanyNodePathByKey,
  normalizeCompanyPayloadForSubmit,
  shouldDisableCompanyCreateForPath,
} from './companyHierarchy';
import {
  filterDepartmentDrawerFields,
  findDefaultDepartmentCompanyPath,
  normalizeDepartmentPayloadForSubmit,
} from './departmentHierarchy';
import {
  createTeamDefaultValues,
  normalizeTeamPayloadForSubmit,
} from './teamHierarchy';
import {
  createPersonnelDefaultValues,
  normalizePersonnelPayloadForSubmit,
} from './personnelHierarchy';
import {
  collectExpandableSystemDataMapKeys,
  normalizeSystemDataMapNodes,
} from './systemDataMapTree';
import type {
  SystemTableConfig,
  SystemTableConfigModule,
  SystemTableFilterKey,
  SystemTableRoleOption,
} from './systemTableConfig';
import {
  applySystemTableConfigColumns,
  calculateSystemTableAutoWidth,
  createDefaultSystemTableConfig,
  getEnabledSystemTableFilterKeys,
  loadSystemTableConfig,
  mergeSystemTableConfig,
  removeSystemTableConfig,
  saveSystemTableConfig,
  sortRowsBySystemTableConfig,
} from './systemTableConfig';
import {
  createSystemTableScrollConfig,
  createSystemCrudState,
} from './useSystemCrudPage';

const props = withDefaults(
  defineProps<{
    cascadeFields?: SystemCascadeField[];
    columns: SystemCrudColumn[];
    companyOptions?: SystemFilterOption[];
    companyHierarchyMode?: boolean;
    departmentHierarchyMode?: boolean;
    departmentOptions?: SystemFilterOption[];
    fields: SystemFieldConfig[];
    hiddenActions?: SystemCrudAction[];
    rowKey?: string;
    rowActions?: SystemRowAction<Record<string, unknown>>[];
    service: SystemCrudStateOptions<Record<string, unknown>>;
    showCascade?: boolean;
    showDataMap?: boolean;
    showStatusFilter?: boolean;
    teamOptions?: SystemFilterOption[];
    tablePageSize?: number;
    tableConfigModule?: SystemTableConfigModule;
    tableConfigTitle?: string;
    personnelHierarchyMode?: boolean;
    teamHierarchyMode?: boolean;
    title: string;
  }>(),
  {
    cascadeFields: () => ['company', 'department', 'team'],
    companyOptions: () => [],
    companyHierarchyMode: false,
    departmentHierarchyMode: false,
    departmentOptions: () => [],
    hiddenActions: () => [],
    rowKey: 'id',
    rowActions: () => [],
    showCascade: true,
    showDataMap: false,
    showStatusFilter: true,
    tablePageSize: 20,
    personnelHierarchyMode: false,
    teamHierarchyMode: false,
    teamOptions: () => [],
  },
);

const emit = defineEmits<{
  organizationSelect: [id: SystemManagementApi.Id | undefined, keys: string[]];
  rowSelect: [record: Record<string, unknown>];
}>();

const userStore = useUserStore();
const state = createSystemCrudState({
  ...props.service,
  afterMutation: refreshMasterDataAfterMutation,
});
const companySelectOptions = ref<SystemFilterOption[]>([...props.companyOptions]);
const departmentSelectOptions = ref<SystemFilterOption[]>([
  ...props.departmentOptions,
]);
const teamSelectOptions = ref<SystemFilterOption[]>([...props.teamOptions]);
const organizationNodes = ref<SystemManagementApi.OrganizationNode[]>([]);
const expandedOrganizationKeys = ref<string[]>([]);
const selectedOrganizationKeys = ref<string[]>([]);
const dataMapCollapsed = ref(false);
const tableConfigOpen = ref(false);
const tableViewConfig = ref<SystemTableConfig>();
const tableConfigRoleOptions = ref<SystemTableRoleOption[]>([]);

const tableConfigTitle = computed(() => props.tableConfigTitle || props.title);

const defaultTableFilterKeys = computed<SystemTableFilterKey[]>(() => {
  const keys: SystemTableFilterKey[] = ['keyword'];
  if (props.showStatusFilter) {
    keys.push('status');
  }
  if (usesCascadeField('company')) {
    keys.push('company');
  }
  if (usesCascadeField('department')) {
    keys.push('department');
  }
  if (usesCascadeField('team')) {
    keys.push('team');
  }
  return keys;
});

const effectiveTableConfig = computed(() => {
  if (!props.tableConfigModule) return undefined;
  return (
    tableViewConfig.value ??
    createDefaultSystemTableConfig(
      props.tableConfigModule,
      tableConfigTitle.value,
      props.columns,
      defaultTableFilterKeys.value,
    )
  );
});

const tableColumns = computed<TableColumnsType>(() => [
  ...applySystemTableConfigColumns(
    props.columns,
    effectiveTableConfig.value,
    state.rows.value,
  ),
  { dataIndex: 'actions', fixed: 'right', title: '操作', width: 136 },
]);

const tableScrollConfig = computed(() =>
  createSystemTableScrollConfig(tableColumns.value),
);

const tableRows = computed(() =>
  sortRowsBySystemTableConfig(state.rows.value, effectiveTableConfig.value),
);

const visibleTableFilters = computed(() =>
  effectiveTableConfig.value
    ? getEnabledSystemTableFilterKeys(effectiveTableConfig.value)
    : defaultTableFilterKeys.value,
);

const selectedCompanyPath = computed(() =>
  props.companyHierarchyMode
    ? findCompanyNodePathByKey(
        organizationNodes.value,
        selectedOrganizationKeys.value[0],
      )
    : [],
);

const selectedDepartmentCompanyPath = computed(() => {
  if (!props.departmentHierarchyMode) {
    return [];
  }
  const selectedPath = findCompanyNodePathByKey(
    organizationNodes.value,
    selectedOrganizationKeys.value[0],
  );
  return selectedPath.length > 0
    ? selectedPath
    : findDefaultDepartmentCompanyPath(
        organizationNodes.value,
        String(userStore.userInfo?.orgPath ?? ''),
      );
});

const selectedTeamCompanyPath = computed(() => {
  if (!props.teamHierarchyMode) {
    return [];
  }
  const selectedPath = findCompanyNodePathByKey(
    organizationNodes.value,
    selectedOrganizationKeys.value[0],
  );
  return selectedPath.length > 0
    ? selectedPath
    : findDefaultDepartmentCompanyPath(
        organizationNodes.value,
        String(userStore.userInfo?.orgPath ?? ''),
      );
});

const selectedPersonnelCompanyPath = computed(() => {
  if (!props.personnelHierarchyMode) {
    return [];
  }
  const selectedPath = findCompanyNodePathByKey(
    organizationNodes.value,
    selectedOrganizationKeys.value[0],
  );
  return selectedPath.length > 0
    ? selectedPath
    : findDefaultDepartmentCompanyPath(
        organizationNodes.value,
        String(userStore.userInfo?.orgPath ?? ''),
      );
});

const drawerFields = computed(() => {
  const companyFields = filterCompanyDrawerFields(props.fields, {
    companyHierarchyMode: props.companyHierarchyMode,
    editing: Boolean(state.editingRecord.value),
  });
  const departmentFields = filterDepartmentDrawerFields(companyFields, {
    departmentHierarchyMode: props.departmentHierarchyMode,
    editing: Boolean(state.editingRecord.value),
  });
  return departmentFields.map((field) => {
    if (props.personnelHierarchyMode) {
      const defaultValues = createPersonnelDefaultValues({
        userInfo: userStore.userInfo,
      });
      if (field.valueKey === 'companyName') {
        return {
          ...field,
          defaultValue: selectedPersonnelCompanyPath.value.at(-1)?.title,
        };
      }
      if (field.valueKey === 'departmentOrgId') {
        return { ...field, options: departmentSelectOptions.value };
      }
      if (field.valueKey === 'teamOrgId') {
        return { ...field, options: teamSelectOptions.value };
      }
      if (field.valueKey === 'submitDate') {
        return { ...field, defaultValue: defaultValues.submitDate };
      }
      if (field.valueKey === 'applicantName') {
        return { ...field, defaultValue: defaultValues.applicantName };
      }
    }
    if (!props.teamHierarchyMode) return field;
    const defaultValues = createTeamDefaultValues({
      userInfo: userStore.userInfo,
    });
    if (field.valueKey === 'companyName') {
      return {
        ...field,
        defaultValue: selectedTeamCompanyPath.value.at(-1)?.title,
      };
    }
    if (field.valueKey === 'groupName') {
      return { ...field, defaultValue: selectedTeamCompanyPath.value[1]?.title };
    }
    if (field.valueKey === 'level1Unit') {
      return { ...field, defaultValue: selectedTeamCompanyPath.value[2]?.title };
    }
    if (field.valueKey === 'level2Unit') {
      return { ...field, defaultValue: selectedTeamCompanyPath.value[3]?.title };
    }
    if (field.valueKey === 'workshopOrgId') {
      return { ...field, options: departmentSelectOptions.value };
    }
    if (field.valueKey === 'submitDate') {
      return { ...field, defaultValue: defaultValues.submitDate };
    }
    if (field.valueKey === 'applicantName') {
      return { ...field, defaultValue: defaultValues.applicantName };
    }
    return field;
  });
});

const tableConfigAutoWidths = computed(() =>
  Object.fromEntries(
    props.columns.map((column) => [
      String(column.dataIndex),
      calculateSystemTableAutoWidth(column, state.rows.value),
    ]),
  ),
);

function resetFilters() {
  state.filters.keyword = '';
  state.setOrganization(undefined);
  selectedOrganizationKeys.value = [];
  state.filters.status = 'all';
  void handleSetCompany(undefined);
  void state.load();
}

function usesCascadeField(field: SystemCascadeField) {
  return props.showCascade && props.cascadeFields.includes(field);
}

function normalizeOptions(options: unknown): SystemFilterOption[] {
  return Array.isArray(options)
    ? options
        .filter(
          (option): option is SystemFilterOption =>
            option !== null &&
            typeof option === 'object' &&
            'label' in option &&
            'value' in option,
        )
        .map((option) => ({
          label: String(option.label),
          value: option.value,
        }))
    : [];
}

async function loadCascadeOptions() {
  if (!props.showCascade) return;
  if (usesCascadeField('company')) {
    companySelectOptions.value = normalizeOptions(await getCompanyOptionsApi());
  }
  if (usesCascadeField('department')) {
    departmentSelectOptions.value = normalizeOptions(
      await getDepartmentOptionsApi(state.filters.companyId),
    );
  }
  if (usesCascadeField('team')) {
    teamSelectOptions.value = normalizeOptions(
      await getTeamOptionsApi(
        state.filters.departmentId ?? state.filters.companyId,
      ),
    );
  }
}

async function loadOrganizationTree() {
  if (!props.showDataMap) return;
  organizationNodes.value = normalizeSystemDataMapNodes(
    await getSystemOrganizationTreeApi(),
  );
  expandedOrganizationKeys.value = collectExpandableSystemDataMapKeys(
    organizationNodes.value,
  );
}

async function refreshMasterDataAfterMutation() {
  await Promise.all([loadCascadeOptions(), loadOrganizationTree()]);
}

function loadTableConfig() {
  if (!props.tableConfigModule) return;
  tableViewConfig.value = loadSystemTableConfig(
    props.tableConfigModule,
    tableConfigTitle.value,
    props.columns,
    defaultTableFilterKeys.value,
  );
}

async function loadTableConfigRoleOptions() {
  if (!props.tableConfigModule || tableConfigRoleOptions.value.length > 0) {
    return;
  }
  try {
    const result = await getRoleListApi({ pageSize: 100 });
    tableConfigRoleOptions.value = Array.isArray(result.items)
      ? result.items.map((role) => ({
          label: role.roleName || role.name || role.roleCode || String(role.id),
          value: role.id,
        }))
      : [];
  } catch {
    tableConfigRoleOptions.value = [];
  }
}

function openTableConfig() {
  if (!props.tableConfigModule) return;
  if (!tableViewConfig.value) {
    loadTableConfig();
  }
  tableConfigOpen.value = true;
  void loadTableConfigRoleOptions();
}

function persistTableConfig(config: SystemTableConfig) {
  if (!props.tableConfigModule) return config;
  const merged = mergeSystemTableConfig(
    config,
    props.tableConfigModule,
    tableConfigTitle.value,
    props.columns,
    defaultTableFilterKeys.value,
  );
  tableViewConfig.value = merged;
  saveSystemTableConfig(props.tableConfigModule, merged);
  return merged;
}

function saveTableConfig(config: SystemTableConfig) {
  persistTableConfig(config);
  tableConfigOpen.value = false;
  message.success('表格配置已保存');
}

function pushTableConfig(config: SystemTableConfig) {
  persistTableConfig(config);
  tableConfigOpen.value = false;
  message.success('表格配置已推送');
}

function deleteTableConfig() {
  if (!props.tableConfigModule) return;
  removeSystemTableConfig(props.tableConfigModule);
  tableViewConfig.value = createDefaultSystemTableConfig(
    props.tableConfigModule,
    tableConfigTitle.value,
    props.columns,
    defaultTableFilterKeys.value,
  );
  tableConfigOpen.value = false;
  message.success('表格配置已恢复');
}

async function handleSetCompany(value?: SystemFilterOption['value']) {
  state.setCompany(value);
  if (usesCascadeField('department')) {
    departmentSelectOptions.value = normalizeOptions(
      await getDepartmentOptionsApi(value),
    );
  }
  if (usesCascadeField('team')) {
    teamSelectOptions.value = normalizeOptions(await getTeamOptionsApi(value));
  }
}

async function handleSetDepartment(value?: SystemFilterOption['value']) {
  state.setDepartment(value);
  if (usesCascadeField('team')) {
    teamSelectOptions.value = normalizeOptions(
      await getTeamOptionsApi(value ?? state.filters.companyId),
    );
  }
}

async function loadPersonnelSelectOptions(companyId?: SystemFilterOption['value'], departmentId?: SystemFilterOption['value']) {
  departmentSelectOptions.value = normalizeOptions(
    await getDepartmentOptionsApi(companyId),
  );
  teamSelectOptions.value = normalizeOptions(
    await getTeamOptionsApi(departmentId ?? companyId),
  );
}

function confirmDelete() {
  const selectedCount = state.selectedCount.value;
  Modal.confirm({
    content:
      selectedCount > 1
        ? `将删除选中的 ${selectedCount} 条数据，删除后数据将不再出现在列表中。`
        : '删除后数据将不再出现在列表中。',
    okText: '删除',
    onOk: () => state.handleDelete(),
    title: selectedCount > 1 ? '确认批量删除？' : '确认删除选中记录？',
  });
}

async function handleCreate() {
  if (
    props.companyHierarchyMode &&
    shouldDisableCompanyCreateForPath(selectedCompanyPath.value)
  ) {
    message.warning('四级子公司下不能继续新增公司');
    return;
  }
  if (
    props.departmentHierarchyMode &&
    selectedDepartmentCompanyPath.value.length === 0
  ) {
    message.warning('未找到当前登录组织对应公司');
    return;
  }
  if (props.teamHierarchyMode) {
    const companyId = selectedTeamCompanyPath.value.at(-1)?.id;
    if (!companyId) {
      message.warning('请选择公司');
      return;
    }
    departmentSelectOptions.value = normalizeOptions(
      await getDepartmentOptionsApi(companyId),
    );
  }
  if (props.personnelHierarchyMode) {
    const companyId = selectedPersonnelCompanyPath.value.at(-1)?.id;
    if (!companyId) {
      message.warning('请选择公司');
      return;
    }
    await loadPersonnelSelectOptions(companyId);
  }
  state.openCreate();
}

async function handleEdit(record = state.selectedRow.value) {
  if (props.teamHierarchyMode && record?.companyOrgId) {
    departmentSelectOptions.value = normalizeOptions(
      await getDepartmentOptionsApi(record.companyOrgId as SystemManagementApi.Id),
    );
  }
  if (props.personnelHierarchyMode && record?.companyOrgId) {
    await loadPersonnelSelectOptions(
      record.companyOrgId as SystemManagementApi.Id,
      record.departmentOrgId as SystemManagementApi.Id | undefined,
    );
  }
  state.openEdit(record);
}

async function handleDrawerSubmit(data: Record<string, unknown>) {
  if (props.companyHierarchyMode) {
    const result = normalizeCompanyPayloadForSubmit(data, {
      mode: state.editingRecord.value ? 'edit' : 'create',
      selectedPath: selectedCompanyPath.value,
    });
    if (result.error) {
      message.warning(result.error);
      return;
    }
    await state.submit(result.data ?? data);
    return;
  }
  if (props.departmentHierarchyMode && !state.editingRecord.value) {
    const result = normalizeDepartmentPayloadForSubmit(data, {
      selectedPath: selectedDepartmentCompanyPath.value,
    });
    if (result.error) {
      message.warning(result.error);
      return;
    }
    await state.submit(result.data ?? data);
    return;
  }
  if (props.teamHierarchyMode && !state.editingRecord.value) {
    const result = normalizeTeamPayloadForSubmit(data, {
      selectedPath: selectedTeamCompanyPath.value,
    });
    if (result.error) {
      message.warning(result.error);
      return;
    }
    await state.submit(result.data ?? data);
    return;
  }
  if (props.personnelHierarchyMode && !state.editingRecord.value) {
    const result = normalizePersonnelPayloadForSubmit(data, {
      selectedPath: selectedPersonnelCompanyPath.value,
    });
    if (result.error) {
      message.warning(result.error);
      return;
    }
    await state.submit(result.data ?? data);
    return;
  }
  await state.submit(data);
}

async function handleDrawerFieldChange(
  key: string,
  value: unknown,
  formState: Record<string, unknown>,
) {
  if (!props.personnelHierarchyMode || key !== 'departmentOrgId') {
    return;
  }
  const companyId =
    (state.editingRecord.value?.companyOrgId as SystemManagementApi.Id | undefined) ??
    selectedPersonnelCompanyPath.value.at(-1)?.id;
  formState.teamOrgId = undefined;
  teamSelectOptions.value = normalizeOptions(
    await getTeamOptionsApi(
      (value as SystemManagementApi.Id | undefined) ?? companyId,
    ),
  );
}

function tagColor(status?: unknown) {
  if (status === 'ACTIVE') return 'green';
  if (status === 'INACTIVE') return 'red';
  return 'default';
}

function statusText(status?: unknown) {
  if (status === 'ACTIVE') return '启用';
  if (status === 'INACTIVE') return '停用';
  if (status === 'DRAFT') return '草稿';
  return String(status ?? '-');
}

function roleDisplayList(value: unknown) {
  if (!Array.isArray(value)) {
    return [];
  }
  return value
    .map((item) => {
      if (typeof item === 'string') return item;
      if (item && typeof item === 'object') {
        const role = item as Record<string, unknown>;
        return role.roleName || role.name || role.roleCode || role.id;
      }
      return item;
    })
    .filter((item) => item !== null && item !== undefined && item !== '')
    .map(String);
}

function handleSelectionChange(
  _keys: unknown[],
  selectedRows: Record<string, unknown>[],
) {
  state.selectRows(selectedRows);
  const selectedRecord = selectedRows[0];
  if (selectedRows.length === 1 && selectedRecord) {
    emit('rowSelect', selectedRecord);
  }
}

async function handleSelectOrganization(
  id?: SystemManagementApi.Id,
  keys: string[] = [],
) {
  selectedOrganizationKeys.value = keys;
  emit('organizationSelect', id, keys);
  state.setOrganization(id);
  state.selectRows([]);
  await state.load();
}

function handleExpandOrganization(keys: string[]) {
  expandedOrganizationKeys.value = keys;
}

onMounted(async () => {
  loadTableConfig();
  await Promise.all([loadCascadeOptions(), loadOrganizationTree()]);
  void state.load();
});
</script>

<template>
  <Page auto-content-height content-class="system-crud-page">
    <div
      :class="[
        'system-crud-layout',
        {
          'system-crud-layout--with-map': showDataMap,
          'system-crud-layout--map-collapsed': dataMapCollapsed,
        },
      ]"
    >
      <SystemDataMap
        v-if="showDataMap"
        :collapsed="dataMapCollapsed"
        :expanded-keys="expandedOrganizationKeys"
        :nodes="organizationNodes"
        :selected-keys="selectedOrganizationKeys"
        @select-organization="handleSelectOrganization"
        @toggle="dataMapCollapsed = !dataMapCollapsed"
        @update-expanded-keys="handleExpandOrganization"
      />
      <div class="system-crud-surface">
        <SystemFilterBar
          :cascade-fields="cascadeFields"
          :company-options="companySelectOptions"
          :department-options="departmentSelectOptions"
          :filters="state.filters"
          :show-cascade="showCascade"
          :show-status-filter="showStatusFilter"
          :team-options="teamSelectOptions"
          :visible-filters="visibleTableFilters"
          @apply="state.load"
          @reset="resetFilters"
          @set-company="handleSetCompany"
          @set-department="handleSetDepartment"
          @set-team="state.setTeam"
        />
        <SystemToolbar
          :can-use-single-row-action="state.canUseSingleRowAction.value"
          :can-use-row-actions="state.canUseRowActions.value"
          :hidden-actions="hiddenActions"
          :selected-count="state.selectedCount.value"
          :show-table-config="Boolean(props.tableConfigModule)"
          @delete="confirmDelete"
          @download-data="state.handleDownloadData"
          @download-template="state.handleDownloadTemplate"
          @edit="handleEdit()"
          @freeze="state.handleStatus('INACTIVE')"
          @new="handleCreate"
          @refresh="state.load"
          @table-config="openTableConfig"
          @unfreeze="state.handleStatus('ACTIVE')"
          @upload-data="state.handleUploadData"
        />
        <Table
          bordered
          class="system-crud-table"
          :columns="tableColumns"
          :data-source="tableRows"
          :loading="state.loading.value"
          :pagination="{ pageSize: tablePageSize, showSizeChanger: false, total: state.total.value }"
          :row-key="rowKey"
          :row-selection="{
            type: 'checkbox',
            selectedRowKeys: state.selectedRowKeys.value,
            onChange: handleSelectionChange,
          }"
          size="small"
          :scroll="tableScrollConfig"
        >
          <template #bodyCell="{ column, record, value }">
            <template v-if="column.dataIndex === 'status'">
              <Tag :color="tagColor(value)">{{ statusText(value) }}</Tag>
            </template>
            <template v-else-if="column.dataIndex === 'active'">
              <Tag :color="value ? 'green' : 'default'">
                {{ value ? '是' : '否' }}
              </Tag>
            </template>
            <template v-else-if="column.dataIndex === 'roles'">
              <template v-if="roleDisplayList(value).length > 0">
                <Tag v-for="role in roleDisplayList(value)" :key="role">
                  {{ role }}
                </Tag>
              </template>
              <span v-else>-</span>
            </template>
            <template v-else-if="column.dataIndex === 'actions'">
              <Button size="small" type="link" @click="handleEdit(record)">
                编辑
              </Button>
              <Button
                v-for="action in rowActions"
                :key="action.label"
                size="small"
                type="link"
                @click="action.onClick(record)"
              >
                {{ action.label }}
              </Button>
            </template>
          </template>
        </Table>
        <SystemUpsertDrawer
          v-model="state.drawerOpen.value"
          :fields="drawerFields"
          :record="state.editingRecord.value"
          :title="title"
          @field-change="handleDrawerFieldChange"
          @submit="handleDrawerSubmit"
        />
        <SystemTableConfigDrawer
          v-if="props.tableConfigModule"
          v-model:open="tableConfigOpen"
          :config="effectiveTableConfig"
          :auto-widths="tableConfigAutoWidths"
          :module-title="tableConfigTitle"
          :role-options="tableConfigRoleOptions"
          @delete="deleteTableConfig"
          @push="pushTableConfig"
          @save="saveTableConfig"
        />
      </div>
    </div>
  </Page>
</template>

<style scoped>
:global(.system-crud-page) {
  background: hsl(var(--background-deep));
  height: 100%;
  padding: 12px;
}

.system-crud-layout {
  height: 100%;
  min-height: 0;
}

.system-crud-layout--with-map {
  display: grid;
  gap: 12px;
  grid-template-columns: minmax(220px, 280px) minmax(0, 1fr);
  transition: grid-template-columns 0.2s ease;
}

.system-crud-layout--map-collapsed {
  grid-template-columns: 56px minmax(0, 1fr);
}

.system-crud-surface {
  background: hsl(var(--background));
  border: 1px solid hsl(var(--border));
  border-radius: 6px;
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.system-crud-table {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-height: 0;
  padding: 0 16px 16px;
}

.system-crud-table :deep(.ant-spin-nested-loading),
.system-crud-table :deep(.ant-spin-container) {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-height: 0;
}

.system-crud-table :deep(.ant-table) {
  flex: 1 1 auto;
  min-height: 0;
}

.system-crud-table :deep(.ant-table-container) {
  min-height: 0;
}

.system-crud-table :deep(.ant-table-body) {
  overscroll-behavior: contain;
}
</style>
