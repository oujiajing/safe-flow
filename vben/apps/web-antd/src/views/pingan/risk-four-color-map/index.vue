<script setup lang="ts">
import type { CSSProperties } from 'vue';

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
  Empty,
  Form,
  Input,
  message,
  Modal,
  Select,
  Space,
  Tree,
  Upload,
} from 'ant-design-vue';

import type { PinganRiskFourColorMapApi } from '#/api/pingan/risk-four-color-map';

import {
  getPinganCompanyOrgTreeApi,
  getPinganOrgTreeApi,
} from '#/api/pingan/pre-shift-meeting';
import {
  createRiskFourColorMapApi,
  deleteRiskFourColorMapApi,
  getRiskFourColorMapsApi,
  updateRiskFourColorMapApi,
  uploadRiskFourColorMapBackgroundApi,
} from '#/api/pingan/risk-four-color-map';
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

type FourColorMap = PinganRiskFourColorMapApi.FourColorMap;

const userStore = useUserStore();
const maps = ref<FourColorMap[]>([]);
const organizationNodes = ref<OrganizationNode[]>([]);
const organizationScopeNodes = ref<OrganizationNode[]>([]);
const companyOptions = ref<Array<{ label: string; value: string | number }>>([]);
const selectedCompanyId = ref<OrganizationId>();
const selectedMapId = ref<PinganRiskFourColorMapApi.Id>();
const expandedOrganizationKeys = ref<string[]>([]);
const selectedOrganizationKeys = ref<string[]>([]);
const keyword = ref('');
const treeKeyword = ref('');
const loadingMaps = ref(false);
const saving = ref(false);
const formOpen = ref(false);
const fullscreenOpen = ref(false);
const isDetailCollapsed = ref(false);
const editingMap = ref<FourColorMap>();
const previewScale = ref(1);

const formState = reactive<PinganRiskFourColorMapApi.Payload>({
  companyId: undefined,
  name: '',
  remark: '',
});

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

const mapOptions = computed(() =>
  maps.value.map((item) => ({
    label: item.name,
    value: item.id,
  })),
);

const currentMap = computed(() =>
  maps.value.find((item) => String(item.id) === String(selectedMapId.value)),
);

const currentBackgroundUrl = computed(
  () => currentMap.value?.backgroundAttachment?.url || '',
);

const previewImageStyle = computed<CSSProperties>(() => ({
  transform: `scale(${previewScale.value})`,
}));

