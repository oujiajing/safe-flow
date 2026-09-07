import type { SystemCrudColumn, SystemFilterOption } from './useSystemCrudPage';

export type SystemTableConfigModule =
  | 'company'
  | 'department'
  | 'personnel'
  | 'risk-level-control'
  | 'safety-ledger'
  | 'team';

export type SystemTableFilterKey =
  | 'company'
  | 'department'
  | 'keyword'
  | 'status'
  | 'team';

export type SystemTableSortOrder = 'ascend' | 'descend' | 'none';
export type SystemTableWidthMode = 'auto' | 'fixed';

export interface SystemTableConfigField {
  enabled: boolean;
  fixed?: 'left' | 'right';
  key: string;
  order: number;
  sort: SystemTableSortOrder;
  title: string;
  width: number;
  widthMode: SystemTableWidthMode;
}

export interface SystemTableConfigFilter {
  enabled: boolean;
  key: SystemTableFilterKey;
  title: string;
}

export interface SystemTableConfig {
  code: string;
  fields: SystemTableConfigField[];
  filters: SystemTableConfigFilter[];
  name: string;
  pinned: boolean;
  roleIds: SystemFilterOption['value'][];
}

export interface SystemTableRoleOption {
  label: string;
  value: SystemFilterOption['value'];
}

export interface SystemTableConfigStorage {
  getItem: (key: string) => null | string;
  removeItem: (key: string) => void;
  setItem: (key: string, value: string) => void;
}

const filterTitles: Record<SystemTableFilterKey, string> = {
  company: '公司',
  department: '部门',
  keyword: '关键词',
  status: '状态',
  team: '班组',
};

function defaultCode(module: SystemTableConfigModule) {
  return `${module.toUpperCase()}_STANDARD`;
}

function toColumnKey(column: SystemCrudColumn) {
  return String(column.dataIndex);
}

function toColumnTitle(column: SystemCrudColumn) {
  return String(column.title);
}

function normalizeWidth(value: unknown, fallback = 120) {
  const width = Number(value);
  return Number.isFinite(width) && width >= 80 ? width : fallback;
}

function normalizeSort(value: unknown): SystemTableSortOrder {
  return value === 'ascend' || value === 'descend' ? value : 'none';
}

function normalizeWidthMode(value: unknown): SystemTableWidthMode {
  return value === 'fixed' ? 'fixed' : 'auto';
}

function normalizeRoleIds(value: unknown): SystemFilterOption['value'][] {
  return Array.isArray(value)
    ? value.filter(
        (item): item is SystemFilterOption['value'] =>
          typeof item === 'number' || typeof item === 'string',
      )
    : [];
}

function createFieldFromColumn(
  column: SystemCrudColumn,
  index: number,
): SystemTableConfigField {
  return {
    enabled: true,
    fixed: column.fixed,
    key: toColumnKey(column),
    order: index + 1,
    sort: 'none',
    title: toColumnTitle(column),
    width: normalizeWidth(column.width),
    widthMode: 'auto',
  };
}

function createFilter(key: SystemTableFilterKey): SystemTableConfigFilter {
  return {
    enabled: true,
    key,
    title: filterTitles[key],
  };
}

export function cloneSystemTableConfig(
  config: SystemTableConfig,
): SystemTableConfig {
  return {
    ...config,
    fields: config.fields.map((field) => ({ ...field })),
    filters: config.filters.map((filter) => ({ ...filter })),
    roleIds: [...config.roleIds],
  };
}

export function createDefaultSystemTableConfig(
  module: SystemTableConfigModule,
  moduleTitle: string,
  columns: SystemCrudColumn[],
  filters: SystemTableFilterKey[] = ['keyword', 'status'],
): SystemTableConfig {
  return {
    code: defaultCode(module),
    fields: columns.map(createFieldFromColumn),
    filters: filters.map(createFilter),
    name: `${moduleTitle}默认视图`,
    pinned: false,
    roleIds: [],
  };
}

