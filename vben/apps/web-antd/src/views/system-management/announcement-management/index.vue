<script setup lang="ts">
import type { TableColumnsType } from 'ant-design-vue';

import { computed, onMounted, reactive, ref } from 'vue';

import { Page } from '@vben/common-ui';

import {
  Button,
  Card,
  Col,
  DatePicker,
  Drawer,
  Empty,
  Form,
  FormItem,
  Input,
  message,
  Modal,
  Progress,
  Row,
  Segmented,
  Select,
  Space,
  Statistic,
  Table,
  Tag,
  Textarea,
} from 'ant-design-vue';

import {
  createAnnouncementApi,
  getAnnouncementDeliveriesApi,
  getAnnouncementListApi,
  publishAnnouncementApi,
  updateAnnouncementApi,
  withdrawAnnouncementApi,
} from '#/api/system-management/announcement';
import type { SystemAnnouncementApi } from '#/api/system-management/announcement';

type Announcement = SystemAnnouncementApi.Announcement;
type AnnouncementInput = SystemAnnouncementApi.AnnouncementInput;
type Delivery = SystemAnnouncementApi.Delivery;

const rows = ref<Announcement[]>([]);
const total = ref(0);
const loading = ref(false);
const page = ref(1);
const pageSize = ref(20);
const status = ref('ALL');
const editorOpen = ref(false);
const auditOpen = ref(false);
const saving = ref(false);
const currentId = ref<string>();
const deliveries = ref<Delivery[]>([]);
const deliveryLoading = ref(false);
const auditTitle = ref('');

const form = reactive({
  audienceIds: '',
  audienceType: 'ALL' as SystemAnnouncementApi.AudienceType,
  content: '',
  effectiveAt: undefined as string | undefined,
  expiresAt: undefined as string | undefined,
  severity: 'INFO' as SystemAnnouncementApi.Severity,
  title: '',
});

const columns: TableColumnsType<Announcement> = [
  { dataIndex: 'title', key: 'title', title: '公告', width: 310 },
  { dataIndex: 'severity', key: 'severity', title: '级别', width: 90 },
  { dataIndex: 'audienceType', key: 'audienceType', title: '发布范围', width: 110 },
  { dataIndex: 'status', key: 'status', title: '状态', width: 100 },
  { dataIndex: 'delivery', key: 'delivery', title: '触达 / 已读', width: 180 },
  { dataIndex: 'updatedAt', key: 'updatedAt', title: '更新时间', width: 170 },
  { key: 'actions', title: '操作', width: 240, fixed: 'right' },
];

const deliveryColumns: TableColumnsType<Delivery> = [
  { dataIndex: 'recipientName', key: 'recipientName', title: '接收人', width: 120 },
  { dataIndex: 'organizationName', key: 'organizationName', title: '组织', width: 180 },
  { dataIndex: 'recipientReason', key: 'recipientReason', title: '命中原因', width: 160 },
  { dataIndex: 'read', key: 'read', title: '阅读状态', width: 100 },
  { dataIndex: 'deliveredAt', key: 'deliveredAt', title: '送达时间', width: 170 },
  { dataIndex: 'readAt', key: 'readAt', title: '阅读时间', width: 170 },
];

const publishedCount = computed(
  () => rows.value.filter((item) => item.status === 'PUBLISHED').length,
);
const deliveryCount = computed(() =>
  rows.value.reduce((sum, item) => sum + item.deliveryCount, 0),
);
const readCount = computed(() =>
  rows.value.reduce((sum, item) => sum + item.readCount, 0),
);
const readRate = computed(() =>
  deliveryCount.value ? Math.round((readCount.value / deliveryCount.value) * 100) : 0,
);
const audienceHint = computed(() => {
  const hints = {
    ALL: '无需填写，发布后覆盖全部有效用户',
    ORG: '填写组织 ID，多个用英文逗号分隔',
    ROLE: '填写角色 ID，多个用英文逗号分隔',
    USER: '填写用户 ID，多个用英文逗号分隔',
  };
  return hints[form.audienceType];
});

