import { beforeEach, describe, expect, it, vi } from 'vitest';

import {
  readRememberedCredentials,
  writeRememberedCredentials,
} from './remember-password';

describe('remember password helpers', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.stubGlobal('location', { hostname: 'localhost' });
  });

  it('persists username and password when remember password is checked', () => {
    writeRememberedCredentials({
      password: '123456',
      remember: true,
      username: 'team_member',
    });

    expect(readRememberedCredentials()).toEqual({
      password: '123456',
      username: 'team_member',
    });
  });

  it('clears remembered password when remember password is unchecked', () => {
    writeRememberedCredentials({
      password: '123456',
      remember: true,
      username: 'team_member',
    });
    writeRememberedCredentials({
      password: '123456',
      remember: false,
      username: 'team_member',
    });

    expect(readRememberedCredentials()).toEqual({
      password: '',
      username: '',
    });
  });
});
