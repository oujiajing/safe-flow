<script setup lang="ts">
import type { TableColumnsType } from 'ant-design-vue';

import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';

import {
  Button,
  Input,
  InputNumber,
  message,
  Modal,
  Select,
  Space,
  Table,
  Tag,
} from 'ant-design-vue';

import { getPinganCompanyOrgTreeApi } from '#/api/pingan/pre-shift-meeting';
import type { PinganPreShiftMeetingApi } from '#/api/pingan/pre-shift-meeting';
import {
  createContentProfileApi,
  deleteContentProfileApi,
  getContentProfileListApi,
  removeContentProfileImageApi,
  removeContentProfileVideoApi,
  updateContentProfileApi,
  updateContentProfileStatusApi,
  uploadContentProfileImageApi,
  uploadContentProfileVideoApi,
} from '#/api/system-management/content-profile';
import type { SystemContentProfileApi } from '#/api/system-management/content-profile';

type ContentProfile = SystemContentProfileApi.ContentProfile;
type ContentProfilePayload = SystemContentProfileApi.ContentProfilePayload;
type ContentProfileStatus = SystemContentProfileApi.Status;
type OrgNode = PinganPreShiftMeetingApi.OrgNode;

interface OrgOption {
  label: string;
  orgType: string;
  value: string | number;
}

const statusOptions: Array<{ label: string; value: ContentProfileStatus | 'all' }> = [
  { label: '全部', value: 'all' },
  { label: '草稿', value: 'DRAFT' },
  { label: '激活', value: 'ACTIVE' },
  { label: '作废', value: 'INACTIVE' },
];

const formStatusOptions = statusOptions.filter((item) => item.value !== 'all');

const statusTone: Record<string, string> = {
  ACTIVE: 'green',
  DRAFT: 'blue',
  INACTIVE: 'red',
};

const statusLabel: Record<string, string> = {
  ACTIVE: '激活',
  DRAFT: '草稿',
  INACTIVE: '作废',
};

const columns: TableColumnsType<ContentProfile> = [
  { dataIndex: 'orgName', fixed: 'left', title: '组织', width: 170 },
  { dataIndex: 'title', title: '简介标题', width: 180 },
  { dataIndex: 'subtitle', title: '副标题', width: 170 },
  { dataIndex: 'description', title: '简介正文', width: 260 },
  { dataIndex: 'imageAttachment', title: '主图', width: 150 },
  { dataIndex: 'videoTitle', title: '视频标题', width: 160 },
  { dataIndex: 'videoAttachment', title: '宣传视频', width: 170 },
  { dataIndex: 'videoSortOrder', title: '轮播排序', width: 100 },
  { dataIndex: 'status', title: '状态', width: 90 },
  { dataIndex: 'actions', fixed: 'right', title: '操作', width: 190 },
];

const filters = reactive<{
  keyword: string;
  orgId: SystemContentProfileApi.Id | undefined;
  status: ContentProfileStatus | 'all';
}>({
  keyword: '',
  orgId: undefined,
  status: 'all',
});
const form = reactive<ContentProfilePayload>(emptyForm());
const rows = ref<ContentProfile[]>([]);
const total = ref(0);
const loading = ref(false);
const saving = ref(false);
const modalOpen = ref(false);
const editingId = ref<SystemContentProfileApi.Id>();
const orgOptions = ref<OrgOption[]>([]);
const imageInputRef = ref<HTMLInputElement>();
const videoInputRef = ref<HTMLInputElement>();
const pendingImage = ref<File>();
const pendingVideo = ref<File>();
const pendingImageUrl = ref('');
const pendingVideoUrl = ref('');
const currentImageAttachment = ref<SystemContentProfileApi.Attachment>();
const currentVideoAttachment = ref<SystemContentProfileApi.Attachment>();
const previewOpen = ref(false);
const previewKind = ref<'IMAGE' | 'VIDEO'>('IMAGE');
const previewTitle = ref('');
const previewUrl = ref('');

