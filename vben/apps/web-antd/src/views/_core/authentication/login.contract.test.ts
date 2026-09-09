import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

import { describe, expect, it } from 'vitest';

import { isLoginCaptchaEnabled } from './loginCaptcha';

function appSourcePath(path: string) {
  return process.cwd().replaceAll('\\', '/').endsWith('/apps/web-antd')
    ? resolve(path)
    : resolve(`apps/web-antd/${path}`);
}

const loginVue = readFileSync(
  appSourcePath('src/views/_core/authentication/login.vue'),
  'utf8',
);

describe('password login page contract', () => {
  it('uses manual username and password login without quick account selection', () => {
    expect(loginVue).toContain("fieldName: 'username'");
    expect(loginVue).toContain("fieldName: 'password'");
    expect(loginVue).not.toContain('selectAccount');
    expect(loginVue).not.toContain('DEMO_LOGIN_ACCOUNTS');
    expect(loginVue).not.toContain('getDemoLoginCredentials');
  });

  it('does not expose the optional verification code field on the login page', () => {
    expect(loginVue).not.toContain('verificationCode');
    expect(loginVue).not.toContain('可选验证码');
  });

  it('disables slider captcha in development until it is explicitly restored', () => {
    expect(
      isLoginCaptchaEnabled({
        DEV: true,
        PROD: false,
        VITE_LOGIN_CAPTCHA_ENABLED: 'false',
      }),
    ).toBe(false);
    expect(
      isLoginCaptchaEnabled({
        DEV: false,
        PROD: true,
        VITE_LOGIN_CAPTCHA_ENABLED: 'true',
      }),
    ).toBe(true);
  });

  it('does not require slider captcha unconditionally', () => {
    expect(loginVue).toContain('isLoginCaptchaEnabled');
    expect(loginVue).toContain('enableLoginCaptcha');
  });
});
