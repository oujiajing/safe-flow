<script setup lang="ts">
import type { TableColumnsType } from 'ant-design-vue';

import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';

import { Page } from '@vben/common-ui';
import { IconifyIcon } from '@vben/icons';
import { useAccessStore, useUserStore } from '@vben/stores';

import { Button, Empty, message, Modal, Table, Tag } from 'ant-design-vue';

import {
  getActiveContentProfileByOrgApi,
  type PinganContentProfileApi,
} from '#/api/pingan/content-profile';
import {
  getPinganCompanyOrgTreeApi,
  getPinganOrgTreeApi,
} from '#/api/pingan/pre-shift-meeting';
import { getThreeCheckRecordsApi } from '#/api/pingan/three-check-record';
import {
  getCompanyListApi,
  getDepartmentListApi,
  getPersonnelListApi,
  getTeamListApi,
} from '#/api/system-management/master-data';
import type { SystemManagementApi } from '#/api/system-management/types';
import { applyOrganizationFilterFromDataMapSelection } from '#/views/pingan/pre-shift-meeting/pre-shift-meeting.data';
import type {
  OrganizationFilterState,
  OrganizationId,
  OrganizationNode,
} from '#/views/pingan/pre-shift-meeting/pre-shift-meeting.data';
import { canViewThreeCheckRecord } from '#/views/pingan/pre-shift-meeting/pre-shift-meeting.view-state';
import DataMapPanel from '#/views/pingan/shared/DataMapPanel.vue';
import { canShowPinganDataMap } from '#/views/pingan/shared/data-map-permission';
import type {
  RelationExpandedChildIds,
  RelationGraphNode,
} from './organization.view-state';
import type { OrganizationProfile } from './organization.profiles';
import { findOrganizationProfile } from './organization.profiles';
import {
  countNodesByType,
  findOrganizationPathById,
  organizationType,
  resolveOrganizationRelationship,
} from './organization.view-state';

type BusinessStatusTone = 'danger' | 'neutral' | 'primary' | 'success' | 'warning';

interface BusinessStatusItem {
  icon: string;
  label: string;
  tone: BusinessStatusTone;
  value: string;
}

interface BusinessEntry {
  icon: string;
  label: string;
  path: string;
  tone: BusinessStatusTone;
}

interface MemberSnapshot {
  id: SystemManagementApi.Id;
  mobile: string;
  name: string;
  post: string;
  role: string;
  status: SystemManagementApi.Status | string;
  teamName: string;
}

interface MetricItem {
  icon: string;
  label: string;
  suffix: string;
  tone: BusinessStatusTone;
  value: number;
}

interface SelectedOrganizationProfile extends OrganizationProfile {
  imageAttachment?: PinganContentProfileApi.Attachment;
}

const router = useRouter();
const accessStore = useAccessStore();
const userStore = useUserStore();

const organizationNodes = ref<OrganizationNode[]>([]);
const organizationOptionNodes = ref<OrganizationNode[]>([]);
const selectedOrganizationKeys = ref<string[]>([]);
const shouldShowDataMap = computed(() =>
  canShowPinganDataMap(userStore.userInfo?.roles),
);
const accessCodes = computed(() => accessStore.accessCodes ?? []);
const selectedNode = ref<OrganizationNode>();
const remoteOrganizationProfile =
  ref<PinganContentProfileApi.ContentProfile | null>(null);
const relationshipRootId = ref<OrganizationId | string>();
const relationExpandedChildIds = ref<RelationExpandedChildIds>();
const profileModalOpen = ref(false);
const loading = ref(false);
const members = ref<MemberSnapshot[]>([]);
const teamRows = ref<SystemManagementApi.Team[]>([]);
const minRelationZoom = 0.8;
const maxRelationZoom = 1.4;
const relationZoomStep = 0.1;
const relationZoom = ref(1);

const overview = reactive({
  activePersonnel: 0,
  companies: 0,
  departments: 0,
  hazardPending: 0,
  midShiftTotal: 0,
  personnelTotal: 0,
  postShiftInspectionTotal: 0,
  preShiftInspectionTotal: 0,
  preShiftTotal: 0,
  teams: 0,
  todayDispatch: 0,
});

const organizationFilter = reactive<OrganizationFilterState>({
  company: '',
  companyId: undefined,
  department: '',
  departmentId: undefined,
  team: '',
  teamId: undefined,
});

const businessEntries: BusinessEntry[] = [
  {
    icon: 'lucide:clipboard-check',
    label: '查看一班三查',
    path: '/pingan/three-checks/pre-shift-meeting',
    tone: 'primary',
  },
  {
    icon: 'lucide:triangle-alert',
    label: '查看隐患',
    path: '/pingan/hazard-inspection/rectification',
    tone: 'warning',
  },
  {
    icon: 'lucide:shield-alert',
    label: '查看风险点',
    path: '/pingan/risk-management/risk-level-control',
    tone: 'danger',
  },
  {
    icon: 'lucide:book-open-check',
    label: '查看培训记录',
    path: '/pingan/training/safety-learning',
    tone: 'success',
  },
];

const memberColumns: TableColumnsType<MemberSnapshot> = [
  { dataIndex: 'name', fixed: 'left', title: '姓名', width: 120 },
  { dataIndex: 'post', title: '岗位', width: 130 },
  { dataIndex: 'role', title: '角色', width: 110 },
  { dataIndex: 'mobile', title: '手机', width: 140 },
  { dataIndex: 'status', title: '今日状态', width: 120 },
  { dataIndex: 'actions', fixed: 'right', title: '业务入口', width: 190 },
];

const relationship = computed(() =>
  resolveOrganizationRelationship(
    organizationOptionNodes.value,
    relationshipRootId.value ?? selectedNode.value?.id ?? selectedNode.value?.key,
    relationExpandedChildIds.value,
  ),
);

const selectedTitle = computed(() => {
  if (organizationFilter.team) return organizationFilter.team;
  if (organizationFilter.department) return organizationFilter.department;
  if (organizationFilter.company) return organizationFilter.company;
  return selectedNode.value?.title || '全部组织';
});

