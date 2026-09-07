import { beforeEach, describe, expect, it, vi } from 'vitest';

import { requestClient } from '#/api/request';

import {
  batchDeleteSafetyLearningContentsApi,
  createSafetyLearningContentApi,
  deleteSafetyLearningContentApi,
  downloadSafetyLearningContentsApi,
  downloadSafetyLearningTemplateApi,
  getSafetyLearningContentsApi,
  importSafetyLearningWorkbookApi,
  removeSafetyLearningCoverImageApi,
  removeSafetyLearningAttachmentApi,
  removeSafetyLearningVideoApi,
  updateSafetyLearningContentApi,
  uploadSafetyLearningAttachmentApi,
  uploadSafetyLearningCoverImageApi,
  uploadSafetyLearningVideoApi,
} from './safety-learning';
import type { PinganSafetyLearningApi } from './safety-learning';

vi.mock('#/api/request', () => ({
  requestClient: {
    delete: vi.fn(),
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    upload: vi.fn(),
  },
}));

describe('safety-learning api', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('maps safety learning CRUD, import/export, and PDF attachment endpoints', async () => {
    const payload: PinganSafetyLearningApi.ContentPayload = {
      attachmentText: '附件文本',
      category: '安全知识',
      code: 'LEARN-001',
      companyId: 4,
      content: '学习内容',
      coverImage: '封面',
      draft: '否',
      durationText: '30分钟',
      htmlExtract: '不提取',
      learningDate: '2026-05-22',
      status: 'ACTIVE',
      title: '高处作业安全学习',
      video: '视频',
    };

    await getSafetyLearningContentsApi({
      category: '安全知识',
      companyId: 4,
      dateEnd: '2026-05-22',
      dateStart: '2026-05-01',
      keyword: '高处',
      page: 1,
      pageSize: 20,
      status: 'ACTIVE',
    });
    await createSafetyLearningContentApi(payload);
    await updateSafetyLearningContentApi(12, payload);
    await deleteSafetyLearningContentApi(12);
    await batchDeleteSafetyLearningContentsApi([12, 13]);
    await uploadSafetyLearningAttachmentApi(12, new File(['pdf'], '安全学习.pdf'));
    await removeSafetyLearningAttachmentApi(12);
    await uploadSafetyLearningCoverImageApi(12, new File(['image'], '封面.png', { type: 'image/png' }));
    await removeSafetyLearningCoverImageApi(12);
    await uploadSafetyLearningVideoApi(12, new File(['video'], '课程.mp4', { type: 'video/mp4' }));
    await removeSafetyLearningVideoApi(12);
    await downloadSafetyLearningTemplateApi();
    await downloadSafetyLearningContentsApi({ keyword: 'LEARN-001' });
    await importSafetyLearningWorkbookApi(new File(['xlsx'], '学习内容.xlsx'));

    expect(requestClient.get).toHaveBeenNthCalledWith(
      1,
      '/pingan/training/safety-learning/contents',
      {
        params: {
          category: '安全知识',
          companyId: 4,
          dateEnd: '2026-05-22',
          dateStart: '2026-05-01',
          keyword: '高处',
          page: 1,
          pageSize: 20,
          status: 'ACTIVE',
        },
      },
    );
    expect(requestClient.post).toHaveBeenNthCalledWith(
      1,
      '/pingan/training/safety-learning/contents',
      payload,
    );
    expect(requestClient.put).toHaveBeenNthCalledWith(
      1,
      '/pingan/training/safety-learning/contents/12',
      payload,
    );
    expect(requestClient.delete).toHaveBeenNthCalledWith(
      1,
      '/pingan/training/safety-learning/contents/12',
    );
    expect(requestClient.post).toHaveBeenNthCalledWith(
      2,
      '/pingan/training/safety-learning/contents/batch-delete',
      { ids: [12, 13] },
    );
    expect(requestClient.upload).toHaveBeenNthCalledWith(
      1,
      '/pingan/training/safety-learning/contents/12/attachment',
      { file: expect.any(File) },
    );
    expect(requestClient.delete).toHaveBeenNthCalledWith(
      2,
      '/pingan/training/safety-learning/contents/12/attachment',
    );
    expect(requestClient.upload).toHaveBeenNthCalledWith(
      2,
      '/pingan/training/safety-learning/contents/12/cover-image',
      { file: expect.any(File) },
    );
    expect(requestClient.delete).toHaveBeenNthCalledWith(
      3,
      '/pingan/training/safety-learning/contents/12/cover-image',
    );
    expect(requestClient.upload).toHaveBeenNthCalledWith(
      3,
      '/pingan/training/safety-learning/contents/12/video',
      { file: expect.any(File) },
    );
    expect(requestClient.delete).toHaveBeenNthCalledWith(
      4,
      '/pingan/training/safety-learning/contents/12/video',
    );
    expect(requestClient.get).toHaveBeenNthCalledWith(
      2,
      '/pingan/training/safety-learning/contents/template',
      { responseType: 'blob' },
    );
    expect(requestClient.get).toHaveBeenNthCalledWith(
      3,
      '/pingan/training/safety-learning/contents/export',
      { params: { keyword: 'LEARN-001' }, responseType: 'blob' },
    );
    expect(requestClient.post).toHaveBeenNthCalledWith(
      3,
      '/pingan/training/safety-learning/contents/import',
      expect.any(FormData),
      { headers: { 'Content-Type': 'multipart/form-data' } },
    );
  });
});
