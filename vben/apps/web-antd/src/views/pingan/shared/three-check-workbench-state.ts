import type { Ref } from 'vue';

import { reactive, ref } from 'vue';

import type { PinganPreShiftMeetingApi } from '#/api/pingan/pre-shift-meeting';
import type { PinganThreeCheckRecordApi } from '#/api/pingan/three-check-record';

import {
  createOrganizationFilterState,
  type MeetingFilters,
  type OrganizationFilterState,
  type OrganizationId,
  type OrganizationNode,
} from '../pre-shift-meeting/pre-shift-meeting.data';

export const THREE_CHECK_LIST_AUTO_REFRESH_INTERVAL_MS = 10_000;

export const threeCheckWorkbenchUserFallbacks: PinganPreShiftMeetingApi.UserOption[] =
  [
    { id: 1, orgId: 1, realName: '系统管理员', username: 'admin' },
    { id: 2, orgId: 4, realName: '班组负责人', username: 'HB_MONITOR' },
    { id: 3, orgId: 4, realName: '安全员', username: 'MQ_SAFE' },
    { id: 4, orgId: 8, realName: '制氧主管', username: 'ZY_SUPERVISOR' },
  ];

export const threeCheckChangeHistoryFilterOptions = [
  { label: '全部', value: 'ALL' },
  { label: '字段', value: 'UPDATE' },
  { label: '状态', value: 'STATUS' },
  { label: '附件', value: 'ATTACHMENT' },
  { label: '流程', value: 'FLOW' },
] as const;

export interface ThreeCheckCreateFormState {
  attendeeNames: string[];
  companyId?: OrganizationId;
  departmentId?: OrganizationId;
  meetingContent: string;
  meetingDate: string;
  ownerUserId: number;
  teamId?: OrganizationId;
}

export interface ThreeCheckWorkbenchFilters extends OrganizationFilterState {
  dateEnd: string;
  dateStart: string;
  pointsReason?: string;
  status: MeetingFilters['status'];
}

export function createSharedThreeCheckWorkbenchState<Row, InspectionLine>(
  organizationNodes: OrganizationNode[],
  defaultExpandedKeys: string[],
) {
  const filters = reactive<ThreeCheckWorkbenchFilters>({
    ...createOrganizationFilterState(),
    dateEnd: '',
    dateStart: '',
    status: 'all',
  });
  const createForm = reactive<ThreeCheckCreateFormState>({
    attendeeNames: ['班组负责人'],
    companyId: undefined,
    departmentId: undefined,
    meetingContent: '',
    meetingDate: new Date().toISOString().slice(0, 10),
    ownerUserId: 2,
    teamId: undefined,
  });

  return {
    changeHistoryFilter: ref('ALL'),
    changeHistoryFilterOptions: threeCheckChangeHistoryFilterOptions,
    changeHistoryKeyword: ref(''),
    changeHistoryLoading: ref(false),
    changeHistoryOpen: ref(false),
    changeHistorySelectedGroupId: ref(''),
    createForm,
    createOpen: ref(false),
    createSaving: ref(false),
    currentDetail: ref<PinganPreShiftMeetingApi.MeetingDetail>(),
    currentLocalDetail: ref<Row>() as Ref<Row | undefined>,
    currentThreeCheckDetail:
      ref<PinganThreeCheckRecordApi.RecordDetail>(),
    dataMapOrganizationNodes: ref(organizationNodes) as Ref<OrganizationNode[]>,
    detailAttachmentSectionRef: ref<HTMLElement>(),
    detailEditForm: reactive<Record<string, any>>({}),
    detailEditing: ref(false),
    detailLoading: ref(false),
    detailOpen: ref(false),
    detailSaving: ref(false),
    documentFlowKeyword: ref(''),
    documentFlowLoading: ref(false),
    documentFlowOpen: ref(false),
    documentFlowSelectedNodeId: ref(''),
    expandedOrganizationKeys: ref(defaultExpandedKeys),
    filters,
    formOrganizationNodes: ref<OrganizationNode[]>([]),
    loading: ref(false),
    localCreateForm: reactive<Record<string, any>>({}),
    rows: ref<Row[]>([]) as Ref<Row[]>,
    selectedOrganizationKeys: ref<string[]>([]),
    selectedRowKeys: ref<string[]>([]),
    tableBottomRailActive: ref(false),
    tablePanelRef: ref<HTMLElement>(),
    teamCheckInspectionLines: ref<InspectionLine[]>([]) as Ref<
      InspectionLine[]
    >,
    teamCheckInspectionLoading: ref(false),
    total: ref(0),
    treeKeyword: ref(''),
    userOptions: ref([...threeCheckWorkbenchUserFallbacks]),
    videoPreviewOpen: ref(false),
    videoPreviewUrl: ref(''),
  };
}