const modalTitle = computed(() => (editingId.value ? '编辑内容配置' : '新增内容配置'));

function emptyForm(): ContentProfilePayload {
  return {
    description: '',
    orgId: undefined,
    status: 'ACTIVE',
    subtitle: '',
    title: '',
    videoSortOrder: 0,
    videoTitle: '',
  };
}

function flattenOrgOptions(nodes: OrgNode[]): OrgOption[] {
  return nodes.flatMap((node) => [
    ...(node.id !== undefined && ['GROUP', 'COMPANY'].includes(node.orgType)
      ? [{ label: node.title, orgType: node.orgType, value: node.id }]
      : []),
    ...flattenOrgOptions(node.children ?? []),
  ]);
}

async function loadOrganizations() {
  try {
    orgOptions.value = flattenOrgOptions(await getPinganCompanyOrgTreeApi());
  } catch {
    orgOptions.value = [];
  }
}

function listParams() {
  return {
    keyword: filters.keyword || undefined,
    orgId: filters.orgId || undefined,
    page: 1,
    pageSize: 20,
    status: filters.status || 'all',
  };
}

async function loadRecords() {
  loading.value = true;
  try {
    const result = await getContentProfileListApi(listParams());
    rows.value = result.items;
    total.value = Number(result.total ?? 0);
  } catch {
    rows.value = [];
    total.value = 0;
    message.error('内容配置加载失败');
  } finally {
    loading.value = false;
  }
}

function resetFilters() {
  filters.keyword = '';
  filters.orgId = undefined;
  filters.status = 'all';
  void loadRecords();
}

function resetForm() {
  Object.assign(form, emptyForm());
  editingId.value = undefined;
  currentImageAttachment.value = undefined;
  currentVideoAttachment.value = undefined;
  clearPendingImage();
  clearPendingVideo();
}

function openCreate() {
  resetForm();
  modalOpen.value = true;
}

function openEdit(record: Record<string, any>) {
  const profile = record as ContentProfile;
  resetForm();
  editingId.value = profile.id;
  Object.assign(form, {
    description: profile.description || '',
    orgId: profile.orgId,
    status: profile.status || 'ACTIVE',
    subtitle: profile.subtitle || '',
    title: profile.title || '',
    videoSortOrder: profile.videoSortOrder ?? 0,
    videoTitle: profile.videoTitle || '',
  });
  currentImageAttachment.value = profile.imageAttachment;
  currentVideoAttachment.value = profile.videoAttachment;
  modalOpen.value = true;
}

async function saveRecord() {
  if (!form.orgId) {
    message.warning('请选择组织');
    return;
  }
  if (!form.title?.trim()) {
    message.warning('请输入简介标题');
    return;
  }
  saving.value = true;
  try {
    const saved = editingId.value
      ? await updateContentProfileApi(editingId.value, form)
      : await createContentProfileApi(form);
    if (pendingImage.value) {
      try {
        await uploadContentProfileImageApi(saved.id, pendingImage.value);
      } catch (error) {
        message.error(`内容基本信息已保存，主图上传失败：${resolveErrorMessage(error)}`);
        return;
      }
    }
    if (pendingVideo.value) {
      try {
        await uploadContentProfileVideoApi(saved.id, pendingVideo.value);
      } catch (error) {
        message.error(`内容基本信息已保存，视频上传失败：${resolveErrorMessage(error)}`);
        return;
      }
    }
    message.success('内容配置已保存');
    modalOpen.value = false;
    resetForm();
    await loadRecords();
  } catch (error) {
    message.error(`内容配置保存失败：${resolveErrorMessage(error)}`);
  } finally {
    saving.value = false;
  }
}

function resolveErrorMessage(error: unknown) {
  const responseData = (error as { response?: { data?: Record<string, unknown> } })
    ?.response?.data;
  const responseMessage = responseData?.error || responseData?.message;
  return typeof responseMessage === 'string' && responseMessage.trim()
    ? responseMessage
    : '请检查文件大小、文件类型或后端存储服务状态';
}

