<script setup lang="ts">
import type { TableColumnsType } from 'ant-design-vue';

import { computed, onMounted, reactive, ref, watch } from 'vue';

import { Page } from '@vben/common-ui';

import {
  Button,
  Dropdown,
  Drawer,
  Input,
  Menu,
  message,
  Modal,
  Segmented,
  Space,
  Table,
  Tag,
} from 'ant-design-vue';

import { getPinganOrgTreeApi } from '#/api/pingan/pre-shift-meeting';
import {
  createTeamCheckItemLibraryApi,
  createTeamCheckTemplateApi,
  deleteTeamCheckItemLibraryApi,
  deleteTeamCheckTemplateApi,
  getTeamCheckItemLibraryApi,
  getTeamCheckTemplateResolveApi,
  getTeamCheckTemplatesApi,
  updateTeamCheckTemplateApi,
  updateTeamCheckTemplateStatusApi,
} from '#/api/system-management/team-check-item-template';
import type { SystemTeamCheckItemTemplateApi } from '#/api/system-management/team-check-item-template';
import type { SystemManagementApi } from '#/api/system-management/types';

import SystemDataMap from '../shared/SystemDataMap.vue';
import SystemFilterBar from '../shared/SystemFilterBar.vue';
import SystemTableConfigDrawer from '../shared/SystemTableConfigDrawer.vue';
import SystemUpsertDrawer from '../shared/SystemUpsertDrawer.vue';
import type { SystemCrudFilters, SystemFilterOption } from '../shared/useSystemCrudPage';
import { createSystemTableScrollConfig } from '../shared/useSystemCrudPage';
import type { SystemTableConfig } from '../shared/systemTableConfig';
import {
  applySystemTableConfigColumns,
  sortRowsBySystemTableConfig,
} from '../shared/systemTableConfig';
import {
  inspectionStageOptions,
  libraryColumns,
  libraryFields,
  preShiftMeetingLibraryFields,
  preShiftMeetingTemplateItemFields,
  safetyConfirmLibraryColumns,
  safetyConfirmTemplateColumns,
  statusTone,
  templateColumns,
  templateItemFields,
} from './team-check-item-template.config';

type Id = SystemManagementApi.Id;
type InspectionStage = SystemTeamCheckItemTemplateApi.InspectionStage;
type LibraryItem = SystemTeamCheckItemTemplateApi.LibraryItem;
type Template = SystemTeamCheckItemTemplateApi.Template;
type TemplateItem = SystemTeamCheckItemTemplateApi.TemplateItem;
type OrgNode = SystemManagementApi.OrganizationNode;

interface TemplateRow extends TemplateItem {
  key: string;
  requireImageLabel: string;
  requireVideoLabel: string;
}

const currentStage = ref<InspectionStage>('PRE_SHIFT_INSPECTION');
const filters = reactive<SystemCrudFilters>({
  companyId: undefined,
  departmentId: undefined,
  keyword: '',
  organizationId: undefined,
  status: 'all',
  teamId: undefined,
});
const orgNodes = ref<OrgNode[]>([]);
const expandedKeys = ref<string[]>([]);
const selectedKeys = ref<string[]>([]);
const dataMapCollapsed = ref(false);
const currentTemplate = ref<Template>();
const upperTemplate = ref<Template>();
const draftItems = ref<TemplateItem[]>([]);
const templateName = ref('');
const loading = ref(false);
const saving = ref(false);
const libraryOpen = ref(false);
const libraryRows = ref<LibraryItem[]>([]);
const libraryLoading = ref(false);
const selectedLibraryRowKeys = ref<Id[]>([]);
const libraryDrawerOpen = ref(false);
const templateItemDrawerOpen = ref(false);
const tableConfigOpen = ref(false);
const tableConfig = ref<SystemTableConfig>(createTableConfig());

const stageSegmentOptions: Array<{ label: string; value: InspectionStage }> =
  inspectionStageOptions.map((item) => ({
    label: item.label,
    value: item.value as InspectionStage,
  }));