const statusOptions = [
  { label: '全部', value: 'ALL' },
  { label: '草稿', value: 'DRAFT' },
  { label: '已发布', value: 'PUBLISHED' },
  { label: '已撤回', value: 'WITHDRAWN' },
];
const severityOptions = [
  { label: '一般通知', value: 'INFO' },
  { label: '重要提醒', value: 'IMPORTANT' },
  { label: '紧急警示', value: 'URGENT' },
];
const audienceOptions = [
  { label: '全员', value: 'ALL' },
  { label: '组织', value: 'ORG' },
  { label: '角色', value: 'ROLE' },
  { label: '指定人员', value: 'USER' },
];

function statusLabel(value: string) {
  return { DRAFT: '草稿', PUBLISHED: '已发布', WITHDRAWN: '已撤回' }[value] || value;
}
function statusColor(value: string) {
  return { DRAFT: 'default', PUBLISHED: 'green', WITHDRAWN: 'orange' }[value] || 'default';
}
function severityLabel(value: string) {
  return { INFO: '一般', IMPORTANT: '重要', URGENT: '紧急' }[value] || value;
}
function severityColor(value: string) {
  return { INFO: 'blue', IMPORTANT: 'orange', URGENT: 'red' }[value] || 'default';
}
function audienceLabel(value: string) {
  return { ALL: '全员', ORG: '按组织', ROLE: '按角色', USER: '指定人员' }[value] || value;
}
function formatTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '—';
}
function parseIds(value: string) {
  return value
    .split(',')
    .map((item) => Number(item.trim()))
    .filter((item) => Number.isInteger(item) && item > 0);
}
function audienceIds(item: Announcement) {
  if (item.audienceType === 'ORG') return item.organizationIds;
  if (item.audienceType === 'ROLE') return item.roleIds;
  if (item.audienceType === 'USER') return item.userIds;
  return [];
}
function resetForm(item?: Announcement) {
  currentId.value = item?.id;
  form.title = item?.title || '';
  form.content = item?.content || '';
  form.severity = item?.severity || 'INFO';
  form.audienceType = item?.audienceType || 'ALL';
  form.audienceIds = item ? audienceIds(item).join(',') : '';
  form.effectiveAt = item?.effectiveAt;
  form.expiresAt = item?.expiresAt;
}
function buildInput(): AnnouncementInput {
  const ids = form.audienceType === 'ALL' ? [] : parseIds(form.audienceIds);
  return {
    audienceType: form.audienceType,
    content: form.content.trim(),
    effectiveAt: form.effectiveAt,
    expiresAt: form.expiresAt,
    organizationIds: form.audienceType === 'ORG' ? ids : [],
    roleIds: form.audienceType === 'ROLE' ? ids : [],
    severity: form.severity,
    title: form.title.trim(),
    userIds: form.audienceType === 'USER' ? ids : [],
  };
}
async function load() {
  loading.value = true;
  try {
    const result = await getAnnouncementListApi({
      page: page.value,
      pageSize: pageSize.value,
      status: status.value === 'ALL' ? undefined : status.value,
    });
    rows.value = result.items;
    total.value = result.total;
  } finally {
    loading.value = false;
  }
}
function openCreate() {
  resetForm();
  editorOpen.value = true;
}
function openEdit(record: Record<string, any>) {
  const item = record as Announcement;
  resetForm(item);
  editorOpen.value = true;
}
async function saveDraft() {
  const input = buildInput();
  if (!input.title || !input.content) {
    message.warning('请填写公告标题和正文');
    return;
  }
  if (input.audienceType !== 'ALL' && audienceIdsFromInput(input).length === 0) {
    message.warning('请填写有效的发布范围 ID');
    return;
  }
  saving.value = true;
  try {
    if (currentId.value) await updateAnnouncementApi(currentId.value, input);
    else await createAnnouncementApi(input);
    message.success('草稿已保存');
    editorOpen.value = false;
    await load();
  } finally {
    saving.value = false;
  }
}
function audienceIdsFromInput(input: AnnouncementInput) {
  return [...input.organizationIds, ...input.roleIds, ...input.userIds];
}
function publish(record: Record<string, any>) {
  const item = record as Announcement;
  Modal.confirm({
    content: '发布后接收范围将冻结并生成站内消息，确认继续？',
    okText: '确认发布',
    title: `发布“${item.title}”`,
    async onOk() {
      await publishAnnouncementApi(item.id);
      message.success('公告已发布');
      await load();
    },
  });
}
function withdraw(record: Record<string, any>) {
  const item = record as Announcement;
  Modal.confirm({
    content: '撤回后，未处理的公告消息将不可再进入业务动作。',
    okButtonProps: { danger: true },
    okText: '确认撤回',
    title: `撤回“${item.title}”`,
    async onOk() {
      await withdrawAnnouncementApi(item.id);
      message.success('公告已撤回');
      await load();
    },
  });
}
async function openAudit(record: Record<string, any>) {
  const item = record as Announcement;
  auditTitle.value = item.title;
  auditOpen.value = true;
  deliveryLoading.value = true;
  try {
    const result = await getAnnouncementDeliveriesApi({
      announcementId: item.id,
      page: 1,
      pageSize: 500,
    });
    deliveries.value = result.items;
  } finally {
    deliveryLoading.value = false;
  }
}

