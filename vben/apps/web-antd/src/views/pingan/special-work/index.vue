<script setup lang="ts">
import type { TableColumnsType, TableProps } from 'ant-design-vue';

import { computed, onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { useAccessStore, useUserStore } from '@vben/stores';

import {
  Button,
  DatePicker,
  Empty,
  Image,
  Input,
  message,
  Modal,
  Select,
  Space,
  Table,
} from 'ant-design-vue';

import {
  batchDeleteSpecialWorkRecordsApi,
  createSpecialWorkRecordApi,
  deleteSpecialWorkRecordApi,
  downloadSpecialWorkRecordsApi,
  executeSpecialWorkActionApi,
  getSpecialWorkRecordsApi,
  updateSpecialWorkRecordApi,
  uploadSpecialWorkImageApi,
} from '#/api/pingan/special-work';
import type { PinganSpecialWorkApi } from '#/api/pingan/special-work';
import {
  getPinganCompanyOrgTreeApi,
  getPinganOrgTreeApi,
} from '#/api/pingan/pre-shift-meeting';
import DataMapPanel from '#/views/pingan/shared/DataMapPanel.vue';
import StatusFilterActions from '#/views/pingan/shared/StatusFilterActions.vue';
import type { DataMapNode } from '#/views/pingan/shared/data-map-state';
import { canShowPinganDataMap } from '#/views/pingan/shared/data-map-permission';
import {
  applyScopedPinganOrganizationDefaults,
  getVisiblePinganOrganizationFilterKeys,
  isPinganOrganizationFilterLocked,
  resolveScopedPinganOrganizationDefaults,
} from '#/views/pingan/shared/organization-filter-permission';

import {
  compactSpecialWorkPayload,
  readSpecialWorkImagePreview,
} from './special-work-form';
import {
  canExecuteSpecialWorkWorkflowAction,
  getSpecialWorkWorkflowAction,
} from './special-work-workflow';

interface CompanyOption {
  label: string;
  value: number | string;
}

type FormState = PinganSpecialWorkApi.RecordPayload & {
  status: PinganSpecialWorkApi.Status;
};

const statusOptions = [
  { label: '全部', value: 'all' },
  { label: '待审批', value: 'PENDING_APPROVAL' },
  { label: '作业中', value: 'IN_PROGRESS' },
  { label: '待验收', value: 'PENDING_ACCEPTANCE' },
  { label: '已完成', value: 'COMPLETED' },
];

const columns: TableColumnsType<PinganSpecialWorkApi.Record> = [
  { dataIndex: 'company', fixed: 'left', title: '公司', width: 180 },
  { dataIndex: 'project', title: '项目', width: 180 },
  { dataIndex: 'workType', title: '作业类型', width: 140 },
  { dataIndex: 'applicationTime', title: '作业申请时间', width: 170 },
  { dataIndex: 'image', title: '图片', width: 130 },
  { dataIndex: 'workContent', title: '作业内容', width: 190 },
  { dataIndex: 'workLocation', title: '作业地点', width: 160 },
  { dataIndex: 'riskIdentificationResult', title: '风险辨识结果', width: 210 },
  {
    dataIndex: 'implementationStartTime',
    title: '作业实施时间(开始)',
    width: 180,
  },
  {
    dataIndex: 'implementationEndTime',
    title: '作业实施时间(结束)',
    width: 180,
  },
  { dataIndex: 'safetyDisclosurePerson', title: '安全交底人', width: 130 },
  { dataIndex: 'guardian', title: '监护人', width: 120 },
  { dataIndex: 'disclosureReceiver', title: '接受交底人', width: 140 },
  { dataIndex: 'completionAcceptor', title: '完工验收人', width: 140 },
  { dataIndex: 'completionAcceptanceTime', title: '完工验收时间', width: 170 },
  { dataIndex: 'statusLabel', title: '状态', width: 120 },
  { dataIndex: 'actions', fixed: 'right', title: '操作', width: 190 },
];

const userStore = useUserStore();
const accessStore = useAccessStore();
const filters = reactive<PinganSpecialWorkApi.ListParams>({
  companyId: undefined,
  dateEnd: '',
  dateStart: '',
  status: 'all',
});
const form = reactive<FormState>(emptyForm());
const rows = ref<PinganSpecialWorkApi.Record[]>([]);
const selectedRowKeys = ref<string[]>([]);
const total = ref(0);
const loading = ref(false);
const saving = ref(false);
const createOpen = ref(false);
const editingId = ref<PinganSpecialWorkApi.Id>();
const imageInputRef = ref<HTMLInputElement>();
const pendingImage = ref<File>();
const formImagePreviewUrl = ref('');
const companyOptions = ref<CompanyOption[]>([]);
const organizationNodes = ref<DataMapNode[]>([]);
const organizationScopeNodes = ref<DataMapNode[]>([]);
const selectedOrganizationKeys = ref<string[]>([]);

const tableRowSelection = computed<TableProps<PinganSpecialWorkApi.Record>['rowSelection']>(
  () => ({
    onChange: (keys) => {
      selectedRowKeys.value = keys.map(String);
    },
    selectedRowKeys: selectedRowKeys.value,
  }),
);

const canBatchDelete = computed(() => selectedRowKeys.value.length > 0);
const accessCodes = computed(() => accessStore.accessCodes ?? []);
const canApply = computed(() =>
  accessCodes.value.includes('PINGAN_SPECIAL_WORK_APPLY'),
);
const canReview = computed(() =>
  accessCodes.value.includes('PINGAN_SPECIAL_WORK_REVIEW'),
);
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
const modalTitle = computed(() => (editingId.value ? '编辑特殊作业' : '新增特殊作业'));

function emptyForm(): FormState {
  return {
    applicationTime: '',
    companyId: '',
    completionAcceptanceTime: '',
    completionAcceptor: '',
    disclosureReceiver: '',
    guardian: '',
    implementationEndTime: '',
    implementationStartTime: '',
    project: '',
    riskIdentificationResult: '',
    safetyDisclosurePerson: '',
    status: 'PENDING_APPROVAL',
    workContent: '',
    workLocation: '',
    workType: '',
  };
}

function flattenCompanies(nodes: DataMapNode[]): CompanyOption[] {
  return nodes.flatMap((node) => [
    ...(node.orgType === 'COMPANY' && node.id !== undefined
      ? [{ label: node.title, value: node.id }]
      : []),
    ...flattenCompanies(node.children ?? []),
  ]);
}

async function loadOrganizations() {
  try {
    const [companyTree, fullTree] = await Promise.all([
      getPinganCompanyOrgTreeApi(),
      getPinganOrgTreeApi(),
    ]);
    organizationNodes.value = companyTree;
    organizationScopeNodes.value = fullTree;
    companyOptions.value = flattenCompanies(organizationNodes.value);
    enforceCompanyScope();
  } catch {
    organizationNodes.value = [];
    organizationScopeNodes.value = [];
    companyOptions.value = [];
  }
}

function enforceCompanyScope() {
  const companyId = currentUserOrgDefaults.value.companyId;
  if (isCompanyFilterLocked.value && companyId !== undefined) {
    filters.companyId = companyId;
  }
}

function enforceFormCompanyScope() {
  Object.assign(
    form,
    applyScopedPinganOrganizationDefaults(
      { companyId: form.companyId },
      currentUserOrgDefaults.value,
      currentUserRoles.value,
    ),
  );
}

function listParams(): PinganSpecialWorkApi.ListParams {
  return {
    companyId: filters.companyId || undefined,
    dateEnd: filters.dateEnd || undefined,
    dateStart: filters.dateStart || undefined,
    page: 1,
    pageSize: 20,
    status: filters.status || 'all',
  };
}

async function loadRecords() {
  loading.value = true;
  try {
    const result = await getSpecialWorkRecordsApi(listParams());
    rows.value = result.items;
    total.value = result.total;
  } catch {
    rows.value = [];
    total.value = 0;
    message.error('特殊作业数据加载失败');
  } finally {
    loading.value = false;
  }
}

function resetFilters() {
  filters.companyId = undefined;
  filters.dateEnd = '';
  filters.dateStart = '';
  filters.status = 'all';
  selectedOrganizationKeys.value = [];
  selectedRowKeys.value = [];
  enforceCompanyScope();
  void loadRecords();
}

function handleDataMapSelect(node?: DataMapNode) {
  if (!shouldShowDataMap.value || isCompanyFilterLocked.value) {
    enforceCompanyScope();
    selectedRowKeys.value = [];
    void loadRecords();
    return;
  }
  const selectedCompanyId = node?.orgType === 'COMPANY' ? node.id : undefined;
  filters.companyId = selectedCompanyId;
  selectedOrganizationKeys.value =
    selectedCompanyId !== undefined && node ? [node.key] : [];
  selectedRowKeys.value = [];
  void loadRecords();
}

function handleCompanyChange(value: unknown) {
  if (isCompanyFilterLocked.value) {
    enforceCompanyScope();
    return;
  }
  filters.companyId =
    typeof value === 'number' || typeof value === 'string' ? value : undefined;
  selectedOrganizationKeys.value = [];
}

function resetForm() {
  Object.assign(form, emptyForm());
  if (companyOptions.value[0]) {
    form.companyId = companyOptions.value[0].value;
  }
  enforceFormCompanyScope();
  formImagePreviewUrl.value = '';
  pendingImage.value = undefined;
}

function openCreateModal() {
  editingId.value = undefined;
  resetForm();
  createOpen.value = true;
}

function asSpecialWorkRecord(record: Record<string, any>) {
  return record as PinganSpecialWorkApi.Record;
}

function openEditModal(record: PinganSpecialWorkApi.Record) {
  editingId.value = record.id;
  Object.assign(form, {
    applicationTime: record.applicationTime,
    companyId: record.companyId,
    completionAcceptanceTime: record.completionAcceptanceTime || '',
    completionAcceptor: record.completionAcceptor || '',
    disclosureReceiver: record.disclosureReceiver || '',
    guardian: record.guardian || '',
    implementationEndTime: record.implementationEndTime || '',
    implementationStartTime: record.implementationStartTime || '',
    project: record.project,
    riskIdentificationResult: record.riskIdentificationResult || '',
    safetyDisclosurePerson: record.safetyDisclosurePerson || '',
    status: record.status,
    workContent: record.workContent || '',
    workLocation: record.workLocation || '',
    workType: record.workType,
  });
  formImagePreviewUrl.value = record.image?.url || '';
  pendingImage.value = undefined;
  enforceFormCompanyScope();
  createOpen.value = true;
}

function compactPayload(): PinganSpecialWorkApi.RecordPayload {
  return compactSpecialWorkPayload(form);
}

async function saveRecord() {
  enforceFormCompanyScope();
  if (!form.companyId || !form.project || !form.workType || !form.applicationTime) {
    message.warning('请填写公司、项目、作业类型和作业申请时间');
    return;
  }
  saving.value = true;
  try {
    const saved = editingId.value
      ? await updateSpecialWorkRecordApi(editingId.value, compactPayload())
      : await createSpecialWorkRecordApi(compactPayload());
    if (pendingImage.value) {
      await uploadSpecialWorkImageApi(saved.id, pendingImage.value);
    }
    message.success(editingId.value ? '特殊作业已更新' : '特殊作业已创建');
    createOpen.value = false;
    await loadRecords();
  } catch {
    message.error('特殊作业保存失败');
  } finally {
    saving.value = false;
  }
}

async function handleImageChange(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  pendingImage.value = file;
  if (file) {
    formImagePreviewUrl.value = await readSpecialWorkImagePreview(file);
  }
  input.value = '';
}

function chooseImage() {
  imageInputRef.value?.click();
}

function closeRecordModal() {
  createOpen.value = false;
}

function deleteRecord(record: PinganSpecialWorkApi.Record) {
  Modal.confirm({
    content: `确认删除特殊作业「${record.project}」？`,
    okText: '删除',
    okType: 'danger',
    onOk: async () => {
      await deleteSpecialWorkRecordApi(record.id);
      selectedRowKeys.value = selectedRowKeys.value.filter(
        (key) => key !== String(record.id),
      );
      message.success('特殊作业已删除');
      await loadRecords();
    },
    title: '确认删除？',
  });
}

function transitionRecord(record: PinganSpecialWorkApi.Record) {
  const transition = getSpecialWorkWorkflowAction(record);
  if (
    !transition ||
    !canExecuteSpecialWorkWorkflowAction(record, accessCodes.value)
  ) {
    return;
  }
  Modal.confirm({
    content: `确认对特殊作业「${record.project}」执行“${transition.label}”？请先在小程序流程页或已接入的流程表单中补齐当前阶段必填证据。`,
    onOk: async () => {
      await executeSpecialWorkActionApi(
        record.id,
        transition.action,
        transition.payload,
      );
      message.success(`${transition.label}成功`);
      await loadRecords();
    },
    title: transition.label,
  });
}

function batchDeleteRecords() {
  if (!selectedRowKeys.value.length) {
    return;
  }
  Modal.confirm({
    content: `将删除已选 ${selectedRowKeys.value.length} 条特殊作业记录。`,
    okText: '删除',
    okType: 'danger',
    onOk: async () => {
      await batchDeleteSpecialWorkRecordsApi([...selectedRowKeys.value]);
      selectedRowKeys.value = [];
      message.success('批量删除成功');
      await loadRecords();
    },
    title: '确认批量删除？',
  });
}

async function downloadRecords() {
  try {
    const blob = await downloadSpecialWorkRecordsApi(listParams());
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = '特殊作业_数据下载.xlsx';
    anchor.click();
    URL.revokeObjectURL(url);
  } catch {
    message.error('特殊作业下载失败');
  }
}

function statusBoxClass(label: string) {
  return [
    'status-box',
    {
      'status-box--blue': label === '作业中',
      'status-box--green': label === '已完成',
      'status-box--orange': label === '待审批' || label === '待验收',
    },
  ];
}

onMounted(async () => {
  await loadOrganizations();
  resetForm();
  enforceCompanyScope();
  await loadRecords();
});

</script>

<template>
  <Page auto-content-height content-class="pingan-page">
    <section class="special-work-shell">
      <DataMapPanel
        v-if="shouldShowDataMap"
        v-model:selected-keys="selectedOrganizationKeys"
        :tree-data="organizationNodes"
        @select="handleDataMapSelect"
      />

      <section class="special-work-workbench">
        <div class="workbench__bar">
          <div v-if="isCompanyFilterVisible" class="filter-item filter-item--company">
            <span>公司</span>
            <Select
              v-model:value="filters.companyId"
              :allow-clear="!isCompanyFilterLocked"
              :disabled="isCompanyFilterLocked"
              :options="companyOptions"
              option-filter-prop="label"
              placeholder="点击选择"
              show-search
              @change="handleCompanyChange"
            />
          </div>
          <div class="filter-item">
            <span>日期（起）</span>
            <DatePicker
              v-model:value="filters.dateStart"
              placeholder="点击选择"
              value-format="YYYY-MM-DD"
            />
          </div>
          <div class="filter-item">
            <span>日期（止）</span>
            <DatePicker
              v-model:value="filters.dateEnd"
              placeholder="点击选择"
              value-format="YYYY-MM-DD"
            />
          </div>
          <StatusFilterActions
            v-model="filters.status"
            :options="statusOptions"
            @reset="resetFilters"
            @search="loadRecords"
          />
        </div>

        <div class="table-panel">
        <div class="table-panel__toolbar">
          <div>
            <div class="table-panel__title">特殊作业</div>
            <div class="table-panel__summary">
              共 {{ total }} 条记录，按作业申请时间筛选
            </div>
          </div>
          <div class="table-panel__actions">
            <span class="table-panel__selection">
              已选 {{ selectedRowKeys.length }} 条
            </span>
            <Button v-if="canReview" :disabled="!canBatchDelete" danger @click="batchDeleteRecords">
              批量删除
            </Button>
            <Button @click="downloadRecords">
              <IconifyIcon icon="lucide:download" />
              下载数据
            </Button>
            <Button v-if="canApply" type="primary" @click="openCreateModal">
              <IconifyIcon icon="lucide:plus" />
              新增特殊作业
            </Button>
          </div>
        </div>

        <Table
          :columns="columns"
          :data-source="rows"
          :loading="loading"
          :pagination="{
            pageSize: 20,
            showSizeChanger: true,
            showTotal: (count) => `共 ${count} 条记录`,
          }"
          :row-selection="tableRowSelection"
          row-key="id"
          size="small"
          bordered
          class="meeting-table"
          :scroll="{ x: 2500, y: 'calc(100vh - 390px)' }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.dataIndex === 'image'">
              <Image
                v-if="record.image?.url"
                :height="38"
                :preview="true"
                :src="record.image.url"
                :width="64"
                class="special-work-table-image"
              />
              <span v-else class="attachment-pill attachment-pill--missing">
                未上传
              </span>
            </template>
            <template v-else-if="column.dataIndex === 'statusLabel'">
              <span :class="statusBoxClass(record.statusLabel)">
                {{ record.statusLabel }}
              </span>
            </template>
            <template v-else-if="column.dataIndex === 'actions'">
              <Space>
                <Button
                  v-if="getSpecialWorkWorkflowAction(asSpecialWorkRecord(record))"
                  :disabled="
                    !canExecuteSpecialWorkWorkflowAction(
                      asSpecialWorkRecord(record),
                      accessCodes,
                    )
                  "
                  size="small"
                  type="link"
                  @click="transitionRecord(asSpecialWorkRecord(record))"
                >
                  {{
                    getSpecialWorkWorkflowAction(asSpecialWorkRecord(record))
                      ?.label
                  }}
                </Button>
                <Button
                  v-if="canReview && record.status !== 'COMPLETED'"
                  size="small"
                  type="link"
                  @click="openEditModal(asSpecialWorkRecord(record))"
                >
                  编辑
                </Button>
                <Button
                  v-if="canReview"
                  danger
                  size="small"
                  type="link"
                  @click="deleteRecord(asSpecialWorkRecord(record))"
                >
                  删除
                </Button>
              </Space>
            </template>
          </template>
          <template #emptyText>
            <Empty description="暂无特殊作业数据" />
          </template>
        </Table>
        </div>
      </section>

      <Modal
        v-model:open="createOpen"
        :confirm-loading="saving"
        :title="modalTitle"
        width="860px"
        @cancel="closeRecordModal"
        @ok="saveRecord"
      >
        <div class="special-work-form">
          <label>
            <span>公司</span>
            <Select
              v-model:value="form.companyId"
              :disabled="isCompanyFilterLocked"
              :options="companyOptions"
              option-filter-prop="label"
              placeholder="选择公司"
              show-search
            />
          </label>
          <label>
            <span>项目</span>
            <Input v-model:value="form.project" placeholder="填写项目" />
          </label>
          <label>
            <span>作业类型</span>
            <Input v-model:value="form.workType" placeholder="填写作业类型" />
          </label>
          <label>
            <span>作业申请时间</span>
            <DatePicker
              v-model:value="form.applicationTime"
              show-time
              value-format="YYYY-MM-DD HH:mm:ss"
            />
          </label>
          <label>
            <span>作业内容</span>
            <Input v-model:value="form.workContent" placeholder="填写作业内容" />
          </label>
          <label>
            <span>作业地点</span>
            <Input v-model:value="form.workLocation" placeholder="填写作业地点" />
          </label>
          <label>
            <span>风险辨识结果</span>
            <Input
              v-model:value="form.riskIdentificationResult"
              placeholder="填写风险辨识结果"
            />
          </label>
          <label>
            <span>作业实施时间(开始)</span>
            <DatePicker
              v-model:value="form.implementationStartTime"
              show-time
              value-format="YYYY-MM-DD HH:mm:ss"
            />
          </label>
          <label>
            <span>作业实施时间(结束)</span>
            <DatePicker
              v-model:value="form.implementationEndTime"
              show-time
              value-format="YYYY-MM-DD HH:mm:ss"
            />
          </label>
          <label>
            <span>安全交底人</span>
            <Input
              v-model:value="form.safetyDisclosurePerson"
              placeholder="填写安全交底人"
            />
          </label>
          <label>
            <span>监护人</span>
            <Input v-model:value="form.guardian" placeholder="填写监护人" />
          </label>
          <label>
            <span>接受交底人</span>
            <Input
              v-model:value="form.disclosureReceiver"
              placeholder="填写接受交底人"
            />
          </label>
          <label>
            <span>完工验收人</span>
            <Input
              v-model:value="form.completionAcceptor"
              placeholder="填写完工验收人"
            />
          </label>
          <label>
            <span>完工验收时间</span>
            <DatePicker
              v-model:value="form.completionAcceptanceTime"
              show-time
              value-format="YYYY-MM-DD HH:mm:ss"
            />
          </label>
          <label v-if="editingId">
            <span>状态</span>
            <Input
              :value="statusOptions.find((item) => item.value === form.status)?.label"
              disabled
            />
          </label>
          <label class="special-work-image-field">
            <span>附件</span>
            <div class="special-work-attachment-card">
              <div class="special-work-form-preview">
                <Image
                  v-if="formImagePreviewUrl"
                  :height="158"
                  :preview="true"
                  :src="formImagePreviewUrl"
                  :width="204"
                  class="special-work-form-preview__image"
                />
                <IconifyIcon
                  v-else
                  class="special-work-form-preview__icon"
                  icon="lucide:image-plus"
                />
              </div>
              <div class="special-work-attachment-card__title">图片打卡</div>
              <Button
                size="small"
                type="primary"
                ghost
                @click.prevent.stop="chooseImage"
              >
                选择图片
              </Button>
              <input
                ref="imageInputRef"
                accept="image/*"
                class="special-work-file-input"
                type="file"
                @change="handleImageChange"
              />
            </div>
          </label>
        </div>
      </Modal>
    </section>
  </Page>
