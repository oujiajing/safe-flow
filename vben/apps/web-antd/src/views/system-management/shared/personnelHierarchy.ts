import type { UserInfo } from '@vben/types';

import type { SystemManagementApi } from '#/api/system-management/types';

type PersonnelPayload = Record<string, unknown>;

interface NormalizePersonnelPayloadOptions {
  selectedPath?: SystemManagementApi.OrganizationNode[];
}

interface NormalizePersonnelPayloadResult {
  data?: PersonnelPayload;
  error?: string;
}

interface PersonnelDefaultOptions {
  today?: string;
  userInfo?: null | Partial<UserInfo>;
}

function compactPayload(payload: PersonnelPayload) {
  return Object.fromEntries(
    Object.entries(payload).filter(
      ([, value]) => value !== '' && value !== null && value !== undefined,
    ),
  );
}

function todayText() {
  const now = new Date();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${now.getFullYear()}-${month}-${day}`;
}

function personnelHierarchyFromPath(
  selectedPath: SystemManagementApi.OrganizationNode[],
): NormalizePersonnelPayloadResult {
  const company = selectedPath.at(-1);
  if (!company) {
    return { error: '请选择公司' };
  }
  return {
    data: {
      companyName: company.title,
      companyOrgId: company.id,
    },
  };
}

export function createPersonnelDefaultValues(
  options: PersonnelDefaultOptions = {},
) {
  return {
    applicantName: options.userInfo?.realName || options.userInfo?.username,
    submitDate: options.today ?? todayText(),
  };
}

export function normalizePersonnelPayloadForSubmit(
  rawPayload: PersonnelPayload,
  options: NormalizePersonnelPayloadOptions,
): NormalizePersonnelPayloadResult {
  const hierarchy = personnelHierarchyFromPath(options.selectedPath ?? []);
  if (hierarchy.error) {
    return { error: hierarchy.error };
  }
  return {
    data: compactPayload({
      ...rawPayload,
      ...(hierarchy.data ?? {}),
    }),
  };
}
