<script setup lang="ts">
import type { CSSProperties } from 'vue';
import type { TableProps } from 'ant-design-vue';

import {
  computed,
  onActivated,
  onBeforeUnmount,
  onMounted,
  reactive,
  ref,
} from 'vue';

import { VbenScrollbar } from '@vben/common-ui';
import { useUserStore } from '@vben/stores';
import {
  Button,
  Drawer,
  Form,
  Input,
  message,
  Select,
  Space,
  Table,
  Tree,
  Upload,
} from 'ant-design-vue';

import type { PinganRiskLevelControlApi } from '#/api/pingan/risk-level-control';
import type { SystemTableConfig } from '#/views/system-management/shared/systemTableConfig';

import {
  getPinganCompanyOrgTreeApi,
  getPinganOrgTreeApi,
} from '#/api/pingan/pre-shift-meeting';
import {
  createRiskControlHazardApi,
  createRiskControlLibraryApi,
  downloadRiskControlDataApi,
  downloadRiskControlTemplateApi,
  getRiskControlHazardsApi,
  getRiskControlLibrariesApi,
  updateRiskControlHazardApi,
  uploadRiskControlDataApi,
} from '#/api/pingan/risk-level-control';
import RiskControlTableConfigDrawer from '#/views/system-management/shared/SystemTableConfigDrawer.vue';
import {
  collectExpandableOrganizationKeys,
  filterOrganizationTree,
  getCascadedOrganizationOptions,
} from '#/views/pingan/pre-shift-meeting/pre-shift-meeting.data';
import type {
  OrganizationId,
  OrganizationNode,
} from '#/views/pingan/pre-shift-meeting/pre-shift-meeting.data';
import {
  createDataMapCollapseState,
  createDataMapResizeState,
  dataMapTreeContinuousScrollConfig,
} from '#/views/pingan/pre-shift-meeting/pre-shift-meeting.view-state';
import { canShowPinganDataMap } from '#/views/pingan/shared/data-map-permission';
import {
  applyScopedPinganOrganizationDefaults,
  getVisiblePinganOrganizationFilterKeys,
  isPinganOrganizationFilterLocked,
  resolveScopedPinganOrganizationDefaults,
} from '#/views/pingan/shared/organization-filter-permission';
import {
  applySystemTableConfigColumns,
  calculateSystemTableAutoWidth,
  createDefaultSystemTableConfig,
  loadSystemTableConfig,
  removeSystemTableConfig,
  saveSystemTableConfig,
} from '#/views/system-management/shared/systemTableConfig';

import {
  riskControlColumns,
  riskControlSearchFilters,
  riskControlTableConfigModule,
} from './risk-level-control.config';

type HazardPayload = PinganRiskLevelControlApi.HazardPayload;
type HazardRow = PinganRiskLevelControlApi.Hazard;
type LibraryRow = PinganRiskLevelControlApi.Library;

const props = withDefaults(defineProps<{ pageTitle?: string }>(), {
  pageTitle: '风险分级管控',
});

const userStore = useUserStore();
const libraryKeyword = ref('');
const libraries = ref<LibraryRow[]>([]);
const hazards = ref<HazardRow[]>([]);
const organizationNodes = ref<OrganizationNode[]>([]);
const organizationScopeNodes = ref<OrganizationNode[]>([]);
const companyOptions = ref<Array<{ label: string; value: string | number }>>([]);
const currentLibraryId = ref<number | string>();
const selectedCompanyId = ref<OrganizationId>();
const selectedHazardId = ref<number | string>();
const expandedOrganizationKeys = ref<string[]>([]);
const selectedOrganizationKeys = ref<string[]>([]);
const treeKeyword = ref('');
const loadingLibraries = ref(false);
const loadingHazards = ref(false);
const saving = ref(false);

const libraryDrawerOpen = ref(false);
const hazardDrawerOpen = ref(false);
const tableConfigOpen = ref(false);
const editingHazard = ref<HazardRow>();
const libraryName = ref('');

const hazardForm = reactive<HazardPayload>(createEmptyHazard());

