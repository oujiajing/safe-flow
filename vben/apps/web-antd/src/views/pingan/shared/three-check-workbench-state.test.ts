import { describe, expect, it } from 'vitest';

import type { OrganizationNode } from '../pre-shift-meeting/pre-shift-meeting.data';

import {
  createSharedThreeCheckWorkbenchState,
  threeCheckChangeHistoryFilterOptions,
  THREE_CHECK_LIST_AUTO_REFRESH_INTERVAL_MS,
} from './three-check-workbench-state';

const organizations: OrganizationNode[] = [
  { id: 1, key: 'company-1', orgType: 'COMPANY', title: '测试公司' },
];

describe('shared three-check workbench state', () => {
  it('provides one canonical refresh and history-filter configuration', () => {
    expect(THREE_CHECK_LIST_AUTO_REFRESH_INTERVAL_MS).toBe(10_000);
    expect(threeCheckChangeHistoryFilterOptions.map((item) => item.value)).toEqual(
      ['ALL', 'UPDATE', 'STATUS', 'ATTACHMENT', 'FLOW'],
    );
  });

  it('creates the same initial state contract for both workbench branches', () => {
    const state = createSharedThreeCheckWorkbenchState<
      { id: string },
      { checkItem: string }
    >(organizations, ['company-1']);

    expect(state.filters).toMatchObject({
      company: '',
      dateEnd: '',
      dateStart: '',
      department: '',
      status: 'all',
      team: '',
    });
    expect(state.createForm).toMatchObject({
      attendeeNames: ['Demo Harbor班长'],
      meetingContent: '',
      ownerUserId: 2,
    });
    expect(state.expandedOrganizationKeys.value).toEqual(['company-1']);
    expect(state.dataMapOrganizationNodes.value).toEqual(organizations);
    expect(state.userOptions.value).toHaveLength(4);
  });

  it('does not leak mutable list or form state between page instances', () => {
    const first = createSharedThreeCheckWorkbenchState<
      { id: string },
      { checkItem: string }
    >(organizations, []);
    const second = createSharedThreeCheckWorkbenchState<
      { id: string },
      { checkItem: string }
    >(organizations, []);

    first.rows.value.push({ id: 'row-1' });
    first.createForm.attendeeNames.push('新增人员');
    first.userOptions.value.pop();

    expect(second.rows.value).toEqual([]);
    expect(second.createForm.attendeeNames).toEqual(['Demo Harbor班长']);
    expect(second.userOptions.value).toHaveLength(4);
  });
});