const selectedKindLabel = computed(() => {
  const type = organizationType(selectedNode.value);
  if (type === 'GROUP') return '集团';
  if (type === 'TEAM') return '班组';
  if (type === 'DEPARTMENT') return '部门';
  if (type === 'COMPANY') return '公司';
  return '组织';
});

const metrics = computed<MetricItem[]>(() => [
  {
    icon: 'lucide:building-2',
    label: '公司',
    suffix: '家',
    tone: 'primary',
    value: overview.companies,
  },
  {
    icon: 'lucide:folder-tree',
    label: '部门',
    suffix: '个',
    tone: 'success',
    value: overview.departments,
  },
  {
    icon: 'lucide:users-round',
    label: '班组',
    suffix: '个',
    tone: 'warning',
    value: overview.teams,
  },
  {
    icon: 'lucide:user-check',
    label: '在岗人员',
    suffix: '人',
    tone: 'primary',
    value: overview.activePersonnel,
  },
  {
    icon: 'lucide:calendar-check',
    label: '今日派班',
    suffix: '个',
    tone: 'neutral',
    value: overview.todayDispatch,
  },
]);

const isGroupSelected = computed(() => organizationType(selectedNode.value) === 'GROUP');

const lowerLevelStatuses = computed<BusinessStatusItem[]>(() => [
  {
    icon: 'lucide:clipboard-list',
    label: '班前检查',
    tone: overview.preShiftInspectionTotal > 0 ? 'success' : 'neutral',
    value: overview.preShiftInspectionTotal > 0 ? '已检查' : '未开始',
  },
  {
    icon: 'lucide:scan-line',
    label: '班中检查',
    tone: overview.midShiftTotal > 0 ? 'warning' : 'neutral',
    value: overview.midShiftTotal > 0 ? '进行中' : '未开始',
  },
  {
    icon: 'lucide:clipboard-check',
    label: '班后检查',
    tone: overview.postShiftInspectionTotal > 0 ? 'success' : 'neutral',
    value: overview.postShiftInspectionTotal > 0 ? '已检查' : '未开始',
  },
]);

const businessStatuses = computed<BusinessStatusItem[]>(() => {
  const baseStatuses: BusinessStatusItem[] = [
    {
      icon: 'lucide:badge-check',
      label: '今日班前会',
      tone: overview.preShiftTotal > 0 ? 'success' : 'neutral',
      value: overview.preShiftTotal > 0 ? '已完成' : '未开始',
    },
    {
      icon: 'lucide:shield-x',
      label: '隐患待整改',
      tone: overview.hazardPending > 0 ? 'danger' : 'success',
      value: `${overview.hazardPending}`,
    },
  ];

  if (isGroupSelected.value) {
    return [];
  }

  return [baseStatuses[0]!, ...lowerLevelStatuses.value, baseStatuses[1]!];
});

const relationGraph = computed(() => relationship.value.graphView);

const relationGraphFooterHint = computed(
  () => relationGraph.value?.footerHint ?? '当前视图仅展示公司层级',
);

const relationGraphLayoutStyle = computed<Record<string, string>>(() => ({
  '--relation-group-count': `${Math.max(relationGraph.value?.groups.length ?? 1, 1)}`,
  transform: `scale(${relationZoom.value})`,
}));

const relationExpandableNodeIds = computed(() =>
  (relationGraph.value?.groups ?? [])
    .filter((group) => group.parent.collapsedChildCount > 0)
    .map((group) => group.parent.node.id ?? group.parent.node.key),
);

const selectedOrganizationProfile = computed<
  SelectedOrganizationProfile | undefined
>(() => {
  const fallbackProfile = findOrganizationProfile(selectedNode.value);
  const remoteProfile = remoteOrganizationProfile.value;

  if (!remoteProfile) {
    return fallbackProfile;
  }

  return {
    description: remoteProfile.description || fallbackProfile?.description || '',
    imageAlt:
      remoteProfile.imageAttachment?.originalName ||
      fallbackProfile?.imageAlt ||
      `${remoteProfile.title}简介图`,
    imageAttachment: remoteProfile.imageAttachment,
    imageUrl:
      remoteProfile.imageAttachment?.url ||
      fallbackProfile?.imageUrl ||
      '/images/organization/guangsheng-group.svg',
    subtitle: remoteProfile.subtitle || fallbackProfile?.subtitle || '',
    title:
      remoteProfile.title ||
      fallbackProfile?.title ||
      selectedNode.value?.title ||
      '组织简介',
  };
});

const profileButtonLabel = computed(() => '集团/公司简介');

const selectedTeamInfo = computed(() => {
  const team = teamRows.value[0];
  const isTeamSelected = organizationType(selectedNode.value) === 'TEAM';
  const leader =
    isTeamSelected
      ? team?.leader ||
        team?.leaderUsername ||
        members.value.find((item) => item.role === '班组长')?.name ||
        '-'
      : '-';
  const safetyOfficer =
    isTeamSelected
      ? team?.safetyOfficer ||
        team?.safetyOfficerUsername ||
        members.value.find((item) => item.role === '安全员')?.name ||
        '-'
      : '-';
  return {
    department:
      organizationFilter.department ||
      (isTeamSelected ? team?.workshopOrgName : selectedKindLabel.value) ||
      '-',
    leader,
    safetyOfficer,
    title: isTeamSelected
      ? organizationFilter.team || team?.name || selectedTitle.value
      : selectedTitle.value,
  };
});

onMounted(async () => {
  await loadOrganizations();
  await loadOverview();
});

async function loadOrganizations() {
  try {
    const [companyTree, fullTree] = await Promise.all([
      getPinganCompanyOrgTreeApi(),
      getPinganOrgTreeApi(),
    ]);
    organizationNodes.value = companyTree;
    organizationOptionNodes.value = fullTree;
    const initial = findInitialOrganizationNode(fullTree, userStore.userInfo?.orgId);
    if (initial) {
      selectedNode.value = initial;
      selectedOrganizationKeys.value = [initial.key];
      syncRelationshipScope(initial);
      applyOrganizationFilterFromDataMapSelection(
        organizationFilter,
        companyTree,
        fullTree,
        initial.key,
      );
      void loadOrganizationProfile(initial);
    }
  } catch {
    organizationNodes.value = [];
    organizationOptionNodes.value = [];
    remoteOrganizationProfile.value = null;
    resetOrganizationFilter();
    message.error('组织架构数据加载失败');
  }
}