const tableConfig = ref<SystemTableConfig>(
  createDefaultSystemTableConfig(
    riskControlTableConfigModule,
    props.pageTitle,
    riskControlColumns,
    [...riskControlSearchFilters],
  ),
);

const { dataMapToggleLabel, isDataMapCollapsed, toggleDataMap } =
  createDataMapCollapseState();
const {
  beginDataMapResize,
  dataMapWidth,
  endDataMapResize,
  isDataMapResizing,
  updateDataMapResize,
} = createDataMapResizeState();

const dataMapGridStyle = computed<CSSProperties>(() => ({
  '--data-map-width': `${dataMapWidth.value}px`,
}));
const shouldShowDataMap = computed(() =>
  canShowPinganDataMap(userStore.userInfo?.roles),
);
const currentUserRoles = computed(() => userStore.userInfo?.roles ?? []);
const currentUserOrgDefaults = computed(() =>
  resolveScopedPinganOrganizationDefaults(
    userStore.userInfo?.orgId,
    organizationScopeNodes.value,
  ),
);
const visibleOrganizationFilterKeys = computed(() =>
  getVisiblePinganOrganizationFilterKeys(currentUserRoles.value),
);
const isCompanyFilterVisible = computed(() =>
  visibleOrganizationFilterKeys.value.includes('company'),
);
const isCompanyFilterLocked = computed(() =>
  isPinganOrganizationFilterLocked(currentUserRoles.value, 'company'),
);

const filteredOrganizationTree = computed(() =>
  filterOrganizationTree(organizationNodes.value, treeKeyword.value),
);

const filteredLibraries = computed(() =>
  libraries.value.filter(
    (item) =>
      !selectedCompanyId.value ||
      String(item.companyId) === String(selectedCompanyId.value),
  ),
);

const libraryOptions = computed(() =>
  filteredLibraries.value.map((item) => ({
    label: item.name,
    value: item.id,
  })),
);

const currentLibrary = computed(() =>
  filteredLibraries.value.find((item) => item.id === currentLibraryId.value),
);

const tableDataSourceText = computed(() =>
  currentLibrary.value
    ? `数据来自 ${currentLibrary.value.name}`
    : '请选择或新建风险库',
);

const configuredColumns = computed(() =>
  applySystemTableConfigColumns(riskControlColumns, tableConfig.value, hazards.value),
);

const autoWidths = computed(() =>
  Object.fromEntries(
    riskControlColumns.map((column) => [
      String(column.dataIndex),
      calculateSystemTableAutoWidth(column, hazards.value),
    ]),
  ),
);

const selectedHazard = computed(() =>
  hazards.value.find((item) => item.id === selectedHazardId.value),
);

const hazardRowSelection = computed<TableProps<HazardRow>['rowSelection']>(() => ({
  onChange: (keys) => {
    selectedHazardId.value = keys[0];
  },
  selectedRowKeys: selectedHazardId.value ? [selectedHazardId.value] : [],
  type: 'radio',
}));

const hazardFields: Array<{
  component?: 'textarea';
  key: keyof HazardPayload;
  label: string;
  required?: boolean;
}> = [
  { key: 'riskPoint', label: '风险点', required: true },
  { key: 'dangerSource', label: '危险源', required: true },
  { key: 'riskInfluenceFactors', label: '风险影响因素', component: 'textarea' },
  { key: 'accidentType', label: '事故类型' },
  { key: 'likelihood', label: '事故发生的可能性（L）' },
  { key: 'exposureFrequency', label: '人员暴露于危险环境中的频繁程度（E）' },
  { key: 'consequence', label: '发生事故可能造成的后果（C）' },
  { key: 'riskValue', label: '风险值（D）' },
  { key: 'riskLevel', label: '风险等级' },
  { key: 'engineeringMeasures', label: '关键技术与工程措施', component: 'textarea' },
  {
    key: 'managementMeasures',
    label: '关键人员素养与系统管理措施',
    component: 'textarea',
  },
  {
    key: 'emergencyMeasures',
    label: '关键个体防护与应急管理措施',
    component: 'textarea',
  },
  { key: 'superiorResponsiblePerson', label: '上级单位责任人' },
  { key: 'responsibleDepartment', label: '责任部门' },
  { key: 'responsibleContact', label: '责任人/联系方式' },
  { key: 'possibleHazard', label: '可能产生的事故隐患', component: 'textarea' },
  { key: 'rectificationMeasures', label: '隐患整治措施', component: 'textarea' },
];

