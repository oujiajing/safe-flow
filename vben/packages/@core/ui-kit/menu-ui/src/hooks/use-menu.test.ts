import { describe, expect, it } from 'vitest';

import { useMenuStyle } from './use-menu';

describe('menu level style', () => {
  it('increments child menu level so nested sidebar items keep a visible staircase', () => {
    const style = useMenuStyle({ level: 1 } as any);

    expect(style.value).toEqual({
      '--menu-level': 2,
    });
  });
});
