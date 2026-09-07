import { readFileSync } from 'node:fs';
import { join } from 'node:path';

import { describe, expect, it } from 'vitest';

function appSourcePath(path: string) {
  return join(
    process.cwd(),
    process.cwd().endsWith(join('apps', 'web-antd'))
      ? path
      : `apps/web-antd/${path}`,
  );
}

describe('safety-learning page', () => {
  it('uses company data map and fixed safety learning filter order', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/safety-learning/index.vue'),
      'utf8',
    );

    expect(page).toContain("import DataMapPanel from '#/views/pingan/shared/DataMapPanel.vue'");
    expect(page).toContain('canShowPinganDataMap');
    expect(page).toContain('useUserStore');
    expect(page).toContain('v-if="shouldShowDataMap"');
    expect(page).toContain('getPinganCompanyOrgTreeApi');
    expect(page).toContain('getPinganOrgTreeApi');
    expect(page).toContain('Promise.all([');
    expect(page).toContain('getPinganCompanyOrgTreeApi(),');
    expect(page).toContain('getPinganOrgTreeApi(),');
    expect(page).toContain('organizationScopeNodes');
    expect(page).toContain('<DataMapPanel');
    expect(page).toContain(':tree-data="organizationNodes"');
    expect(page).toContain('@select="handleDataMapSelect"');
    expect(page).toContain('safety-learning-workbench');
    expect(page).toContain(':global(.pingan-page)');
    expect(page).toMatch(/\.safety-learning-shell\s*\{[\s\S]*height:\s*100%;[\s\S]*min-height:\s*0;/);
    expect(page).toMatch(/\.safety-learning-workbench\s*\{[\s\S]*min-height:\s*0;[\s\S]*overflow:\s*hidden;/);

    expect(page).toContain('getVisiblePinganOrganizationFilterKeys');
    expect(page).toContain('isPinganOrganizationFilterLocked');
    expect(page).toContain('resolveScopedPinganOrganizationDefaults');
    expect(page).toContain('isCompanyFilterVisible');
    expect(page).toContain('isCompanyFilterLocked');
    expect(page).toContain('enforceCompanyScope');
    expect(page).toContain('v-if="isCompanyFilterVisible"');

    const labels = [
      '<span>学习内容类型</span>',
      '<span>公司</span>',
      '<span>关键词</span>',
      '<span>日期（起）</span>',
      '<span>日期（止）</span>',
      '<span>状态</span>',
    ];
    expect(labels.map((label) => page.indexOf(label))).toEqual(
      [...labels.map((label) => page.indexOf(label))].sort((a, b) => a - b),
    );
    expect(page).toMatch(
      /\.workbench__bar \.filter-item :deep\(\.ant-select-selection-item\),[\s\S]*align-items: center;/,
    );
    expect(page).toMatch(
      /\.workbench__bar \.filter-item :deep\(\.ant-select-selection-placeholder\)[\s\S]*line-height: normal !important;/,
    );
  });

  it('keeps template column order and table actions', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/safety-learning/index.vue'),
      'utf8',
    );

    for (const text of [
      "title: '公司'",
      "title: '分类'",
      "title: '标题'",
      "title: '内容'",
      "title: '封面图'",
      "title: '视频'",
      "title: '日期'",
      "title: '计时'",
      "title: '编码'",
      "title: '附件'",
      "title: 'HTML提取'",
      "title: '草稿'",
      "title: '状态'",
      "title: '操作'",
      '新增',
      '下载模板',
      '下载数据',
      '上传数据',
      '批量删除',
      ':disabled="!canBatchDelete"',
      '编辑',
      '删除',
    ]) {
      expect(page).toContain(text);
    }
  });

  it('locks create and edit company fields to the current role scope', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/safety-learning/index.vue'),
      'utf8',
    );

    expect(page).toContain('applyScopedPinganOrganizationDefaults');
    expect(page).toContain('function enforceFormCompanyScope()');
    expect(page).toMatch(
      /async function saveRecord\(\) \{[\s\S]*enforceFormCompanyScope\(\);/,
    );
    expect(page).toContain(':disabled="isCompanyFilterLocked"');
  });

  it('gates safety learning actions by submodule operation permissions', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/safety-learning/index.vue'),
      'utf8',
    );

    for (const text of [
      'useAccessStore',
      'useUserStore',
      'PINGAN_TRAINING_SAFETY_LEARNING_CREATE',
      'PINGAN_TRAINING_SAFETY_LEARNING_DELETE',
      'PINGAN_TRAINING_SAFETY_LEARNING_VOID',
      'PINGAN_TRAINING_SAFETY_LEARNING_DOWNLOAD',
      'canCreateSafetyLearning',
      'canDeleteSafetyLearning',
      'canVoidSafetyLearning',
      'canDownloadSafetyLearning',
      'selectedRowKeys.value.length > 0 && canDeleteSafetyLearning.value',
      '<Button :disabled="!canBatchDelete" v-if="canDeleteSafetyLearning" danger @click="batchDeleteRecords">',
      '<Button v-if="canCreateSafetyLearning" type="primary" @click="openCreateModal">',
      '<Button v-if="canDownloadSafetyLearning" @click="downloadTemplate">',
      '<Button v-if="canDownloadSafetyLearning" @click="downloadRecords">',
      '<Button v-if="canCreateSafetyLearning" @click="chooseImportFile">',
      '<Button v-if="canEditSafetyLearning" size="small" type="link" @click="openEditModal(asSafetyLearningContent(record))">',
      '<Button v-if="canDeleteSafetyLearning" danger size="small" type="link" @click="deleteRecord(asSafetyLearningContent(record))">',
      '<Button v-if="canDeleteSafetyLearning" aria-label="删除封面图"',
      '<Button v-if="canCreateSafetyLearning" @click="chooseCoverImage">',
      '<Button v-if="canDeleteSafetyLearning" aria-label="删除视频"',
      '<Button v-if="canCreateSafetyLearning" @click="chooseVideo">',
      '<Button v-if="canDeleteSafetyLearning" aria-label="删除附件"',
      '<Button v-if="canCreateSafetyLearning" @click="chooseAttachment">',
    ]) {
      expect(page).toContain(text);
    }
  });

  it('places PDF attachment upload at form bottom and supports table link preview', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/safety-learning/index.vue'),
      'utf8',
    );

    expect(page.indexOf('<span>状态</span>')).toBeLessThan(
      page.indexOf('class="safety-learning-attachment-field"'),
    );
    expect(page).toContain('accept=".pdf,application/pdf"');
    expect(page).toContain('pendingAttachment');
    expect(page).toContain('uploadSafetyLearningAttachmentApi');
    expect(page).toContain('removeSafetyLearningAttachmentApi');
    expect(page).toContain('removeCurrentAttachment');
    expect(page).toContain('class="attachment-card__remove"');
    expect(page).toContain('lucide:x');
    expect(page).toContain('openAttachment');
    expect(page).toContain('record.attachment?.url');
    expect(page).toContain('lucide:file-up');
  });

  it('supports cover image and video upload cards instead of plain text fields', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/safety-learning/index.vue'),
      'utf8',
    );

    expect(page).toContain('pendingCoverImage');
    expect(page).toContain('pendingVideo');
    expect(page).toContain('currentCoverImageAttachment');
    expect(page).toContain('currentVideoAttachment');
    expect(page).toContain('uploadSafetyLearningCoverImageApi');
    expect(page).toContain('uploadSafetyLearningVideoApi');
    expect(page).toContain('removeSafetyLearningCoverImageApi');
    expect(page).toContain('removeSafetyLearningVideoApi');
    expect(page).toContain('accept="image/*"');
    expect(page).toContain('accept="video/*"');
    expect(page).toContain('chooseCoverImage');
    expect(page).toContain('chooseVideo');
    expect(page).toContain('removeCurrentCoverImage');
    expect(page).toContain('removeCurrentVideo');
    expect(page).toContain('class="media-card media-card--cover"');
    expect(page).toContain('class="media-card media-card--video"');
    expect(page).toContain('<img');
    expect(page).toContain('<video');
    expect(page).not.toContain('支持图片上传，列表和编辑中可预览');
    expect(page).not.toContain('支持视频上传，保存后关联到当前内容');
    expect(page).toMatch(/\.media-card\s*\{[\s\S]*grid-template-columns:\s*168px minmax\(0,\s*1fr\) auto;/);
    expect(page).toContain('mediaPreviewOpen');
    expect(page).toContain('openMediaPreview');
    expect(page).toContain('closeMediaPreview');
    expect(page).toContain('v-model:open="mediaPreviewOpen"');
    expect(page).toContain('media-preview__image');
    expect(page).toContain('media-preview__video');
    expect(page).toContain("@click=\"openMediaPreview('IMAGE', record.coverImageAttachment)\"");
    expect(page).toContain("@click=\"openMediaPreview('VIDEO', record.videoAttachment)\"");
    expect(page).toContain("@click=\"openMediaPreviewFromUrl('IMAGE'");
    expect(page).toContain("@click=\"openMediaPreviewFromUrl('VIDEO'");
    expect(page).toMatch(/\.media-card__preview\s*\{[\s\S]*width:\s*168px;[\s\S]*height:\s*168px;/);
  });
});