async function loadOverview() {
  loading.value = true;
  try {
    const today = formatLocalDate(new Date());
    const systemParams = systemListParams();
    const recordParams = threeCheckListParams();
    const [
      companies,
      departments,
      teams,
      activePersonnel,
      memberSnapshot,
      selectedTeams,
      teamDispatch,
      curtainWallDispatch,
      preShiftMeeting,
      preShiftInspection,
      midShiftInspection,
      postShiftInspection,
      hazardRectification,
    ] = await Promise.all([
      getCompanyListApi({ page: 1, pageSize: 1, status: 'all' }),
      getDepartmentListApi({
        ...systemParams.department,
        page: 1,
        pageSize: 1,
        status: 'all',
      }),
      getTeamListApi({
        ...systemParams.team,
        page: 1,
        pageSize: 1,
        status: 'all',
      }),
      getPersonnelListApi({
        ...systemParams.personnel,
        page: 1,
        pageSize: 1,
        status: 'ACTIVE',
      }),
      getPersonnelListApi({
        ...systemParams.personnel,
        page: 1,
        pageSize: 6,
        status: 'all',
      }),
      getTeamListApi({
        ...systemParams.team,
        keyword: organizationFilter.team || undefined,
        page: 1,
        pageSize: 6,
        status: 'all',
      }),
      loadBusinessOverviewPage('PinganTeamDispatch', 'team-dispatch', {
        ...recordParams,
        dateEnd: today,
        dateStart: today,
        page: 1,
        pageSize: 1,
      }),
      loadBusinessOverviewPage(
        'PinganCurtainWallTeamDispatch',
        'curtain-wall-team-dispatch',
        {
          ...recordParams,
          dateEnd: today,
          dateStart: today,
          page: 1,
          pageSize: 1,
        },
      ),
      loadBusinessOverviewPage('PreShiftMeeting', 'pre-shift-meeting', {
        ...recordParams,
        dateEnd: today,
        dateStart: today,
        page: 1,
        pageSize: 1,
      }),
      loadBusinessOverviewPage('PinganPreShiftInspection', 'pre-shift-inspection', {
        ...recordParams,
        dateEnd: today,
        dateStart: today,
        page: 1,
        pageSize: 1,
      }),
      loadBusinessOverviewPage('PinganMidShiftInspection', 'mid-shift-inspection', {
        ...recordParams,
        dateEnd: today,
        dateStart: today,
        page: 1,
        pageSize: 1,
      }),
      loadBusinessOverviewPage('PinganPostShiftInspection', 'post-shift-inspection', {
        ...recordParams,
        dateEnd: today,
        dateStart: today,
        page: 1,
        pageSize: 1,
      }),
      loadBusinessOverviewPage(
        'PinganHazardRectification',
        'hazard-rectification',
        {
          ...recordParams,
          page: 1,
          pageSize: 1,
          status: 'PENDING_RECTIFICATION',
        },
      ),
    ]);

    overview.companies = filteredTreeCount('COMPANY', normalizeTotal(companies.total));
    overview.departments = filteredTreeCount(
      'DEPARTMENT',
      normalizeTotal(departments.total),
    );
    overview.teams = filteredTreeCount('TEAM', normalizeTotal(teams.total));
    overview.activePersonnel = normalizeTotal(activePersonnel.total);
    overview.personnelTotal = normalizeTotal(memberSnapshot.total);
    overview.todayDispatch =
      normalizeTotal(teamDispatch.total) + normalizeTotal(curtainWallDispatch.total);
    overview.preShiftTotal = normalizeTotal(preShiftMeeting.total);
    overview.preShiftInspectionTotal = normalizeTotal(preShiftInspection.total);
    overview.midShiftTotal = normalizeTotal(midShiftInspection.total);
    overview.postShiftInspectionTotal = normalizeTotal(postShiftInspection.total);
    overview.hazardPending = normalizeTotal(hazardRectification.total);
    members.value = memberSnapshot.items.map(normalizeMemberSnapshot);
    teamRows.value = selectedTeams.items;
  } catch {
    message.error('组织架构概览加载失败');
  } finally {
    loading.value = false;
  }
}

function emptyBusinessOverviewPage() {
  return { items: [], total: 0 };
}

function loadBusinessOverviewPage(
  routeName: string,
  moduleKey: string,
  params: Parameters<typeof getThreeCheckRecordsApi>[1],
) {
  if (!canViewThreeCheckRecord(routeName, accessCodes.value)) {
    return Promise.resolve(emptyBusinessOverviewPage());
  }
  return getThreeCheckRecordsApi(moduleKey, params);
}

function handleDataMapSelect(node?: OrganizationNode) {
  selectOrganizationNode(node);
}

function selectRelationshipNode(node?: OrganizationNode) {
  if (!node) return;

  if (
    relationGraph.value?.mode === 'GROUP' &&
    ['COMPANY', 'SCHOOL'].includes(organizationType(node))
  ) {
    expandGroupCompanyNode(node);
    return;
  }

  if (organizationType(node) === 'COMPANY') {
    selectOrganizationNode(node);
    return;
  }

  if (['DEPARTMENT', 'TEAM'].includes(organizationType(node))) {
    selectOrganizationNode(node, {
      selectionSource: relationshipSelectionSource(node),
      syncRelationshipScope: false,
    });
    return;
  }

  selectOrganizationNode(node);
}

function expandGroupCompanyNode(node: OrganizationNode) {
  relationExpandedChildIds.value = [node.id ?? node.key];
  selectOrganizationNode(node, {
    selectionSource: organizationOptionNodes.value,
    syncRelationshipScope: false,
  });
}

function expandRelationGraph() {
  relationExpandedChildIds.value = [...relationExpandableNodeIds.value];
}

function collapseRelationGraph() {
  relationExpandedChildIds.value = [];
}

