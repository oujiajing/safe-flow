import type { PinganSpecialWorkApi } from '#/api/pingan/special-work';

interface SpecialWorkWorkflowAction {
  action: PinganSpecialWorkApi.Action;
  label: string;
  payload: PinganSpecialWorkApi.ActionPayload;
  permission: string;
}

export function getSpecialWorkWorkflowAction(
  record: PinganSpecialWorkApi.Record,
): SpecialWorkWorkflowAction | undefined {
  switch (record.status) {
    case 'PENDING_APPROVAL': {
      return {
        action: 'APPROVE_AND_START',
        label: '审批',
        payload: {
          disclosureReceiver: record.disclosureReceiver,
          guardian: record.guardian,
          implementationStartTime: record.implementationStartTime,
          safetyDisclosurePerson: record.safetyDisclosurePerson,
        },
        permission: 'PINGAN_SPECIAL_WORK_APPROVE',
      };
    }
    case 'IN_PROGRESS': {
      return {
        action: 'SUBMIT_ACCEPTANCE',
        label: '提交验收',
        payload: { implementationEndTime: record.implementationEndTime },
        permission: 'PINGAN_SPECIAL_WORK_APPLY',
      };
    }
    case 'PENDING_ACCEPTANCE': {
      return {
        action: 'COMPLETE_ACCEPTANCE',
        label: '验收',
        payload: {
          completionAcceptanceTime: record.completionAcceptanceTime,
          completionAcceptor: record.completionAcceptor,
        },
        permission: 'PINGAN_SPECIAL_WORK_REVIEW',
      };
    }
    case 'COMPLETED': {
      return undefined;
    }
  }
}

export function canExecuteSpecialWorkWorkflowAction(
  record: PinganSpecialWorkApi.Record,
  accessCodes: string[],
) {
  const transition = getSpecialWorkWorkflowAction(record);
  return Boolean(
    transition && accessCodes.includes(transition.permission),
  );
}
