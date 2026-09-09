<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';

import { Modal } from 'ant-design-vue';

import SystemCrudPage from '../shared/SystemCrudPage.vue';
import type { SystemFieldOption } from '../shared/useSystemCrudPage';

import type { SystemManagementApi } from '#/api/system-management/types';

import { getSystemFullOrganizationTreeApi } from '#/api/system-management/options';
import {
  createAccountApi,
  deleteAccountApi,
  freezeAccountApi,
  getAccountListApi,
  getRoleListApi,
  resetAccountPasswordApi,
  unfreezeAccountApi,
  updateAccountApi,
} from '#/api/system-management/security';

import {
  accountColumns,
  buildAccountFields,
  normalizeOrganizationTreeOptions,
  normalizeRoleOptions,
  scopeOrganizationTreeToCompany,
} from './account-management.config';

const organizationTree = ref<SystemManagementApi.OrganizationNode[]>([]);
const roleOptions = ref<SystemFieldOption[]>([]);
const selectedDataMapOrganizationId = ref<SystemManagementApi.Id>();

const fields = computed(() =>
  buildAccountFields({
    organizations: normalizeOrganizationTreeOptions(
      scopeOrganizationTreeToCompany(
        organizationTree.value,
        selectedDataMapOrganizationId.value,
      ),
    ),
    roles: roleOptions.value,
  }),
);

const columns = accountColumns;

function accountId(record: Record<string, unknown>) {
  const id = record.id;
  return typeof id === 'number' || typeof id === 'string' ? id : '';
}

function resetPassword(record: Record<string, unknown>) {
  Modal.confirm({
    content: '将生成并保存新的登录密码。',
    onOk: () => resetAccountPasswordApi(accountId(record), 'P@ssw0rd'),
    title: '重置密码',
  });
}

const rowActions = [
  { label: '重置密码', onClick: resetPassword },
];

const service = {
  createRecord: createAccountApi,
  deleteRecord: deleteAccountApi,
  fetchList: getAccountListApi,
  updateRecord: updateAccountApi,
  updateStatus: (id: SystemManagementApi.Id, status: SystemManagementApi.Status) =>
    status === 'ACTIVE' ? unfreezeAccountApi(id) : freezeAccountApi(id),
};

function handleOrganizationSelect(id?: SystemManagementApi.Id) {
  selectedDataMapOrganizationId.value = id;
}

onMounted(async () => {
  const [roles, organizations] = await Promise.all([
    getRoleListApi({ pageSize: 200 }),
    getSystemFullOrganizationTreeApi(),
  ]);
  roleOptions.value = normalizeRoleOptions(roles);
  organizationTree.value = organizations;
});
</script>

<template>
  <SystemCrudPage
    :columns="columns"
    :fields="fields"
    :hidden-actions="['downloadTemplate', 'downloadData', 'uploadData']"
    :row-actions="rowActions"
    :service="service"
    :show-cascade="false"
    show-data-map
    title="账户"
    @organization-select="handleOrganizationSelect"
  />
</template>
