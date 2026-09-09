import { beforeEach, describe, expect, it, vi } from 'vitest';

import { requestClient } from '#/api/request';

import {
  batchDeleteExamPapersApi,
  batchDeleteExamQuestionBankApi,
  batchDeleteExamResultsApi,
  batchDeleteExamTasksApi,
  confirmExamQuestionBankPdfApi,
  createExamPaperApi,
  createExamQuestionBankApi,
  createExamResultApi,
  createExamTaskApi,
  deleteExamPaperApi,
  deleteExamQuestionBankApi,
  deleteExamResultApi,
  deleteExamTaskApi,
  downloadExamQuestionBankApi,
  downloadExamQuestionBankTemplateApi,
  downloadExamQuestionTemplateApi,
  downloadExamTaskQuestionsApi,
  getExamQuestionBankApi,
  getExamPapersApi,
  getExamResultsApi,
  getExamResultDetailApi,
  getExamTaskDetailApi,
  getExamTaskResultsApi,
  getExamTasksApi,
  previewExamQuestionBankPdfApi,
  reviewExamResultApi,
  updateExamPaperApi,
  updateExamQuestionBankApi,
  uploadExamQuestionBankWorkbookApi,
  uploadExamQuestionWorkbookApi,
} from './training-exam';
import type { PinganTrainingExamApi } from './training-exam';

vi.mock('#/api/request', () => ({
  requestClient: {
    delete: vi.fn(),
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
  },
}));

