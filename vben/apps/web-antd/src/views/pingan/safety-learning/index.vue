<script setup lang="ts">
import type { TableColumnsType, TableProps } from 'ant-design-vue';
import type { Ref } from 'vue';

import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { useAccessStore, useUserStore } from '@vben/stores';

import {
  Button,
  DatePicker,
  Input,
  message,
  Modal,
  Select,
  Space,
  Table,
} from 'ant-design-vue';

import {
  getPinganCompanyOrgTreeApi,
  getPinganOrgTreeApi,
} from '#/api/pingan/pre-shift-meeting';
import {
  batchDeleteSafetyLearningContentsApi,
  createSafetyLearningContentApi,
  deleteSafetyLearningContentApi,
  downloadSafetyLearningContentsApi,
  downloadSafetyLearningTemplateApi,
  getSafetyLearningContentsApi,
  importSafetyLearningWorkbookApi,
  removeSafetyLearningAttachmentApi,
  removeSafetyLearningCoverImageApi,
  removeSafetyLearningVideoApi,
  updateSafetyLearningContentApi,
  uploadSafetyLearningAttachmentApi,
  uploadSafetyLearningCoverImageApi,
  uploadSafetyLearningVideoApi,
} from '#/api/pingan/safety-learning';
import type { PinganSafetyLearningApi } from '#/api/pingan/safety-learning';
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

interface CompanyOption {
  label: string;
  value: number | string;
}

type FormState = PinganSafetyLearningApi.ContentPayload;
type MediaPreviewKind = 'IMAGE' | 'VIDEO';

const accessStore = useAccessStore();
const userStore = useUserStore();

const safetyLearningPermissionCodes = {
  create: 'PINGAN_TRAINING_SAFETY_LEARNING_CREATE',
  delete: 'PINGAN_TRAINING_SAFETY_LEARNING_DELETE',
  download: 'PINGAN_TRAINING_SAFETY_LEARNING_DOWNLOAD',
  legacyManage: 'PINGAN_TRAINING_MANAGE',
  legacyView: 'PINGAN_TRAINING_VIEW',
  void: 'PINGAN_TRAINING_SAFETY_LEARNING_VOID',
} as const;

const categoryOptions = [
  { label: '全部', value: 'all' },
  { label: '设备操作规程', value: '设备操作规程' },
  { label: '安全设施', value: '安全设施' },
  { label: '法律知识', value: '法律知识' },
  { label: '安全知识', value: '安全知识' },
  { label: '隐患知识', value: '隐患知识' },
  { label: '安全课程', value: '安全课程' },
  { label: '事故案例', value: '事故案例' },
  { label: '应急管理', value: '应急管理' },
];

const formCategoryOptions = categoryOptions.filter((item) => item.value !== 'all');

const statusOptions = [
  { label: '全部', value: 'all' },
  { label: '草稿', value: 'DRAFT' },
  { label: '激活', value: 'ACTIVE' },
  { label: '作废', value: 'INACTIVE' },
];

const formStatusOptions = statusOptions.filter((item) => item.value !== 'all');

const columns: TableColumnsType<PinganSafetyLearningApi.Content> = [
  { dataIndex: 'company', fixed: 'left', title: '公司', width: 170 },
  { dataIndex: 'category', title: '分类', width: 130 },
  { dataIndex: 'title', title: '标题', width: 180 },
  { dataIndex: 'content', title: '内容', width: 220 },
  { dataIndex: 'coverImage', title: '封面图', width: 150 },
  { dataIndex: 'video', title: '视频', width: 150 },
  { dataIndex: 'learningDate', title: '日期', width: 120 },
  { dataIndex: 'durationText', title: '计时', width: 100 },
  { dataIndex: 'code', title: '编码', width: 160 },
  { dataIndex: 'attachment', title: '附件', width: 180 },
  { dataIndex: 'htmlExtract', title: 'HTML提取', width: 160 },
  { dataIndex: 'draft', title: '草稿', width: 90 },
  { dataIndex: 'statusLabel', title: '状态', width: 100 },
  { dataIndex: 'actions', fixed: 'right', title: '操作', width: 140 },
];