onMounted(async () => {
  tableConfig.value = loadSystemTableConfig(
    riskControlTableConfigModule,
    '风险分级管控',
    riskControlColumns,
    [...riskControlSearchFilters],
  );
  await loadCompanies();
  await loadLibraries();
  openDataMap();
});

onActivated(() => {
  openDataMap();
});

onBeforeUnmount(() => {
  cleanupDataMapResizeListeners();
});

function openDataMap() {
  if (isDataMapCollapsed.value) {
    toggleDataMap();
  }
  if (organizationNodes.value.length) {
    expandedOrganizationKeys.value = collectExpandableOrganizationKeys(
      organizationNodes.value,
    );
  }
}

function createEmptyHazard(): HazardPayload {
  return {
    accidentType: '',
    company: '',
    companyId: undefined,
    consequence: '',
    dangerSource: '',
    emergencyMeasures: '',
    engineeringMeasures: '',
    exposureFrequency: '',
    likelihood: '',
    managementMeasures: '',
    possibleHazard: '',
    rectificationMeasures: '',
    responsibleContact: '',
    responsibleDepartment: '',
    riskInfluenceFactors: '',
    riskLevel: '',
    riskPoint: '',
    riskValue: '',
    superiorResponsiblePerson: '',
  };
}

function findOrganizationMatch(
  nodes: OrganizationNode[],
  key: string,
  ancestors: OrganizationNode[] = [],
): { ancestors: OrganizationNode[]; node: OrganizationNode } | undefined {
  for (const node of nodes) {
    if (node.key === key) {
      return { ancestors, node };
    }
    const matched = findOrganizationMatch(node.children ?? [], key, [
      ...ancestors,
      node,
    ]);
    if (matched) {
      return matched;
    }
  }
  return undefined;
}

function companyIdFromDataMapKey(key?: string): OrganizationId | undefined {
  if (!key) {
    return undefined;
  }
  const matched = findOrganizationMatch(organizationNodes.value, key);
  if (!matched) {
    return undefined;
  }
  if (matched.node.orgType?.toUpperCase() === 'COMPANY') {
    return matched.node.id;
  }
  return matched.ancestors
    .slice()
    .reverse()
    .find((node) => node.orgType?.toUpperCase() === 'COMPANY')?.id;
}

function syncCurrentLibrarySelection() {
  const exists = filteredLibraries.value.some(
    (item) => item.id === currentLibraryId.value,
  );
  if (!exists) {
    currentLibraryId.value = filteredLibraries.value[0]?.id;
  }
}

async function loadCompanies() {
  const [companyTree, fullTree] = await Promise.all([
    getPinganCompanyOrgTreeApi(),
    getPinganOrgTreeApi(),
  ]);
  organizationNodes.value = companyTree;
  organizationScopeNodes.value = fullTree;
  expandedOrganizationKeys.value = collectExpandableOrganizationKeys(companyTree);
  companyOptions.value = getCascadedOrganizationOptions(companyTree, undefined, [
    'COMPANY',
  ]).map((item) => ({
    label: item.label,
    value: item.value,
  }));
  enforceCompanyScope();
}

function enforceCompanyScope() {
  const companyId = currentUserOrgDefaults.value.companyId;
  if (isCompanyFilterLocked.value && companyId !== undefined) {
    selectedCompanyId.value = companyId;
  }
}

function enforceHazardFormCompanyScope() {
  Object.assign(
    hazardForm,
    applyScopedPinganOrganizationDefaults(
      { companyId: hazardForm.companyId },
      currentUserOrgDefaults.value,
      currentUserRoles.value,
    ),
  );
  hazardForm.company =
    selectedCompanyLabel(hazardForm.companyId) || hazardForm.company;
}

