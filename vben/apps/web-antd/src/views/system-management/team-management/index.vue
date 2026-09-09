<script setup lang="ts">
import SystemCrudPage from '../shared/SystemCrudPage.vue';

import type { SystemManagementApi } from '#/api/system-management/types';

import {
  createTeamApi,
  deleteTeamApi,
  getTeamListApi,
  updateTeamApi,
  updateTeamStatusApi,
} from '#/api/system-management/master-data';
import {
  downloadSystemDataApi,
  downloadSystemTemplateApi,
  uploadSystemDataApi,
} from '#/api/system-management/import-export';
import {
  getMasterDataColumns,
  getTeamManagementFields,
} from '../shared/masterDataTemplates';

const statusOptions = [
  { label: '启用', value: 'ACTIVE' },
  { label: '草稿', value: 'DRAFT' },
  { label: '停用', value: 'INACTIVE' },
];

const fields = getTeamManagementFields(statusOptions);
const columns = getMasterDataColumns('team');

const service = {
  createRecord: createTeamApi,
  deleteRecord: deleteTeamApi,
  downloadData: (params: SystemManagementApi.ListParams) =>
    downloadSystemDataApi('team', params),
  downloadTemplate: () => downloadSystemTemplateApi('team'),
  fetchList: getTeamListApi,
  filterParamMap: {
    companyId: 'companyOrgId',
    departmentId: 'workshopOrgId',
    teamId: null,
  },
  updateRecord: updateTeamApi,
  updateStatus: updateTeamStatusApi,
  uploadData: (file: File) => uploadSystemDataApi('team', file),
};
</script>

<template>
  <SystemCrudPage
    :columns="columns"
    :fields="fields"
    :service="service"
    :show-cascade="false"
    :show-data-map="true"
    :team-hierarchy-mode="true"
    table-config-module="team"
    table-config-title="班组管理"
    title="班组"
  />
</template>
