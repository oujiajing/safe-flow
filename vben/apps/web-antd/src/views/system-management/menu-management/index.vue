<script setup lang="ts">
import SystemCrudPage from '../shared/SystemCrudPage.vue';

import {
  createMenuApi,
  deleteMenuApi,
  getMenuListApi,
  updateMenuApi,
} from '#/api/system-management/security';

const visibleOptions = [
  { label: '显示', value: 'true' },
  { label: '隐藏', value: 'false' },
];

const fields = [
  { label: '父菜单ID', type: 'number' as const, valueKey: 'parentId' },
  { label: '菜单编码', required: true, valueKey: 'menuCode' },
  { label: '标题', required: true, valueKey: 'title' },
  { label: '路由路径', required: true, valueKey: 'routePath' },
  { label: '组件', required: true, valueKey: 'component' },
  { label: '图标', valueKey: 'icon' },
  { label: '权限码', required: true, valueKey: 'permissionCode' },
  { label: '排序', type: 'number' as const, valueKey: 'sortOrder' },
  {
    component: 'select' as const,
    label: '显示状态',
    options: visibleOptions,
    required: true,
    valueKey: 'visible',
  },
];

const columns = [
  { dataIndex: 'menuCode', title: '菜单编码', width: 180 },
  { dataIndex: 'title', fixed: 'left' as const, title: '标题', width: 160 },
  { dataIndex: 'routePath', title: '路由路径', width: 220 },
  { dataIndex: 'component', title: '组件', width: 240 },
  { dataIndex: 'icon', title: '图标', width: 140 },
  { dataIndex: 'permissionCode', title: '权限码', width: 200 },
  { dataIndex: 'sortOrder', title: '排序', width: 90 },
  { dataIndex: 'visible', title: '显示状态', width: 110 },
];

const service = {
  createRecord: createMenuApi,
  deleteRecord: deleteMenuApi,
  fetchList: getMenuListApi,
  updateRecord: updateMenuApi,
};
</script>

<template>
  <SystemCrudPage
    :columns="columns"
    :fields="fields"
    :hidden-actions="['freeze', 'unfreeze', 'downloadTemplate', 'downloadData', 'uploadData']"
    :service="service"
    :show-cascade="false"
    title="菜单"
  />
</template>