describe('training-exam api', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('maps exam task and exam result endpoints', async () => {
    const taskPayload: PinganTrainingExamApi.ExamTaskPayload = {
      companyId: 4,
      departmentId: 101105,
      durationMinutes: 45,
      exam: '安全月考试',
      examDate: '2026-05-22',
      examPersonUserIds: [10005, 10006],
      questions: [
        {
          actualScore: 5,
          allOptions: 'A、B、C、D',
          answer: 'A',
          questionText: '安全生产的方针是？',
          questionType: '单选题',
          score: 5,
          selectedOption: 'A',
        },
      ],
      remark: '安全管理部上半年安全知识考试',
      status: 'ACTIVE',
    };
    const resultPayload = {
      examPersonUserId: 10006,
      score: 96.5,
      status: 'EXAMED',
      taskId: 12,
    } as const;

    await getExamTasksApi({
      companyId: 4,
      dateEnd: '2026-05-22',
      dateStart: '2026-05-01',
      departmentId: 101105,
      page: 1,
      pageSize: 20,
      status: 'ACTIVE',
    });
    await createExamTaskApi(taskPayload);
    await deleteExamTaskApi(12);
    await batchDeleteExamTasksApi([12, 13]);
    await getExamTaskDetailApi(12);
    await getExamTaskResultsApi(12);
    await getExamResultsApi({ taskId: 12, status: 'EXAMED' });
    await getExamResultDetailApi(99);
    await createExamResultApi(resultPayload);
    await reviewExamResultApi(99, {
      scores: [{ actualScore: 8, questionId: 501 }],
    });
    await deleteExamResultApi(99);
    await batchDeleteExamResultsApi([99, 100]);
    await downloadExamQuestionTemplateApi();
    const file = new File(['xlsx'], 'questions.xlsx');
    await uploadExamQuestionWorkbookApi(file);

    expect(requestClient.get).toHaveBeenNthCalledWith(
      1,
      '/pingan/training/exam-tasks',
      {
        params: {
          companyId: 4,
          dateEnd: '2026-05-22',
          dateStart: '2026-05-01',
          departmentId: 101105,
          page: 1,
          pageSize: 20,
          status: 'ACTIVE',
        },
      },
    );
    expect(requestClient.post).toHaveBeenNthCalledWith(
      1,
      '/pingan/training/exam-tasks',
      taskPayload,
    );
    expect(requestClient.delete).toHaveBeenNthCalledWith(
      1,
      '/pingan/training/exam-tasks/12',
    );
    expect(requestClient.post).toHaveBeenNthCalledWith(
      2,
      '/pingan/training/exam-tasks/batch-delete',
      { ids: [12, 13] },
    );
    expect(requestClient.get).toHaveBeenNthCalledWith(
      2,
      '/pingan/training/exam-tasks/12',
    );
    expect(requestClient.get).toHaveBeenNthCalledWith(
      3,
      '/pingan/training/exam-tasks/12/results',
      { params: undefined },
    );
    expect(requestClient.get).toHaveBeenNthCalledWith(
      4,
      '/pingan/training/exam-results',
      { params: { status: 'EXAMED', taskId: 12 } },
    );
    expect(requestClient.get).toHaveBeenNthCalledWith(
      5,
      '/pingan/training/exam-results/99',
    );
    expect(requestClient.post).toHaveBeenNthCalledWith(
      3,
      '/pingan/training/exam-results',
      resultPayload,
    );
    expect(requestClient.post).toHaveBeenNthCalledWith(
      4,
      '/pingan/training/exam-results/99/review',
      { scores: [{ actualScore: 8, questionId: 501 }] },
    );
    expect(requestClient.delete).toHaveBeenNthCalledWith(
      2,
      '/pingan/training/exam-results/99',
    );
    expect(requestClient.post).toHaveBeenNthCalledWith(
      5,
      '/pingan/training/exam-results/batch-delete',
      { ids: [99, 100] },
    );
    expect(requestClient.get).toHaveBeenNthCalledWith(
      6,
      '/pingan/training/exam-tasks/questions/template',
      { responseReturn: 'body', responseType: 'blob' },
    );
    expect(requestClient.post).toHaveBeenNthCalledWith(
      6,
      '/pingan/training/exam-tasks/questions/import',
      expect.any(FormData),
      { headers: { 'Content-Type': 'multipart/form-data' } },
    );
  });

  it('maps question bank downloads as raw blob responses', async () => {
    const bankPayload: PinganTrainingExamApi.ExamQuestionBankPayload = {
      answerExplanation: '安全生产坚持安全第一、预防为主、综合治理。',
      caseMaterial: '',
      children: [],
      companyId: 4,
      correctAnswers: ['A'],
      departmentId: 101105,
      options: [
        { content: '安全第一、预防为主、综合治理', key: 'A' },
        { content: '生产优先', key: 'B' },
        { content: '效益优先', key: 'C' },
        { content: '速度优先', key: 'D' },
      ],
      questionText: '安全生产的方针是？',
      questionType: 'SINGLE_CHOICE',
      referenceAnswer: '',
      score: 5,
    };
    const file = new File(['xlsx'], 'question-bank.xlsx');
    const pdf = new File(['%PDF-1.7'], 'question-bank.pdf', {
      type: 'application/pdf',
    });
    const pdfDraft: PinganTrainingExamApi.ExamQuestionBankPdfDraft = {
      answerExplanation: '解析',
      caseMaterial: '',
      children: [],
      correctAnswers: ['A'],
      options: bankPayload.options,
      questionText: 'PDF题目',
      questionType: 'SINGLE_CHOICE',
      questionTypeLabel: '单选题',
      referenceAnswer: '',
      score: 1,
      sourceLabel: '单选题 1',
    };

    await getExamQuestionBankApi({
      companyId: 4,
      departmentId: 101105,
      keyword: '安全',
      questionType: '单选题',
    });
    await createExamQuestionBankApi(bankPayload);
    await updateExamQuestionBankApi(7, bankPayload);
    await deleteExamQuestionBankApi(7);
    await downloadExamQuestionBankTemplateApi();
    await downloadExamQuestionBankApi({ companyId: 4, keyword: '安全' });
    await downloadExamTaskQuestionsApi(12);
    await uploadExamQuestionBankWorkbookApi(4, 101105, file);
    await previewExamQuestionBankPdfApi(pdf);
    await confirmExamQuestionBankPdfApi({
      companyId: 4,
      departmentId: 101105,
      questions: [pdfDraft],
      target: 'QUESTION_BANK',
    });
    await confirmExamQuestionBankPdfApi({
      questions: [pdfDraft],
      target: 'PAPER',
    });

    expect(requestClient.get).toHaveBeenNthCalledWith(
      1,
      '/pingan/training/exam-question-bank',
      {
        params: {
          companyId: 4,
          departmentId: 101105,
          keyword: '安全',
          questionType: '单选题',
        },
      },
    );
    expect(requestClient.post).toHaveBeenNthCalledWith(
      1,
      '/pingan/training/exam-question-bank',
      bankPayload,
    );
    expect(requestClient.put).toHaveBeenCalledWith(
      '/pingan/training/exam-question-bank/7',
      bankPayload,
    );
    expect(requestClient.delete).toHaveBeenCalledWith(
      '/pingan/training/exam-question-bank/7',
    );
    expect(requestClient.get).toHaveBeenNthCalledWith(
      2,
      '/pingan/training/exam-question-bank/template',
      { responseReturn: 'body', responseType: 'blob' },
    );
    expect(requestClient.get).toHaveBeenNthCalledWith(
      3,
      '/pingan/training/exam-question-bank/export',
      {
        params: { companyId: 4, keyword: '安全' },
        responseReturn: 'body',
        responseType: 'blob',
      },
    );
    expect(requestClient.get).toHaveBeenNthCalledWith(
      4,
      '/pingan/training/exam-tasks/12/questions/export',
      { responseReturn: 'body', responseType: 'blob' },
    );
    expect(requestClient.post).toHaveBeenNthCalledWith(
      2,
      '/pingan/training/exam-question-bank/import',
      expect.any(FormData),
      { headers: { 'Content-Type': 'multipart/form-data' } },
    );
    expect(requestClient.post).toHaveBeenNthCalledWith(
      3,
      '/pingan/training/exam-question-bank/pdf/preview',
      expect.any(FormData),
      { headers: { 'Content-Type': 'multipart/form-data' } },
    );
    expect(requestClient.post).toHaveBeenNthCalledWith(
      4,
      '/pingan/training/exam-question-bank/pdf/confirm',
      {
        companyId: 4,
        departmentId: 101105,
        questions: [pdfDraft],
        target: 'QUESTION_BANK',
      },
    );
    expect(requestClient.post).toHaveBeenNthCalledWith(
      5,
      '/pingan/training/exam-question-bank/pdf/confirm',
      {
        questions: [pdfDraft],
        target: 'PAPER',
      },
    );
  });

  it('maps reusable exam paper and question bank batch-delete endpoints', async () => {
    const paperPayload: PinganTrainingExamApi.ExamPaperPayload = {
      companyId: 4,
      departmentId: 101105,
      description: '安全月复用试卷',
      paperName: '安全月 A 卷',
      questions: [
        {
          actualScore: 0,
          allOptions: 'A、B、C、D',
          answer: 'A',
          questionText: '安全生产方针是？',
          questionType: '单选题',
          score: 5,
          selectedOption: '',
        },
      ],
      teamId: 1011001,
    };

    await getExamPapersApi({
      companyId: 4,
      departmentId: 101105,
      keyword: '安全月',
      teamId: 1011001,
    });
    await createExamPaperApi(paperPayload);
    await updateExamPaperApi(8, paperPayload);
    await deleteExamPaperApi(8);
    await batchDeleteExamPapersApi([8, 9]);
    await batchDeleteExamQuestionBankApi([10, 11]);

    expect(requestClient.get).toHaveBeenCalledWith(
      '/pingan/training/exam-papers',
      {
        params: {
          companyId: 4,
          departmentId: 101105,
          keyword: '安全月',
          teamId: 1011001,
        },
      },
    );
    expect(requestClient.post).toHaveBeenCalledWith(
      '/pingan/training/exam-papers',
      paperPayload,
    );
    expect(requestClient.put).toHaveBeenCalledWith(
      '/pingan/training/exam-papers/8',
      paperPayload,
    );
    expect(requestClient.delete).toHaveBeenCalledWith(
      '/pingan/training/exam-papers/8',
    );
    expect(requestClient.post).toHaveBeenCalledWith(
      '/pingan/training/exam-papers/batch-delete',
      { ids: [8, 9] },
    );
    expect(requestClient.post).toHaveBeenCalledWith(
      '/pingan/training/exam-question-bank/batch-delete',
      { ids: [10, 11] },
    );
  });
});
