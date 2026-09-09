import { describe, expect, it } from 'vitest';

import type { PinganSpecialWorkApi } from '#/api/pingan/special-work';

import {
  canExecuteSpecialWorkWorkflowAction,
  getSpecialWorkWorkflowAction,
} from './special-work-workflow';

function record(
  status: PinganSpecialWorkApi.Status,
): PinganSpecialWorkApi.Record {
  return {
    applicationTime: '2026-07-29 08:00:00',
    company: '测试公司',
    companyId: 1,
    id: 1,
    project: '动火作业',
    status,
    statusLabel: '',
    workType: '动火作业',
  };
}

describe('special-work workflow actions', () => {
  it.each([
    ['PENDING_APPROVAL', 'APPROVE_AND_START', '审批'],
    ['IN_PROGRESS', 'SUBMIT_ACCEPTANCE', '提交验收'],
    ['PENDING_ACCEPTANCE', 'COMPLETE_ACCEPTANCE', '验收'],
  ] as const)(
    'maps %s to the visible operation %s',
    (status, action, label) => {
      expect(getSpecialWorkWorkflowAction(record(status))).toMatchObject({
        action,
        label,
      });
    },
  );

  it('does not expose another workflow action after completion', () => {
    expect(getSpecialWorkWorkflowAction(record('COMPLETED'))).toBeUndefined();
  });

  it('keeps the state action visible while permission controls execution', () => {
    const pendingApproval = record('PENDING_APPROVAL');

    expect(getSpecialWorkWorkflowAction(pendingApproval)?.label).toBe('审批');
    expect(
      canExecuteSpecialWorkWorkflowAction(pendingApproval, [
        'PINGAN_SPECIAL_WORK_VIEW',
      ]),
    ).toBe(false);
    expect(
      canExecuteSpecialWorkWorkflowAction(pendingApproval, [
        'PINGAN_SPECIAL_WORK_APPROVE',
      ]),
    ).toBe(true);
  });
});