const libraryTableColumns = computed<TableColumnsType<LibraryItem>>(
  () =>
    (currentStage.value === 'PRE_SHIFT_MEETING_CONFIRMATION'
      ? safetyConfirmLibraryColumns
      : libraryColumns) as TableColumnsType<LibraryItem>,
);
const templateColumnsForCurrentStage = computed(() =>
  currentStage.value === 'PRE_SHIFT_MEETING_CONFIRMATION'
    ? safetyConfirmTemplateColumns
    : templateColumns,
);
const stageScopedLibraryFields = libraryFields.filter(
  (field) => field.valueKey !== 'applicableStage',
);
const postShiftLibraryFields = stageScopedLibraryFields.filter(
  (field) => field.valueKey !== 'riskType',
);
const postShiftTemplateItemFields = templateItemFields.filter(
  (field) => field.valueKey !== 'riskType',
);
const libraryFieldsForCurrentStage = computed(() =>
  currentStage.value === 'PRE_SHIFT_MEETING_CONFIRMATION'
    ? preShiftMeetingLibraryFields
    : currentStage.value === 'POST_SHIFT_INSPECTION'
    ? postShiftLibraryFields
    : stageScopedLibraryFields,
);
const templateItemFieldsForCurrentStage = computed(() =>
  currentStage.value === 'PRE_SHIFT_MEETING_CONFIRMATION'
    ? preShiftMeetingTemplateItemFields
    : currentStage.value === 'POST_SHIFT_INSPECTION'
    ? postShiftTemplateItemFields
    : templateItemFields,
);
const libraryScroll = computed(() => createSystemTableScrollConfig(libraryTableColumns.value));
const canAddSelectedLibraryItems = computed(
  () =>
    selectedLibraryRowKeys.value.length > 0 &&
    !libraryLoading.value &&
    Boolean(filters.companyId),
);
const canDeleteSelectedLibraryItems = computed(
  () => selectedLibraryRowKeys.value.length > 0 && !libraryLoading.value,
);

const flatOrganizations = computed(() => flattenOrganizations(orgNodes.value));
const companyOptions = computed(() => optionByType('COMPANY'));
const departmentOptions = computed(() => childOptionsUnderSelectedCompany('DEPARTMENT'));
const teamOptions = computed(() => childOptionsUnderSelectedCompany('TEAM'));
const selectedPath = computed(() => {
  const selectedId = filters.teamId ?? filters.departmentId ?? filters.companyId;
  return selectedId === undefined ? [] : findOrganizationPath(orgNodes.value, selectedId);
});
const selectedScope = computed(() => {
  if (filters.teamId) return 'TEAM';
  if (filters.departmentId) return 'DEPARTMENT';
  if (filters.companyId) return 'COMPANY';
  return '';
});
const rawTemplateRows = computed<TemplateRow[]>(() =>
  draftItems.value.map((item, index) => ({
    ...item,
    key: `${item.libraryItemId ?? item.checkItem}-${index}`,
    requireImageLabel: item.requireImage ? '需要' : '不需要',
    requireVideoLabel: item.requireVideo ? '需要' : '不需要',
    sortOrder: item.sortOrder ?? index + 1,
  })),
);
const tableColumns = computed<TableColumnsType<TemplateRow>>(
  () =>
    applySystemTableConfigColumns(
      templateColumnsForCurrentStage.value,
      tableConfig.value,
      rawTemplateRows.value as unknown as Array<Record<string, unknown>>,
    ) as TableColumnsType<TemplateRow>,
);
const tableScroll = computed(() => createSystemTableScrollConfig(tableColumns.value));
const templateRows = computed<TemplateRow[]>(
  () =>
    sortRowsBySystemTableConfig(
      rawTemplateRows.value as unknown as Array<Record<string, unknown>>,
      tableConfig.value,
    ) as unknown as TemplateRow[],
);
const sourceTagText = computed(() => {
  if (currentTemplate.value) return '当前组织模板';
  if (upperTemplate.value && selectedScope.value !== 'COMPANY') return '上级模板可复制';
  return filters.companyId ? '未配置模板' : '未选择组织';
});
const sourceTagColor = computed(() => {
  if (currentTemplate.value) return 'green';
  if (upperTemplate.value && selectedScope.value !== 'COMPANY') return 'orange';
  return 'default';
});
const currentTemplateStatusLabel = computed(
  () => currentTemplate.value?.statusLabel || currentTemplate.value?.status || '未配置',
);
const currentTemplateStatusColor = computed(
  () => statusTone[currentTemplate.value?.status || ''] || 'default',
);
const canSave = computed(() => Boolean(filters.companyId) && draftItems.value.length > 0);