function selectOrganizationNode(
  node?: OrganizationNode,
  options: {
    selectionSource?: OrganizationNode[];
    syncRelationshipScope?: boolean;
  } = {},
) {
  selectedNode.value = node;
  selectedOrganizationKeys.value = node ? [node.key] : [];
  if (options.syncRelationshipScope !== false) {
    syncRelationshipScope(node);
  }
  if (!node) {
    remoteOrganizationProfile.value = null;
    profileModalOpen.value = false;
    resetOrganizationFilter();
    void loadOverview();
    return;
  }
  applyOrganizationFilterFromDataMapSelection(
    organizationFilter,
    options.selectionSource ?? organizationNodes.value,
    organizationOptionNodes.value,
    node.key,
  );
  void loadOrganizationProfile(node);
  void loadOverview();
}

async function loadOrganizationProfile(node?: OrganizationNode) {
  const type = organizationType(node);
  if (!node?.id || !['COMPANY', 'GROUP', 'SCHOOL'].includes(type)) {
    remoteOrganizationProfile.value = null;
    return;
  }

  try {
    const profile = await getActiveContentProfileByOrgApi(node.id);
    if (isSameOrganizationNode(selectedNode.value, node)) {
      remoteOrganizationProfile.value = profile;
    }
  } catch {
    if (isSameOrganizationNode(selectedNode.value, node)) {
      remoteOrganizationProfile.value = null;
    }
  }
}

function relationshipSelectionSource(node?: OrganizationNode) {
  return ['DEPARTMENT', 'TEAM'].includes(organizationType(node))
    ? organizationOptionNodes.value
    : organizationNodes.value;
}

function relationNodeBadgeLabel(node: RelationGraphNode) {
  if (node.collapsedChildCount <= 0) return node.kindLabel;
  if (node.kindLabel === '班组') return `${node.collapsedChildCount}个班组`;
  return `${node.kindLabel} ${node.collapsedChildCount}个下级`;
}

function fitRelationshipGraph() {
  relationZoom.value = 1;
}

function zoomRelationshipGraph(direction: -1 | 1) {
  const nextZoom = relationZoom.value + direction * relationZoomStep;
  relationZoom.value = Math.min(
    maxRelationZoom,
    Math.max(minRelationZoom, Number(nextZoom.toFixed(2))),
  );
}

function syncRelationshipScope(node?: OrganizationNode) {
  if (!node) {
    relationshipRootId.value = undefined;
    relationExpandedChildIds.value = undefined;
    return;
  }

  relationshipRootId.value = node.id ?? node.key;
  relationExpandedChildIds.value = undefined;
}

function isSameOrganizationNode(left?: OrganizationNode, right?: OrganizationNode) {
  return Boolean(
    left &&
      right &&
      (String(left.id) === String(right.id) || left.key === right.key),
  );
}

function isSelectedRelationshipNode(node?: OrganizationNode) {
  return isSameOrganizationNode(selectedNode.value, node);
}

function goTo(path: string) {
  void router.push(path);
}

function openProfileModal() {
  if (!selectedOrganizationProfile.value) return;
  profileModalOpen.value = true;
}

function findInitialOrganizationNode(
  nodes: OrganizationNode[],
  orgId: OrganizationId | string | undefined,
) {
  if (orgId === undefined || orgId === null || orgId === '') {
    return nodes[0];
  }
  const match = findOrganizationPathById(nodes, orgId);
  return match?.node ?? nodes[0];
}

function resetOrganizationFilter() {
  organizationFilter.company = '';
  organizationFilter.companyId = undefined;
  organizationFilter.department = '';
  organizationFilter.departmentId = undefined;
  organizationFilter.team = '';
  organizationFilter.teamId = undefined;
}

function systemListParams() {
  return {
    department: {
      ...(organizationFilter.companyId
        ? { companyOrgId: organizationFilter.companyId }
        : {}),
    },
    personnel: {
      ...(organizationFilter.companyId
        ? { companyOrgId: organizationFilter.companyId }
        : {}),
      ...(organizationFilter.departmentId
        ? { departmentOrgId: organizationFilter.departmentId }
        : {}),
      ...(organizationFilter.teamId ? { teamOrgId: organizationFilter.teamId } : {}),
    },
    team: {
      ...(organizationFilter.companyId
        ? { companyOrgId: organizationFilter.companyId }
        : {}),
      ...(organizationFilter.departmentId
        ? { workshopOrgId: organizationFilter.departmentId }
        : {}),
    },
  };
}

function threeCheckListParams() {
  return {
    ...(selectedNode.value?.id ? { organizationId: selectedNode.value.id } : {}),
    ...(organizationFilter.companyId ? { companyId: organizationFilter.companyId } : {}),
    ...(organizationFilter.departmentId
      ? { departmentId: organizationFilter.departmentId }
      : {}),
    ...(organizationFilter.teamId ? { teamId: organizationFilter.teamId } : {}),
  };
}

function normalizeMemberSnapshot(
  item: SystemManagementApi.Personnel,
): MemberSnapshot {
  const roles = Array.isArray(item.roles) ? item.roles.filter(Boolean) : [];
  return {
    id: item.id,
    mobile: maskMobile(item.mobile),
    name: item.name || item.applicantName || item.username || '-',
    post: item.post || item.positionName || item.employeeType || '-',
    role: roleLabel(item, roles),
    status: item.status || 'ACTIVE',
    teamName: item.teamName || organizationFilter.team || '-',
  };
}

function roleLabel(item: SystemManagementApi.Personnel, roles: string[]) {
  const roleText = [item.systemRoleCode, ...roles].join(' ');
  if (/leader|班组长/i.test(roleText)) return '班组长';
  if (/safe|安全/i.test(roleText)) return '安全员';
  if (/骨干/i.test(roleText)) return '骨干员工';
  return roles[0] || '普通员工';
}

function maskMobile(value?: string) {
  return value ? value.replace(/^(\d{3})\d{4}(\d+)/, '$1****$2') : '-';
}

function normalizeTotal(value: number | string | undefined) {
  const total = Number(value ?? 0);
  return Number.isFinite(total) ? total : 0;
}