async function loadLibraries() {
  loadingLibraries.value = true;
  try {
    const result = await getRiskControlLibrariesApi({
      keyword: libraryKeyword.value || undefined,
      page: 1,
      pageSize: 100,
    });
    libraries.value = result.items;
    syncCurrentLibrarySelection();
    if (currentLibraryId.value) {
      await loadHazards();
    } else {
      hazards.value = [];
    }
  } finally {
    loadingLibraries.value = false;
  }
}

async function handleSearch() {
  await loadLibraries();
}

async function handleCompanyFilterChange(value: unknown) {
  if (isCompanyFilterLocked.value) {
    enforceCompanyScope();
    syncCurrentLibrarySelection();
    await loadHazards();
    return;
  }
  selectedCompanyId.value =
    typeof value === 'number' || typeof value === 'string' ? value : undefined;
  selectedOrganizationKeys.value = [];
  syncCurrentLibrarySelection();
  await loadHazards();
}

async function handleLibraryChange() {
  await loadHazards();
}

function handleDataMapSelect(keys: unknown[]) {
  if (!shouldShowDataMap.value || isCompanyFilterLocked.value) {
    enforceCompanyScope();
    syncCurrentLibrarySelection();
    void loadHazards();
    return;
  }
  const selectedKey =
    typeof keys[0] === 'number' || typeof keys[0] === 'string'
      ? String(keys[0])
      : undefined;
  selectedOrganizationKeys.value = selectedKey ? [selectedKey] : [];
  selectedCompanyId.value = companyIdFromDataMapKey(selectedKey);
  syncCurrentLibrarySelection();
  void loadHazards();
}

function cleanupDataMapResizeListeners() {
  window.removeEventListener('pointermove', handleDataMapResizeMove);
  window.removeEventListener('pointerup', handleDataMapResizeEnd);
  window.removeEventListener('pointercancel', handleDataMapResizeEnd);
  document.body.style.cursor = '';
  document.body.style.userSelect = '';
}

function handleDataMapResizeMove(event: PointerEvent) {
  updateDataMapResize(event.clientX);
}

function handleDataMapResizeEnd() {
  endDataMapResize();
  cleanupDataMapResizeListeners();
}

function handleDataMapResizeStart(event: PointerEvent) {
  event.preventDefault();
  beginDataMapResize(event.clientX);
  document.body.style.cursor = 'col-resize';
  document.body.style.userSelect = 'none';
  window.addEventListener('pointermove', handleDataMapResizeMove);
  window.addEventListener('pointerup', handleDataMapResizeEnd);
  window.addEventListener('pointercancel', handleDataMapResizeEnd);
}

async function resetFilters() {
  libraryKeyword.value = '';
  selectedCompanyId.value = undefined;
  selectedOrganizationKeys.value = [];
  treeKeyword.value = '';
  enforceCompanyScope();
  await loadLibraries();
}

async function loadHazards() {
  if (!currentLibraryId.value) {
    hazards.value = [];
    return;
  }
  loadingHazards.value = true;
  try {
    const result = await getRiskControlHazardsApi(currentLibraryId.value, {
      page: 1,
      pageSize: 500,
    });
    hazards.value = result.items;
    selectedHazardId.value = undefined;
  } finally {
    loadingHazards.value = false;
  }
}

function resetHazardForm(source?: Partial<HazardPayload>) {
  Object.assign(hazardForm, createEmptyHazard(), source);
  enforceHazardFormCompanyScope();
}

function openCreateLibrary() {
  const firstCompany =
    companyOptions.value.find(
      (item) => String(item.value) === String(selectedCompanyId.value),
    ) ?? companyOptions.value[0];
  libraryName.value = '';
  editingHazard.value = undefined;
  resetHazardForm({
    company: firstCompany?.label,
    companyId: firstCompany?.value,
  });
  libraryDrawerOpen.value = true;
}