export function mergeSystemTableConfig(
  saved: Partial<SystemTableConfig> | undefined,
  module: SystemTableConfigModule,
  moduleTitle: string,
  columns: SystemCrudColumn[],
  filters: SystemTableFilterKey[] = ['keyword', 'status'],
): SystemTableConfig {
  const defaults = createDefaultSystemTableConfig(
    module,
    moduleTitle,
    columns,
    filters,
  );
  if (!saved || typeof saved !== 'object') {
    return defaults;
  }

  const defaultFieldByKey = new Map(
    defaults.fields.map((field) => [field.key, field]),
  );
  const savedFields = Array.isArray(saved.fields) ? saved.fields : [];
  const mergedFieldKeys = new Set<string>();
  const mergedFields = savedFields
    .slice()
    .sort((left, right) => Number(left.order) - Number(right.order))
    .map((field) => {
      const key = String(field.key ?? '');
      const freshField = defaultFieldByKey.get(key);
      if (!freshField) return undefined;
      mergedFieldKeys.add(key);
      return {
        ...freshField,
        enabled: field.enabled !== false,
        order: mergedFieldKeys.size,
        sort: normalizeSort(field.sort),
        width: normalizeWidth(field.width, freshField.width),
        widthMode: normalizeWidthMode(field.widthMode),
      };
    })
    .filter(Boolean) as SystemTableConfigField[];

  const missingFields = defaults.fields
    .filter((field) => !mergedFieldKeys.has(field.key))
    .map((field, index) => ({
      ...field,
      order: mergedFields.length + index + 1,
    }));

  const savedFilterByKey = new Map(
    (Array.isArray(saved.filters) ? saved.filters : []).map((filter) => [
      filter.key,
      filter,
    ]),
  );

  return {
    ...defaults,
    code:
      typeof saved.code === 'string' && saved.code.trim()
        ? saved.code
        : defaults.code,
    fields: [...mergedFields, ...missingFields],
    filters: defaults.filters.map((filter) => ({
      ...filter,
      enabled: savedFilterByKey.get(filter.key)?.enabled !== false,
    })),
    name:
      typeof saved.name === 'string' && saved.name.trim()
        ? saved.name
        : defaults.name,
    pinned: saved.pinned === true,
    roleIds: normalizeRoleIds(saved.roleIds),
  };
}

export function applySystemTableConfigColumns(
  columns: SystemCrudColumn[],
  config?: SystemTableConfig,
  rows: Record<string, unknown>[] = [],
) {
  if (!config) return columns;
  const columnByKey = new Map(columns.map((column) => [toColumnKey(column), column]));

  return config.fields
    .filter((field) => field.enabled && columnByKey.has(field.key))
    .slice()
    .sort((left, right) => left.order - right.order)
    .map((field) => {
      const column = columnByKey.get(field.key) as SystemCrudColumn;
      return {
        ...column,
        fixed: field.fixed ?? column.fixed,
        sortOrder: field.sort === 'none' ? undefined : field.sort,
        sorter: field.sort === 'none' ? undefined : true,
        width:
          field.widthMode === 'fixed'
            ? field.width
            : calculateSystemTableAutoWidth(column, rows),
      };
    });
}

function formatCellText(value: unknown) {
  if (Array.isArray(value)) {
    const text = value
      .map((item) => String(item ?? '').trim())
      .filter(Boolean)
      .join(',');
    return text || '-';
  }
  if (value === undefined || value === null || value === '') {
    return '-';
  }
  if (typeof value === 'object') {
    return String(
      (value as Record<string, unknown>).label ??
        (value as Record<string, unknown>).name ??
        JSON.stringify(value),
    );
  }
  return String(value);
}

function textWidth(text: string) {
  return Array.from(text).reduce((total, char) => {
    return total + (/[\u4E00-\u9FFF]/.test(char) ? 14 : 8);
  }, 0);
}

function isLongTextColumn(column: SystemCrudColumn) {
  const key = String(column.dataIndex).toLowerCase();
  const title = String(column.title);
  return (
    key.includes('description') ||
    key.includes('intro') ||
    key.includes('attachment') ||
    title.includes('描述') ||
    title.includes('介绍') ||
    title.includes('附属文件')
  );
}