function formatLocalDate(date: Date) {
  const year = date.getFullYear();
  const month = `${date.getMonth() + 1}`.padStart(2, '0');
  const day = `${date.getDate()}`.padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function selectedSubtree() {
  if (!selectedNode.value?.id) return organizationOptionNodes.value;
  return findOrganizationPathById(organizationOptionNodes.value, selectedNode.value.id)
    ?.node
    ? [
        findOrganizationPathById(
          organizationOptionNodes.value,
          selectedNode.value.id,
        )!.node,
      ]
    : organizationOptionNodes.value;
}

function filteredTreeCount(type: string, fallback: number) {
  const count = selectedSubtree().reduce(
    (total, node) => total + countNodesByType(node, type),
    0,
  );
  return count || fallback;
}

function relationNodeClass(item: RelationGraphNode | undefined, layer: string) {
  const type = organizationType(item?.node).toLowerCase();
  return [
    'relation-node',
    `relation-node--${layer}`,
    type && `relation-node--${type}`,
    isSelectedRelationshipNode(item?.node) && 'relation-node--active',
  ];
}

function statusColor(status: string) {
  if (status === 'ACTIVE') return 'success';
  if (status === 'DRAFT') return 'warning';
  return 'default';
}

function statusLabel(status: string) {
  if (status === 'ACTIVE') return '在岗';
  if (status === 'DRAFT') return '待确认';
  return '休息';
}
</script>

<template>
  <Page auto-content-height content-class="organization-page">
    <section
      :class="[
        'organization-shell',
        { 'organization-shell--no-map': !shouldShowDataMap },
      ]"
    >
      <DataMapPanel
        v-if="shouldShowDataMap"
        v-model:selected-keys="selectedOrganizationKeys"
        :tree-data="organizationNodes"
        @select="handleDataMapSelect"
      />

      <main class="organization-workbench">
        <header class="organization-header">
          <div>
            <div class="organization-title-row">
              <h1>组织架构</h1>
              <Tag color="blue">业务视角</Tag>
            </div>
            <p>基础数据由系统管理维护</p>
          </div>
          <Button type="link" @click="goTo('/system/personnel-management')">
            系统管理维护
            <IconifyIcon icon="lucide:arrow-right" />
          </Button>
        </header>

        <section class="metric-grid">
          <div
            v-for="item in metrics"
            :key="item.label"
            :class="['metric-item', `metric-item--${item.tone}`]"
          >
            <span class="metric-item__icon">
              <IconifyIcon :icon="item.icon" />
            </span>
            <div>
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}<em>{{ item.suffix }}</em></strong>
            </div>
          </div>
        </section>

        <section
          :class="[
            'overview-grid',
            relationGraph?.mode === 'GROUP' && 'overview-grid--group',
          ]"
        >
          <section class="relation-panel">
            <div class="panel-title">
              <h2>组织关系图</h2>
              <div class="relation-graph-toolbar">
                <Tag v-if="relationGraph" color="blue">
                  {{ relationGraph.scopeLabel }}
                </Tag>
                <Button
                  :disabled="relationExpandableNodeIds.length === 0"
                  size="small"
                  @click="expandRelationGraph"
                >
                  <IconifyIcon icon="lucide:chevrons-down" />
                  一键展开
                </Button>
                <Button
                  :disabled="relationExpandableNodeIds.length === 0"
                  size="small"
                  @click="collapseRelationGraph"
                >
                  <IconifyIcon icon="lucide:chevrons-up" />
                  一键折叠
                </Button>
                <Button
                  v-if="selectedOrganizationProfile"
                  size="small"
                  @click="openProfileModal"
                >
                  <IconifyIcon icon="lucide:scan-text" />
                  {{ profileButtonLabel }}
                </Button>
                <Button size="small">
                  <IconifyIcon icon="lucide:refresh-cw" />
                  刷新
                </Button>
              </div>
            </div>

            <div
              v-if="relationGraph"
              :class="[
                'relation-canvas',
                `relation-canvas--${relationGraph.mode.toLowerCase()}`,
              ]"
            >
              <div class="relation-stage" :style="relationGraphLayoutStyle">
                <div class="relation-chain">
                  <template
                    v-for="(chainItem, chainIndex) in relationGraph.chain"
                    :key="chainItem.node.key"
                  >
                    <button
                      :aria-pressed="isSelectedRelationshipNode(chainItem.node)"
                      :class="relationNodeClass(chainItem, 'chain')"
                      type="button"
                      @click="selectRelationshipNode(chainItem.node)"
                    >
                      <span class="relation-node__content">
                        <strong>{{ chainItem.node.title }}</strong>
                        <em>{{ chainItem.kindLabel }}</em>
                      </span>
                    </button>
                    <div
                      v-if="chainIndex < relationGraph.chain.length - 1"
                      class="relation-chain__line"
                    />
                  </template>
                </div>

                <div
                  v-if="relationGraph.groups.length > 0"
                  class="relation-root-connector"
                />

                <div
                  v-if="relationGraph.groups.length > 0"
                  class="relation-group-grid"
                >
                  <div
                    v-for="group in relationGraph.groups"
                    :key="group.parent.node.key"
                    class="relation-group"
                  >
                    <button
                      :aria-pressed="isSelectedRelationshipNode(group.parent.node)"
                      :class="relationNodeClass(group.parent, 'card')"
                      type="button"
                      @click="selectRelationshipNode(group.parent.node)"
                    >
                      <span class="relation-node__content">
                        <strong>{{ group.parent.node.title }}</strong>
                        <em class="relation-node__badge">
                          {{ relationNodeBadgeLabel(group.parent) }}
                        </em>
                      </span>
                    </button>

                    <div v-if="group.children.length > 0" class="relation-team-list">
                      <button
                        v-for="child in group.children"
                        :key="child.node.key"
                        :aria-pressed="isSelectedRelationshipNode(child.node)"
                        :class="relationNodeClass(child, 'team')"
                        type="button"
                        @click="selectRelationshipNode(child.node)"
                      >
                        <span class="relation-node__content">
                          <strong>{{ child.node.title }}</strong>
                          <em>{{ child.kindLabel }}</em>
                        </span>
                      </button>
                    </div>
                  </div>
                </div>

                <p class="relation-hint">
                  <IconifyIcon icon="lucide:info" />
                  {{ relationGraphFooterHint }}
                </p>
              </div>

              <div
                class="relation-zoom-tools relation-zoom-tools--top-right"
                aria-label="关系图视图工具"
              >
                <Button
                  :disabled="relationZoom === 1"
                  size="small"
                  @click="fitRelationshipGraph"
                >
                  <IconifyIcon icon="lucide:maximize-2" />
                  适应
                </Button>
                <Button
                  :disabled="relationZoom >= maxRelationZoom"
                  size="small"
                  @click="zoomRelationshipGraph(1)"
                >
                  <IconifyIcon icon="lucide:plus" />
                  放大
                </Button>
                <Button
                  :disabled="relationZoom <= minRelationZoom"
                  size="small"
                  @click="zoomRelationshipGraph(-1)"
                >
                  <IconifyIcon icon="lucide:minus" />
                  缩小
                </Button>
              </div>
            </div>
            <Empty v-else description="暂无组织关系" />
          </section>

          <aside class="detail-panel">
            <div class="detail-panel__header">
              <div>
                <h2>{{ selectedTeamInfo.title }}</h2>
                <span>{{ selectedKindLabel }}概览</span>
              </div>
              <div class="detail-panel__actions">
                <Button
                  v-if="selectedOrganizationProfile"
                  class="profile-trigger"
                  size="small"
                  type="link"
                  @click="openProfileModal"
                >
                  <IconifyIcon icon="lucide:scan-text" />
                  {{ profileButtonLabel }}
                </Button>
                <Tag color="green">在岗</Tag>
              </div>
            </div>

            <dl class="detail-list">
              <div>
                <dt>所属部门</dt>
                <dd>{{ selectedTeamInfo.department }}</dd>
              </div>
              <div>
                <dt>班组长</dt>
                <dd>{{ selectedTeamInfo.leader }}</dd>
              </div>
              <div>
                <dt>安全员</dt>
                <dd>{{ selectedTeamInfo.safetyOfficer }}</dd>
              </div>
              <div>
                <dt>在岗人数</dt>
                <dd>{{ overview.activePersonnel }}人</dd>
              </div>
            </dl>

            <div v-if="businessStatuses.length > 0" class="status-list">
              <div
                v-for="item in businessStatuses"
                :key="item.label"
                :class="['status-item', `status-item--${item.tone}`]"
              >
                <span>
                  <IconifyIcon :icon="item.icon" />
                  {{ item.label }}
                </span>
                <strong>{{ item.value }}</strong>
              </div>
            </div>

            <div class="business-entry-grid">
              <Button
                v-for="item in businessEntries"
                :key="item.path"
                :class="['business-entry', `business-entry--${item.tone}`]"
                @click="goTo(item.path)"
              >
                <IconifyIcon :icon="item.icon" />
                {{ item.label }}
              </Button>
            </div>
          </aside>
        </section>

        <section class="member-panel">
          <div class="panel-title">
            <h2>成员快照</h2>
            <span>{{ selectedTitle }} 在岗 {{ overview.activePersonnel }}人</span>
          </div>
          <Table
            :columns="memberColumns"
            :data-source="members"
            :loading="loading"
            :pagination="false"
            :row-key="(record) => String(record.id)"
            :scroll="{ x: 900 }"
            size="small"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.dataIndex === 'role'">
                <Tag
                  :color="
                    record.role === '班组长'
                      ? 'blue'
                      : record.role === '安全员'
                        ? 'green'
                        : record.role === '骨干员工'
                          ? 'orange'
                          : 'default'
                  "
                >
                  {{ record.role }}
                </Tag>
              </template>
              <template v-else-if="column.dataIndex === 'status'">
                <Tag :color="statusColor(record.status)">
                  {{ statusLabel(record.status) }}
                </Tag>
              </template>
              <template v-else-if="column.dataIndex === 'actions'">
                <Button size="small" @click="goTo('/system/personnel-management')">
                  查看档案
                </Button>
                <Button
                  class="member-action"
                  size="small"
                  @click="goTo('/pingan/three-checks/pre-shift-meeting')"
                >
                  业务记录
                </Button>
              </template>
            </template>
          </Table>
        </section>
      </main>
    </section>

    <Modal
      v-model:open="profileModalOpen"
      centered
      class="organization-profile-modal"
      :footer="null"
      :title="profileButtonLabel"
      width="min(1280px, calc(100vw - 48px))"
    >
      <section v-if="selectedOrganizationProfile" class="profile-modal">
        <div class="profile-modal__content">
          <h2 class="profile-modal__title">
            {{ selectedOrganizationProfile.title }}
          </h2>
          <p class="profile-modal__description">
            {{ selectedOrganizationProfile.description }}
          </p>
        </div>
        <div class="profile-modal__media">
          <img
            class="profile-modal__image"
            :alt="selectedOrganizationProfile.imageAlt"
            :data-managed-image-url="selectedOrganizationProfile.imageAttachment?.url"
            :src="selectedOrganizationProfile.imageUrl"
          />
        </div>
      </section>
    </Modal>
  </Page>
