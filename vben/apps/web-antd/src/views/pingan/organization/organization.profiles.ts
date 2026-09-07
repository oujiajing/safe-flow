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
    description:
      'Demo RegionDemo控股集团有限公司（简称“Demo控股集团”）成立于1999年，注册资本金100亿元，是Demo Region属国有独资重点企业。经过25年的改革发展，Demo控股集团已成长为以矿产资源、电子信息为主业，环保、工程地产、金融协同发展的大型跨国企业集团，入选国务院国资委国企改革“双百企业”名单。截至2024年底，Demo控股集团资产总额1786.9亿元，全年实现营业收入1027.4亿元、利润总额38.3亿元、净利润29.8亿元。现拥有员工5万余人，其中海外员工5000多人，中共党员6000多人；位居2024中国企业500强第203位、中国战略性新兴产业领军企业100强第29位、中国跨国公司100大第88位。',
    imageAlt: 'Demo RegionDemo控股集团形象图',
    imageUrl: '/images/organization/guangsheng-group.svg',
    subtitle: '省属国有独资重点企业',
    title: 'Demo RegionDemo控股集团有限公司',
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
            compactTitle.includes('Demo控股集团') ||
            profileTitle.includes(compactTitle)
          );
        })
      : undefined;

  if (matchedProfile) return matchedProfile;

  return {
    description: `${node.title}纳入Demo Safety Holdings组织体系统一管理，相关基础数据、人员组织、班组运行和安全业务记录在平安班组平台中集中呈现。页面可结合组织关系图查看其下级单位、部门班组和业务运行情况。`,
    imageAlt: `${node.title}组织形象图`,
    imageUrl: '/images/organization/guangsheng-group.svg',
    subtitle: type === 'SCHOOL' ? '培训教学单位' : 'Demo Safety Holdings所属单位',
    title: `${node.title}简介`,
  };
}
