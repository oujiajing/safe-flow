<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';

import { Button, Tree, message } from 'ant-design-vue';

import SystemCrudPage from '../shared/SystemCrudPage.vue';

import {
  createRoleApi,
  deleteRoleApi,
  getMenuTreeApi,
  getRoleMenusApi,
  getRoleListApi,
  updateRoleApi,
  updateRoleMenusApi,
} from '#/api/system-management/security';
import type { SystemManagementApi } from '#/api/system-management/types';
import { resetRoutes } from '#/router';
import { refreshAccessRoutes } from '#/router/guard';

const checkedPermissionCodes = ref<string[]>([]);
const menuTree = ref<SystemManagementApi.Menu[]>([]);
const router = useRouter();
const selectedRoleId = ref<SystemManagementApi.Id>();

const fields = [
  { label: '角色编码', required: true, valueKey: 'roleCode' },
  { label: '角色名称', required: true, valueKey: 'roleName' },
  { label: '数据范围', valueKey: 'dataScope' },
  { component: 'textarea' as const, label: '描述', valueKey: 'description' },
];

const columns = [
  { dataIndex: 'roleCode', fixed: 'left' as const, title: '角色编码', width: 140 },
  { dataIndex: 'roleName', title: '角色名称', width: 140 },
  { dataIndex: 'dataScope', title: '数据范围', width: 140 },
  { dataIndex: 'description', title: '描述', width: 220 },
];

const service = {
  createRecord: createRoleApi,
  deleteRecord: deleteRoleApi,
  fetchList: getRoleListApi,
  updateRecord: updateRoleApi,
};

function normalizeTree(nodes: SystemManagementApi.Menu[]): any[] {
  return nodes.map((node) => ({
    children: normalizeTree(node.children ?? []),
    key: node.id,
    title: node.title || node.name || node.code,
  }));
}

function toId(record: Record<string, unknown>) {
  const id = record.id;
  return typeof id === 'number' || typeof id === 'string' ? id : undefined;
}

async function selectRole(record: Record<string, unknown>) {
  selectedRoleId.value = toId(record);
  if (!selectedRoleId.value) {
    checkedPermissionCodes.value = [];
    return;
  }

  const grantedMenuIds = await getRoleMenusApi(selectedRoleId.value);
  checkedPermissionCodes.value = grantedMenuIds.map(String);
}

async function saveRoleMenus() {
  if (!selectedRoleId.value) return;
  await updateRoleMenusApi(selectedRoleId.value, checkedPermissionCodes.value);
  await refreshAccessRoutes(router, resetRoutes);
  message.success('保存授权成功');
}

onMounted(async () => {
  const result = await getMenuTreeApi();
  menuTree.value = Array.isArray(result) ? result : [];
});
</script>

<template>
  <div class="system-role-page">
    <SystemCrudPage
      :columns="columns"
      :fields="fields"
      :hidden-actions="['freeze', 'unfreeze', 'downloadTemplate', 'downloadData', 'uploadData']"
      :service="service"
      :show-cascade="false"
      :show-status-filter="false"
      title="角色"
      @row-select="selectRole"
    />
    <section class="system-role-permissions">
      <div class="system-role-permissions__toolbar">
        <span>菜单权限</span>
        <Button size="small" type="primary" @click="saveRoleMenus">保存授权</Button>
      </div>
      <Tree
        v-model:checkedKeys="checkedPermissionCodes"
        checkable
        :tree-data="normalizeTree(menuTree)"
      />
    </section>
  </div>
</template>

<style scoped>
.system-role-page {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 12px;
  height: 100%;
}

.system-role-permissions {
  background: hsl(var(--background));
  border: 1px solid hsl(var(--border));
  border-radius: 6px;
  margin: 12px 12px 12px 0;
  overflow: auto;
}

.system-role-permissions__toolbar {
  align-items: center;
  border-bottom: 1px solid hsl(var(--border));
  display: flex;
  height: 48px;
  justify-content: space-between;
  padding: 0 12px;
}
</style>