const filters = reactive<PinganSafetyLearningApi.ListParams>({
  category: 'all',
  companyId: undefined,
  dateEnd: '',
  dateStart: '',
  keyword: '',
  status: 'all',
});
const form = reactive<FormState>(emptyForm());
const rows = ref<PinganSafetyLearningApi.Content[]>([]);
const selectedRowKeys = ref<string[]>([]);
const total = ref(0);
const loading = ref(false);
const saving = ref(false);
const modalOpen = ref(false);
const editingId = ref<PinganSafetyLearningApi.Id>();
const companyOptions = ref<CompanyOption[]>([]);
const organizationNodes = ref<DataMapNode[]>([]);
const organizationScopeNodes = ref<DataMapNode[]>([]);
const selectedOrganizationKeys = ref<string[]>([]);
const attachmentInputRef = ref<HTMLInputElement>();
const coverImageInputRef = ref<HTMLInputElement>();
const importInputRef = ref<HTMLInputElement>();
const videoInputRef = ref<HTMLInputElement>();
const pendingAttachment = ref<File>();
const pendingCoverImage = ref<File>();
const pendingVideo = ref<File>();
const pendingCoverImageUrl = ref('');
const pendingVideoUrl = ref('');
const currentAttachment = ref<PinganSafetyLearningApi.Attachment>();
const currentCoverImageAttachment = ref<PinganSafetyLearningApi.Attachment>();
const currentVideoAttachment = ref<PinganSafetyLearningApi.Attachment>();
const mediaPreviewKind = ref<MediaPreviewKind>('IMAGE');
const mediaPreviewOpen = ref(false);
const mediaPreviewTitle = ref('');
const mediaPreviewUrl = ref('');

const tableRowSelection = computed<TableProps<PinganSafetyLearningApi.Content>['rowSelection']>(
  () => ({
    onChange: (keys) => {
      selectedRowKeys.value = keys.map(String);
    },
    selectedRowKeys: selectedRowKeys.value,
  }),
);

const accessCodes = computed(() => accessStore.accessCodes ?? []);
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
const canCreateSafetyLearning = computed(() =>
  hasAnyAccessCode([
    safetyLearningPermissionCodes.create,
    safetyLearningPermissionCodes.legacyManage,
  ]),
);
const canDeleteSafetyLearning = computed(() =>
  hasAnyAccessCode([
    safetyLearningPermissionCodes.delete,
    safetyLearningPermissionCodes.legacyManage,
  ]),
);
const canVoidSafetyLearning = computed(() =>
  hasAnyAccessCode([
    safetyLearningPermissionCodes.void,
    safetyLearningPermissionCodes.legacyManage,
  ]),
);
const canDownloadSafetyLearning = computed(() =>
  hasAnyAccessCode([
    safetyLearningPermissionCodes.download,
    safetyLearningPermissionCodes.legacyView,
    safetyLearningPermissionCodes.legacyManage,
  ]),
);
const canEditSafetyLearning = computed(() => canCreateSafetyLearning.value);
const canBatchDelete = computed(
  () => selectedRowKeys.value.length > 0 && canDeleteSafetyLearning.value,
);
const modalTitle = computed(() => (editingId.value ? '编辑安全学习' : '新增安全学习'));
const mediaPreviewModalTitle = computed(
  () => mediaPreviewTitle.value || (mediaPreviewKind.value === 'IMAGE' ? '封面图' : '视频'),
);

function hasAnyAccessCode(codes: string[]) {
  return codes.some((code) => accessCodes.value.includes(code));
}

