import type { UserInfo } from '@vben/types';

import type { SystemManagementApi } from '#/api/system-management/types';

type TeamPayload = Record<string, unknown>;

interface NormalizeTeamPayloadOptions {
  selectedPath?: SystemManagementApi.OrganizationNode[];
}

interface NormalizeTeamPayloadResult {
  data?: TeamPayload;
  error?: string;
}

interface TeamDefaultOptions {
  today?: string;
  userInfo?: null | Partial<UserInfo>;
}

function text(value: unknown) {
  return typeof value === 'string' ? value.trim() : '';
}

function compactPayload(payload: TeamPayload) {
  return Object.fromEntries(
    Object.entries(payload).filter(
      ([, value]) => value !== '' && value !== null && value !== undefined,
    ),
  );
}

function requireText(payload: TeamPayload, key: string, label: string) {
  return text(payload[key]) ? undefined : `${label}不能为空`;
}

function todayText() {
  const now = new Date();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${now.getFullYear()}-${month}-${day}`;
}

function teamHierarchyFromPath(
  selectedPath: SystemManagementApi.OrganizationNode[],
): NormalizeTeamPayloadResult {
  const company = selectedPath.at(-1);
  if (!company) {
    return { error: '请选择公司' };
  }
  return {
    data: {
      companyName: company.title,
      companyOrgId: company.id,
      groupName: selectedPath[1]?.title,
      level1Unit: selectedPath[2]?.title,
      level2Unit: selectedPath[3]?.title,
    },
  };
}

function validateTeamPayload(payload: TeamPayload) {
  return (
    requireText(payload, 'code', '编码') ??
    requireText(payload, 'name', '名称') ??
    requireText(payload, 'status', '状态') ??
    (payload.companyOrgId === undefined ? '公司不能为空' : undefined) ??
    (payload.workshopOrgId === undefined ? '部门不能为空' : undefined) ??
    requireText(payload, 'submitDate', '提交日期') ??
    requireText(payload, 'applicantName', '申请人')
  );
}

export function createTeamDefaultValues(options: TeamDefaultOptions = {}) {
  return {
    applicantName: options.userInfo?.realName || options.userInfo?.username,
    submitDate: options.today ?? todayText(),
  };
}

export function normalizeTeamPayloadForSubmit(
  rawPayload: TeamPayload,
  options: NormalizeTeamPayloadOptions,
): NormalizeTeamPayloadResult {
  const hierarchy = teamHierarchyFromPath(options.selectedPath ?? []);
  if (hierarchy.error) {
    return { error: hierarchy.error };
  }
  const data = compactPayload({
    ...rawPayload,
    ...(hierarchy.data ?? {}),
  });
  const error = validateTeamPayload(data);
  return error ? { error } : { data };
}