function openCreateHazard() {
  if (!currentLibrary.value) {
    message.warning('请先新建风险库');
    return;
  }
  editingHazard.value = undefined;
  resetHazardForm({
    company: currentLibrary.value.company,
    companyId: currentLibrary.value.companyId,
  });
  hazardDrawerOpen.value = true;
}

function openEditHazard() {
  if (!selectedHazard.value) {
    message.warning('请选择一条隐患');
    return;
  }
  editingHazard.value = selectedHazard.value;
  resetHazardForm(selectedHazard.value);
  hazardDrawerOpen.value = true;
}

function selectedCompanyLabel(companyId?: number | string) {
  return companyOptions.value.find((item) => item.value === companyId)?.label;
}

async function saveLibrary() {
  enforceHazardFormCompanyScope();
  saving.value = true;
  try {
    hazardForm.company = selectedCompanyLabel(hazardForm.companyId) || hazardForm.company;
    const library = await createRiskControlLibraryApi({
      hazard: { ...hazardForm },
      name: libraryName.value || `${hazardForm.company || '风险'}风险库`,
    });
    libraryDrawerOpen.value = false;
    currentLibraryId.value = library.id;
    await loadLibraries();
    message.success('风险库已创建');
  } finally {
    saving.value = false;
  }
}

async function saveHazard() {
  if (!currentLibraryId.value) return;
  enforceHazardFormCompanyScope();
  saving.value = true;
  try {
    hazardForm.company = selectedCompanyLabel(hazardForm.companyId) || hazardForm.company;
    if (editingHazard.value) {
      await updateRiskControlHazardApi(
        currentLibraryId.value,
        editingHazard.value.id,
        { ...hazardForm },
      );
      message.success('隐患已更新');
    } else {
      await createRiskControlHazardApi(currentLibraryId.value, { ...hazardForm });
      message.success('隐患已新增');
    }
    hazardDrawerOpen.value = false;
    await loadHazards();
  } finally {
    saving.value = false;
  }
}

async function downloadTemplate() {
  const blob = await downloadRiskControlTemplateApi();
  saveBlob(blob, '风险分级管控_导入模板.xlsx');
}

async function downloadData() {
  if (!currentLibraryId.value) {
    message.warning('请选择风险库');
    return;
  }
  const blob = await downloadRiskControlDataApi(currentLibraryId.value);
  saveBlob(blob, `${currentLibrary.value?.name || '风险分级管控'}_数据导出.xlsx`);
}

async function uploadData(file: File) {
  const result = await uploadRiskControlDataApi(file);
  selectedCompanyId.value = result.library.companyId;
  currentLibraryId.value = result.library.id;
  await loadLibraries();
  message.success(`已导入 ${result.successRows} 条隐患`);
  return false;
}

function saveBlob(blob: Blob, fileName: string) {
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = fileName;
  anchor.click();
  URL.revokeObjectURL(url);
}

function saveTableConfig(config: SystemTableConfig) {
  saveSystemTableConfig(riskControlTableConfigModule, config);
  tableConfig.value = config;
  tableConfigOpen.value = false;
  message.success('表格配置已保存');
}

function deleteTableConfig() {
  removeSystemTableConfig(riskControlTableConfigModule);
  tableConfig.value = createDefaultSystemTableConfig(
    riskControlTableConfigModule,
    '风险分级管控',
    riskControlColumns,
    [...riskControlSearchFilters],
  );
  tableConfigOpen.value = false;
  message.success('表格配置已恢复');
}
</script>

