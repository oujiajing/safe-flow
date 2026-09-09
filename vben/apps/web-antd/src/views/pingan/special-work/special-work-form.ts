import type { PinganSpecialWorkApi } from '#/api/pingan/special-work';

export function normalizeSpecialWorkDateTime(value?: string) {
  if (!value) {
    return undefined;
  }
  return value.replace('T', ' ').replace(/\.\d+$/, '').slice(0, 19);
}

export function compactSpecialWorkPayload(
  form: PinganSpecialWorkApi.RecordPayload & { status?: string },
): PinganSpecialWorkApi.RecordPayload {
  const {
    completionAcceptanceTime,
    completionAcceptor,
    disclosureReceiver,
    guardian,
    implementationEndTime,
    implementationStartTime,
    safetyDisclosurePerson,
    status,
    ...application
  } = form;
  const workflowFields =
    status === 'PENDING_APPROVAL'
      ? {
          disclosureReceiver,
          guardian,
          implementationStartTime: normalizeSpecialWorkDateTime(
            implementationStartTime,
          ),
          safetyDisclosurePerson,
        }
      : status === 'IN_PROGRESS'
        ? {
            implementationEndTime:
              normalizeSpecialWorkDateTime(implementationEndTime),
          }
        : status === 'PENDING_ACCEPTANCE'
          ? {
              completionAcceptanceTime: normalizeSpecialWorkDateTime(
                completionAcceptanceTime,
              ),
              completionAcceptor,
            }
          : {};
  return {
    ...application,
    ...workflowFields,
    applicationTime: normalizeSpecialWorkDateTime(form.applicationTime) || '',
  };
}

export function readSpecialWorkImagePreview(file: File) {
  return new Promise<string>((resolve, reject) => {
    const reader = new FileReader();
    reader.addEventListener('load', () => {
      resolve(String(reader.result || ''));
    });
    reader.addEventListener('error', () => reject(reader.error));
    reader.readAsDataURL(file);
  });
}