onMounted(load);
</script>

<template>
  <Page title="公告管理" description="统一编排安全通知，发布范围冻结、送达与阅读过程可追溯。">
    <div class="announcement-page">
      <section class="command-panel">
        <div>
          <h2>消息发布控制台</h2>
          <p>在这里完成草稿、定向发布、撤回和触达审计。</p>
        </div>
        <Button size="large" type="primary" @click="openCreate">新建公告</Button>
      </section>

      <Row :gutter="[16, 16]" class="metrics">
        <Col :lg="6" :sm="12" :xs="24"><Card><Statistic title="当前页公告" :value="rows.length" /></Card></Col>
        <Col :lg="6" :sm="12" :xs="24"><Card><Statistic title="已发布" :value="publishedCount" /></Card></Col>
        <Col :lg="6" :sm="12" :xs="24"><Card><Statistic title="累计触达" :value="deliveryCount" /></Card></Col>
        <Col :lg="6" :sm="12" :xs="24">
          <Card>
            <Statistic title="累计已读" :value="readCount" />
            <Progress :percent="readRate" size="small" :show-info="false" stroke-color="#14866d" />
          </Card>
        </Col>
      </Row>

      <Card class="list-card" :bordered="false">
        <div class="toolbar">
          <Segmented v-model:value="status" :options="statusOptions" @change="() => { page = 1; load(); }" />
          <Button @click="load">刷新</Button>
        </div>
        <Table
          :columns="columns"
          :data-source="rows"
          :loading="loading"
          :pagination="{ current: page, pageSize, total, showSizeChanger: true }"
          :row-key="(item: Announcement) => item.id"
          :scroll="{ x: 1200 }"
          @change="(pager: any) => { page = pager.current; pageSize = pager.pageSize; load(); }"
        >
          <template #emptyText><Empty description="暂无公告" /></template>
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'title'">
              <div class="title-cell">
                <strong>{{ record.title }}</strong>
                <span>{{ record.content }}</span>
              </div>
            </template>
            <template v-else-if="column.key === 'severity'">
              <Tag :color="severityColor(record.severity)">{{ severityLabel(record.severity) }}</Tag>
            </template>
            <template v-else-if="column.key === 'audienceType'">{{ audienceLabel(record.audienceType) }}</template>
            <template v-else-if="column.key === 'status'">
              <Tag :color="statusColor(record.status)">{{ statusLabel(record.status) }}</Tag>
            </template>
            <template v-else-if="column.key === 'delivery'">
              <button class="audit-link" type="button" @click="openAudit(record)">
                {{ record.deliveryCount }} 人 / {{ record.readCount }} 已读
              </button>
            </template>
            <template v-else-if="column.key === 'updatedAt'">{{ formatTime(record.updatedAt) }}</template>
            <template v-else-if="column.key === 'actions'">
              <Space>
                <Button v-if="record.status === 'DRAFT'" size="small" @click="openEdit(record)">编辑</Button>
                <Button v-if="record.status === 'DRAFT'" size="small" type="primary" @click="publish(record)">发布</Button>
                <Button v-if="record.status === 'PUBLISHED'" danger size="small" @click="withdraw(record)">撤回</Button>
                <Button size="small" @click="openAudit(record)">审计</Button>
              </Space>
            </template>
          </template>
        </Table>
      </Card>
    </div>

    <Drawer v-model:open="editorOpen" :title="currentId ? '编辑公告草稿' : '新建公告草稿'" width="620">
      <Form layout="vertical">
        <FormItem label="公告标题" required><Input v-model:value="form.title" :maxlength="100" show-count /></FormItem>
        <FormItem label="通知级别"><Select v-model:value="form.severity" :options="severityOptions" /></FormItem>
        <FormItem label="发布范围"><Select v-model:value="form.audienceType" :options="audienceOptions" /></FormItem>
        <FormItem v-if="form.audienceType !== 'ALL'" label="范围 ID" :help="audienceHint">
          <Input v-model:value="form.audienceIds" placeholder="例如：12, 18, 27" />
        </FormItem>
        <FormItem label="公告正文" required><Textarea v-model:value="form.content" :auto-size="{ minRows: 8, maxRows: 14 }" :maxlength="4000" show-count /></FormItem>
        <Row :gutter="12">
          <Col :span="12"><FormItem label="生效时间"><DatePicker v-model:value="form.effectiveAt" show-time value-format="YYYY-MM-DDTHH:mm:ss" class="full-width" /></FormItem></Col>
          <Col :span="12"><FormItem label="失效时间"><DatePicker v-model:value="form.expiresAt" show-time value-format="YYYY-MM-DDTHH:mm:ss" class="full-width" /></FormItem></Col>
        </Row>
      </Form>
      <template #footer>
        <Space><Button @click="editorOpen = false">取消</Button><Button :loading="saving" type="primary" @click="saveDraft">保存草稿</Button></Space>
      </template>
    </Drawer>

    <Drawer v-model:open="auditOpen" :title="`触达审计 · ${auditTitle}`" width="820">
      <Table
        :columns="deliveryColumns"
        :data-source="deliveries"
        :loading="deliveryLoading"
        :pagination="{ pageSize: 20 }"
        :row-key="(item: Delivery) => item.notificationId"
        :scroll="{ x: 900 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'read'"><Tag :color="record.read ? 'green' : 'default'">{{ record.read ? '已读' : '未读' }}</Tag></template>
          <template v-else-if="column.key === 'deliveredAt'">{{ formatTime(record.deliveredAt) }}</template>
          <template v-else-if="column.key === 'readAt'">{{ formatTime(record.readAt) }}</template>
        </template>
      </Table>
    </Drawer>
  </Page>