function updateStatus(record: Record<string, any>, status: ContentProfileStatus) {
  const profile = record as ContentProfile;
  void updateContentProfileStatusApi(profile.id, status)
    .then(loadRecords)
    .then(() => message.success('状态已更新'))
    .catch(() => message.error('状态更新失败'));
}

function deleteRecord(record: Record<string, any>) {
  const profile = record as ContentProfile;
  Modal.confirm({
    content: `确认删除「${profile.title}」？`,
    onOk: async () => {
      await deleteContentProfileApi(profile.id);
      message.success('内容配置已删除');
      await loadRecords();
    },
    title: '删除内容配置',
  });
}

function chooseImage() {
  imageInputRef.value?.click();
}

function chooseVideo() {
  videoInputRef.value?.click();
}

function handleImageChange(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0];
  if (!file) return;
  if (!file.type.startsWith('image/')) {
    message.warning('主图仅支持图片文件');
    return;
  }
  clearPendingImage();
  pendingImage.value = file;
  pendingImageUrl.value = URL.createObjectURL(file);
  (event.target as HTMLInputElement).value = '';
}

function handleVideoChange(event: Event) {
  const file = (event.target as HTMLInputElement).files?.[0];
  if (!file) return;
  if (!file.type.startsWith('video/')) {
    message.warning('宣传视频仅支持视频文件');
    return;
  }
  clearPendingVideo();
  pendingVideo.value = file;
  pendingVideoUrl.value = URL.createObjectURL(file);
  (event.target as HTMLInputElement).value = '';
}

function clearPendingImage() {
  if (pendingImageUrl.value) {
    URL.revokeObjectURL(pendingImageUrl.value);
  }
  pendingImage.value = undefined;
  pendingImageUrl.value = '';
}

function clearPendingVideo() {
  if (pendingVideoUrl.value) {
    URL.revokeObjectURL(pendingVideoUrl.value);
  }
  pendingVideo.value = undefined;
  pendingVideoUrl.value = '';
}

async function removeCurrentImage() {
  if (!editingId.value || !currentImageAttachment.value) return;
  const updated = await removeContentProfileImageApi(editingId.value);
  currentImageAttachment.value = updated.imageAttachment;
  message.success('主图已删除');
}

async function removeCurrentVideo() {
  if (!editingId.value || !currentVideoAttachment.value) return;
  const updated = await removeContentProfileVideoApi(editingId.value);
  currentVideoAttachment.value = updated.videoAttachment;
  message.success('宣传视频已删除');
}

function openPreview(kind: 'IMAGE' | 'VIDEO', url?: string, title?: string) {
  if (!url) return;
  previewKind.value = kind;
  previewUrl.value = url;
  previewTitle.value = title || (kind === 'IMAGE' ? '主图' : '宣传视频');
  previewOpen.value = true;
}

onMounted(() => {
  void loadOrganizations();
  void loadRecords();
});

onBeforeUnmount(() => {
  clearPendingImage();
  clearPendingVideo();
});
</script>