<template>
  <div class="risk-control-page">
    <div
      :style="dataMapGridStyle"
      :class="[
        'pingan-shell',
        {
          'pingan-shell--map-collapsed': isDataMapCollapsed,
          'pingan-shell--no-map': !shouldShowDataMap,
        },
      ]"
    >
      <aside
        v-if="shouldShowDataMap"
        class="data-map"
        :class="{
          'data-map--collapsed': isDataMapCollapsed,
          'data-map--resizing': isDataMapResizing,
        }"
      >
        <div class="data-map__header">
          <div>
            <div class="data-map__title">数据地图</div>
            <div class="data-map__subtitle">组织层级快速定位</div>
          </div>
          <Button
            :aria-expanded="!isDataMapCollapsed"
            :aria-label="dataMapToggleLabel"
            class="data-map__collapse"
            size="small"
            type="text"
            @click="toggleDataMap"
          >
            {{ dataMapToggleLabel }}
          </Button>
        </div>
        <div v-if="!isDataMapCollapsed" class="data-map__body">
          <Input
            v-model:value="treeKeyword"
            allow-clear
            placeholder="输入关键字搜索"
          />
          <VbenScrollbar
            class="data-map__tree-scroll"
            shadow
            shadow-border
          >
            <Tree
              v-model:expanded-keys="expandedOrganizationKeys"
              v-model:selected-keys="selectedOrganizationKeys"
              :item-height="dataMapTreeContinuousScrollConfig.itemHeight"
              :tree-data="filteredOrganizationTree"
              :virtual="dataMapTreeContinuousScrollConfig.virtual"
              block-node
              class="data-map__tree"
              @select="handleDataMapSelect"
            />
          </VbenScrollbar>
        </div>
        <button
          v-if="!isDataMapCollapsed"
          aria-label="拖拽调整数据地图宽度"
          class="data-map__resize-handle"
          type="button"
          @pointerdown="handleDataMapResizeStart"
        ></button>
      </aside>

      <section class="risk-control-workbench">
        <div class="workbench__bar">
          <div v-if="isCompanyFilterVisible" class="filter-item filter-item--company">
            <span>公司</span>
            <Select
              v-model:value="selectedCompanyId"
              :options="companyOptions"
              :allow-clear="!isCompanyFilterLocked"
              :disabled="isCompanyFilterLocked"
              option-filter-prop="label"
              placeholder="点击选择"
              show-search
              @change="handleCompanyFilterChange"
            />
          </div>
          <div class="filter-item">
            <span>风险库</span>
            <Select
              v-model:value="currentLibraryId"
              :loading="loadingLibraries"
              :options="libraryOptions"
              option-filter-prop="label"
              placeholder="点击选择"
              show-search
              @change="handleLibraryChange"
            />
          </div>
          <div class="filter-item filter-item--keyword">
            <span>关键词</span>
            <Input
              v-model:value="libraryKeyword"
              allow-clear
              placeholder="请输入风险库名称"
              @press-enter="handleSearch"
            />
          </div>
          <Space class="filter-actions">
            <Button type="primary" @click="handleSearch">搜索</Button>
            <Button @click="resetFilters">重置</Button>
          </Space>
        </div>

        <section class="table-panel">
          <div class="table-panel__toolbar">
            <div>
              <div class="table-panel__title">{{ props.pageTitle }}</div>
              <div class="table-panel__summary">
                共 {{ hazards.length }} 条记录，{{ tableDataSourceText }}
              </div>
            </div>
            <div class="table-panel__actions">
              <Button type="primary" @click="openCreateLibrary">新建</Button>
              <Button :disabled="!currentLibraryId" @click="openCreateHazard">
                新增隐患
              </Button>
              <Button :disabled="!selectedHazard" @click="openEditHazard">
                编辑隐患
              </Button>
              <Button @click="downloadTemplate">下载模板</Button>
              <Button :disabled="!currentLibraryId" @click="downloadData">
                下载数据
              </Button>
              <Upload
                accept=".xlsx,.xls"
                :before-upload="uploadData"
                :show-upload-list="false"
              >
                <Button>上传数据</Button>
              </Upload>
              <Button @click="tableConfigOpen = true">表格配置</Button>
            </div>
          </div>

          <Table
            bordered
            class="meeting-table"
            :columns="configuredColumns"
            :data-source="hazards"
            :loading="loadingHazards"
            :pagination="{
              pageSize: 20,
              showSizeChanger: true,
              showTotal: (total) => `共 ${total} 条记录`,
            }"
            :row-selection="hazardRowSelection"
            :scroll="{ x: 3000, y: 'calc(100vh - 390px)' }"
            row-key="id"
            size="small"
          />
        </section>
      </section>
    </div>

    <Drawer
      v-model:open="libraryDrawerOpen"
      width="900"
      title="新建 风险隐患库"
    >
      <Form layout="vertical">
        <Form.Item label="库名称">
          <Input v-model:value="libraryName" placeholder="请输入库名称" />
        </Form.Item>
        <Form.Item label="公司" required>
          <Select
            v-model:value="hazardForm.companyId"
            :disabled="isCompanyFilterLocked"
            :options="companyOptions"
            placeholder="点击选择"
            show-search
          />
        </Form.Item>
        <div class="risk-control-form-grid">
          <Form.Item
            v-for="field in hazardFields"
            :key="field.key"
            :label="field.label"
            :required="field.required"
          >
            <Input.TextArea
              v-if="field.component === 'textarea'"
              v-model:value="hazardForm[field.key]"
              :auto-size="{ minRows: 1, maxRows: 3 }"
            />
            <Input v-else v-model:value="hazardForm[field.key]" />
          </Form.Item>
        </div>
      </Form>
      <template #footer>
        <Space>
          <Button type="primary" :loading="saving" @click="saveLibrary">
            创建
          </Button>
          <Button @click="libraryDrawerOpen = false">取消</Button>
        </Space>
      </template>
    </Drawer>

    <Drawer
      v-model:open="hazardDrawerOpen"
      width="900"
      :title="editingHazard ? '编辑隐患' : '新增隐患'"
    >
      <Form layout="vertical">
        <div class="risk-control-form-grid">
          <Form.Item
            v-for="field in hazardFields"
            :key="field.key"
            :label="field.label"
            :required="field.required"
          >
            <Input.TextArea
              v-if="field.component === 'textarea'"
              v-model:value="hazardForm[field.key]"
              :auto-size="{ minRows: 1, maxRows: 3 }"
            />
            <Input v-else v-model:value="hazardForm[field.key]" />
          </Form.Item>
        </div>
      </Form>
      <template #footer>
        <Space>
          <Button type="primary" :loading="saving" @click="saveHazard">
            保存
          </Button>
          <Button @click="hazardDrawerOpen = false">取消</Button>
        </Space>
      </template>
    </Drawer>

    <RiskControlTableConfigDrawer
      v-model:open="tableConfigOpen"
      :auto-widths="autoWidths"
      :config="tableConfig"
      :module-title="props.pageTitle"
      @delete="deleteTableConfig"
      @push="saveTableConfig"
      @save="saveTableConfig"
    />
  </div>
