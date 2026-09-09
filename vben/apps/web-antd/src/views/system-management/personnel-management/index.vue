<script setup lang="ts">
import SystemCrudPage from '../shared/SystemCrudPage.vue';

import type { SystemManagementApi } from '#/api/system-management/types';

import {
  createPersonnelApi,
  deletePersonnelApi,
  getPersonnelListApi,
  updatePersonnelApi,
  updatePersonnelStatusApi,
} from '#/api/system-management/master-data';
import {
  downloadSystemDataApi,
  downloadSystemTemplateApi,
  uploadSystemDataApi,
} from '#/api/system-management/import-export';
import {
  getMasterDataColumns,
  getPersonnelManagementFields,
} from '../shared/masterDataTemplates';

const statusOptions = [
  { label: '启用', value: 'ACTIVE' },
  { label: '草稿', value: 'DRAFT' },
  { label: '停用', value: 'INACTIVE' },
];

const fields = getPersonnelManagementFields(statusOptions);
const columns = getMasterDataColumns('personnel');

const service = {
  createRecord: createPersonnelApi,
  deleteRecord: deletePersonnelApi,
  downloadData: (params: SystemManagementApi.ListParams) =>
    downloadSystemDataApi('personnel', params),
  downloadTemplate: () => downloadSystemTemplateApi('personnel'),
  fetchList: getPersonnelListApi,
  filterParamMap: {
    companyId: 'companyOrgId',
    departmentId: 'departmentOrgId',
    teamId: 'teamOrgId',
  },
  updateRecord: updatePersonnelApi,
  updateStatus: updatePersonnelStatusApi,
  uploadData: (file: File) => uploadSystemDataApi('personnel', file),
};
</script>

<template>
  <SystemCrudPage
    :columns="columns"
    :fields="fields"
    :service="service"
    :personnel-hierarchy-mode="true"
    :show-cascade="false"
    :show-data-map="true"
    table-config-module="personnel"
    table-config-title="人员管理"
    title="人员"
  />
</template>