</template>

<style scoped>
.announcement-page { display: grid; gap: 16px; }
.command-panel { align-items: center; background: #ffffff; border: 1px solid #e6ebe8; border-radius: 14px; display: flex; justify-content: space-between; padding: 26px 30px; }
.command-panel h2 { color: #1f2d29; font-size: 24px; font-weight: 700; margin: 0 0 5px; }
.command-panel p { color: #71807b; margin: 0; }
.metrics :deep(.ant-col) { display: flex; }
.metrics :deep(.ant-card) { border: 1px solid #e6ebe8; box-shadow: 0 5px 18px rgb(24 76 65 / 5%); width: 100%; }
.metrics :deep(.ant-card-body) { min-height: 108px; }
.list-card { box-shadow: 0 8px 28px rgb(24 76 65 / 6%); }
.toolbar { display: flex; justify-content: space-between; margin-bottom: 18px; }
.title-cell { display: grid; gap: 4px; }
.title-cell span { color: #7d8985; max-width: 290px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.audit-link { background: none; border: 0; color: #14866d; cursor: pointer; padding: 0; }
.audit-link:hover { text-decoration: underline; }
.full-width { width: 100%; }
@media (max-width: 640px) {
  .command-panel { align-items: flex-start; flex-direction: column; gap: 18px; padding: 22px; }
  .toolbar { align-items: flex-start; flex-direction: column; gap: 12px; }
}
</style>