</template>

<style scoped>
.organization-shell {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 16px;
  height: 100%;
  min-height: 0;
}

.organization-shell--no-map {
  grid-template-columns: minmax(0, 1fr);
}

.organization-workbench {
  display: flex;
  min-width: 0;
  min-height: 0;
  flex-direction: column;
  gap: 14px;
}

.organization-header,
.metric-item,
.relation-panel,
.detail-panel,
.member-panel {
  background: #fff;
  border: 1px solid #e6eaf2;
  border-radius: 8px;
  box-shadow: 0 8px 24px rgb(15 23 42 / 4%);
}

.organization-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 20px;
}

.organization-title-row {
  display: flex;
  gap: 10px;
  align-items: center;
}

.organization-title-row h1 {
  margin: 0;
  color: #0f172a;
  font-size: 22px;
  font-weight: 800;
  line-height: 1.25;
}

.organization-header p {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 13px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
}

.metric-item {
  display: flex;
  gap: 14px;
  align-items: center;
  min-height: 94px;
  padding: 14px;
}

.metric-item__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 46px;
  height: 46px;
  border-radius: 8px;
  font-size: 23px;
}

.metric-item > div {
  min-width: 0;
}

.metric-item > div > span {
  color: #475569;
  font-size: 13px;
  white-space: nowrap;
}

