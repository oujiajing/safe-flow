import { describe, expect, it } from 'vitest';

import {
  findSafetyLedgerEntryByRouteName,
  filterSafetyLedgerNavigation,
  getSafetyLedgerAncestorRouteNames,
  getSafetyLedgerTitlePath,
  safetyLedgerEntries,
  safetyLedgerSections,
} from './safety-ledger.config';

describe('safety ledger config', () => {
  it('registers every confirmed safety ledger leaf', () => {
    expect(safetyLedgerEntries.map((entry) => entry.title)).toEqual([
      '安全生产规章制度',
      '安全操作规程',
      '岗位安全生产职责',
      '全员责任制',
      '员工花名册',
      '安全生产工作年度计划',
      '重点工作和专项活动方案',
      '安全风险分级管控文档',
      '派班记录表',
      '派班记录表',
      '班前会议记录表',
      '班前安全活动',
      '“双重预防机制”现场管控表',
      '“双重预防机制”现场管控表',
      '重点场所',
      '个人积分榜单台账',
      '班组积分榜单台账',
      '个人积分流水台账',
      '班组积分流水台账',
      '复工复产“六个一”',
      '安全生产检查与隐患排查治理文档',
      '日周月检',
      '安全检查及整改记录表',
      '安全检查表',
      '生产安全事故隐患排查治理清单',
      '特殊作业台账',
      '隐患整改',
    ]);
  });

  it('keeps shared headers attached to the expected leaves', () => {
    const documentHeaderTitles = [
      '序号*',
      '名称*',
      '富文本',
      '文件*',
      '所属企业',
      '上传时间',
      '操作',
    ];

    expect(
      safetyLedgerEntries
        .filter((entry) => entry.headerKey === 'document-basic')
        .map((entry) => entry.title),
    ).toEqual([
      '安全生产规章制度',
      '安全操作规程',
      '岗位安全生产职责',
      '全员责任制',
      '安全生产工作年度计划',
      '重点工作和专项活动方案',
      '复工复产“六个一”',
    ]);
    expect(
      findSafetyLedgerEntryByRouteName(
        'PinganSafetyLedgerSafetyRules',
      )?.columns.map((column) => column.title),
    ).toEqual(documentHeaderTitles);
  });

  it('keeps route names and paths unique', () => {
    expect(new Set(safetyLedgerEntries.map((entry) => entry.routeName)).size).toBe(
      safetyLedgerEntries.length,
    );
    expect(new Set(safetyLedgerEntries.map((entry) => entry.path)).size).toBe(
      safetyLedgerEntries.length,
    );
  });

  it('configures operation columns as blank display columns', () => {
    const operationColumns = safetyLedgerEntries.flatMap((entry) =>
      entry.columns.filter((column) => column.title === '操作'),
    );

    expect(operationColumns.length).toBeGreaterThan(0);
    expect(operationColumns.every((column) => column.kind === 'blank')).toBe(true);
  });

  it('marks confirmed self-maintained ledgers as document ledgers', () => {
    expect(
      safetyLedgerEntries
        .filter((entry) => entry.sourceKind === 'DOCUMENT_LEDGER')
        .map((entry) => entry.title),
    ).toEqual([
      '安全生产规章制度',
      '安全操作规程',
      '岗位安全生产职责',
      '全员责任制',
      '安全生产工作年度计划',
      '重点工作和专项活动方案',
      '安全风险分级管控文档',
      '复工复产“六个一”',
      '安全生产检查与隐患排查治理文档',
    ]);
  });

  it('keeps confirmed synchronized ledgers on existing source adapters', () => {
    const synchronizedEntries = safetyLedgerEntries.filter(
      (entry) => entry.sourceKind !== 'DOCUMENT_LEDGER',
    );

    expect(synchronizedEntries).toHaveLength(18);
    expect(synchronizedEntries.map((entry) => entry.title)).toEqual([
      '员工花名册',
      '派班记录表',
      '派班记录表',
      '班前会议记录表',
      '班前安全活动',
      '“双重预防机制”现场管控表',
      '“双重预防机制”现场管控表',
      '重点场所',
      '个人积分榜单台账',
      '班组积分榜单台账',
      '个人积分流水台账',
      '班组积分流水台账',
      '日周月检',
      '安全检查及整改记录表',
      '安全检查表',
      '生产安全事故隐患排查治理清单',
      '特殊作业台账',
      '隐患整改',
    ]);
  });

  it('maps each ledger entry to the confirmed filter bar fields', () => {
    const filtersByTitle = Object.fromEntries(
      safetyLedgerEntries.map((entry) => [entry.title, entry.filters]),
    );

    expect(filtersByTitle['安全生产规章制度']).toEqual([
      'company',
      'uploadedStart',
      'uploadedEnd',
    ]);
    expect(filtersByTitle['员工花名册']).toEqual([
      'company',
      'department',
      'team',
    ]);
    expect(filtersByTitle['安全生产工作年度计划']).toEqual([
      'company',
      'uploadedStart',
      'uploadedEnd',
    ]);
    expect(filtersByTitle['派班记录表']).toEqual([
      'company',
      'department',
      'team',
      'dateStart',
      'dateEnd',
    ]);
    expect(filtersByTitle['个人积分榜单台账']).toEqual([
      'company',
      'department',
      'dateStart',
      'dateEnd',
    ]);
    expect(filtersByTitle['安全检查表']).toEqual([
      'checkUnit',
      'inspectedUnit',
      'dateStart',
      'dateEnd',
    ]);
    expect(filtersByTitle['生产安全事故隐患排查治理清单']).toEqual([
      'company',
      'uploadedStart',
      'uploadedEnd',
      'status',
    ]);
    expect(filtersByTitle['特殊作业台账']).toEqual([
      'company',
      'dateStart',
      'dateEnd',
    ]);
    expect(filtersByTitle['隐患整改']).toEqual([
      'company',
      'workshop',
      'team',
      'dateStart',
      'dateEnd',
    ]);
  });

  it('builds the internal ledger directory from the confirmed sections', () => {
    expect(safetyLedgerSections.map((section) => section.title)).toEqual([
      '安全生产规章制度与责任体系',
      '安全生产组织机构与资格证',
      '安全计划与总结',
      '安全风险分级管控',
      '安全生产宣教培训',
      '安全生产检查与隐患排查治理',
    ]);
    expect(safetyLedgerEntries).toHaveLength(27);
  });

  it('filters the internal directory by ledger keyword', () => {
    expect(
      filterSafetyLedgerNavigation(safetyLedgerSections, '积分').map(
        (section) => section.title,
      ),
    ).toEqual(['安全生产宣教培训']);
    expect(
      filterSafetyLedgerNavigation(safetyLedgerSections, '隐患').map(
        (section) => section.title,
      ),
    ).toEqual(['安全生产检查与隐患排查治理']);
    expect(
      filterSafetyLedgerNavigation(safetyLedgerSections, '派班')[0]?.children.map(
        (item) => item.title,
      ),
    ).toEqual(['派班记录表', '派班记录表']);
  });

  it('resolves ancestor route names for the current ledger entry', () => {
    expect(
      getSafetyLedgerAncestorRouteNames(
        safetyLedgerSections,
        'PinganSafetyLedgerHazardRectification',
      ),
    ).toEqual([
      'PinganSafetyLedgerHazardGovernance',
      'PinganSafetyLedgerHazardRectificationLedger',
    ]);
  });

  it('resolves the full title path for hierarchical navigation', () => {
    expect(
      getSafetyLedgerTitlePath(
        safetyLedgerSections,
        'PinganSafetyLedgerIndividualPointsRanking',
      ),
    ).toEqual(['安全生产宣教培训', '安全考试', '个人积分榜单台账']);
    expect(
      getSafetyLedgerTitlePath(
        safetyLedgerSections,
        'PinganSafetyLedgerHazardRectification',
      ),
    ).toEqual([
      '安全生产检查与隐患排查治理',
      '隐患整改台账',
      '隐患整改',
    ]);
  });
});
