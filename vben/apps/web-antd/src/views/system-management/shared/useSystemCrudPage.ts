import type { Ref } from 'vue';

import { computed, reactive, ref } from 'vue';

import type { SystemManagementApi } from '#/api/system-management/types';

export type SystemCrudAction =
  | 'delete'
  | 'downloadData'
  | 'downloadTemplate'
  | 'edit'
  | 'freeze'
  | 'new'
  | 'refresh'
  | 'unfreeze'
  | 'uploadData';

export interface SystemFilterOption {
  label: string;
  value: SystemManagementApi.Id;
}

export interface SystemFieldOption {
  children?: SystemFieldOption[];
  label: string;
  value?: null | number | string;
}

export interface SystemFieldConfig {
  component?: 'input' | 'select' | 'textarea' | 'tree-select';
  defaultValue?: unknown | (() => unknown);
  dropdownMatchSelectWidth?: boolean | number;
  formatValue?: (value: unknown) => unknown;
  label: string;
  listHeight?: number;
  mode?: 'multiple';
  normalizeValue?: (value: unknown) => unknown;
  options?: SystemFieldOption[];
  placeholder?: string;
  readonly?: boolean;
  required?: boolean;
  showSearch?: boolean;
  treeDefaultExpandAll?: boolean;
  type?: 'date' | 'number' | 'text';
  valueKey: string;
}

export interface SystemCrudColumn {
  dataIndex: string;
  fixed?: 'left' | 'right';
  sortOrder?: 'ascend' | 'descend';
  sorter?: boolean;
  title: string;
  width?: number;
}

export interface SystemRowAction<TRecord = Record<string, unknown>> {
  label: string;
  onClick: (record: TRecord) => Promise<unknown> | unknown;
}

export interface SystemCrudFilters extends SystemManagementApi.ListParams {
  companyId?: SystemManagementApi.Id;
  departmentId?: SystemManagementApi.Id;
  keyword?: string;
  organizationId?: SystemManagementApi.Id;
  status?: SystemManagementApi.Status | 'all';
  teamId?: SystemManagementApi.Id;
}

export type SystemCascadeField = 'company' | 'department' | 'team';

export type SystemFilterParamMap = Partial<
  Record<keyof SystemCrudFilters, null | string>
>;

export const systemCrudTableScrollConfig = {
  y: 'calc(100vh - 360px)',
} as const;

const systemTableSelectionColumnWidth = 48;
const systemTableDefaultColumnWidth = 120;

function normalizeSystemTableColumnWidth(width: unknown) {
  if (typeof width === 'number' && Number.isFinite(width) && width > 0) {
    return width;
  }
  if (typeof width === 'string') {
    const numericWidth = Number.parseFloat(width);
    if (Number.isFinite(numericWidth) && numericWidth > 0) {
      return numericWidth;
    }
  }
  return systemTableDefaultColumnWidth;
}

export function createSystemTableScrollConfig<TColumn extends { width?: unknown }>(
  columns: TColumn[],
) {
  return {
    ...systemCrudTableScrollConfig,
    x:
      columns.reduce(
        (total, column) =>
          total + normalizeSystemTableColumnWidth(column.width),
        0,
      ) + systemTableSelectionColumnWidth,
  };
}

export interface SystemCrudStateOptions<TRecord extends { id?: unknown }> {
  afterMutation?: () => Promise<unknown> | unknown;
  createRecord?: (data: Partial<TRecord>) => Promise<unknown>;
  defaultListParams?: SystemManagementApi.ListParams;
  deleteRecord?: (id: SystemManagementApi.Id) => Promise<unknown>;
  downloadData?: (params: SystemManagementApi.ListParams) => Promise<unknown>;
  downloadTemplate?: () => Promise<unknown>;
  fetchList: (
    params: SystemManagementApi.ListParams,
  ) => Promise<SystemManagementApi.PageResult<TRecord> | TRecord[] | unknown>;
  filterParamMap?: SystemFilterParamMap;
  updateRecord?: (
    id: SystemManagementApi.Id,
    data: Partial<TRecord>,
  ) => Promise<unknown>;
  updateStatus?: (
    id: SystemManagementApi.Id,
    status: SystemManagementApi.Status,
  ) => Promise<unknown>;
  uploadData?: (file: File) => Promise<unknown>;
}

function compactObject<T extends Record<string, unknown>>(source: T) {
  return Object.fromEntries(
    Object.entries(source).filter(
      ([, value]) =>
        value !== '' && value !== null && value !== undefined && value !== 'all',
    ),
  );
}

export function serializeSystemFilters(
  filters: SystemCrudFilters,
  paramMap: SystemFilterParamMap = {},
) {
  const result: Record<string, unknown> = {};
  for (const [key, value] of Object.entries(compactObject({ ...filters }))) {
    const mappedKey = Object.prototype.hasOwnProperty.call(paramMap, key)
      ? paramMap[key as keyof SystemCrudFilters]
      : key;
    if (mappedKey === null) {
      continue;
    }
    result[mappedKey ?? key] = value;
  }
  return result;
}

export function getSystemRowKey<TRecord extends Record<string, unknown>>(
  record: TRecord | undefined,
  rowKey: string,
) {
  const value = record?.[rowKey];
  return typeof value === 'number' || typeof value === 'string'
    ? value
    : undefined;
}

function normalizePageResult<TRecord>(result: unknown) {
  if (Array.isArray(result)) {
    return { items: result as TRecord[], total: result.length };
  }

  if (
    result &&
    typeof result === 'object' &&
    'items' in result &&
    Array.isArray((result as SystemManagementApi.PageResult<TRecord>).items)
  ) {
    const page = result as SystemManagementApi.PageResult<TRecord>;
    return {
      items: page.items,
      total: normalizeTotal(page.total),
    };
  }

  return { items: [] as TRecord[], total: 0 };
}