.metric-item strong {
  display: block;
  margin-top: 4px;
  color: #0f172a;
  font-size: 30px;
  font-weight: 800;
  line-height: 1;
}

.metric-item em {
  margin-left: 4px;
  color: #64748b;
  font-size: 14px;
  font-style: normal;
  font-weight: 600;
}

.metric-item--primary .metric-item__icon {
  color: #1264f0;
  background: #eaf2ff;
}

.metric-item--success .metric-item__icon {
  color: #16a34a;
  background: #ecfdf3;
}

.metric-item--warning .metric-item__icon {
  color: #f59e0b;
  background: #fff7e6;
}

.metric-item--neutral .metric-item__icon {
  color: #7c3aed;
  background: #f3edff;
}

.overview-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(300px, 0.85fr);
  gap: 10px;
  min-height: 430px;
}

.overview-grid--group {
  grid-template-columns: minmax(0, 1fr);
}

.overview-grid--group .detail-panel {
  display: none;
}

.relation-panel,
.detail-panel,
.member-panel {
  min-width: 0;
  padding: 18px;
}

.relation-panel {
  overflow: hidden;
}

.panel-title,
.detail-panel__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.panel-title h2,
.detail-panel__header h2 {
  margin: 0;
  color: #0f172a;
  font-size: 17px;
  font-weight: 800;
}

.panel-title span,
.detail-panel__header span {
  color: #64748b;
  font-size: 13px;
}

.relation-graph-toolbar {
  display: inline-flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.relation-graph-toolbar :deep(.ant-btn) {
  display: inline-flex;
  gap: 6px;
  align-items: center;
}

.relation-canvas {
  --relation-zoom-safe-top: 72px;
  --relation-canvas-side-space: 24px;

  position: relative;
  height: clamp(420px, 58vh, 620px);
  margin-top: 18px;
  overflow: auto;
  overscroll-behavior: contain;
  padding: var(--relation-zoom-safe-top) var(--relation-canvas-side-space) 86px;
  border: 1px solid #edf2f8;
  border-radius: 8px;
  background: #fbfdff;
}

.relation-stage {
  --relation-card-width: 178px;
  --relation-group-gap: 12px;

  width: 100%;
  min-width: 0;
  margin: 0 auto;
  transform-origin: top center;
  transition: transform 0.16s ease;
}

.relation-chain {
  display: flex;
  align-items: center;
  flex-direction: column;
  width: 100%;
  min-width: 0;
}

.relation-chain__line {
  width: 1px;
  height: 34px;
  background: #8bb7f7;
}

.relation-root-connector {
  width: 1px;
  height: 32px;
  margin: 0 auto;
  background: #8bb7f7;
}

.relation-node {
  appearance: none;
  position: relative;
  z-index: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: var(--relation-card-width);
  min-height: 72px;
  padding: 13px 16px;
  color: #0f172a;
  border: 1px solid #dce5f3;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 8px 20px rgb(15 23 42 / 7%);
  cursor: pointer;
  font-family: inherit;
  text-align: center;
  width: var(--relation-card-width);
  transition:
    border-color 0.16s ease,
    box-shadow 0.16s ease,
    transform 0.16s ease;
}

.relation-node:hover {
  border-color: #1264f0;
  box-shadow: 0 14px 30px rgb(18 100 240 / 13%);
  transform: translateY(-1px);
}

.relation-node:focus-visible {
  outline: 2px solid #1264f0;
  outline-offset: 2px;
}

.relation-node--active {
  border-color: #1264f0;
  box-shadow:
    0 0 0 2px rgb(18 100 240 / 12%),
    0 12px 28px rgb(18 100 240 / 14%);
}

.relation-node--active::after {
  position: absolute;
  top: 10px;
  right: 10px;
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: #2f7cf6;
  content: '';
}

.relation-node__content {
  display: flex;
  min-width: 0;
  align-items: center;
  flex-direction: column;
  gap: 6px;
  width: 100%;
}

.relation-node__content strong {
  color: #0f172a;
  font-size: 16px;
  font-weight: 800;
  line-height: 1.25;
  overflow-wrap: anywhere;
}

.relation-node__content em {
  max-width: 100%;
  color: #2563eb;
  border-radius: 6px;
  background: #eaf2ff;
  font-size: 12px;
  font-style: normal;
  font-weight: 700;
  line-height: 1;
  padding: 5px 8px;
}

.relation-node__badge {
  color: #09825d !important;
  background: #e8f8f2 !important;
}

.relation-node--team {
  min-width: 150px;
  min-height: 42px;
  padding: 8px 12px;
  border-color: #cfeae2;
  background: #f7fffc;
  box-shadow: none;
  width: 150px;
}

.relation-node--team .relation-node__content {
  gap: 2px;
}

.relation-node--team .relation-node__content strong {
  font-size: 13px;
}

.relation-node--team .relation-node__content em {
  display: none;
}

.relation-group-grid {
  position: relative;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(124px, 1fr));
  gap: 46px var(--relation-group-gap);
  justify-content: center;
  margin-top: 0;
  width: 100%;
  min-width: 0;
}

.relation-group-grid::before {
  position: absolute;
  top: -24px;
  right: 48px;
  left: 48px;
  height: 1px;
  background: #8bb7f7;
  content: '';
}

.relation-group {
  position: relative;
  display: flex;
  align-items: center;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
}

.relation-node--card {
  width: 100%;
  min-width: 0;
  padding: 12px 10px;
}

.relation-node--card .relation-node__content {
  gap: 4px;
}

.relation-node--card .relation-node__content strong {
  font-size: 13px;
}

.relation-node--card .relation-node__content em {
  padding: 4px 6px;
  font-size: 11px;
}

.relation-canvas--group .relation-node--chain {
  width: 260px;
  min-width: 260px;
  min-height: 64px;
  padding: 12px 18px;
}

