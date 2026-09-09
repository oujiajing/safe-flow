import { computed, ref } from 'vue';

export interface DataMapNode {
  children?: DataMapNode[];
  companyType?: string;
  id?: number | string;
  key: string;
  orgType?: string;
  title: string;
}

export const dataMapTreeContinuousScrollConfig = {
  itemHeight: 28,
  virtual: false,
} as const;

export const dataMapResizeConfig = {
  defaultWidth: 280,
  minWidth: 220,
} as const;

export function clampDataMapWidth(width: number) {
  return Math.max(dataMapResizeConfig.minWidth, Math.round(width));
}

export function createDataMapCollapseState() {
  const isDataMapCollapsed = ref(false);
  const dataMapToggleLabel = computed(() =>
    isDataMapCollapsed.value ? '展开' : '收起',
  );

  function toggleDataMap() {
    isDataMapCollapsed.value = !isDataMapCollapsed.value;
  }

  return {
    dataMapToggleLabel,
    isDataMapCollapsed,
    toggleDataMap,
  };
}

export function createDataMapResizeState(
  initialWidth = dataMapResizeConfig.defaultWidth,
) {
  const dataMapWidth = ref(clampDataMapWidth(initialWidth));
  const isDataMapResizing = ref(false);
  let startClientX = 0;
  let startWidth = dataMapWidth.value;

  function beginDataMapResize(clientX: number) {
    startClientX = clientX;
    startWidth = dataMapWidth.value;
    isDataMapResizing.value = true;
  }

  function updateDataMapResize(clientX: number) {
    if (!isDataMapResizing.value) {
      return;
    }
    dataMapWidth.value = clampDataMapWidth(
      startWidth + clientX - startClientX,
    );
  }

  function endDataMapResize() {
    isDataMapResizing.value = false;
  }

  return {
    beginDataMapResize,
    dataMapWidth,
    endDataMapResize,
    isDataMapResizing,
    updateDataMapResize,
  };
}