function normalizeTotal(value: number | string) {
  const total = Number(value);
  return Number.isFinite(total) ? total : 0;
}

function recordIdValue(record: { id?: unknown }) {
  return typeof record.id === 'number' || typeof record.id === 'string'
    ? record.id
    : undefined;
}

export function createSystemCrudState<TRecord extends { id?: unknown } = Record<string, unknown>>(
  options: SystemCrudStateOptions<TRecord>,
) {
  const filters = reactive<SystemCrudFilters>({
    companyId: undefined,
    departmentId: undefined,
    keyword: '',
    organizationId: undefined,
    status: 'all',
    teamId: undefined,
  });
  const drawerOpen = ref(false);
  const editingRecord = ref<TRecord>();
  const loading = ref(false);
  const rows = ref<TRecord[]>([]) as Ref<TRecord[]>;
  const selectedRow = ref<TRecord>();
  const selectedRows = ref<TRecord[]>([]) as Ref<TRecord[]>;
  const total = ref(0);

  const selectedIds = computed(() =>
    selectedRows.value
      .map((record) => record.id)
      .filter(
        (id): id is SystemManagementApi.Id =>
          typeof id === 'number' || typeof id === 'string',
      ),
  );
  const selectedRowKeys = computed(() => selectedIds.value);
  const selectedCount = computed(() => selectedIds.value.length);
  const selectedId = computed(() =>
    selectedIds.value.length === 1 ? selectedIds.value[0] : undefined,
  );
  const canUseRowActions = computed(() => selectedCount.value > 0);
  const canUseSingleRowAction = computed(() => selectedCount.value === 1);

  async function load() {
    loading.value = true;
    try {
      const params = {
        ...(options.defaultListParams ?? {}),
        ...serializeSystemFilters(filters, options.filterParamMap),
      };
      const result = normalizePageResult<TRecord>(
        await options.fetchList(params),
      );
      rows.value = result.items;
      total.value = result.total;
      pruneSelectionToLoadedRows();
    } finally {
      loading.value = false;
    }
  }

  function pruneSelectionToLoadedRows() {
    const loadedIds = new Set(
      rows.value
        .map(recordIdValue)
        .filter(
          (id): id is SystemManagementApi.Id =>
            typeof id === 'number' || typeof id === 'string',
        ),
    );
    selectRows(
      selectedRows.value.filter((record) => {
        const id = recordIdValue(record);
        return id !== undefined && loadedIds.has(id);
      }),
    );
  }

  function setCompany(value?: SystemManagementApi.Id) {
    filters.companyId = value;
    filters.departmentId = undefined;
    filters.teamId = undefined;
  }

  function setDepartment(value?: SystemManagementApi.Id) {
    filters.departmentId = value;
    filters.teamId = undefined;
  }

  function setTeam(value?: SystemManagementApi.Id) {
    filters.teamId = value;
  }

  function setOrganization(value?: SystemManagementApi.Id) {
    filters.organizationId = value;
  }

  function selectRow(record?: TRecord) {
    selectRows(record ? [record] : []);
  }

  function selectRows(records: TRecord[]) {
    selectedRows.value = records;
    selectedRow.value = records.length === 1 ? records[0] : undefined;
  }

  function openCreate() {
    editingRecord.value = undefined;
    drawerOpen.value = true;
  }

  function openEdit(record = selectedRow.value) {
    editingRecord.value = record;
    drawerOpen.value = true;
  }

  async function afterMutation() {
    await options.afterMutation?.();
  }

  async function submit(data: Partial<TRecord>) {
    const editingId = editingRecord.value?.id;
    const targetId =
      selectedId.value ??
      (typeof editingId === 'number' || typeof editingId === 'string'
        ? editingId
        : undefined);
    if (editingRecord.value && targetId !== undefined) {
      await options.updateRecord?.(targetId, {
        ...editingRecord.value,
        ...data,
      });
    } else {
      await options.createRecord?.(data);
    }
    drawerOpen.value = false;
    selectRows([]);
    await afterMutation();
    await load();
  }

  async function handleDelete() {
    if (selectedIds.value.length === 0) return;
    await Promise.all(selectedIds.value.map((id) => options.deleteRecord?.(id)));
    selectRows([]);
    await afterMutation();
    await load();
  }

  async function handleStatus(status: SystemManagementApi.Status) {
    if (selectedIds.value.length === 0) return;
    await Promise.all(
      selectedIds.value.map((id) => options.updateStatus?.(id, status)),
    );
    await afterMutation();
    await load();
  }

  async function handleDownloadTemplate() {
    await options.downloadTemplate?.();
  }

  async function handleDownloadData() {
    await options.downloadData?.(
      serializeSystemFilters(filters, options.filterParamMap),
    );
  }

  async function handleUploadData(file: File) {
    await options.uploadData?.(file);
    await afterMutation();
    await load();
  }

  return {
    canUseRowActions,
    canUseSingleRowAction,
    drawerOpen,
    editingRecord,
    filters,
    handleDelete,
    handleDownloadData,
    handleDownloadTemplate,
    handleStatus,
    handleUploadData,
    load,
    loading,
    openCreate,
    openEdit,
    rows,
    selectRows,
    selectRow,
    selectedCount,
    selectedIds,
    selectedRow,
    selectedRowKeys,
    setCompany,
    setDepartment,
    setOrganization,
    setTeam,
    submit,
    total,
  };
}
