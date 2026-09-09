type LoginCaptchaEnv = {
  DEV?: boolean;
  PROD?: boolean;
  VITE_LOGIN_CAPTCHA_ENABLED?: string;
};

export function isLoginCaptchaEnabled(env: LoginCaptchaEnv) {
  return env.VITE_LOGIN_CAPTCHA_ENABLED === 'true';
}
