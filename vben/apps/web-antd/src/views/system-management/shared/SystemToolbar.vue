<script setup lang="ts">
import { Button, Space, Tooltip, Upload } from 'ant-design-vue';

import type { SystemCrudAction } from './useSystemCrudPage';

const props = withDefaults(
  defineProps<{
    canUseRowActions: boolean;
    canUseSingleRowAction?: boolean;
    hiddenActions?: SystemCrudAction[];
    selectedCount?: number;
    showTableConfig?: boolean;
  }>(),
  {
    canUseSingleRowAction: false,
    hiddenActions: () => [],
    selectedCount: 0,
    showTableConfig: false,
  },
);

const emit = defineEmits<{
  delete: [];
  downloadData: [];
  downloadTemplate: [];
  edit: [];
  freeze: [];
  new: [];
  refresh: [];
  tableConfig: [];
  unfreeze: [];
  uploadData: [file: File];
}>();

function isVisible(action: SystemCrudAction) {
  return !props.hiddenActions.includes(action);
}

function beforeUpload(file: File) {
  emit('uploadData', file);
  return false;
}
</script>

<template>
  <div class="system-toolbar">
    <Space :size="8" wrap>
      <Tooltip title="新建">
        <Button v-if="isVisible('new')" type="primary" @click="emit('new')">
          新建
        </Button>
      </Tooltip>
      <Tooltip title="编辑选中行">
        <Button
          v-if="isVisible('edit')"
          :disabled="!canUseSingleRowAction"
          @click="emit('edit')"
        >
          编辑
        </Button>
      </Tooltip>
      <Tooltip title="删除选中行">
        <Button
          v-if="isVisible('delete')"
          danger
          :disabled="!canUseRowActions"
          @click="emit('delete')"
        >
          {{ selectedCount > 1 ? `批量删除(${selectedCount})` : '删除' }}
        </Button>
      </Tooltip>
      <Tooltip title="停用选中行">
        <Button
          v-if="isVisible('freeze')"
          :disabled="!canUseRowActions"
          @click="emit('freeze')"
        >
          {{ selectedCount > 1 ? `批量停用(${selectedCount})` : '停用' }}
        </Button>
      </Tooltip>
      <Tooltip title="启用选中行">
        <Button
          v-if="isVisible('unfreeze')"
          :disabled="!canUseRowActions"
          @click="emit('unfreeze')"
        >
          {{ selectedCount > 1 ? `批量启用(${selectedCount})` : '启用' }}
        </Button>
      </Tooltip>
      <Button
        v-if="isVisible('downloadTemplate')"
        @click="emit('downloadTemplate')"
      >
        下载模板
      </Button>
      <Button v-if="isVisible('downloadData')" @click="emit('downloadData')">
        下载数据
      </Button>
      <Upload
        v-if="isVisible('uploadData')"
        accept=".xlsx,.xls"
        :before-upload="beforeUpload"
        :show-upload-list="false"
      >
        <Button>上传数据</Button>
      </Upload>
      <Button v-if="isVisible('refresh')" @click="emit('refresh')">刷新</Button>
      <Button v-if="showTableConfig" @click="emit('tableConfig')">
        表格配置
      </Button>
    </Space>
  </div>
</template>

<style scoped>
.system-toolbar {
  padding: 12px 16px;
}
</style>