function createTableConfig(): SystemTableConfig {
  return {
    code: 'TEAM_CHECK_ITEM_TEMPLATE_STANDARD',
    fields: templateColumns.map((column, index) => ({
      enabled: true,
      fixed: column.fixed,
      key: String(column.dataIndex),
      order: index + 1,
      sort: 'none',
      title: String(column.title),
      width: Number(column.width ?? 120),
      widthMode: 'auto',
    })),
    filters: [
      { enabled: true, key: 'keyword', title: '关键词' },
      { enabled: true, key: 'status', title: '状态' },
      { enabled: true, key: 'company', title: '公司' },
      { enabled: true, key: 'department', title: '部门' },
      { enabled: true, key: 'team', title: '班组' },
    ],
    name: '班组检查项模板',
    pinned: false,
    roleIds: [],
  };
}

function saveTableConfig(config: SystemTableConfig) {
  tableConfig.value = config;
  tableConfigOpen.value = false;
  message.success('表格配置已保存');
}

function flattenOrganizations(nodes: OrgNode[]): OrgNode[] {
  return nodes.flatMap((node) => [node, ...flattenOrganizations(node.children ?? [])]);
}

function optionByType(orgType: string): SystemFilterOption[] {
  return flatOrganizations.value
    .filter((node) => node.orgType === orgType)
    .map((node) => ({ label: node.title, value: node.id }));
}

function childOptionsUnderSelectedCompany(orgType: 'DEPARTMENT' | 'TEAM'): SystemFilterOption[] {
  if (orgType === 'TEAM' && filters.departmentId !== undefined) {
    return optionsUnder(filters.departmentId, orgType);
  }
  if (filters.companyId !== undefined) {
    return optionsUnder(filters.companyId, orgType);
  }
  return optionByType(orgType);
}

function optionsUnder(parentId: Id, orgType: 'DEPARTMENT' | 'TEAM'): SystemFilterOption[] {
  const path = findOrganizationPath(orgNodes.value, parentId);
  const parent = path.at(-1);
  return flattenOrganizations(parent?.children ?? [])
    .filter((node) => node.orgType === orgType)
    .map((node) => ({ label: node.title, value: node.id }));
}

function findOrganizationPath(nodes: OrgNode[], id: Id): OrgNode[] {
  for (const node of nodes) {
    if (String(node.id) === String(id)) return [node];
    const childPath = findOrganizationPath(node.children ?? [], id);
    if (childPath.length) return [node, ...childPath];
  }
  return [];
}

function applyPath(path: OrgNode[]) {
  const company = [...path].reverse().find((node) => node.orgType === 'COMPANY');
  const department = [...path].reverse().find((node) => node.orgType === 'DEPARTMENT');
  const team = [...path].reverse().find((node) => node.orgType === 'TEAM');
  filters.companyId = company?.id;
  filters.departmentId = department?.id;
  filters.teamId = team?.id;
}

function initializeCompanyLevelExpandedKeys(nodes: OrgNode[]) {
  const keys = new Set<string>();

  function visit(node: OrgNode, ancestorKeys: string[]) {
    if (node.orgType === 'COMPANY') {
      for (const key of ancestorKeys) {
        keys.add(key);
      }
      return;
    }
    for (const child of node.children ?? []) {
      visit(child, [...ancestorKeys, node.key]);
    }
  }

  for (const node of nodes) {
    visit(node, []);
  }
  return [...keys];
}

async function loadOrganizations() {
  const nodes = (await getPinganOrgTreeApi()) as OrgNode[];
  orgNodes.value = nodes;
  expandedKeys.value = initializeCompanyLevelExpandedKeys(nodes);
}

