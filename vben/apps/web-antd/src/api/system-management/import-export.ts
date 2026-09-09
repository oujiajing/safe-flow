import { requestClient } from '#/api/request';
import { downloadFileFromBlob } from '@vben/utils';

import type { SystemManagementApi } from './types';

type ImportExportModule = SystemManagementApi.ImportExportModule;
type ListParams = SystemManagementApi.ListParams;

export function downloadSystemTemplateApi(module: ImportExportModule) {
  return requestClient
    .get<Blob>(`/system/${module}/template`, {
      responseReturn: 'body',
      responseType: 'blob',
    })
    .then((source) => {
      downloadFileFromBlob({
        fileName: `system-${module}-template.xlsx`,
        source,
      });
      return source;
    });
}

export function downloadSystemDataApi(
  module: ImportExportModule,
  params: ListParams = {},
) {
  return requestClient
    .get<Blob>(`/system/${module}/export`, {
      params,
      responseReturn: 'body',
      responseType: 'blob',
    })
    .then((source) => {
      downloadFileFromBlob({
        fileName: `system-${module}-data.xlsx`,
        source,
      });
      return source;
    });
}

export function uploadSystemDataApi(module: ImportExportModule, file: File) {
  const formData = new FormData();
  formData.append('file', file);

  return requestClient.post(`/system/${module}/import`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
}