function emptyForm(): FormState {
  return {
    attachmentText: '',
    category: '安全知识',
    code: '',
    companyId: '',
    content: '',
    coverImage: '',
    draft: '否',
    durationText: '',
    htmlExtract: '',
    learningDate: '',
    status: 'ACTIVE',
    title: '',
    video: '',
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

function listParams(): PinganSafetyLearningApi.ListParams {
  return {
    category:
      filters.category && filters.category !== 'all'
        ? filters.category
        : undefined,
    companyId: filters.companyId || undefined,
    dateEnd: filters.dateEnd || undefined,
    dateStart: filters.dateStart || undefined,
    keyword: filters.keyword || undefined,
    page: 1,
    pageSize: 20,
    status: filters.status || 'all',
  };
}

async function loadRecords() {
  loading.value = true;
  try {
    const result = await getSafetyLearningContentsApi(listParams());
    rows.value = result.items;
    total.value = result.total;
  } catch {
    rows.value = [];
    total.value = 0;
    message.error('安全学习数据加载失败');
  } finally {
    loading.value = false;
  }
}

function resetFilters() {
  filters.category = 'all';
  filters.companyId = undefined;
  filters.dateEnd = '';
  filters.dateStart = '';
  filters.keyword = '';
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
  clearPendingCoverImage();
  clearPendingVideo();
  pendingAttachment.value = undefined;
  currentAttachment.value = undefined;
  currentCoverImageAttachment.value = undefined;
  currentVideoAttachment.value = undefined;
}

function openCreateModal() {
  editingId.value = undefined;
  resetForm();
  modalOpen.value = true;
}

function asSafetyLearningContent(record: Record<string, any>) {
  return record as PinganSafetyLearningApi.Content;
}

function openEditModal(record: PinganSafetyLearningApi.Content) {
  editingId.value = record.id;
  Object.assign(form, {
    attachmentText: record.attachmentText || '',
    category: record.category,
    code: record.code || '',
    companyId: record.companyId,
    content: record.content || '',
    coverImage: record.coverImage || '',
    draft: record.draft || '',
    durationText: record.durationText || '',
    htmlExtract: record.htmlExtract || '',
    learningDate: record.learningDate,
    status: record.status,
    title: record.title,
    video: record.video || '',
  });
  pendingAttachment.value = undefined;
  clearPendingCoverImage();
  clearPendingVideo();
  currentAttachment.value = record.attachment;
  currentCoverImageAttachment.value = record.coverImageAttachment;
  currentVideoAttachment.value = record.videoAttachment;
  enforceFormCompanyScope();
  modalOpen.value = true;
}

function payload(): FormState {
  return {
    attachmentText: form.attachmentText?.trim(),
    category: form.category,
    code: form.code?.trim(),
    companyId: form.companyId,
    content: form.content?.trim(),
    coverImage: form.coverImage?.trim(),
    draft: form.draft?.trim(),
    durationText: form.durationText?.trim(),
    htmlExtract: form.htmlExtract?.trim(),
    learningDate: form.learningDate,
    status: form.status,
    title: form.title.trim(),
    video: form.video?.trim(),
  };
}

async function saveRecord() {
  enforceFormCompanyScope();
  if (!form.companyId || !form.category || !form.title || !form.learningDate) {
    message.warning('请填写公司、分类、标题和日期');
    return;
  }
  saving.value = true;
  try {
    const saved = editingId.value
      ? await updateSafetyLearningContentApi(editingId.value, payload())
      : await createSafetyLearningContentApi(payload());
    if (pendingCoverImage.value) {
      await uploadSafetyLearningCoverImageApi(saved.id, pendingCoverImage.value);
    }
    if (pendingVideo.value) {
      await uploadSafetyLearningVideoApi(saved.id, pendingVideo.value);
    }
    if (pendingAttachment.value) {
      await uploadSafetyLearningAttachmentApi(saved.id, pendingAttachment.value);
    }
    message.success(editingId.value ? '安全学习已更新' : '安全学习已创建');
    modalOpen.value = false;
    await loadRecords();
  } catch {
    message.error('安全学习保存失败');
  } finally {
    saving.value = false;
  }
}

function chooseAttachment() {
  attachmentInputRef.value?.click();
}

function chooseCoverImage() {
  coverImageInputRef.value?.click();
}

function chooseVideo() {
  videoInputRef.value?.click();
}

function resetObjectUrl(target: Ref<string>) {
  if (target.value) {
    URL.revokeObjectURL(target.value);
    target.value = '';
  }
}

function handleCoverImageChange(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (file && !file.type.startsWith('image/')) {
    message.warning('仅支持图片文件');
    input.value = '';
    return;
  }
  clearPendingCoverImage();
  pendingCoverImage.value = file;
  pendingCoverImageUrl.value = file ? URL.createObjectURL(file) : '';
  input.value = '';
}

function handleVideoChange(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (file && !file.type.startsWith('video/')) {
    message.warning('仅支持视频文件');
    input.value = '';
    return;
  }
  clearPendingVideo();
  pendingVideo.value = file;
  pendingVideoUrl.value = file ? URL.createObjectURL(file) : '';
  input.value = '';
}

function handleAttachmentChange(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (file && file.type !== 'application/pdf' && !file.name.toLowerCase().endsWith('.pdf')) {
    message.warning('仅支持 PDF 附件');
    input.value = '';
    return;
  }
  pendingAttachment.value = file;
  input.value = '';
}

function clearPendingAttachment() {
  pendingAttachment.value = undefined;
}

function clearPendingCoverImage() {
  pendingCoverImage.value = undefined;
  resetObjectUrl(pendingCoverImageUrl);
}

function clearPendingVideo() {
  pendingVideo.value = undefined;
  resetObjectUrl(pendingVideoUrl);
}

function openAttachment(attachment?: PinganSafetyLearningApi.Attachment) {
  if (attachment?.url) {
    window.open(attachment.url, '_blank', 'noopener,noreferrer');
  }
}

function openMediaPreview(
  kind: MediaPreviewKind,
  attachment?: PinganSafetyLearningApi.Attachment,
) {
  openMediaPreviewFromUrl(kind, attachment?.url, attachment?.originalName);
}

function openMediaPreviewFromUrl(
  kind: MediaPreviewKind,
  url?: string,
  title?: string,
) {
  if (!url) {
    return;
  }
  mediaPreviewKind.value = kind;
  mediaPreviewUrl.value = url;
  mediaPreviewTitle.value = title || (kind === 'IMAGE' ? '封面图' : '视频');
  mediaPreviewOpen.value = true;
}

function closeMediaPreview() {
  mediaPreviewOpen.value = false;
}

function removeCurrentAttachment() {
  if (!editingId.value || !currentAttachment.value) {
    return;
  }
  const attachmentName = currentAttachment.value.originalName;
  Modal.confirm({
    content: `确认删除附件「${attachmentName}」？`,
    okText: '删除',
    okType: 'danger',
    onOk: async () => {
      const updated = await removeSafetyLearningAttachmentApi(editingId.value!);
      currentAttachment.value = updated.attachment;
      form.attachmentText = updated.attachmentText || '';
      message.success('附件已删除');
      await loadRecords();
    },
    title: '删除附件？',
  });
}

function removeCurrentCoverImage() {
  if (!editingId.value || !currentCoverImageAttachment.value) {
    return;
  }
  const attachmentName = currentCoverImageAttachment.value.originalName;
  Modal.confirm({
    content: `确认删除封面图「${attachmentName}」？`,
    okText: '删除',
    okType: 'danger',
    onOk: async () => {
      const updated = await removeSafetyLearningCoverImageApi(editingId.value!);
      currentCoverImageAttachment.value = updated.coverImageAttachment;
      form.coverImage = updated.coverImage || '';
      message.success('封面图已删除');
      await loadRecords();
    },
    title: '删除封面图？',
  });
}

function removeCurrentVideo() {
  if (!editingId.value || !currentVideoAttachment.value) {
    return;
  }
  const attachmentName = currentVideoAttachment.value.originalName;
  Modal.confirm({
    content: `确认删除视频「${attachmentName}」？`,
    okText: '删除',
    okType: 'danger',
    onOk: async () => {
      const updated = await removeSafetyLearningVideoApi(editingId.value!);
      currentVideoAttachment.value = updated.videoAttachment;
      form.video = updated.video || '';
      message.success('视频已删除');
      await loadRecords();
    },
    title: '删除视频？',
  });
}

function deleteRecord(record: PinganSafetyLearningApi.Content) {
  Modal.confirm({
    content: `确认删除安全学习「${record.title}」？`,
    okText: '删除',
    okType: 'danger',
    onOk: async () => {
      await deleteSafetyLearningContentApi(record.id);
      selectedRowKeys.value = selectedRowKeys.value.filter(
        (key) => key !== String(record.id),
      );
      message.success('安全学习已删除');
      await loadRecords();
    },
    title: '确认删除？',
  });
}

function contentPayloadFromRecord(
  record: PinganSafetyLearningApi.Content,
  status: PinganSafetyLearningApi.Status,
): FormState {
  return {
    attachmentText: record.attachmentText || '',
    category: record.category,
    code: record.code || '',
    companyId: record.companyId,
    content: record.content || '',
    coverImage: record.coverImage || '',
    draft: record.draft || '',
    durationText: record.durationText || '',
    htmlExtract: record.htmlExtract || '',
    learningDate: record.learningDate,
    status,
    title: record.title,
    video: record.video || '',
  };
}

function voidRecord(record: PinganSafetyLearningApi.Content) {
  Modal.confirm({
    content: `确认作废安全学习「${record.title}」？`,
    okText: '作废',
    okType: 'danger',
    onOk: async () => {
      await updateSafetyLearningContentApi(
        record.id,
        contentPayloadFromRecord(record, 'INACTIVE'),
      );
      message.success('安全学习已作废');
      await loadRecords();
    },
    title: '确认作废？',
  });
}

function batchDeleteRecords() {
  if (!selectedRowKeys.value.length) {
    return;
  }
  Modal.confirm({
    content: `将删除已选 ${selectedRowKeys.value.length} 条安全学习内容。`,
    okText: '删除',
    okType: 'danger',
    onOk: async () => {
      await batchDeleteSafetyLearningContentsApi([...selectedRowKeys.value]);
      selectedRowKeys.value = [];
      message.success('批量删除成功');
      await loadRecords();
    },
    title: '确认批量删除？',
  });
}

async function saveBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = filename;
  anchor.click();
  URL.revokeObjectURL(url);
}

async function downloadTemplate() {
  try {
    await saveBlob(await downloadSafetyLearningTemplateApi(), '安全学习_导入模板.xlsx');
  } catch {
    message.error('模板下载失败');
  }
}

async function downloadRecords() {
  try {
    await saveBlob(await downloadSafetyLearningContentsApi(listParams()), '安全学习_数据下载.xlsx');
  } catch {
    message.error('数据下载失败');
  }
}

function chooseImportFile() {
  importInputRef.value?.click();
}

async function handleImportChange(event: Event) {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  if (!file) {
    return;
  }
  try {
    const result = await importSafetyLearningWorkbookApi(file);
    if (result.errors.length) {
      message.warning(`导入成功 ${result.successRows} 条，失败 ${result.errors.length} 条`);
    } else {
      message.success(`导入成功 ${result.successRows} 条`);
    }
    await loadRecords();
  } catch {
    message.error('上传数据失败');
  } finally {
    input.value = '';
  }
}

function statusBoxClass(label: string) {
  return [
    'status-box',
    {
      'status-box--blue': label === '激活',
      'status-box--gray': label === '作废',
      'status-box--orange': label === '草稿',
    },
  ];
}

onMounted(async () => {
  await loadOrganizations();
  resetForm();
  enforceCompanyScope();
  await loadRecords();
});

onBeforeUnmount(() => {
  clearPendingCoverImage();
  clearPendingVideo();
});
</script>

<template>
  <Page auto-content-height content-class="pingan-page">
    <section class="safety-learning-shell">
      <DataMapPanel
        v-if="shouldShowDataMap"
        v-model:selected-keys="selectedOrganizationKeys"
        :tree-data="organizationNodes"
        @select="handleDataMapSelect"
      />

      <section class="safety-learning-workbench">
        <div class="workbench__bar">
          <div class="filter-item">
            <span>学习内容类型</span>
            <Select v-model:value="filters.category" :options="categoryOptions" />
          </div>
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
          <div class="filter-item filter-item--keyword">
            <span>关键词</span>
            <Input
              v-model:value="filters.keyword"
              allow-clear
              placeholder="使用 编码,标题,分类 搜索"
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
              <div class="table-panel__title">安全学习</div>
              <div class="table-panel__summary">
                共 {{ total }} 条记录，按学习日期筛选
              </div>
            </div>
            <div class="table-panel__actions">
              <span class="table-panel__selection">
                已选 {{ selectedRowKeys.length }} 条
              </span>
              <Button :disabled="!canBatchDelete" v-if="canDeleteSafetyLearning" danger @click="batchDeleteRecords">
                批量删除
              </Button>
              <Button v-if="canCreateSafetyLearning" type="primary" @click="openCreateModal">
                <IconifyIcon icon="lucide:plus" />
                新增
              </Button>
              <Button v-if="canDownloadSafetyLearning" @click="downloadTemplate">
                <IconifyIcon icon="lucide:file-spreadsheet" />
                下载模板
              </Button>
              <Button v-if="canDownloadSafetyLearning" @click="downloadRecords">
                <IconifyIcon icon="lucide:download" />
                下载数据
              </Button>
              <Button v-if="canCreateSafetyLearning" @click="chooseImportFile">
                <IconifyIcon icon="lucide:upload" />
                上传数据
              </Button>
              <input
                ref="importInputRef"
                accept=".xlsx,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                hidden
                type="file"
                @change="handleImportChange"
              />
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
            :scroll="{ x: 2050, y: 'calc(100vh - 390px)' }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'coverImage'">
                <button
                  v-if="record.coverImageAttachment?.url"
                  class="table-media table-media--image"
                  type="button"
                  @click="openMediaPreview('IMAGE', record.coverImageAttachment)"
                >
                  <img
                    :alt="record.coverImageAttachment.originalName"
                    :src="record.coverImageAttachment.url"
                  />
                </button>
                <span v-else class="attachment-pill attachment-pill--missing">
                  {{ record.coverImage || '未上传' }}
                </span>
              </template>
              <template v-else-if="column.dataIndex === 'video'">
                <button
                  v-if="record.videoAttachment?.url"
                  class="table-media table-media--video"
                  type="button"
                  @click="openMediaPreview('VIDEO', record.videoAttachment)"
                >
                  <IconifyIcon icon="lucide:video" />
                  <span>{{ record.videoAttachment.originalName }}</span>
                </button>
                <span v-else class="attachment-pill attachment-pill--missing">
                  {{ record.video || '未上传' }}
                </span>
              </template>
              <template v-else-if="column.dataIndex === 'attachment'">
                <Button
                  v-if="record.attachment?.url"
                  size="small"
                  type="link"
                  @click="openAttachment(record.attachment)"
                >
                  {{ record.attachment.originalName }}
                </Button>
                <span v-else class="attachment-pill attachment-pill--missing">
                  {{ record.attachmentText || '未上传' }}
                </span>
              </template>
              <template v-else-if="column.dataIndex === 'statusLabel'">
                <span :class="statusBoxClass(record.statusLabel)">
                  {{ record.statusLabel }}
                </span>
              </template>
              <template v-else-if="column.dataIndex === 'actions'">
                <Space>
                  <Button v-if="canEditSafetyLearning" size="small" type="link" @click="openEditModal(asSafetyLearningContent(record))">
                    编辑
                  </Button>
                  <Button
                    v-if="canVoidSafetyLearning && record.status !== 'INACTIVE'"
                    danger
                    size="small"
                    type="link"
                    @click="voidRecord(asSafetyLearningContent(record))"
                  >
                    作废
                  </Button>
                  <Button v-if="canDeleteSafetyLearning" danger size="small" type="link" @click="deleteRecord(asSafetyLearningContent(record))">
                    删除
                  </Button>
                </Space>
              </template>
            </template>
          </Table>
        </div>
      </section>
    </section>

    <Modal
      v-model:open="modalOpen"
      :confirm-loading="saving"
      :title="modalTitle"
      ok-text="保存"
      width="860px"
      @ok="saveRecord"
    >
      <div class="safety-learning-form">
        <div class="form-grid">
          <label>
            <span>公司</span>
            <Select
              v-model:value="form.companyId"
              :disabled="isCompanyFilterLocked"
              :options="companyOptions"
              option-filter-prop="label"
              placeholder="点击选择"
              show-search
            />
          </label>
          <label>
            <span>分类</span>
            <Select v-model:value="form.category" :options="formCategoryOptions" />
          </label>
          <label>
            <span>标题</span>
            <Input v-model:value="form.title" placeholder="请输入标题" />
          </label>
          <label>
            <span>日期</span>
            <DatePicker
              v-model:value="form.learningDate"
              placeholder="点击选择"
              value-format="YYYY-MM-DD"
            />
          </label>
          <label>
            <span>计时</span>
            <Input v-model:value="form.durationText" placeholder="如 30分钟" />
          </label>
          <label>
            <span>编码</span>
            <Input v-model:value="form.code" placeholder="为空时自动生成" />
          </label>
          <label>
            <span>草稿</span>
            <Select
              v-model:value="form.draft"
              :options="[
                { label: '否', value: '否' },
                { label: '是', value: '是' },
              ]"
            />
          </label>
          <label>
            <span>状态</span>
            <Select v-model:value="form.status" :options="formStatusOptions" />
          </label>
        </div>
        <div class="media-grid">
          <div class="media-card media-card--cover">
            <button
              class="media-card__preview"
              type="button"
              @click="openMediaPreviewFromUrl('IMAGE', pendingCoverImageUrl || currentCoverImageAttachment?.url, pendingCoverImage?.name || currentCoverImageAttachment?.originalName || '封面图')"
            >
              <img
                v-if="pendingCoverImageUrl || currentCoverImageAttachment?.url"
                alt="封面图预览"
                :src="pendingCoverImageUrl || currentCoverImageAttachment?.url"
              />
              <IconifyIcon v-else icon="lucide:image" />
            </button>
            <div class="media-card__body">
              <div class="media-card__title">封面图</div>
              <div v-if="currentCoverImageAttachment?.url" class="media-card__file">
                <Button type="link" @click="openAttachment(currentCoverImageAttachment)">
                  {{ currentCoverImageAttachment.originalName }}
                </Button>
                <Button v-if="canDeleteSafetyLearning" aria-label="删除封面图"
                  class="attachment-card__remove"
                  danger
                  shape="circle"
                  size="small"
                  type="text"
                  @click.stop="removeCurrentCoverImage"
                >
                  <IconifyIcon icon="lucide:x" />
                </Button>
              </div>
              <div v-if="pendingCoverImage" class="media-card__file media-card__file--pending">
                <span>待上传：{{ pendingCoverImage.name }}</span>
                <Button
                  aria-label="移除待上传封面图"
                  class="attachment-card__remove"
                  shape="circle"
                  size="small"
                  type="text"
                  @click.stop="clearPendingCoverImage"
                >
                  <IconifyIcon icon="lucide:x" />
                </Button>
              </div>
            </div>
            <Button v-if="canCreateSafetyLearning" @click="chooseCoverImage">
              <IconifyIcon icon="lucide:image-up" />
              选择图片
            </Button>
            <input
              ref="coverImageInputRef"
              accept="image/*"
              hidden
              type="file"
              @change="handleCoverImageChange"
            />
          </div>
          <div class="media-card media-card--video">
            <button
              class="media-card__preview"
              type="button"
              @click="openMediaPreviewFromUrl('VIDEO', pendingVideoUrl || currentVideoAttachment?.url, pendingVideo?.name || currentVideoAttachment?.originalName || '视频')"
            >
              <video
                v-if="pendingVideoUrl || currentVideoAttachment?.url"
                :src="pendingVideoUrl || currentVideoAttachment?.url"
                muted
                playsinline
              ></video>
              <IconifyIcon v-else icon="lucide:video" />
            </button>
            <div class="media-card__body">
              <div class="media-card__title">视频</div>
              <div v-if="currentVideoAttachment?.url" class="media-card__file">
                <Button type="link" @click="openAttachment(currentVideoAttachment)">
                  {{ currentVideoAttachment.originalName }}
                </Button>
                <Button v-if="canDeleteSafetyLearning" aria-label="删除视频"
                  class="attachment-card__remove"
                  danger
                  shape="circle"
                  size="small"
                  type="text"
                  @click.stop="removeCurrentVideo"
                >
                  <IconifyIcon icon="lucide:x" />
                </Button>
              </div>
              <div v-if="pendingVideo" class="media-card__file media-card__file--pending">
                <span>待上传：{{ pendingVideo.name }}</span>
                <Button
                  aria-label="移除待上传视频"
                  class="attachment-card__remove"
                  shape="circle"
                  size="small"
                  type="text"
                  @click.stop="clearPendingVideo"
                >
                  <IconifyIcon icon="lucide:x" />
                </Button>
              </div>
            </div>
            <Button v-if="canCreateSafetyLearning" @click="chooseVideo">
              <IconifyIcon icon="lucide:video" />
              选择视频
            </Button>
            <input
              ref="videoInputRef"
              accept="video/*"
              hidden
              type="file"
              @change="handleVideoChange"
            />
          </div>
        </div>
        <label class="form-full">
          <span>内容</span>
          <Input.TextArea v-model:value="form.content" :rows="3" placeholder="请输入学习内容" />
        </label>
        <label class="form-full">
          <span>HTML提取</span>
          <Input.TextArea v-model:value="form.htmlExtract" :rows="2" placeholder="文本记录，不做提取" />
        </label>
        <div class="safety-learning-attachment-field">
          <div class="attachment-card">
            <div>
              <div class="attachment-card__title">PDF附件</div>
              <div class="attachment-card__hint">
                仅上传 PDF 文件，保存后会关联到当前安全学习内容
              </div>
              <div v-if="currentAttachment?.url" class="attachment-card__file">
                <Button type="link" @click="openAttachment(currentAttachment)">
                  {{ currentAttachment.originalName }}
                </Button>
                <Button v-if="canDeleteSafetyLearning" aria-label="删除附件"
                  class="attachment-card__remove"
                  danger
                  shape="circle"
                  size="small"
                  type="text"
                  @click.stop="removeCurrentAttachment"
                >
                  <IconifyIcon icon="lucide:x" />
                </Button>
              </div>
              <div v-if="pendingAttachment" class="attachment-card__pending">
                <span>待上传：{{ pendingAttachment.name }}</span>
                <Button
                  aria-label="移除待上传附件"
                  class="attachment-card__remove"
                  shape="circle"
                  size="small"
                  type="text"
                  @click.stop="clearPendingAttachment"
                >
                  <IconifyIcon icon="lucide:x" />
                </Button>
              </div>
            </div>
            <Button v-if="canCreateSafetyLearning" @click="chooseAttachment">
              <IconifyIcon icon="lucide:file-up" />
              选择PDF
            </Button>
            <input
              ref="attachmentInputRef"
              accept=".pdf,application/pdf"
              hidden
              type="file"
              @change="handleAttachmentChange"
            />
          </div>
        </div>
      </div>
    </Modal>

    <Modal
      v-model:open="mediaPreviewOpen"
      :footer="null"
      :title="mediaPreviewModalTitle"
      width="760px"
      centered
      destroy-on-close
      @cancel="closeMediaPreview"
    >
      <div class="media-preview">
        <img
          v-if="mediaPreviewKind === 'IMAGE'"
          :alt="mediaPreviewModalTitle"
          class="media-preview__image"
          :src="mediaPreviewUrl"
        />
        <video
          v-else
          class="media-preview__video"
          controls
          :src="mediaPreviewUrl"
        ></video>
      </div>
    </Modal>
  </Page>
