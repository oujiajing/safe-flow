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

describe('training-exam page', () => {
  it('uses the shared data-map shell and fixed filter order', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/training-exam/index.vue'),
      'utf8',
    );

    expect(page).toContain(
      "import DataMapPanel from '#/views/pingan/shared/DataMapPanel.vue'",
    );
    expect(page).toContain('canShowPinganDataMap');
    expect(page).toContain('useUserStore');
    expect(page).toContain('v-if="shouldShowDataMap"');
    expect(page).toContain('getPinganCompanyOrgTreeApi');
    expect(page).toContain('getPinganOrgTreeApi');
    expect(page).toContain('await getPinganCompanyOrgTreeApi()');
    expect(page).toContain('await getPinganOrgTreeApi()');
    expect(page).toContain('<DataMapPanel');
    expect(page).toContain(':tree-data="organizationNodes"');
    expect(page).toContain('@select="handleDataMapSelect"');

    const labels = [
      '<span>公司</span>',
      '<span>部门</span>',
      '<span>日期（起）</span>',
      '<span>日期（止）</span>',
      '<span>状态</span>',
    ];
    expect(labels.map((label) => page.indexOf(label))).toEqual(
      [...labels.map((label) => page.indexOf(label))].sort((a, b) => a - b),
    );

    expect(page).toContain('training-exam-workbench');
    expect(page).toContain('批量删除');
    expect(page).toContain(':disabled="!canBatchDelete"');
    expect(page).toMatch(
      /\.workbench__bar \.filter-item :deep\(\.ant-select-selection-item\),[\s\S]*align-items: center;/,
    );
    expect(page).toMatch(
      /\.workbench__bar \.filter-item :deep\(\.ant-picker-input > input\)[\s\S]*line-height: normal !important;/,
    );
  });

  it('loads departments from the full organization tree instead of the company navigation tree', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/training-exam/index.vue'),
      'utf8',
    );

    expect(page).toContain(
      'const organizationOptionNodes = ref<DataMapNode[]>([])',
    );
    expect(page).toContain(
      'return flattenDepartments(organizationOptionNodes.value)',
    );
    expect(page).toContain('organizationOptionNodes.value,');
  });

  it('scopes list organization filters by the current user role and organization', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/training-exam/index.vue'),
      'utf8',
    );

    expect(page).toContain('getVisiblePinganOrganizationFilterKeys');
    expect(page).toContain('isPinganOrganizationFilterLocked');
    expect(page).toContain('resolveScopedPinganOrganizationDefaults');
    expect(page).toContain('isOrganizationFilterVisible');
    expect(page).toContain('isOrganizationFieldLocked');
    expect(page).toContain('enforceUserOrganizationScope');
    expect(page).toContain('v-if="isOrganizationFilterVisible(\'company\')"');
    expect(page).toContain(
      'v-if="isOrganizationFilterVisible(\'department\')"',
    );
  });

  it('defines exam task and result columns with task detail behavior', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/training-exam/index.vue'),
      'utf8',
    );

    for (const text of [
      "title: '公司'",
      "title: '部门'",
      "title: '考试'",
      "title: '考试日期'",
      "title: '状态'",
      "title: '创建时间'",
      "title: '操作'",
      "title: '考试人员'",
      "title: '考分'",
      'getExamTaskResultsApi',
      'getExamTaskDetailApi',
      'getExamResultDetailApi',
      '考试任务明细',
      '人员考试明细',
      '明细',
    ]) {
      expect(page).toContain(text);
    }
  });

  it('renders same-workbench task creation and nested detail views', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/training-exam/index.vue'),
      'utf8',
    );

    for (const text of [
      '新增考试任务',
      'viewMode',
      'createTask',
      'taskDetail',
      'resultDetail',
      '考试任务明细',
      '人员考试明细',
      'formatDisplayDateTime',
      'formatDisplayDateTime(record.createdAt)',
      'formatDisplayDateTime(taskDetail?.createdAt)',
      '基础信息',
      '考题',
      '返回',
      '草稿',
      '创建',
      '下载模板',
      '上传数据',
      'v-model:value="taskForm.exam"',
      'v-model:value="taskForm.examDate"',
      'v-model:value="taskForm.durationMinutes"',
      'v-model:value="taskForm.remark"',
      'questionColumns',
      'departmentDetailColumns',
      'openResultDetail',
    ]) {
      expect(page).toContain(text);
    }
    expect(page).not.toContain('v-model:value="taskForm.examPersonUserIds"');
  });

  it('expands task targets by company, department, or team and scopes bank selection', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/training-exam/index.vue'),
      'utf8',
    );
    const api = readFileSync(
      appSourcePath('src/api/pingan/training-exam.ts'),
      'utf8',
    );

    for (const text of [
      'teamsForDepartment',
      'v-model:value="taskForm.teamId"',
      '不选则公司全部部门',
      '不选则部门全部班组',
      '考试人员将在创建任务时按所选公司、部门或班组自动生成。',
      'v-model:value="questionBankFilters.teamId"',
      'v-model:value="questionBankForm.teamId"',
      'applicable: questionBankOpen.value || undefined',
      'teamId: questionBankFilters.teamId || undefined',
      'teamId: questionBankFilters.teamId',
    ]) {
      expect(page).toContain(text);
    }
    for (const text of [
      'applicable?: boolean',
      'teamId?: Id',
      "target?: 'PAPER' | 'QUESTION_BANK'",
    ]) {
      expect(api).toContain(text);
    }
  });

  it('locks task and question-bank organization fields to the current role scope', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/training-exam/index.vue'),
      'utf8',
    );

    expect(page).toContain('applyScopedPinganOrganizationDefaults');
    expect(page).toContain('function enforceTaskFormOrganizationScope()');
    expect(page).toContain(
      'function enforceQuestionBankFormOrganizationScope()',
    );
    expect(page).toMatch(
      /async function saveTask\(status: PinganTrainingExamApi\.TaskStatus\) \{[\s\S]*enforceTaskFormOrganizationScope\(\);/,
    );
    expect(page).toMatch(
      /async function saveQuestionBankRecord\(\) \{[\s\S]*enforceQuestionBankFormOrganizationScope\(\);/,
    );
    expect(page).toContain(
      ':disabled="isOrganizationFieldLocked(\'company\')"',
    );
    expect(page).toContain(
      ':disabled="isOrganizationFieldLocked(\'department\')"',
    );
  });

  it('derives new exam result status from score instead of a manual selector', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/training-exam/index.vue'),
      'utf8',
    );

    expect(page).toContain('derivedResultStatus');
    expect(page).toContain('derivedResultStatusLabel');
    expect(page).toContain('status: derivedResultStatus.value');
    expect(page).not.toContain('resultCreateStatusOptions');
    expect(page).not.toContain('v-model:value="resultForm.status"');
  });

  it('wires independent question bank management into exam tasks', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/training-exam/index.vue'),
      'utf8',
    );
    const api = readFileSync(
      appSourcePath('src/api/pingan/training-exam.ts'),
      'utf8',
    );

    for (const text of [
      'getExamQuestionBankApi',
      'createExamQuestionBankApi',
      'updateExamQuestionBankApi',
      'deleteExamQuestionBankApi',
      'downloadExamQuestionBankTemplateApi',
      'downloadExamQuestionBankApi',
      'uploadExamQuestionBankWorkbookApi',
      'previewExamQuestionBankPdfApi',
      'confirmExamQuestionBankPdfApi',
      'downloadExamTaskQuestionsApi',
    ]) {
      expect(api).toContain(text);
      expect(page).toContain(text);
    }

    expect(page).not.toContain('class="flow-steps"');
    expect(page).not.toContain("goToFlowStep('list')");

    for (const text of [
      '题库管理',
      '从题库选择',
      '下载试题',
      'questionBankOpen',
      'questionBankRows',
      'selectedQuestionBankRowKeys',
      'appendQuestionBankSelection',
      "viewMode.value = 'questionBank'",
      "openQuestionBankSelector('task')",
      'questionBankTarget.value',
      'PDF 试题解析预览',
      '确认导入题库',
      'openQuestionBankPdfDraft',
      'removeQuestionBankPdfDraft',
    ]) {
      expect(page).toContain(text);
    }
  });

  it('renders paper and question-bank child pages with reusable paper composition and deletion controls', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/training-exam/index.vue'),
      'utf8',
    );
    const api = readFileSync(
      appSourcePath('src/api/pingan/training-exam.ts'),
      'utf8',
    );

    for (const text of [
      "viewMode === 'papers'",
      "viewMode === 'paperEdit'",
      "viewMode === 'questionBank'",
      "viewMode === 'questionBankEdit'",
      '试卷管理',
      '题库管理',
      '新建试卷',
      '带入试卷',
      '从题库选择',
      "batchRemoveQuestions('task')",
      "batchRemoveQuestions('paper')",
      'batchDeleteQuestionBank',
      'selectedRows',
      'row-key="id"',
      'batchDeletePapers',
      'deleteQuestionBank',
      'removeQuestion',
      '不选则全局共享',
      '不选则公司内共享',
      'class="training-exam-form paper-form"',
      'questionTypeChinese',
      'paperAppliesToTask',
      'v-model:value="paperFilters.teamId"',
      'v-model:value="paperForm.teamId"',
      'closePaperEditor',
      'closeQuestionBankEditor',
      'questionBankEditorReturnMode',
      'PDF 解析导入',
      '导入当前试卷',
      'handlePaperPdf',
      "pdfImportTarget.value === 'paper'",
      "target: 'PAPER'",
      'result.paperQuestions',
      '保存试卷后生效',
      'paperEditableQuestionColumns',
      'paper-question-table-region',
      'paper-question-table',
      'paper-cell-textarea',
      'paper-options-editor',
      'paper-case-inline-editor',
      'expandedPaperQuestionRowKeys',
      ':expanded-row-keys="expandedPaperQuestionRowKeys"',
      '@expand="handlePaperQuestionExpand"',
      'handlePaperQuestionExpand',
      'resetPaperCaseExpansion',
      'togglePaperCaseEditor',
      'paperCaseExpanded',
      ':show-expand-column="false"',
      'paper-scope-card',
      'paper-question-card',
      '.paper-editor__toolbar b',
      'font-size: 16px',
      'paper-cell-textarea--fill',
      'max-height: clamp(220px, calc(100vh - 470px), 560px)',
      'v-model="record.questionText"',
      'v-model:value="option.content"',
      'v-model:value="record.correctAnswers"',
      'syncPaperQuestionLegacyFields',
      'paperQuestionRowExpandable',
      "dataIndex: 'questionNo'",
      "title: '题号'",
      'class="paper-question-no"',
      '{{ index + 1 }}',
      'resetQuestionBankManagementFilters',
      'class="subpage-heading"',
      'class="subpage-back"',
      'icon="lucide:arrow-left"',
      'table-layout="fixed"',
      'class="subpage-filter__actions"',
    ]) {
      expect(page).toContain(text);
    }
    expect(page).not.toContain('v-model:open="paperEditOpen"');
    expect(page).not.toContain('v-model:open="questionBankEditOpen"');
    expect(page).not.toContain('<h2>{{ paperEditorTitle }}</h2>');
    expect(page).not.toContain('设置试卷适用范围并维护当前试卷考题');
    expect(page).not.toContain(':row-key="(record) => String(record.id)"');
    expect(page).toMatch(
      /function resetPaperCaseExpansion\(\)\s*\{\s*expandedPaperQuestionRowKeys\.value = \[\]/,
    );
    const paperToolbar = page.slice(
      page.indexOf('class="paper-editor__toolbar"'),
      page.indexOf('ref="paperPdfFileInput"'),
    );
    expect(paperToolbar).toContain('aria-label="返回试卷管理"');
    expect(paperToolbar).not.toContain('>返回试卷管理</Button>');
    expect(paperToolbar.indexOf('保存试卷')).toBeLessThan(
      paperToolbar.indexOf('批量删除'),
    );
    const papersPage = page.slice(
      page.indexOf(`<div v-else-if="viewMode === 'papers'"`),
      page.indexOf(`<div v-else-if="viewMode === 'questionBank'"`),
    );
    const questionBankPage = page.slice(
      page.indexOf(`<div v-else-if="viewMode === 'questionBank'"`),
      page.indexOf(`v-else-if="viewMode === 'createTask'"`),
    );
    expect(papersPage).not.toContain('<Button @click="goBack">返回</Button>');
    expect(questionBankPage).not.toContain(
      '<Button @click="goBack">返回</Button>',
    );
    const questionBankColumnConfig = page.slice(
      page.indexOf('const questionBankColumns'),
      page.indexOf('const questionBankPdfColumns'),
    );
    expect(questionBankColumnConfig).not.toContain("dataIndex: 'company'");
    expect(questionBankColumnConfig).not.toContain("dataIndex: 'department'");
    expect(questionBankColumnConfig).not.toContain("dataIndex: 'team'");
    expect(page).not.toContain(
      `:scroll="{ x: 1280, y: 'calc(100vh - 410px)' }"`,
    );
    const questionBankToolbar = page.slice(
      page.indexOf('class="question-bank-toolbar"'),
      page.indexOf('ref="questionBankFileInput"'),
    );
    expect(questionBankToolbar.indexOf('新建试题')).toBeLessThan(
      questionBankToolbar.indexOf('批量删除'),
    );
    expect(page).not.toContain(
      '公司、部门、班组均不选择时为全局共享；选择到哪一级，试卷就共享到对应组织范围。',
    );
    for (const text of [
      'getExamPapersApi',
      'createExamPaperApi',
      'updateExamPaperApi',
      'deleteExamPaperApi',
      'batchDeleteExamPapersApi',
      'batchDeleteExamQuestionBankApi',
    ]) {
      expect(api).toContain(text);
      expect(page).toContain(text);
    }
  });

  it('previews and confirms PDF question-bank imports before writing data', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/training-exam/index.vue'),
      'utf8',
    );
    const api = readFileSync(
      appSourcePath('src/api/pingan/training-exam.ts'),
      'utf8',
    );

    for (const text of [
      'ExamQuestionBankPdfDraft',
      'ExamQuestionBankPdfPreview',
      'previewExamQuestionBankPdfApi',
      'confirmExamQuestionBankPdfApi',
      '/exam-question-bank/pdf/preview',
      '/exam-question-bank/pdf/confirm',
    ]) {
      expect(api).toContain(text);
    }
    for (const text of [
      'triggerQuestionBankPdfUpload',
      'handleQuestionBankPdf',
      'questionBankPdfPreview',
      'questionBankPdfPreview.value = await previewExamQuestionBankPdfApi(file)',
      'PDF 解析',
      '校正 PDF 解析草稿',
      'questionBankPdfPreview.warnings',
      'confirmQuestionBankPdf',
      "target: 'QUESTION_BANK'",
      "target: 'PAPER'",
    ]) {
      expect(page).toContain(text);
    }
    expect(page).not.toContain(
      'await confirmExamQuestionBankPdfApi' +
        '(questionBankPdfPreview.value.questions)',
    );
  });

  it('provides structured custom question editors for all supported bank types', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/training-exam/index.vue'),
      'utf8',
    );
    const api = readFileSync(
      appSourcePath('src/api/pingan/training-exam.ts'),
      'utf8',
    );

    for (const text of [
      'SINGLE_CHOICE',
      'MULTIPLE_CHOICE',
      'SHORT_ANSWER',
      'CASE_ANALYSIS',
      '选项（固定 A-D）',
      '选项（A-D，可兼容 A-E）',
      'question-bank-org-row',
      'grid-template-columns: repeat(3, minmax(0, 1fr))',
      '正确答案',
      '参考答案',
      '答案解析',
      '案例材料',
      '案例子题（可混合单选、多选和问答）',
      'addQuestionBankChild',
      'removeQuestionBankChild',
      'questionBankValidationMessage',
      'answerExplanation: question.answerExplanation',
      'caseMaterial: question.caseMaterial',
      'children: question.children.map',
      'correctAnswers: [...question.correctAnswers]',
      'options: question.options.map',
      '完成人工评分',
      'submitResultReview',
      'reviewExamResultApi',
      'PENDING_REVIEW',
      ':aria-label="questionBankEditorReturnLabel"',
      ':title="questionBankEditorReturnLabel"',
      'class="question-bank-title-field"',
      "questionBankForm.questionType !== 'CASE_ANALYSIS'",
    ]) {
      expect(page).toContain(text);
    }
    for (const text of [
      'ExamQuestionBankChild',
      'ExamQuestionOption',
      'correctAnswers',
      'answerExplanation',
      'caseMaterial',
      'children',
    ]) {
      expect(api).toContain(text);
    }
    expect(page).not.toContain('v-model:value="questionBankForm.actualScore"');
    expect(page).not.toContain('v-model:value="questionBankForm.allOptions"');
    const questionBankEditorHeader = page.slice(
      page.indexOf(`v-if="viewMode === 'questionBankEdit'"`),
      page.indexOf(
        'class="training-exam-form training-exam-form--question-bank"',
      ),
    );
    expect(questionBankEditorHeader).toContain(
      '@click="closeQuestionBankEditor"',
    );
    expect(questionBankEditorHeader).not.toContain(
      '{{ questionBankEditorReturnLabel }}',
    );
  });

  it('gates task and result actions by submodule operation permissions', () => {
    const page = readFileSync(
      appSourcePath('src/views/pingan/training-exam/index.vue'),
      'utf8',
    );

    for (const text of [
      'useAccessStore',
      'useUserStore',
      'PINGAN_TRAINING_EXAM_TASKS_CREATE',
      'PINGAN_TRAINING_EXAM_TASKS_DELETE',
      'PINGAN_TRAINING_EXAM_TASKS_DOWNLOAD',
      'PINGAN_TRAINING_EXAM_RESULTS_CREATE',
      'PINGAN_TRAINING_EXAM_RESULTS_DELETE',
      'canCreateCurrent',
      'canDeleteCurrent',
      'canDownloadExamTasks',
      'selectedRowKeys.value.length > 0 && canDeleteCurrent.value',
      '<Button :disabled="!canBatchDelete" v-if="canDeleteCurrent" danger @click="batchDeleteRecords">',
      'v-if="isTaskMode && canCreateExamTasks"',
      '@click="openQuestionBankPage"',
      '<Button v-if="canCreateCurrent" type="primary" @click="openCreateEntry">',
      '<Button v-if="canDeleteExamTasks" danger size="small" type="link" @click="deleteTask(asTask(record))">',
      '<Button v-if="canDeleteExamResults" danger size="small" type="link" @click="deleteResult(asResult(record))">',
      '<Button v-if="canDownloadExamTasks" size="small" @click="downloadQuestionTemplate">',
      '<Button v-if="canCreateExamTasks" size="small" @click="triggerQuestionUpload">',
      '<Button v-if="canCreateExamTasks" size="small" type="primary" @click="addQuestion">',
      '<Button v-if="canDownloadExamTasks" @click="downloadTaskQuestions">',
      '<Button v-if="canCreateExamTasks" type="primary" @click="openQuestionBankCreate">',
      '<Button v-if="canDownloadExamTasks" @click="downloadQuestionBankTemplate">',
      '<Button v-if="canCreateExamTasks" @click="triggerQuestionBankUpload">',
      '<Button v-if="canCreateExamTasks" :loading="questionBankPdfLoading" @click="triggerQuestionBankPdfUpload">',
      '<Button v-if="canDownloadExamTasks" @click="downloadQuestionBank">',
      '<Button v-if="canCreateExamTasks" size="small" type="link" @click="openQuestionBankEdit(asQuestionBank(record))">',
      '<Button v-if="canDeleteExamTasks" danger size="small" type="link" @click="deleteQuestionBank(asQuestionBank(record))">',
    ]) {
      const normalizedPage = page.replace(/\s+/g, ' ').replace(/\s+>/g, '>');
      const normalizedText = text.replace(/\s+/g, ' ').replace(/\s+>/g, '>');
      expect(normalizedPage).toContain(normalizedText);
    }
  });
});