.relation-canvas--group .relation-node--chain .relation-node__content {
  gap: 3px;
}

.relation-canvas--group .relation-node--chain .relation-node__content strong {
  font-size: 18px;
}

.relation-canvas--group .relation-node--chain .relation-node__content em {
  padding: 4px 6px;
  font-size: 11px;
}

.relation-canvas--group .relation-chain__line,
.relation-canvas--group .relation-root-connector {
  height: 26px;
}

.relation-canvas--group .relation-group-grid {
  gap: 30px var(--relation-group-gap);
}

.relation-canvas--group .relation-group-grid::before {
  top: -16px;
}

.relation-canvas--group .relation-node--card {
  min-height: 74px;
}

.relation-node--card::before {
  position: absolute;
  top: -24px;
  left: 50%;
  width: 1px;
  height: 24px;
  background: #8bb7f7;
  content: '';
  transform: translateX(-50%);
}

.relation-canvas--group .relation-node--card::before {
  top: -16px;
  height: 16px;
}

.relation-team-list {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-width: 166px;
  padding: 14px 12px;
  border: 1px solid #d9e8f3;
  border-radius: 8px;
  background: #fff;
}

.relation-team-list::before {
  position: absolute;
  top: -16px;
  left: 50%;
  width: 1px;
  height: 16px;
  background: #8bb7f7;
  content: '';
  transform: translateX(-50%);
}

.relation-canvas--company .relation-stage {
  --relation-card-width: 190px;
  --relation-group-gap: 28px;
}

.relation-canvas--company .relation-group-grid {
  grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
  gap: 54px var(--relation-group-gap);
}

.relation-canvas--company .relation-group {
  gap: 22px;
}

.relation-canvas--company .relation-node--card {
  min-height: 76px;
}

.relation-canvas--company .relation-team-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  align-items: stretch;
  gap: 12px;
  min-width: 190px;
  width: 100%;
  padding: 16px;
}

.relation-canvas--company .relation-node--team {
  width: 100%;
  min-width: 0;
  min-height: 46px;
}

.relation-hint {
  display: flex;
  gap: 6px;
  align-items: center;
  justify-content: center;
  margin: 44px 0 0;
  color: #64748b;
  font-size: 13px;
  width: 100%;
  min-width: 0;
}

.relation-zoom-tools {
  position: absolute;
  top: 10px;
  right: 10px;
  display: inline-flex;
  overflow: hidden;
  flex-direction: column;
  border: 1px solid #e1e8f0;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 10px 22px rgb(15 23 42 / 8%);
}

.relation-zoom-tools--top-right {
  z-index: 5;
}

.relation-zoom-tools :deep(.ant-btn) {
  display: inline-flex;
  gap: 4px;
  align-items: center;
  justify-content: center;
  width: 72px;
  border: 0;
  border-radius: 0;
}

.detail-panel {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.detail-panel__actions {
  display: inline-flex;
  gap: 8px;
  align-items: center;
  flex-shrink: 0;
}

.profile-trigger {
  display: inline-flex;
  gap: 6px;
  align-items: center;
  padding: 0;
  font-weight: 700;
}

.detail-list {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px 20px;
  margin: 0;
  padding-bottom: 16px;
  border-bottom: 1px solid #edf1f7;
}

.detail-list dt {
  color: #94a3b8;
  font-size: 13px;
}

.detail-list dd {
  margin: 6px 0 0;
  color: #1e293b;
  font-size: 15px;
  font-weight: 700;
}

.status-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-bottom: 16px;
  border-bottom: 1px solid #edf1f7;
}

.status-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 38px;
  color: #334155;
}

.status-item span {
  display: inline-flex;
  gap: 8px;
  align-items: center;
}

.status-item strong {
  font-weight: 800;
}

.status-item--success strong,
.status-item--success svg {
  color: #16a34a;
}

.status-item--warning strong,
.status-item--warning svg {
  color: #f59e0b;
}

.status-item--danger strong,
.status-item--danger svg {
  color: #ef4444;
}

.status-item--primary strong,
.status-item--primary svg {
  color: #1264f0;
}

.business-entry-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.business-entry {
  display: inline-flex;
  align-items: center;
  justify-content: flex-start;
  min-height: 42px;
  font-weight: 700;
}

.business-entry svg {
  margin-right: 8px;
}

.business-entry--primary svg {
  color: #1264f0;
}

.business-entry--success svg {
  color: #16a34a;
}

.business-entry--warning svg {
  color: #f59e0b;
}

.business-entry--danger svg {
  color: #ef4444;
}

.member-panel {
  min-height: 250px;
}

.member-panel :deep(.ant-table-wrapper) {
  margin-top: 14px;
}

.member-action {
  margin-left: 8px;
}

.profile-modal {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.organization-profile-modal :deep(.ant-modal-body) {
  padding: 18px 32px 32px;
}

.profile-modal__content {
  max-width: 100%;
}

.profile-modal__title {
  margin: 0 0 12px;
  color: #0f172a;
  font-size: 20px;
  font-weight: 800;
  text-align: center;
  line-height: 1.35;
}

.profile-modal__description {
  margin: 0;
  color: #1e293b;
  font-size: 15px;
  line-height: 1.75;
  text-align: justify;
}

.profile-modal__media {
  display: block;
  width: min(100%, 1040px);
  margin: 0 auto;
}

.profile-modal__image {
  display: block;
  width: 100%;
  aspect-ratio: 16 / 6.3;
  object-fit: cover;
  background: #eaf2ff;
}

@media (max-width: 1100px) {
  .metric-grid {
    grid-template-columns: repeat(3, minmax(150px, 1fr));
  }

  .overview-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .organization-shell {
    grid-template-columns: 1fr;
  }

  .metric-grid,
  .business-entry-grid {
    grid-template-columns: 1fr;
  }

  .organization-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .organization-profile-modal :deep(.ant-modal-body) {
    padding: 16px 18px 22px;
  }

  .profile-modal__title {
    font-size: 18px;
  }

  .profile-modal__description {
    font-size: 14px;
  }
}
</style>
