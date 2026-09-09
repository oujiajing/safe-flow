import type { OrganizationNode } from '#/views/pingan/pre-shift-meeting/pre-shift-meeting.data';

import { organizationType } from './organization.view-state';

export interface OrganizationProfile {
  description: string;
  imageAlt: string;
  imageUrl: string;
  subtitle: string;
  title: string;
}

export const organizationProfiles: OrganizationProfile[] = [
  {
    description: '演示公司集团是用于展示安全生产数字化管控流程的虚构企业集团，业务覆盖装配运营、设备保障和安全管理。',
    imageAlt: '演示公司集团形象图',
    imageUrl: '/images/organization/guangsheng-group.svg',
    subtitle: '省属国有独资重点企业',
    title: '演示公司集团',
  },
];

export function findOrganizationProfile(node?: OrganizationNode) {
  const type = organizationType(node);
  if (!['COMPANY', 'GROUP', 'SCHOOL'].includes(type) || !node) return undefined;

  const nodeId = node?.id === undefined ? '' : String(node.id);
  const nodeKey = node?.key ?? '';
  const nodeTitle = node?.title ?? '';
  const compactTitle = nodeTitle.replaceAll(/\s/g, '');

  const matchedProfile =
    type === 'GROUP'
      ? organizationProfiles.find((profile) => {
          const profileTitle = profile.title.replaceAll(/\s/g, '');
          return (
            nodeId.toLowerCase().includes('guangsheng') ||
            nodeKey.toLowerCase().includes('guangsheng') ||
            compactTitle.includes('演示安全集团') ||
            profileTitle.includes(compactTitle)
          );
        })
      : undefined;

  if (matchedProfile) return matchedProfile;

  return {
    description: `${node.title}纳入演示公司组织体系统一管理，相关基础数据、人员组织、班组运行和安全业务记录在安全生产数字化管控平台中集中呈现。页面可结合组织关系图查看其下级单位、部门班组和业务运行情况。`,
    imageAlt: `${node.title}组织形象图`,
    imageUrl: '/images/organization/guangsheng-group.svg',
    subtitle: type === 'SCHOOL' ? '培训教学单位' : '演示公司所属单位',
    title: `${node.title}简介`,
  };
}