</template>

<style scoped>
:global(.pingan-page) {
  height: 100%;
  padding: 12px;
  background: #f5f7fb;
}

.special-work-shell {
  display: flex;
  gap: 12px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.special-work-workbench {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  background: #fff;
  border: 1px solid #e6eaf2;
  border-radius: 8px;
}

.workbench__bar {
  display: flex;
  flex-wrap: nowrap;
  align-items: end;
  gap: 10px;
  padding: 12px;
  overflow: hidden;
  background: #f8fafc;
  border-bottom: 1px solid #e6eaf2;
}

.filter-item {
  flex: 1 1 132px;
  min-width: 0;
}

.filter-item span {
  display: block;
  margin-bottom: 5px;
  color: #334155;
  font-size: 12px;
  font-weight: 700;
}

.filter-item :deep(.ant-picker),
.filter-item :deep(.ant-select) {
  width: 100%;
  height: 40px;
  min-height: 40px;
}

.filter-item :deep(.ant-picker),
.filter-item :deep(.ant-select-single .ant-select-selector) {
  display: flex;
  align-items: center;
}

.filter-item :deep(.ant-select-single .ant-select-selector) {
  height: 40px;
}

.table-panel {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
  margin: 12px;
  overflow: hidden;
}

.table-panel__toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  padding: 12px;
  border-bottom: 1px solid #eef2f7;
}