</template>

<style scoped>
:global(.pingan-page) {
  height: 100%;
  padding: 12px;
  background: #f5f7fb;
}

.safety-learning-shell {
  display: flex;
  gap: 12px;
  height: 100%;
  min-height: 0;
  overflow: hidden;
}

.safety-learning-workbench {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}

.workbench__bar,
.table-panel {
  border: 1px solid #d7e6f8;
  border-radius: 6px;
  background: #fff;
}

.workbench__bar {
  display: flex;
  flex-wrap: nowrap;
  align-items: flex-end;
  gap: 10px;
  padding: 12px;
  overflow: hidden;
}

.filter-item {
  display: flex;
  flex: 1 1 126px;
  flex-direction: column;
  gap: 5px;
  min-width: 0;
}

.filter-item > span {
  color: #334155;
  font-size: 12px;
  font-weight: 700;
  line-height: 18px;
}

.filter-item :deep(.ant-select),
.filter-item :deep(.ant-picker),
.filter-item :deep(.ant-input-affix-wrapper) {
  width: 100%;
  height: 40px;
}

.filter-item :deep(.ant-select-single .ant-select-selector),
.filter-item :deep(.ant-picker) {
  height: 40px;
}

.workbench__bar .filter-item :deep(.ant-select-single .ant-select-selector),
.workbench__bar .filter-item :deep(.ant-select-selection-item),
.workbench__bar .filter-item :deep(.ant-select-selection-placeholder) {
  display: flex;
  align-items: center;
}

