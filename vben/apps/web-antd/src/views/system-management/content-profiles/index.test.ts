import { readFileSync } from 'node:fs';
import { join } from 'node:path';

import { describe, expect, it } from 'vitest';

function appSourcePath(path: string) {
  return join(
    process.cwd(),
    process.cwd().endsWith(join('apps', 'web-antd'))
      ? path
      : `apps/web-antd/${path}`,
  );
}

describe('system content profiles page', () => {
  it('provides organization-scoped text, image, and video management controls', () => {
    const source = readFileSync(
      appSourcePath('src/views/system-management/content-profiles/index.vue'),
      'utf8',
    );

    for (const text of [
      '内容配置',
      '组织',
      '简介标题',
      '简介正文',
      '主图',
      '宣传视频',
      'uploadContentProfileImageApi',
      'uploadContentProfileVideoApi',
      'removeContentProfileVideoApi',
      'getContentProfileListApi',
      'getPinganCompanyOrgTreeApi',
      'content-profile-page',
      'media-upload-grid',
    ]) {
      expect(source).toContain(text);
    }
    expect(source).toContain('accept="image/*"');
    expect(source).toContain('accept="video/*"');
  });
});
