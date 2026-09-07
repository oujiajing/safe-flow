import { beforeEach, describe, expect, it, vi } from 'vitest';

import { requestClient } from '#/api/request';

import {
  createPreShiftMeetingApi,
  deletePreShiftMeetingApi,
  deletePreShiftMeetingAttachmentApi,
  getPreShiftMeetingDetailApi,
  getPinganCompanyOrgTreeApi,
  getPinganUsersApi,
  getPinganOrgTreeApi,
  getPreShiftMeetingsApi,
  remindPreShiftMeetingApi,
  submitPreShiftMeetingApi,
  updatePreShiftMeetingApi,
  uploadPreShiftMeetingAttachmentApi,
  withdrawPreShiftMeetingApi,
} from './pre-shift-meeting';

vi.mock('#/api/request', () => ({
  requestClient: {
    delete: vi.fn(),
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
  },
}));

describe('pre-shift-meeting api', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('maps organization tree and paged list endpoints', async () => {
    await getPinganOrgTreeApi();
    await getPinganCompanyOrgTreeApi();
    await getPinganUsersApi();
    await getPreShiftMeetingsApi({
      company: '源成公司',
      companyId: 4,
      dateStart: '2025-05-01',
      departmentId: 101_109,
      page: 1,
      pageSize: 20,
      sourceChannel: 'WECHAT_MINI_PROGRAM',
      status: 'OPENED',
      teamId: 1_011_001,
    });

    expect(requestClient.get).toHaveBeenNthCalledWith(1, '/pingan/org/tree');
    expect(requestClient.get).toHaveBeenNthCalledWith(
      2,
      '/pingan/org/company-tree',
    );
    expect(requestClient.get).toHaveBeenNthCalledWith(3, '/pingan/users');
    expect(requestClient.get).toHaveBeenNthCalledWith(
      4,
      '/pingan/pre-shift-meetings',
      {
        params: {
          company: '源成公司',
          companyId: 4,
          dateStart: '2025-05-01',
          departmentId: 101_109,
          page: 1,
          pageSize: 20,
          sourceChannel: 'WECHAT_MINI_PROGRAM',
          status: 'OPENED',
          teamId: 1_011_001,
        },
      },
    );
  });

  it('maps create and workflow action endpoints', async () => {
    const payload = {
      attendees: ['湖贝班长'],
      companyId: 4,
      departmentId: 4,
      meetingDate: '2025-05-13',
      teamId: 4,
    };

    await createPreShiftMeetingApi({
      ...payload,
      clientRequestId: 'pc-req-001',
    });
    await getPreShiftMeetingDetailApi('2001');
    await updatePreShiftMeetingApi('2001', {
      ...payload,
      version: 1,
    });
    await submitPreShiftMeetingApi('2001');
    await withdrawPreShiftMeetingApi('2001', '内容填写错误');
    await remindPreShiftMeetingApi('2001');
    await deletePreShiftMeetingApi('2001');

    expect(requestClient.post).toHaveBeenNthCalledWith(
      1,
      '/pingan/pre-shift-meetings',
      {
        ...payload,
        clientRequestId: 'pc-req-001',
      },
    );
    expect(requestClient.get).toHaveBeenCalledWith(
      '/pingan/pre-shift-meetings/2001',
    );
    expect(requestClient.put).toHaveBeenCalledWith(
      '/pingan/pre-shift-meetings/2001',
      {
        ...payload,
        version: 1,
      },
    );
    expect(requestClient.post).toHaveBeenNthCalledWith(
      2,
      '/pingan/pre-shift-meetings/2001/submit',
    );
    expect(requestClient.post).toHaveBeenNthCalledWith(
      3,
      '/pingan/pre-shift-meetings/2001/withdraw',
      { reason: '内容填写错误' },
    );
    expect(requestClient.post).toHaveBeenNthCalledWith(
      4,
      '/pingan/pre-shift-meetings/2001/remind',
    );
    expect(requestClient.delete).toHaveBeenCalledWith(
      '/pingan/pre-shift-meetings/2001',
    );
  });

  it('maps attachment upload to multipart endpoint', async () => {
    const file = new File(['image'], 'meeting.jpg', { type: 'image/jpeg' });

    await uploadPreShiftMeetingAttachmentApi('2001', 'IMAGE', file);
    await deletePreShiftMeetingAttachmentApi('2001', '9001');

    expect(requestClient.post).toHaveBeenCalledWith(
      '/pingan/pre-shift-meetings/2001/attachments',
      expect.any(FormData),
      {
        headers: { 'Content-Type': 'multipart/form-data' },
      },
    );
    expect(requestClient.delete).toHaveBeenCalledWith(
      '/pingan/pre-shift-meetings/2001/attachments/9001',
    );
  });
});