.workbench__bar .filter-item :deep(.ant-select-selection-item),
.workbench__bar .filter-item :deep(.ant-select-selection-placeholder) {
  height: 100%;
  line-height: normal !important;
}

.table-panel {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}

.table-panel__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px;
}

.table-panel__title {
  color: #1f3b57;
  font-size: 16px;
  font-weight: 600;
}

.table-panel__summary,
.table-panel__selection {
  color: #6b7d90;
  font-size: 12px;
}

.table-panel__actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

.meeting-table :deep(.ant-table-thead > tr > th) {
  background: #eaf4ff;
  color: #244c73;
  font-weight: 600;
}

.meeting-table {
  min-height: 0;
}

.attachment-pill {
  display: inline-flex;
  max-width: 150px;
  color: #47667f;
  font-size: 12px;
}

.attachment-pill--missing {
  color: #9aa9b8;
}

.table-media {
  display: inline-flex;
  max-width: 132px;
  align-items: center;
  gap: 6px;
  border: 0;
  background: transparent;
  color: #1769c2;
  cursor: pointer;
  font-size: 12px;
}

.table-media--image img {
  width: 64px;
  height: 38px;
  border: 1px solid #d7e6f8;
  border-radius: 4px;
  object-fit: cover;
}

.table-media--video span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-box {
  display: inline-flex;
  min-width: 52px;
  justify-content: center;
  border-radius: 4px;
  padding: 2px 8px;
  font-size: 12px;
}

