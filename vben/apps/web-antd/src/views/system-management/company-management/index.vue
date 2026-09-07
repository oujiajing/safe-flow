<script setup lang="ts">
import SystemCrudPage from '../shared/SystemCrudPage.vue';

import type { SystemManagementApi } from '#/api/system-management/types';

import {
  createCompanyApi,
  deleteCompanyApi,
  getCompanyListApi,
  updateCompanyApi,
  updateCompanyStatusApi,
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

const fields = getMasterDataFields('company', statusOptions);
const columns = getMasterDataColumns('company');

const service = {
  createRecord: createCompanyApi,
  defaultListParams: { pageSize: 100 },
  deleteRecord: deleteCompanyApi,
  downloadData: (params: SystemManagementApi.ListParams) =>
    downloadSystemDataApi('company', params),
  downloadTemplate: () => downloadSystemTemplateApi('company'),
  fetchList: getCompanyListApi,
  updateRecord: updateCompanyApi,
  updateStatus: updateCompanyStatusApi,
  uploadData: (file: File) => uploadSystemDataApi('company', file),
};
</script>

<template>
  <SystemCrudPage
    :columns="columns"
    :company-hierarchy-mode="true"
    :fields="fields"
    :service="service"
    :show-data-map="true"
    :show-cascade="false"
    :table-page-size="100"
    table-config-module="company"
    table-config-title="公司管理"
    title="公司"
  />
</template>
