import { requestClient } from '#/api/request';

export namespace {{entityName}}Api {
  export type SourceChannel = 'API_IMPORT' | 'PC' | 'WECHAT_MINI_PROGRAM';
}

export function list{{entityName}}Api(params: Record<string, unknown>) {
  return requestClient.get('{{apiBase}}', { params });
}
