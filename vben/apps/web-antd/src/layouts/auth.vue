<script lang="ts" setup>
import { AuthPageLayout } from '@vben/layouts';
import { preferences } from '@vben/preferences';

import { $t } from '#/locales';

const appName = '安全生产数字化管控平台';
const logo = preferences.logo.source;
const logoDark = preferences.logo.sourceDark;
</script>

<template>
  <AuthPageLayout
    :app-name="appName"
    :logo="logo"
    :logo-dark="logoDark"
    :page-description="$t('authentication.pageDesc')"
    :page-title="$t('authentication.pageTitle')"
  >
    <!-- 自定义工具栏 -->
    <!-- <template #toolbar></template> -->
  </AuthPageLayout>
</template>