</template>

<style scoped>
.risk-control-page {
  min-height: 100%;
  background: #f4f7fb;
}

.pingan-shell {
  display: grid;
  grid-template-columns: minmax(220px, var(--data-map-width, 280px)) minmax(
      0,
      1fr
    );
  gap: 12px;
  min-height: calc(100vh - 112px);
  padding: 12px;
  transition: grid-template-columns 0.2s ease;
}

.pingan-shell--map-collapsed {
  grid-template-columns: 56px minmax(0, 1fr);
}

.pingan-shell--no-map {
  grid-template-columns: minmax(0, 1fr);
}

.data-map,
.table-panel {
  min-width: 0;
  overflow: hidden;
  background: #fff;
  border: 1px solid #dfe7f1;
  border-radius: 6px;
  box-shadow: 0 1px 2px rgb(15 23 42 / 4%);
}

.data-map {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 0;
  padding: 14px;
  overflow: hidden;
  transition:
    padding 0.2s ease,
    border-color 0.2s ease;
}

.data-map__header,
.table-panel__toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
}

.table-panel__toolbar {
  padding: 12px;
  border-bottom: 1px solid #eef2f7;
}

.data-map__header > div {
  min-width: 0;
}

.data-map__title,
.table-panel__title {
  color: #1f2937;
  font-size: 15px;
  font-weight: 800;
  line-height: 1.35;
}

.data-map__subtitle,
.table-panel__summary {
  margin-top: 3px;
  color: #64748b;
  font-size: 12px;
}

