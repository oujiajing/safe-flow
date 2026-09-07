import type { BasicUserInfo } from '@vben-core/typings';

/** 用户信息 */
interface UserInfo extends BasicUserInfo {
  /**
   * 用户描述
   */
  desc: string;
  /**
   * 首页地址
   */
  homePath: string;

  /**
   * 登录用户所属组织
   */
  orgId?: number | string;

  /**
   * 登录用户所属组织路径
   */
  orgPath?: string;

  /**
   * accessToken
   */
  token: string;
}

export type { UserInfo };
