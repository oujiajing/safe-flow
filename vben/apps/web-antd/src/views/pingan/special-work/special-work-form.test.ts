import { describe, expect, it } from 'vitest';

import {
  compactSpecialWorkPayload,
  normalizeSpecialWorkDateTime,
  readSpecialWorkImagePreview,
} from './special-work-form';

describe('special-work form helpers', () => {
  it('normalizes browser ISO datetime values to backend datetime format', () => {
    expect(normalizeSpecialWorkDateTime('2026-05-22T06:02:02')).toBe(
      '2026-05-22 06:02:02',
    );
    expect(normalizeSpecialWorkDateTime('2026-05-22T06:02:02.000')).toBe(
      '2026-05-22 06:02:02',
    );
  });

  it('compacts optional datetime fields before saving', () => {
    expect(
      compactSpecialWorkPayload({
        applicationTime: '2026-05-22T06:02:02',
        companyId: 4,
        completionAcceptanceTime: '',
        implementationEndTime: '2026-05-22T08:00:00',
        implementationStartTime: '2026-05-22T07:00:00',
        project: '动火作业',
        status: 'IN_PROGRESS',
        workType: '动火',
      }),
    ).toMatchObject({
      applicationTime: '2026-05-22 06:02:02',
      implementationEndTime: '2026-05-22 08:00:00',
    });
    expect(
      compactSpecialWorkPayload({
        applicationTime: '2026-05-22T06:02:02',
        companyId: 4,
        implementationEndTime: '2026-05-22T08:00:00',
        implementationStartTime: '2026-05-22T07:00:00',
        project: '动火作业',
        status: 'PENDING_APPROVAL',
        workType: '动火',
      }),
    ).toMatchObject({
      implementationStartTime: '2026-05-22 07:00:00',
    });
  });

  it('reads selected image files as previewable data urls', async () => {
    const file = new File(['image'], 'special-work.png', { type: 'image/png' });

    await expect(readSpecialWorkImagePreview(file)).resolves.toContain(
      'data:image/png;base64,',
    );
  });
});