async function loadTemplate() {
  if (!filters.companyId) {
    currentTemplate.value = undefined;
    draftItems.value = cloneItems([]);
    templateName.value = '';
    return;
  }
  loading.value = true;
  try {
    const result = await getTeamCheckTemplatesApi({
      companyOrgId: filters.companyId,
      departmentOrgId: filters.departmentId,
      exactScope: true,
      page: 1,
      pageSize: 1,
      stage: currentStage.value,
      status: 'all',
      teamOrgId: filters.teamId,
    });
    const exactTemplate = result.items[0];
    currentTemplate.value = exactTemplate || undefined;
    draftItems.value = cloneItems(exactTemplate?.items ?? []);
    templateName.value = exactTemplate?.name ?? defaultTemplateName();
  } catch {
    currentTemplate.value = undefined;
    draftItems.value = cloneItems([]);
    templateName.value = defaultTemplateName();
  } finally {
    loading.value = false;
  }
}

async function loadUpperTemplate() {
  upperTemplate.value = undefined;
  if (!filters.companyId || selectedScope.value === 'COMPANY') {
    return;
  }
  if (filters.teamId && filters.departmentId) {
    upperTemplate.value = await resolveTemplateSource({
      companyOrgId: filters.companyId,
      departmentOrgId: filters.departmentId,
      stage: currentStage.value,
    });
    if (upperTemplate.value) return;
  }
  upperTemplate.value = await resolveTemplateSource({
    companyOrgId: filters.companyId,
    stage: currentStage.value,
  });
}

async function resolveTemplateSource(params: {
  companyOrgId: Id;
  departmentOrgId?: Id;
  stage: InspectionStage;
}) {
  try {
    return (await getTeamCheckTemplateResolveApi(params)) || undefined;
  } catch {
    return undefined;
  }
}

async function refreshTemplates() {
  await Promise.all([loadTemplate(), loadUpperTemplate()]);
}

function cloneItems(items: TemplateItem[]) {
  return items.map((item) => ({ ...item }));
}

function defaultTemplateName() {
  const orgName = selectedPath.value.at(-1)?.title ?? '当前组织';
  const stageLabel = inspectionStageOptions.find((item) => item.value === currentStage.value)?.label ?? '';
  return `${orgName}${stageLabel}模板`;
}

function resetFilters() {
  filters.companyId = undefined;
  filters.departmentId = undefined;
  filters.teamId = undefined;
  filters.keyword = '';
  filters.status = 'all';
  selectedKeys.value = [];
  void refreshTemplates();
}

function handleSelectOrganization(id?: Id, keys: string[] = []) {
  selectedKeys.value = keys;
  const path = id === undefined ? [] : findOrganizationPath(orgNodes.value, id);
  applyPath(path);
  void refreshTemplates();
}

function setCompany(value?: Id) {
  if (value === undefined) {
    filters.companyId = undefined;
    filters.departmentId = undefined;
    filters.teamId = undefined;
  } else {
    applyPath(findOrganizationPath(orgNodes.value, value));
  }
  void refreshTemplates();
}

function setDepartment(value?: Id) {
  filters.departmentId = value;
  filters.teamId = undefined;
  if (value !== undefined) applyPath(findOrganizationPath(orgNodes.value, value));
  void refreshTemplates();
}

function setTeam(value?: Id) {
  filters.teamId = value;
  if (value !== undefined) applyPath(findOrganizationPath(orgNodes.value, value));
  void refreshTemplates();
}

function copyInheritedTemplate() {
  draftItems.value = cloneItems(upperTemplate.value?.items ?? []);
  if (!currentTemplate.value) {
    templateName.value = defaultTemplateName();
  }
  message.success('已复制上级模板，可保存为当前组织模板');
}

async function openCreateTemplate() {
  if (!filters.companyId) {
    message.warning('请选择组织');
    return;
  }
  if (currentTemplate.value) {
    message.warning('当前组织已存在模板，可直接编辑当前模板');
    return;
  }
  draftItems.value = cloneItems([]);
  if (!currentTemplate.value) {
    templateName.value = defaultTemplateName();
  }
  message.success('已进入当前组织模板编辑');
}