export function calculateSystemTableAutoWidth(
  column: SystemCrudColumn,
  rows: Record<string, unknown>[] = [],
) {
  const key = toColumnKey(column);
  const maxWidth = isLongTextColumn(column) ? 360 : 320;
  const contentWidth = [String(column.title), ...rows.map((row) => formatCellText(row[key]))]
    .map(textWidth)
    .reduce((max, width) => Math.max(max, width), 0);
  return Math.min(maxWidth, Math.max(96, Math.ceil(contentWidth + 40)));
}

export function reorderSystemTableConfigFields(
  fields: SystemTableConfigField[],
  oldIndex: number,
  newIndex: number,
) {
  if (
    oldIndex === newIndex ||
    oldIndex < 0 ||
    newIndex < 0 ||
    oldIndex >= fields.length ||
    newIndex >= fields.length
  ) {
    return fields.map((field, index) => ({ ...field, order: index + 1 }));
  }

  const nextFields = fields.map((field) => ({ ...field }));
  const [field] = nextFields.splice(oldIndex, 1);
  if (field) {
    nextFields.splice(newIndex, 0, field);
  }
  return nextFields.map((item, index) => ({ ...item, order: index + 1 }));
}

function compareUnknownValue(left: unknown, right: unknown) {
  if (left === right) return 0;
  if (left === undefined || left === null || left === '') return 1;
  if (right === undefined || right === null || right === '') return -1;
  if (typeof left === 'number' && typeof right === 'number') {
    return left - right;
  }
  return String(left).localeCompare(String(right), 'zh-Hans-CN', {
    numeric: true,
  });
}

export function sortRowsBySystemTableConfig<
  TRecord extends Record<string, unknown>,
>(rows: TRecord[], config?: SystemTableConfig) {
  const sortedField = config?.fields
    .slice()
    .sort((left, right) => left.order - right.order)
    .find((field) => field.enabled && field.sort !== 'none');
  if (!sortedField) return rows;

  const direction = sortedField.sort === 'descend' ? -1 : 1;
  return [...rows].sort(
    (left, right) =>
      compareUnknownValue(left[sortedField.key], right[sortedField.key]) *
      direction,
  );
}

export function getEnabledSystemTableFilterKeys(
  config?: SystemTableConfig,
): SystemTableFilterKey[] {
  return config
    ? config.filters
        .filter((filter) => filter.enabled)
        .map((filter) => filter.key)
    : ['keyword', 'status'];
}

export function getSystemTableConfigStorageKey(
  module: SystemTableConfigModule,
) {
  return `pingan:system-table-config:${module}`;
}

export function getSystemTableConfigStorage():
  | SystemTableConfigStorage
  | undefined {
  return typeof window === 'undefined' ? undefined : window.localStorage;
}

export function loadSystemTableConfig(
  module: SystemTableConfigModule,
  moduleTitle: string,
  columns: SystemCrudColumn[],
  filters: SystemTableFilterKey[],
  storage = getSystemTableConfigStorage(),
) {
  const raw = storage?.getItem(getSystemTableConfigStorageKey(module));
  if (!raw) {
    return createDefaultSystemTableConfig(module, moduleTitle, columns, filters);
  }
  try {
    return mergeSystemTableConfig(
      JSON.parse(raw) as Partial<SystemTableConfig>,
      module,
      moduleTitle,
      columns,
      filters,
    );
  } catch {
    return createDefaultSystemTableConfig(module, moduleTitle, columns, filters);
  }
}

export function saveSystemTableConfig(
  module: SystemTableConfigModule,
  config: SystemTableConfig,
  storage = getSystemTableConfigStorage(),
) {
  storage?.setItem(
    getSystemTableConfigStorageKey(module),
    JSON.stringify(config),
  );
}

export function removeSystemTableConfig(
  module: SystemTableConfigModule,
  storage = getSystemTableConfigStorage(),
) {
  storage?.removeItem(getSystemTableConfigStorageKey(module));
}