<template>
  <Page class="content-profile-page">
    <section class="content-profile-toolbar">
      <Input
        v-model:value="filters.keyword"
        allow-clear
        class="filter-keyword"
        placeholder="标题、正文"
        @press-enter="loadRecords"
      />
      <Select
        v-model:value="filters.orgId"
        allow-clear
        class="filter-org"
        :options="orgOptions"
        placeholder="组织"
      />
      <Select
        v-model:value="filters.status"
        class="filter-status"
        :options="statusOptions"
      />
      <Button type="primary" @click="loadRecords">
        <IconifyIcon icon="lucide:search" />
        查询
      </Button>
      <Button @click="resetFilters">
        <IconifyIcon icon="lucide:rotate-ccw" />
        重置
      </Button>
      <Button type="primary" @click="openCreate">
        <IconifyIcon icon="lucide:plus" />
        新增
      </Button>
    </section>

    <Table
      bordered
      :columns="columns"
      :data-source="rows"
      :loading="loading"
      row-key="id"
      :pagination="{ pageSize: 20, showTotal: () => `共 ${total} 条`, total }"
      :scroll="{ x: 1480 }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'description'">
          <span class="text-ellipsis">{{ record.description || '-' }}</span>
        </template>
        <template v-else-if="column.dataIndex === 'imageAttachment'">
          <Button
            v-if="record.imageAttachment?.url"
            type="link"
            @click="openPreview('IMAGE', record.imageAttachment.url, record.imageAttachment.originalName)"
          >
            主图
          </Button>
          <span v-else>-</span>
        </template>
        <template v-else-if="column.dataIndex === 'videoAttachment'">
          <Button
            v-if="record.videoAttachment?.url"
            type="link"
            @click="openPreview('VIDEO', record.videoAttachment.url, record.videoAttachment.originalName)"
          >
            {{ record.videoAttachment.originalName }}
          </Button>
          <span v-else>-</span>
        </template>
        <template v-else-if="column.dataIndex === 'status'">
          <Tag :color="statusTone[record.status || 'DRAFT']">
            {{ statusLabel[record.status || 'DRAFT'] }}
          </Tag>
        </template>
        <template v-else-if="column.dataIndex === 'actions'">
          <Space>
            <Button size="small" type="link" @click="openEdit(record)">编辑</Button>
            <Button
              v-if="record.status !== 'ACTIVE'"
              size="small"
              type="link"
              @click="updateStatus(record, 'ACTIVE')"
            >
              启用
            </Button>
            <Button
              v-else
              size="small"
              type="link"
              @click="updateStatus(record, 'INACTIVE')"
            >
              作废
            </Button>
            <Button danger size="small" type="link" @click="deleteRecord(record)">删除</Button>
          </Space>
        </template>
      </template>
    </Table>

    <Modal
      v-model:open="modalOpen"
      :confirm-loading="saving"
      :title="modalTitle"
      width="920px"
      @ok="saveRecord"
    >
      <section class="content-profile-form">
        <label>
          <span>组织</span>
          <Select
            v-model:value="form.orgId"
            :disabled="Boolean(editingId)"
            :options="orgOptions"
            placeholder="选择集团或公司"
          />
        </label>
        <label>
          <span>状态</span>
          <Select v-model:value="form.status" :options="formStatusOptions" />
        </label>
        <label>
          <span>简介标题</span>
          <Input v-model:value="form.title" />
        </label>
        <label>
          <span>副标题</span>
          <Input v-model:value="form.subtitle" />
        </label>
        <label class="form-span-2">
          <span>简介正文</span>
          <Input.TextArea v-model:value="form.description" :rows="5" />
        </label>
        <label>
          <span>视频标题</span>
          <Input v-model:value="form.videoTitle" />
        </label>
        <label>
          <span>轮播排序</span>
          <InputNumber v-model:value="form.videoSortOrder" :min="0" />
        </label>
      </section>

      <section class="media-upload-grid">
        <div class="media-upload-card">
          <div class="media-upload-card__head">
            <strong>主图</strong>
            <Button size="small" @click="chooseImage">
              <IconifyIcon icon="lucide:image-plus" />
              上传
            </Button>
          </div>
          <button
            v-if="pendingImageUrl || currentImageAttachment?.url"
            class="media-preview media-preview--image"
            type="button"
            @click="openPreview('IMAGE', pendingImageUrl || currentImageAttachment?.url, pendingImage?.name || currentImageAttachment?.originalName)"
          >
            <img
              alt="内容配置主图"
              :src="pendingImageUrl || currentImageAttachment?.url"
            />
          </button>
          <div v-else class="media-empty">暂无主图</div>
          <Space v-if="pendingImage || currentImageAttachment" class="media-actions">
            <span>{{ pendingImage?.name || currentImageAttachment?.originalName }}</span>
            <Button v-if="pendingImage" size="small" type="link" @click="clearPendingImage">移除</Button>
            <Button
              v-else-if="currentImageAttachment"
              danger
              size="small"
              type="link"
              @click="removeCurrentImage"
            >
              删除
            </Button>
          </Space>
          <input
            ref="imageInputRef"
            accept="image/*"
            class="hidden-file-input"
            type="file"
            @change="handleImageChange"
          />
        </div>

        <div class="media-upload-card">
          <div class="media-upload-card__head">
            <strong>宣传视频</strong>
            <Button size="small" @click="chooseVideo">
              <IconifyIcon icon="lucide:video" />
              上传
            </Button>
          </div>
          <button
            v-if="pendingVideoUrl || currentVideoAttachment?.url"
            class="media-preview media-preview--video"
            type="button"
            @click="openPreview('VIDEO', pendingVideoUrl || currentVideoAttachment?.url, pendingVideo?.name || currentVideoAttachment?.originalName)"
          >
            <video :src="pendingVideoUrl || currentVideoAttachment?.url"></video>
          </button>
          <div v-else class="media-empty">暂无宣传视频</div>
          <Space v-if="pendingVideo || currentVideoAttachment" class="media-actions">
            <span>{{ pendingVideo?.name || currentVideoAttachment?.originalName }}</span>
            <Button v-if="pendingVideo" size="small" type="link" @click="clearPendingVideo">移除</Button>
            <Button
              v-else-if="currentVideoAttachment"
              danger
              size="small"
              type="link"
              @click="removeCurrentVideo"
            >
              删除
            </Button>
          </Space>
          <input
            ref="videoInputRef"
            accept="video/*"
            class="hidden-file-input"
            type="file"
            @change="handleVideoChange"
          />
        </div>
      </section>
    </Modal>

    <Modal v-model:open="previewOpen" :footer="null" :title="previewTitle" width="860px">
      <img
        v-if="previewKind === 'IMAGE'"
        alt="内容配置预览图"
        class="preview-image"
        :src="previewUrl"
      />
      <video v-else class="preview-video" controls :src="previewUrl"></video>
    </Modal>
  </Page>
