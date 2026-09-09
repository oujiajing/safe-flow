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

describe('organization profile config', () => {
  it('keeps group profile content as frontend configuration', () => {
    const source = readFileSync(
      appSourcePath('src/views/pingan/organization/organization.profiles.ts'),
      'utf8',
    );

    expect(source).toContain('organizationProfiles');
    expect(source).toContain('findOrganizationProfile');
    expect(source).toContain('演示公司集团');
    expect(source).toContain('用于展示安全生产数字化管控流程的虚构企业集团');
    expect(source).toContain('/images/organization/guangsheng-group.svg');
    expect(source).toContain('省属国有独资重点企业');
    expect(source).not.toContain('highlights');
  });
});