.status-box--blue {
  background: #e8f3ff;
  color: #1769c2;
}

.status-box--orange {
  background: #fff4e5;
  color: #b66300;
}

.status-box--gray {
  background: #eef1f4;
  color: #5f6f80;
}

.safety-learning-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.form-grid label,
.form-full {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-grid span,
.form-full span {
  color: #34516f;
  font-size: 13px;
}

.form-grid :deep(.ant-picker) {
  width: 100%;
}

.media-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.media-card {
  display: grid;
  grid-template-columns: 168px minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  border: 1px solid #d7e6f8;
  border-radius: 6px;
  background: #f8fbff;
  padding: 12px;
}

.media-card__preview {
  display: grid;
  width: 168px;
  height: 168px;
  place-items: center;
  overflow: hidden;
  border: 1px solid #cfe1f4;
  border-radius: 6px;
  background: #eef6ff;
  color: #5f7f9d;
  cursor: pointer;
  padding: 0;
}

.media-card__preview img,
.media-card__preview video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.media-card__body {
  min-width: 0;
}

.media-card__title {
  color: #244c73;
  font-weight: 600;
}

.media-card__file {
  display: flex;
  max-width: 100%;
  align-items: center;
  gap: 6px;
  color: #6b7d90;
  font-size: 12px;
}

.media-card__file :deep(.ant-btn-link),
.media-card__file span {
  max-width: 220px;
  overflow: hidden;
  padding-inline: 0;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.attachment-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border: 1px dashed #9fc3e8;
  border-radius: 6px;
  background: #f7fbff;
  padding: 14px;
}

.attachment-card__title {
  color: #244c73;
  font-weight: 600;
}

.attachment-card__hint,
.attachment-card__pending {
  color: #6b7d90;
  font-size: 12px;
}

.attachment-card__file,
.attachment-card__pending {
  display: inline-flex;
  max-width: 100%;
  align-items: center;
  gap: 6px;
}

.attachment-card__file :deep(.ant-btn-link) {
  max-width: 320px;
  overflow: hidden;
  padding-inline: 0;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.attachment-card__pending span {
  max-width: 320px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.attachment-card__remove {
  flex: 0 0 auto;
}

.media-preview {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 360px;
  background: #f6f9fc;
}

.media-preview__image,
.media-preview__video {
  max-width: 100%;
  max-height: 70vh;
  border-radius: 6px;
  object-fit: contain;
}

.media-preview__video {
  width: 100%;
  background: #000;
}

@media (max-width: 960px) {
  .safety-learning-shell {
    flex-direction: column;
    height: auto;
    overflow: visible;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .media-grid {
    grid-template-columns: 1fr;
  }

  .media-card {
    grid-template-columns: 140px minmax(0, 1fr);
  }

  .workbench__bar {
    flex-wrap: wrap;
    overflow: visible;
  }

  .filter-item {
    flex-basis: calc(50% - 5px);
  }
}
</style>
