<script setup lang="ts">
import { ref } from 'vue';

import AccidentAnalysis from './accident-analysis.vue';
import DataAnalysis from './data-analysis.vue';
import ScreenDashboard from './screen.vue';
import TeamAnalysis from './team-analysis.vue';

type MonitorModule = 'accident' | 'analysis' | 'screen' | 'team';

const activeModule = ref<MonitorModule>('screen');

const moduleTabs: Array<{ label: string; value: MonitorModule }> = [
  { label: '平安班组大屏', value: 'screen' },
  { label: '平安班组数据分析中心', value: 'analysis' },
  { label: '事故看板', value: 'accident' },
  { label: '班组分析', value: 'team' },
];

</script>

<template>
  <div class="monitor-center-shell">
    <nav class="monitor-module-tabs" aria-label="平安班组监控中心模块">
      <button
        v-for="item in moduleTabs"
        :key="item.value"
        :class="{ active: activeModule === item.value }"
        type="button"
        @click="activeModule = item.value"
      >
        {{ item.label }}
      </button>
    </nav>

    <ScreenDashboard v-if="activeModule === 'screen'" />
    <DataAnalysis v-else-if="activeModule === 'analysis'" />
    <AccidentAnalysis v-else-if="activeModule === 'accident'" />
    <TeamAnalysis v-else-if="activeModule === 'team'" />
  </div>
</template>

<style scoped>
.monitor-center-shell {
  min-height: calc(100vh - 112px);
  overflow-x: auto;
  background: #f3f6fb;
}

.monitor-module-tabs {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  height: 48px;
  gap: 8px;
  padding: 10px 16px 0;
  background: #fff;
}

.monitor-module-tabs button {
  height: 32px;
  padding: 0 16px;
  color: #334155;
  background: #fff;
  border: 1px solid #d4dce8;
  border-radius: 4px;
  box-shadow: 0 1px 2px rgb(15 23 42 / 6%);
  font-size: 14px;
  font-weight: 700;
  line-height: 30px;
}

.monitor-module-tabs button.active {
  color: #fff;
  background: #1d6fff;
  border-color: #1d6fff;
  box-shadow: 0 6px 14px rgb(29 111 255 / 22%);
}

</style>