</template>

<style scoped>
.content-profile-page {
  min-height: 100%;
}

.content-profile-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  margin-bottom: 12px;
}

.filter-keyword {
  width: 220px;
}

.filter-org {
  width: 240px;
}

.filter-status {
  width: 120px;
}

.text-ellipsis {
  display: inline-block;
  max-width: 240px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: bottom;
}

.content-profile-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px 18px;
}

.content-profile-form label {
  display: grid;
  gap: 6px;
  min-width: 0;
  color: #334155;
  font-weight: 600;
}

.content-profile-form label > span {
  font-size: 13px;
}

.form-span-2 {
  grid-column: 1 / -1;
}

.media-upload-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-top: 18px;
}

.media-upload-card {
  min-width: 0;
  padding: 12px;
  border: 1px solid #d8dee8;
  border-radius: 6px;
  background: #f8fafc;
}

.media-upload-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
}

.media-preview,
.media-empty {
  display: flex;
  width: 100%;
  aspect-ratio: 16 / 8;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  color: #64748b;
  background: #e8eef6;
  border: 0;
  border-radius: 4px;
}

.media-preview img,
.media-preview video {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.media-actions {
  width: 100%;
  margin-top: 8px;
}

.media-actions span {
  max-width: 240px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hidden-file-input {
  display: none;
}

.preview-image,
.preview-video {
  display: block;
  width: 100%;
  max-height: 70vh;
  object-fit: contain;
  background: #0f172a;
}

@media (max-width: 900px) {
  .content-profile-form,
  .media-upload-grid {
    grid-template-columns: 1fr;
  }
}
</style>