.table-panel__title {
  color: #0f172a;
  font-size: 16px;
  font-weight: 700;
}

.table-panel__summary,
.table-panel__selection {
  color: #64748b;
  font-size: 13px;
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
  color: #4b5563;
}

.meeting-table :deep(.ant-table-thead > tr > th) {
  color: #475569;
  font-size: 12px;
  font-weight: 700;
  text-align: center;
  background: #f8fafc;
}

.meeting-table :deep(.ant-table-cell) {
  text-align: center;
  vertical-align: middle;
}

.status-box {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 64px;
  height: 26px;
  padding: 0 9px;
  color: #475569;
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
  white-space: nowrap;
  background: #f8fafc;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
}

.status-box--blue {
  color: #1d4ed8;
  background: #eff6ff;
  border-color: #93c5fd;
}

.status-box--green {
  color: #15803d;
  background: #f0fdf4;
  border-color: #86efac;
}

.status-box--orange {
  color: #c2410c;
  background: #fff7ed;
  border-color: #fdba74;
}

.attachment-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 26px;
  padding: 0 10px;
  font-size: 12px;
  border-radius: 999px;
}

.attachment-pill--missing {
  color: #64748b;
  background: #f1f5f9;
}

.special-work-table-image {
  object-fit: cover;
  border-radius: 6px;
}

.special-work-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 16px;
}

.special-work-form label {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.special-work-form label span {
  color: #334155;
  font-size: 12px;
  font-weight: 700;
}

.special-work-form :deep(.ant-picker),
.special-work-form :deep(.ant-select),
.special-work-form :deep(.ant-input) {
  width: 100%;
}

.special-work-image-field {
  grid-column: 1 / -1;
}

.special-work-attachment-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 230px;
  padding: 10px 12px;
  background: #f8fbff;
  border: 1px dashed #bfdbfe;
  border-radius: 6px;
}

.special-work-form-preview {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 204px;
  height: 158px;
  overflow: hidden;
  background: #fff;
  border: 1px solid #d8e2ef;
  border-radius: 4px;
}

.special-work-form-preview__image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.special-work-form-preview__icon {
  width: 26px;
  height: 26px;
  color: #2563eb;
}

.special-work-attachment-card__title {
  margin-top: 8px;
  margin-bottom: 6px;
  color: #1f2937;
  font-size: 13px;
  font-weight: 700;
  line-height: 1.2;
}

.special-work-file-input {
  display: none;
}
</style>