onMounted(async () => {
  await loadCompanies();
  await loadMaps();
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
  await enforceCompanyScope();
}

async function enforceCompanyScope() {
  const companyId = currentUserOrgDefaults.value.companyId;
  if (isCompanyFilterLocked.value && companyId !== undefined) {
    selectedCompanyId.value = companyId;
  }
}

function enforceFormCompanyScope() {
  Object.assign(
    formState,
    applyScopedPinganOrganizationDefaults(
      { companyId: formState.companyId },
      currentUserOrgDefaults.value,
      currentUserRoles.value,
    ),
  );
}

async function loadMaps() {
  loadingMaps.value = true;
  try {
    const result = await getRiskFourColorMapsApi({
      companyId: selectedCompanyId.value,
      keyword: keyword.value || undefined,
      page: 1,
      pageSize: 100,
    });
    maps.value = result.items;
    syncSelectedMap();
  } finally {
    loadingMaps.value = false;
  }
}

function syncSelectedMap() {
  const exists = maps.value.some(
    (item) => String(item.id) === String(selectedMapId.value),
  );
  if (!exists) {
    selectedMapId.value = maps.value[0]?.id;
  }
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

function handleDataMapSelect(keys: unknown[]) {
  if (!shouldShowDataMap.value || isCompanyFilterLocked.value) {
    void enforceCompanyScope().then(loadMaps);
    return;
  }
  const selectedKey =
    typeof keys[0] === 'number' || typeof keys[0] === 'string'
      ? String(keys[0])
      : undefined;
  selectedOrganizationKeys.value = selectedKey ? [selectedKey] : [];
  selectedCompanyId.value = companyIdFromDataMapKey(selectedKey);
  void loadMaps();
}

function handleDataMapResizeMove(event: PointerEvent) {
  updateDataMapResize(event.clientX);
}

function cleanupDataMapResizeListeners() {
  window.removeEventListener('pointermove', handleDataMapResizeMove);
  window.removeEventListener('pointerup', handleDataMapResizeEnd);
  window.removeEventListener('pointercancel', handleDataMapResizeEnd);
  document.body.style.cursor = '';
  document.body.style.userSelect = '';
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

function resetForm(source?: Partial<PinganRiskFourColorMapApi.Payload>) {
  Object.assign(formState, {
    companyId: selectedCompanyId.value ?? companyOptions.value[0]?.value,
    name: '',
    remark: '',
  });
  Object.assign(formState, source);
  enforceFormCompanyScope();
}

function openCreateMap() {
  editingMap.value = undefined;
  resetForm();
  formOpen.value = true;
}

function openEditMap() {
  if (!currentMap.value) {
    message.warning('请选择四色图');
    return;
  }
  editingMap.value = currentMap.value;
  resetForm({
    companyId: currentMap.value.companyId,
    name: currentMap.value.name,
    remark: currentMap.value.remark,
  });
  formOpen.value = true;
}

async function saveMap() {
  enforceFormCompanyScope();
  if (!formState.companyId) {
    message.warning('请选择公司');
    return;
  }
  if (!formState.name?.trim()) {
    message.warning('请输入四色图名称');
    return;
  }
  saving.value = true;
  try {
    const payload = { ...formState, name: formState.name.trim() };
    const saved = editingMap.value
      ? await updateRiskFourColorMapApi(editingMap.value.id, payload)
      : await createRiskFourColorMapApi(payload);
    selectedCompanyId.value = saved.companyId;
    selectedMapId.value = saved.id;
    formOpen.value = false;
    await loadMaps();
    message.success(editingMap.value ? '四色图已更新' : '四色图已创建');
  } finally {
    saving.value = false;
  }
}

async function removeCurrentMap() {
  if (!currentMap.value) {
    message.warning('请选择四色图');
    return;
  }
  await deleteRiskFourColorMapApi(currentMap.value.id);
  message.success('四色图已删除');
  await loadMaps();
}

async function uploadBackground(file: File) {
  if (!currentMap.value) {
    message.warning('请先新建或选择四色图');
    return false;
  }
  const updated = await uploadRiskFourColorMapBackgroundApi(currentMap.value.id, file);
  selectedMapId.value = updated.id;
  await loadMaps();
  message.success('底图已上传');
  return false;
}

async function handleSearch() {
  await loadMaps();
}

async function resetFilters() {
  selectedCompanyId.value = undefined;
  selectedOrganizationKeys.value = [];
  keyword.value = '';
  treeKeyword.value = '';
  await enforceCompanyScope();
  await loadMaps();
}

async function handleCompanyChange(value: unknown) {
  if (isCompanyFilterLocked.value) {
    await enforceCompanyScope();
    await loadMaps();
    return;
  }
  selectedCompanyId.value =
    typeof value === 'number' || typeof value === 'string' ? value : undefined;
  selectedOrganizationKeys.value = [];
  await loadMaps();
}

function fitScreen() {
  previewScale.value = 1;
}

function zoomIn() {
  previewScale.value = Math.min(2.5, Number((previewScale.value + 0.1).toFixed(2)));
}

function zoomOut() {
  previewScale.value = Math.max(0.4, Number((previewScale.value - 0.1).toFixed(2)));
}
</script>

<template>
  <div class="risk-four-color-page">
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

      <section class="four-color-workbench">
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
              @change="handleCompanyChange"
            />
          </div>
          <div class="filter-item">
            <span>四色图名称</span>
            <Select
              v-model:value="selectedMapId"
              :loading="loadingMaps"
              :options="mapOptions"
              allow-clear
              option-filter-prop="label"
              placeholder="点击选择"
              show-search
            />
          </div>
          <div class="filter-item filter-item--keyword">
            <span>关键词</span>
            <Input
              v-model:value="keyword"
              allow-clear
              placeholder="请输入四色图名称或备注"
              @press-enter="handleSearch"
            />
          </div>
          <Space class="filter-actions">
            <Button type="primary" @click="handleSearch">搜索</Button>
            <Button @click="resetFilters">重置</Button>
          </Space>
        </div>

        <section class="risk-map-panel">
          <div class="risk-map-panel__toolbar">
            <div>
              <div class="risk-map-panel__title">风险四色图</div>
              <div class="risk-map-panel__summary">
                共 {{ maps.length }} 张四色图，当前：
                {{ currentMap?.name || '请选择或新建四色图' }}
              </div>
            </div>
            <div class="risk-map-panel__actions">
              <Button type="primary" @click="openCreateMap">新建四色图</Button>
              <Button :disabled="!currentMap" @click="openEditMap">编辑</Button>
              <Button danger :disabled="!currentMap" @click="removeCurrentMap">
                删除
              </Button>
              <Upload
                accept="image/*"
                :before-upload="uploadBackground"
                :disabled="!currentMap"
                :show-upload-list="false"
              >
                <Button :disabled="!currentMap">上传/替换底图</Button>
              </Upload>
            </div>
          </div>

          <div
            :class="[
              'risk-map-layout',
              {
                'risk-map-layout--detail-collapsed': isDetailCollapsed,
              },
            ]"
          >
            <div class="risk-map-preview">
              <div class="risk-map-preview__tools">
                <Space>
                  <Button size="small" @click="fitScreen">适应屏幕</Button>
                  <Button size="small" @click="zoomOut">缩小</Button>
                  <Button size="small" @click="zoomIn">放大</Button>
                  <Button size="small" @click="fitScreen">还原</Button>
                  <Button
                    size="small"
                    :disabled="!currentBackgroundUrl"
                    @click="fullscreenOpen = true"
                  >
                    全屏查看
                  </Button>
                </Space>
                <span class="risk-map-preview__scale">
                  {{ Math.round(previewScale * 100) }}%
                </span>
              </div>
              <div class="risk-map-preview__canvas">
                <img
                  v-if="currentBackgroundUrl"
                  :src="currentBackgroundUrl"
                  :style="previewImageStyle"
                  alt="风险四色图底图"
                  class="risk-map-preview__image"
                />
                <div v-else class="risk-map-preview__empty">
                  <Empty description="暂无四色图底图" />
                  <Upload
                    accept="image/*"
                    :before-upload="uploadBackground"
                    :disabled="!currentMap"
                    :show-upload-list="false"
                  >
                    <Button type="primary" :disabled="!currentMap">
                      上传/替换底图
                    </Button>
                  </Upload>
                </div>
              </div>
            </div>

            <aside
              class="risk-map-detail"
              :class="{ 'risk-map-detail--collapsed': isDetailCollapsed }"
            >
              <div class="risk-map-detail__header">
                <div class="risk-map-detail__title">基础信息</div>
                <Button
                  class="risk-map-detail__toggle"
                  size="small"
                  type="text"
                  @click="isDetailCollapsed = !isDetailCollapsed"
                >
                  {{ isDetailCollapsed ? '展开' : '收起' }}
                </Button>
              </div>
              <dl v-if="!isDetailCollapsed">
                <dt>四色图名称</dt>
                <dd>{{ currentMap?.name || '-' }}</dd>
                <dt>公司</dt>
                <dd>{{ currentMap?.company || '-' }}</dd>
                <dt>底图文件</dt>
                <dd>{{ currentMap?.backgroundAttachment?.originalName || '-' }}</dd>
                <dt>备注</dt>
                <dd>{{ currentMap?.remark || '-' }}</dd>
                <dt>更新时间</dt>
                <dd>{{ currentMap?.updatedAt || '-' }}</dd>
              </dl>
            </aside>
          </div>
        </section>
      </section>
    </div>

    <Drawer
      v-model:open="formOpen"
      width="520"
      :title="editingMap ? '编辑四色图' : '新建四色图'"
    >
      <Form layout="vertical">
        <Form.Item label="公司" required>
          <Select
            v-model:value="formState.companyId"
            :disabled="isCompanyFilterLocked"
            :options="companyOptions"
            option-filter-prop="label"
            placeholder="点击选择"
            show-search
          />
        </Form.Item>
        <Form.Item label="四色图名称" required>
          <Input v-model:value="formState.name" placeholder="请输入四色图名称" />
        </Form.Item>
        <Form.Item label="备注">
          <Input.TextArea
            v-model:value="formState.remark"
            :auto-size="{ minRows: 3, maxRows: 5 }"
            placeholder="请输入备注"
          />
        </Form.Item>
      </Form>
      <template #footer>
        <Space>
          <Button type="primary" :loading="saving" @click="saveMap">保存</Button>
          <Button @click="formOpen = false">取消</Button>
        </Space>
      </template>
    </Drawer>

    <Modal
      v-model:open="fullscreenOpen"
      width="96vw"
      title="风险四色图"
      :footer="null"
    >
      <div class="risk-map-fullscreen">
        <img
          v-if="currentBackgroundUrl"
          :src="currentBackgroundUrl"
          alt="风险四色图底图"
        />
      </div>
    </Modal>
  </div>
</template>

<style scoped>
.risk-four-color-page {
  height: 100%;
  min-height: 0;
  overflow: hidden;
  background: #f4f7fb;
}

.pingan-shell {
  display: grid;
  grid-template-columns: minmax(220px, var(--data-map-width, 280px)) minmax(
      0,
      1fr
    );
  gap: 12px;
  height: calc(100vh - 112px);
  min-height: 0;
  padding: 12px;
  overflow: hidden;
  transition: grid-template-columns 0.2s ease;
}

.pingan-shell--map-collapsed {
  grid-template-columns: 56px minmax(0, 1fr);
}

.pingan-shell--no-map {
  grid-template-columns: minmax(0, 1fr);
}

.data-map,
.risk-map-panel {
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
.risk-map-panel__toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
}

.risk-map-panel__toolbar {
  flex-wrap: wrap;
  min-height: 78px;
  padding: 12px;
  border-bottom: 1px solid #eef2f7;
}

.data-map__header > div {
  min-width: 0;
}

.data-map__title,
.risk-map-panel__title {
  color: #1f2937;
  font-size: 15px;
  font-weight: 800;
  line-height: 1.35;
}

.data-map__subtitle,
.risk-map-panel__summary {
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

.four-color-workbench {
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
  overflow-y: hidden;
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

.risk-map-panel {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.risk-map-panel__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  justify-content: flex-end;
}

.risk-map-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 280px;
  gap: 12px;
  flex: 1 1 auto;
  min-height: 0;
  overflow: hidden;
  padding: 12px;
}

.risk-map-layout--detail-collapsed {
  grid-template-columns: minmax(0, 1fr) 48px;
}

.risk-map-preview {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  border: 1px solid #dfe7f1;
  border-radius: 6px;
}

.risk-map-preview__tools {
  display: flex;
  gap: 8px;
  align-items: center;
  justify-content: space-between;
  min-height: 46px;
  padding: 8px 10px;
  background: #f8fafc;
  border-bottom: 1px solid #e5ebf3;
}

.risk-map-preview__scale {
  color: #64748b;
  font-size: 12px;
}

.risk-map-preview__canvas {
  position: relative;
  display: grid;
  flex: 1 1 auto;
  min-height: 0;
  overflow: auto;
  place-items: center;
  background:
    linear-gradient(90deg, rgb(148 163 184 / 10%) 1px, transparent 1px),
    linear-gradient(rgb(148 163 184 / 10%) 1px, transparent 1px),
    #fff;
  background-size: 24px 24px;
}

.risk-map-preview__image {
  display: block;
  max-width: 100%;
  max-height: calc(100vh - 380px);
  object-fit: contain;
  transform-origin: center center;
  transition: transform 0.15s ease;
}

.risk-map-preview__empty {
  display: grid;
  gap: 16px;
  place-items: center;
  max-width: 360px;
  padding: 24px;
}

.risk-map-detail {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  padding: 14px;
  overflow: hidden;
  border: 1px solid #dfe7f1;
  border-radius: 6px;
  transition:
    padding 0.2s ease,
    width 0.2s ease;
}

.risk-map-detail--collapsed {
  align-items: center;
  padding: 12px 6px;
}

.risk-map-detail__header {
  display: flex;
  gap: 8px;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.risk-map-detail--collapsed .risk-map-detail__header {
  flex: 1;
  flex-direction: column;
  justify-content: flex-start;
  margin-bottom: 0;
}

.risk-map-detail__title {
  color: #1f2937;
  font-size: 14px;
  font-weight: 800;
}

.risk-map-detail--collapsed .risk-map-detail__title {
  line-height: 1.3;
  letter-spacing: 4px;
  writing-mode: vertical-rl;
}

.risk-map-detail__toggle {
  flex: 0 0 auto;
}

.risk-map-detail dl {
  display: grid;
  gap: 10px;
  min-height: 0;
  margin: 0;
  overflow: auto;
}

.risk-map-detail dt {
  color: #64748b;
  font-size: 12px;
}

.risk-map-detail dd {
  margin: 0;
  color: #1f2937;
  font-size: 13px;
  line-height: 1.6;
  word-break: break-word;
}

.risk-map-fullscreen {
  display: grid;
  min-height: 72vh;
  overflow: auto;
  place-items: center;
  background: #f8fafc;
}

.risk-map-fullscreen img {
  max-width: 100%;
}

@media (max-width: 1180px) {
  .pingan-shell,
  .risk-map-layout {
    grid-template-columns: 1fr;
  }

  .workbench__bar {
    grid-template-columns: repeat(2, minmax(180px, 1fr));
  }
}
</style>