async function saveTemplate() {
  if (!canSave.value) {
    message.warning('请选择组织并添加检查项');
    return;
  }
  saving.value = true;
  try {
    const payload = {
      companyOrgId: filters.companyId,
      departmentOrgId: filters.departmentId,
      inspectionStage: currentStage.value,
      items: draftItems.value,
      name: templateName.value || defaultTemplateName(),
      status: currentTemplate.value?.status || 'ACTIVE' as const,
      teamOrgId: filters.teamId,
    };
    if (currentTemplate.value) {
      await updateTeamCheckTemplateApi(currentTemplate.value.id, payload);
    } else {
      await createTeamCheckTemplateApi(payload);
    }
    message.success('班组检查项模板已保存');
    await refreshTemplates();
  } finally {
    saving.value = false;
  }
}

async function removeTemplateItem(row: Record<string, any>) {
  const nextItems = draftItems.value.filter(
    (item, index) => `${item.libraryItemId ?? item.checkItem}-${index}` !== row.key,
  );
  if (nextItems.length === 0) {
    message.warning('模板至少保留一个检查项');
    return;
  }
  draftItems.value = nextItems;
  await saveTemplate();
}

async function openLibrary() {
  libraryOpen.value = true;
  await loadLibrary();
}

async function loadLibrary() {
  libraryLoading.value = true;
  try {
    const result = await getTeamCheckItemLibraryApi({
      keyword: filters.keyword || undefined,
      page: 1,
      pageSize: 100,
      stage: currentStage.value,
      status: 'ACTIVE',
    });
    libraryRows.value = result.items;
    selectedLibraryRowKeys.value = selectedLibraryRowKeys.value.filter((id) =>
      result.items.some((item) => String(item.id) === String(id)),
    );
  } finally {
    libraryLoading.value = false;
  }
}

function handleLibrarySelectionChange(keys: Array<number | string>) {
  selectedLibraryRowKeys.value = keys as Id[];
}

function templateItemFromLibraryItem(item: LibraryItem, sortOrder: number): TemplateItem {
  return {
    checkItem: item.checkItem,
    defaultCheckResult: item.defaultCheckResult || '无隐患',
    defaultFollowUpPlan: item.defaultFollowUpPlan,
    defaultRectificationDescription: item.defaultRectificationDescription,
    libraryItemId: item.id,
    requireImage: item.requireImage,
    requireVideo: item.requireVideo,
    riskType: item.riskType,
    sortOrder,
  };
}

async function addLibraryItem(record: Record<string, any>) {
  if (!filters.companyId) {
    message.warning('请选择组织');
    return;
  }
  const item = record as LibraryItem;
  draftItems.value = [
    ...draftItems.value,
    templateItemFromLibraryItem(item, draftItems.value.length + 1),
  ];
  await saveTemplate();
}

async function addSelectedLibraryItems() {
  if (!filters.companyId) {
    message.warning('请选择组织');
    return;
  }
  const selectedIds = new Set(selectedLibraryRowKeys.value.map((id) => String(id)));
  const selectedItems = libraryRows.value.filter((item) => selectedIds.has(String(item.id)));
  if (selectedItems.length === 0) {
    message.warning('请选择要加入的公共检查项');
    return;
  }
  const startOrder = draftItems.value.length;
  draftItems.value = [
    ...draftItems.value,
    ...selectedItems.map((item, index) =>
      templateItemFromLibraryItem(item, startOrder + index + 1),
    ),
  ];
  await saveTemplate();
  selectedLibraryRowKeys.value = [];
}

async function addTemplateOnlyItem(data: Record<string, unknown>) {
  if (!filters.companyId) {
    message.warning('请选择组织');
    return;
  }
  draftItems.value = [
    ...draftItems.value,
    {
      checkItem: String(data.checkItem ?? ''),
      defaultCheckResult: String(data.defaultCheckResult ?? '无隐患'),
      defaultFollowUpPlan: String(data.defaultFollowUpPlan ?? ''),
      defaultRectificationDescription: String(data.defaultRectificationDescription ?? ''),
      requireImage: Boolean(data.requireImage),
      requireVideo: Boolean(data.requireVideo),
      riskType: String(data.riskType ?? ''),
      sortOrder:
        data.sortOrder === undefined || data.sortOrder === ''
          ? draftItems.value.length + 1
          : Number(data.sortOrder),
    },
  ];
  templateItemDrawerOpen.value = false;
  await saveTemplate();
}

