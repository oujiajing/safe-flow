import type { Recordable } from '@vben/types';

const REMEMBER_ME_PREFIX = 'REMEMBER_ME_CREDENTIALS';

function storageKey(hostname = globalThis.location?.hostname ?? 'localhost') {
  return `${REMEMBER_ME_PREFIX}_${hostname}`;
}

export function readRememberedCredentials() {
  const raw = localStorage.getItem(storageKey());
  if (!raw) {
    return { password: '', username: '' };
  }

  try {
    const parsed = JSON.parse(raw) as Recordable<string>;
    return {
      password: parsed.password || '',
      username: parsed.username || '',
    };
  } catch {
    return { password: '', username: '' };
  }
}

export function writeRememberedCredentials(options: {
  password?: string;
  remember: boolean;
  username?: string;
}) {
  if (!options.remember) {
    localStorage.removeItem(storageKey());
    return;
  }

  localStorage.setItem(
    storageKey(),
    JSON.stringify({
      password: options.password || '',
      username: options.username || '',
    }),
  );
}
