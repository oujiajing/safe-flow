<script setup lang="ts">
import SystemCrudPage from '../shared/SystemCrudPage.vue';

import type { SystemManagementApi } from '#/api/system-management/types';

import {
  createDepartmentApi,
  deleteDepartmentApi,
  getDepartmentListApi,
  updateDepartmentApi,
  updateDepartmentStatusApi,
} from '#/api/system-management/master-data';
import {
  downloadSystemDataApi,
  downloadSystemTemplateApi,
  uploadSystemDataApi,
} from '#/api/system-management/import-export';
import {
  getMasterDataColumns,
  getMasterDataFields,
} from '../shared/masterDataTemplates';

const statusOptions = [
  { label: '启用', value: 'ACTIVE' },
  { label: '草稿', value: 'DRAFT' },
  { label: '停用', value: 'INACTIVE' },
];

const fields = getMasterDataFields('department', statusOptions);
const columns = getMasterDataColumns('department');

const service = {
  createRecord: createDepartmentApi,
  deleteRecord: deleteDepartmentApi,
  downloadData: (params: SystemManagementApi.ListParams) =>
    downloadSystemDataApi('department', params),
  downloadTemplate: () => downloadSystemTemplateApi('department'),
  fetchList: getDepartmentListApi,
  filterParamMap: {
    companyId: 'companyOrgId',
    departmentId: null,
    teamId: null,
  },
  updateRecord: updateDepartmentApi,
  updateStatus: updateDepartmentStatusApi,
  uploadData: (file: File) => uploadSystemDataApi('department', file),
};
</script>

<template>
  <SystemCrudPage
    :columns="columns"
    :department-hierarchy-mode="true"
    :fields="fields"
    :service="service"
    :show-cascade="false"
    :show-data-map="true"
    table-config-module="department"
    table-config-title="部门管理"
    title="部门"
  />
</template>
