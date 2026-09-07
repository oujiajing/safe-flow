import { describe, expect, it } from 'vitest';

import { createOneShiftThreeCheckWorkflowPanelState } from './detail-adapter';

describe('one-shift-three-check detail adapter', () => {
  it('maps real workflow logs into document flow and change history panel items', () => {
    const state = createOneShiftThreeCheckWorkflowPanelState({
      changeHistory: [
        {
          action: 'WITHDRAW',
          actionLabel: '撤回',
          fromStatus: 'OPENED',
          fromStatusLabel: '已检查',
          id: '5',
          occurredAt: '2026-05-20T09:05:00',
          operatorId: 1,
          operatorName: '系统管理员',
          remark: '检查项需要补充',
          toStatus: 'WITHDRAWN',
          toStatusLabel: '待检查',
        },
        {
          action: 'SUBMIT',
          actionLabel: '提交',
          fromStatus: 'DRAFT',
          fromStatusLabel: '待检查',
          id: '3',
          occurredAt: '2026-05-20T09:03:00',
          operatorId: 1,
          operatorName: '系统管理员',
          remark: '提交一班三查记录',
          toStatus: 'OPENED',
          toStatusLabel: '已检查',
        },
      ],
      documentFlow: [
        {
          action: 'CREATE',
          actionLabel: '创建记录',
          fromStatus: undefined,
          fromStatusLabel: undefined,
          id: '1',
          occurredAt: '2026-05-20T09:01:00',
          operatorId: 1,
          operatorName: '系统管理员',
          remark: '创建一班三查记录',
          toStatus: 'DRAFT',
          toStatusLabel: '待检查',
        },
        {
          action: 'SUBMIT',
          actionLabel: '提交',
          fromStatus: 'DRAFT',
          fromStatusLabel: '待检查',
          id: '3',
          occurredAt: '2026-05-20T09:03:00',
          operatorId: 1,
          operatorName: '系统管理员',
          remark: '提交一班三查记录',
          toStatus: 'OPENED',
          toStatusLabel: '已检查',
        },
      ],
    });

    expect(state.documentFlowItems).toEqual([
      {
        key: '1',
        label: '创建记录',
        meta: '系统管理员 · 2026-05-20T09:01:00',
        value: '待检查 · 创建一班三查记录',
      },
      {
        key: '3',
        label: '提交',
        meta: '系统管理员 · 2026-05-20T09:03:00',
        value: '待检查 → 已检查 · 提交一班三查记录',
      },
    ]);
    expect(state.changeHistoryItems[0]).toEqual({
      key: '5',
      label: '撤回',
      meta: '系统管理员 · 2026-05-20T09:05:00',
      value: '已检查 → 待检查 · 检查项需要补充',
    });
  });
});
