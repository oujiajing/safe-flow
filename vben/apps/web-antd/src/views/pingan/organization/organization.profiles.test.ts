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
    expect(source).toContain('广东省广晟控股集团有限公司');
    expect(source).toContain('成立于1999年');
    expect(source).toContain('/images/organization/guangsheng-group.svg');
    expect(source).toContain('注册资本金100亿元');
    expect(source).not.toContain('highlights');
  });
});