.data-map__collapse {
  flex: 0 0 auto;
}

.data-map__body {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 12px;
  min-height: 0;
}

.data-map__tree-scroll {
  flex: 1;
  min-height: 0;
}

.data-map__tree {
  padding-right: 6px;
  font-size: 13px;
}

.data-map__resize-handle {
  position: absolute;
  top: 0;
  right: -7px;
  z-index: 4;
  width: 14px;
  height: 100%;
  padding: 0;
  cursor: col-resize;
  touch-action: none;
  background: transparent;
  border: 0;
  outline: none;
}

.data-map__resize-handle::after {
  position: absolute;
  top: 12px;
  bottom: 12px;
  left: 6px;
  width: 2px;
  content: '';
  background: transparent;
  border-radius: 999px;
  transition:
    background 0.15s ease,
    opacity 0.15s ease;
}

.data-map__resize-handle:focus-visible::after,
.data-map__resize-handle:hover::after,
.data-map--resizing .data-map__resize-handle::after {
  background: rgb(59 130 246 / 65%);
}

.data-map--collapsed {
  align-items: center;
  gap: 8px;
  padding: 12px 8px;
}

.data-map--collapsed .data-map__header {
  flex: 1;
  flex-direction: column;
  justify-content: flex-start;
  width: 100%;
}

.data-map--collapsed .data-map__header > div {
  display: flex;
  justify-content: center;
}

.data-map--collapsed .data-map__title {
  line-height: 1.3;
  letter-spacing: 4px;
  writing-mode: vertical-rl;
}

.data-map--collapsed .data-map__subtitle {
  display: none;
}

.risk-control-workbench {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 12px;
  min-width: 0;
  min-height: 0;
}

.workbench__bar {
  --filter-control-width: 266px;

  display: grid;
  grid-template-columns: repeat(3, var(--filter-control-width)) auto;
  gap: 12px;
  align-items: end;
  min-width: 0;
  padding: 0 0 12px;
  overflow-x: auto;
}

.filter-item {
  display: grid;
  gap: 6px;
}

.filter-item span {
  color: #0f172a;
  font-size: 14px;
  font-weight: 700;
}

.filter-item :deep(.ant-select),
.filter-item :deep(.ant-input-affix-wrapper) {
  width: 100%;
}

.filter-item :deep(.ant-input-affix-wrapper),
.filter-item :deep(.ant-select-single .ant-select-selector) {
  min-height: 44px;
  border-color: #d8e1ea;
  border-radius: 6px;
}

.filter-item :deep(.ant-select-single .ant-select-selector) {
  align-items: center;
}

.filter-actions {
  padding-bottom: 0;
}

.table-panel {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.table-panel__toolbar {
  flex-wrap: wrap;
  min-height: 78px;
}

.table-panel__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  justify-content: flex-end;
}

.meeting-table {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-height: 0;
}

.meeting-table :deep(.ant-spin-nested-loading),
.meeting-table :deep(.ant-spin-container) {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-height: 0;
}

.meeting-table :deep(.ant-table) {
  flex: 1 1 auto;
  min-height: 0;
  color: #1f2937;
  border-color: #dfe7f1;
}

.meeting-table :deep(.ant-table-container) {
  border-color: #dfe7f1;
}

.meeting-table :deep(.ant-table-thead > tr > th) {
  color: #475569;
  font-size: 12px;
  font-weight: 700;
  text-align: center;
  background: #f8fafc;
  border-color: #dfe7f1;
}

.meeting-table :deep(.ant-table-tbody > tr > td) {
  color: #334155;
  font-size: 13px;
  border-color: #e5ebf3;
}

.meeting-table :deep(.ant-table-pagination) {
  margin: 12px;
}

.risk-control-form-grid {
  display: grid;
  gap: 4px 24px;
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

@media (max-width: 1100px) {
  .pingan-shell {
    grid-template-columns: 1fr;
  }

  .workbench__bar {
    grid-template-columns: repeat(2, minmax(180px, 1fr));
  }

  .risk-control-form-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