async function createLibraryItem(data: Record<string, unknown>) {
  await createTeamCheckItemLibraryApi({
    applicableStage: currentStage.value,
    checkItem: String(data.checkItem ?? ''),
    defaultCheckResult: String(data.defaultCheckResult ?? '无隐患'),
    requireImage: false,
    requireVideo: false,
    riskType: String(data.riskType ?? ''),
    sortOrder: Number(data.sortOrder ?? 0),
    status: (data.status as SystemTeamCheckItemTemplateApi.Status) || 'ACTIVE',
  });
  libraryDrawerOpen.value = false;
  await loadLibrary();
}

function deleteSelectedLibraryItems() {
  if (selectedLibraryRowKeys.value.length === 0) {
    message.warning('请选择要删除的公共检查项');
    return;
  }
  const selectedIds = [...selectedLibraryRowKeys.value];
  Modal.confirm({
    content: `确认删除已选 ${selectedIds.length} 条公共检查项？`,
    onOk: async () => {
      await Promise.all(selectedIds.map((id) => deleteTeamCheckItemLibraryApi(id)));
      selectedLibraryRowKeys.value = [];
      message.success('已删除选中的公共检查项');
      await loadLibrary();
    },
    title: '批量删除公共检查项',
  });
}

function updateStatus(status: SystemTeamCheckItemTemplateApi.Status) {
  if (!currentTemplate.value) return;
  void updateTeamCheckTemplateStatusApi(currentTemplate.value.id, status).then(refreshTemplates);
}

function deleteCurrentTemplate() {
  if (!currentTemplate.value) return;
  Modal.confirm({
    content: `确认删除「${currentTemplate.value.name}」？`,
    onOk: async () => {
      await deleteTeamCheckTemplateApi(currentTemplate.value!.id);
      message.success('模板已删除');
      await refreshTemplates();
    },
    title: '删除班组检查项模板',
  });
}

watch(currentStage, () => {
  void refreshTemplates();
});

onMounted(async () => {
  await loadOrganizations();
  await refreshTemplates();
});
</script>

