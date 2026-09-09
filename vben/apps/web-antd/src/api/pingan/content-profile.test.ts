import { beforeEach, describe, expect, it, vi } from 'vitest';

import { requestClient } from '#/api/request';

import {
  getActiveContentProfileByOrgApi,
  getScreenContentProfileVideosApi,
} from './content-profile';

vi.mock('#/api/request', () => ({
  requestClient: {
    get: vi.fn(),
  },
}));

describe('pingan content profile api clients', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('maps organization profile and screen video reads to pingan endpoints', async () => {
    await getActiveContentProfileByOrgApi(4);
    await getScreenContentProfileVideosApi({ orgId: 4 });
    await getScreenContentProfileVideosApi();

    expect(requestClient.get).toHaveBeenNthCalledWith(
      1,
      '/pingan/content-profiles/by-org/4',
    );
    expect(requestClient.get).toHaveBeenNthCalledWith(
      2,
      '/pingan/content-profiles/screen-videos',
      { params: { orgId: 4 } },
    );
    expect(requestClient.get).toHaveBeenNthCalledWith(
      3,
      '/pingan/content-profiles/screen-videos',
      { params: undefined },
    );
  });
});
