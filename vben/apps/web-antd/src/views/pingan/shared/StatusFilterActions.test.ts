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

describe('StatusFilterActions source contract', () => {
  const source = readFileSync(
    appSourcePath('src/views/pingan/shared/StatusFilterActions.vue'),
    'utf8',
  );

  it('keeps status, search and reset bottom-aligned on one line', () => {
    expect(source).toContain('<span>状态</span>');
    expect(source).toContain("emit('search')");
    expect(source).toContain("emit('reset')");
    expect(source).toMatch(
      /\.status-filter-actions\s*\{[\s\S]*display: flex;[\s\S]*flex: 0 1 300px;[\s\S]*align-items: flex-end;[\s\S]*min-width: 258px;/,
    );
    expect(source).toContain('flex: 1 1 126px');
    expect(source).toContain('min-width: 64px');
    expect(source).toContain('height: 40px');
    expect(source).toContain('align-items: center');
    expect(source).toContain('justify-content: center');
    expect(source).toContain('line-height: 1');
  });

  it.each([
    'pre-shift-meeting/index.vue',
    'one-shift-three-checks/workbench/index.vue',
    'special-work/index.vue',
    'safety-learning/index.vue',
    'training-exam/index.vue',
    'hazard-rectification-order/index.vue',
  ])('is reused by %s', (page) => {
    const pageSource = readFileSync(
      appSourcePath(`src/views/pingan/${page}`),
      'utf8',
    );
    expect(pageSource).toContain('<StatusFilterActions');
  });

  it.each([
    'pre-shift-meeting/index.vue',
    'one-shift-three-checks/workbench/index.vue',
    'special-work/index.vue',
    'safety-learning/index.vue',
    'training-exam/index.vue',
  ])('keeps the desktop filter toolbar on one line in %s', (page) => {
    const pageSource = readFileSync(
      appSourcePath(`src/views/pingan/${page}`),
      'utf8',
    );
    expect(pageSource).toMatch(
      /\.workbench__bar\s*\{[\s\S]*?flex-wrap: nowrap;[\s\S]*?overflow: hidden;/,
    );
  });
});