<template>
  <Page class="team-check-item-template-page">
    <div
      :class="[
        'team-check-item-template-page__layout',
        {
          'team-check-item-template-page__layout--map-collapsed':
            dataMapCollapsed,
        },
      ]"
    >
      <SystemDataMap
        :collapsed="dataMapCollapsed"
        :expanded-keys="expandedKeys"
        :nodes="orgNodes"
        :selected-keys="selectedKeys"
        @select-organization="handleSelectOrganization"
        @toggle="dataMapCollapsed = !dataMapCollapsed"
        @update-expanded-keys="expandedKeys = $event"
      />

      <section class="team-check-item-template-page__main">
        <SystemFilterBar
          :company-options="companyOptions"
          :department-options="departmentOptions"
          :filters="filters"
          :team-options="teamOptions"
          @apply="refreshTemplates"
          @reset="resetFilters"
          @set-company="setCompany"
          @set-department="setDepartment"
          @set-team="setTeam"
        />

        <div class="team-check-item-template-page__stage">
          <Segmented v-model:value="currentStage" :options="stageSegmentOptions" />
          <Space>
            <Tag :color="sourceTagColor">
              {{ sourceTagText }}
            </Tag>
            <Tag :color="currentTemplateStatusColor">
              模板状态：{{ currentTemplateStatusLabel }}
            </Tag>
            <Input
              v-model:value="templateName"
              class="team-check-item-template-page__name"
              placeholder="模板名称"
            />
            <Button type="primary" @click="openCreateTemplate">新建模板</Button>
            <Button @click="templateItemDrawerOpen = true">新增模板检查项</Button>
            <Button :disabled="!upperTemplate" @click="copyInheritedTemplate">
              复制上级模板
            </Button>
            <Button @click="openLibrary">检查项库</Button>
            <Button @click="tableConfigOpen = true">表格配置</Button>
            <Button :loading="saving" type="primary" @click="saveTemplate">
              保存模板
            </Button>
            <Dropdown>
              <Button :disabled="!currentTemplate">模板操作</Button>
              <template #overlay>
                <Menu>
                  <Menu.Item key="disable" @click="updateStatus('INACTIVE')">
                    停用
                  </Menu.Item>
                  <Menu.Item key="enable" @click="updateStatus('ACTIVE')">
                    启用
                  </Menu.Item>
                  <Menu.Item key="delete" danger @click="deleteCurrentTemplate">
                    删除
                  </Menu.Item>
                </Menu>
              </template>
            </Dropdown>
          </Space>
        </div>

        <Table
          bordered
          :columns="tableColumns"
          :data-source="templateRows"
          :loading="loading"
          :pagination="false"
          row-key="key"
          size="small"
          :scroll="tableScroll"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'actions'">
              <Button danger size="small" type="link" @click="removeTemplateItem(record)">
                移除
              </Button>
            </template>
          </template>
        </Table>
      </section>
    </div>

    <Drawer
      :open="libraryOpen"
      title="检查项库"
      width="900"
      @close="libraryOpen = false"
      @update:open="libraryOpen = $event"
    >
      <div class="team-check-item-template-page__library-toolbar">
        <Button type="primary" @click="libraryDrawerOpen = true">
          新增公共检查项
        </Button>
        <Button
          :disabled="!canAddSelectedLibraryItems"
          type="primary"
          @click="addSelectedLibraryItems"
        >
          批量加入
        </Button>
        <Button
          danger
          :disabled="!canDeleteSelectedLibraryItems"
          @click="deleteSelectedLibraryItems"
        >
          批量删除
        </Button>
        <Button @click="loadLibrary">刷新</Button>
      </div>
      <Table
        bordered
        :columns="libraryTableColumns"
        :data-source="libraryRows"
        :loading="libraryLoading"
        row-key="id"
        :row-selection="{
          onChange: handleLibrarySelectionChange,
          selectedRowKeys: selectedLibraryRowKeys,
        }"
        size="small"
        :scroll="libraryScroll"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'requireImageLabel'">
            {{ record.requireImage ? '需要' : '不需要' }}
          </template>
          <template v-else-if="column.dataIndex === 'requireVideoLabel'">
            {{ record.requireVideo ? '需要' : '不需要' }}
          </template>
          <template v-else-if="column.dataIndex === 'statusLabel'">
            <Tag :color="statusTone[record.status || 'DRAFT']">
              {{ record.statusLabel || record.status }}
            </Tag>
          </template>
          <template v-else-if="column.dataIndex === 'actions'">
            <Button size="small" type="link" @click="addLibraryItem(record)">
              加入
            </Button>
          </template>
        </template>
      </Table>
    </Drawer>

    <SystemUpsertDrawer
      v-model="libraryDrawerOpen"
      :fields="libraryFieldsForCurrentStage"
      title="公共检查项"
      @submit="createLibraryItem"
    />

    <SystemUpsertDrawer
      v-model="templateItemDrawerOpen"
      :fields="templateItemFieldsForCurrentStage"
      title="模板检查项"
      @submit="addTemplateOnlyItem"
    />

    <SystemTableConfigDrawer
      v-model:open="tableConfigOpen"
      :config="tableConfig"
      module-title="班组检查项模板"
      @save="saveTableConfig"
    />
  </Page>
</template>

<style scoped>
.team-check-item-template-page {
  min-height: 100%;
}

.team-check-item-template-page__layout {
  display: grid;
  grid-template-columns: minmax(220px, 280px) minmax(0, 1fr);
  gap: 12px;
  min-height: calc(100vh - 150px);
}

.team-check-item-template-page__layout--map-collapsed {
  grid-template-columns: 56px minmax(0, 1fr);
}

.team-check-item-template-page__main {
  min-width: 0;
  overflow: hidden;
  border: 1px solid hsl(var(--border));
  border-radius: 6px;
  background: hsl(var(--background));
}

.team-check-item-template-page__stage {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  justify-content: space-between;
  border-top: 1px solid hsl(var(--border));
  border-bottom: 1px solid hsl(var(--border));
  padding: 12px 16px;
}

.team-check-item-template-page__name {
  width: 260px;
}

.team-check-item-template-page__library-toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}

@media (max-width: 960px) {
  .team-check-item-template-page__layout {
    grid-template-columns: 1fr;
  }
}
</style>
