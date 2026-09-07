import { requestClient } from '#/api/request';

export namespace PinganTrainingExamApi {
  export type Id = number | string;
  export type TaskStatus = 'ACTIVE' | 'INACTIVE';
  export type ResultStatus = 'EXAMED' | 'PENDING_EXAM' | 'PENDING_REVIEW';
  export type QuestionType =
    | 'CASE_ANALYSIS'
    | 'MULTIPLE_CHOICE'
    | 'SHORT_ANSWER'
    | 'SINGLE_CHOICE';

  export interface ExamQuestionOption {
    content: string;
    key: string;
  }

  export interface ExamQuestionBankChild {
    answerExplanation: string;
    correctAnswers: string[];
    options: ExamQuestionOption[];
    questionText: string;
    questionType: Exclude<QuestionType, 'CASE_ANALYSIS'>;
    questionTypeLabel?: string;
    referenceAnswer: string;
    score: number;
  }

  export interface PageResult<T> {
    items: T[];
    total: number;
  }

  export interface ListParams {
    companyId?: Id;
    dateEnd?: string;
    dateStart?: string;
    departmentId?: Id;
    page?: number;
    pageSize?: number;
    status?: string;
    taskId?: Id;
  }

  export interface QuestionBankListParams {
    applicable?: boolean;
    companyId?: Id;
    departmentId?: Id;
    keyword?: string;
    page?: number;
    pageSize?: number;
    questionType?: string;
    teamId?: Id;
  }

  export interface PaperListParams {
    companyId?: Id;
    departmentId?: Id;
    keyword?: string;
    page?: number;
    pageSize?: number;
    teamId?: Id;
  }

  export interface ExamTask {
    code?: string;
    company: string;
    companyId: Id;
    createdAt: string;
    department: string;
    departmentId?: Id;
    durationMinutes: number;
    exam: string;
    examDate: string;
    id: Id;
    remark?: string;
    status: TaskStatus;
    statusLabel: string;
    team: string;
    teamId?: Id;
    updatedAt: string;
  }

  export interface ExamQuestion {
    actualScore: number;
    allOptions: string;
    answer: string;
    answerExplanation?: string;
    caseMaterial?: string;
    children?: ExamQuestionBankChild[];
    correctAnswers?: string[];
    id?: Id;
    options?: ExamQuestionOption[];
    questionText: string;
    questionType: string;
    questionTypeLabel?: string;
    referenceAnswer?: string;
    score: number;
    selectedOption: string;
    sortOrder?: number;
  }

  export interface ExamQuestionBank extends ExamQuestion {
    answerExplanation: string;
    caseMaterial: string;
    children: ExamQuestionBankChild[];
    company: string;
    companyId: Id;
    createdAt: string;
    department: string;
    departmentId?: Id;
    id: Id;
    correctAnswers: string[];
    options: ExamQuestionOption[];
    questionTypeLabel: string;
    referenceAnswer: string;
    team: string;
    teamId?: Id;
    updatedAt: string;
  }

  export interface ExamQuestionBankPayload {
    answerExplanation: string;
    caseMaterial: string;
    children: ExamQuestionBankChild[];
    companyId: Id;
    correctAnswers: string[];
    departmentId?: Id;
    options: ExamQuestionOption[];
    questionText: string;
    questionType: QuestionType;
    referenceAnswer: string;
    score: number;
    teamId?: Id;
  }

  export interface ExamReviewPayload {
    scores: {
      actualScore: number;
      questionId: Id;
    }[];
  }

  export interface ExamQuestionBankPdfDraft extends Omit<
    ExamQuestionBankPayload,
    'companyId' | 'departmentId' | 'teamId'
  > {
    questionTypeLabel: string;
    sourceLabel: string;
  }

  export interface ExamQuestionBankPdfPreview {
    characterCount: number;
    errors: string[];
    filename: string;
    pageCount: number;
    previewId: string;
    questions: ExamQuestionBankPdfDraft[];
    warnings: string[];
  }

  export interface ExamQuestionBankPdfConfirmPayload {
    companyId?: Id;
    departmentId?: Id;
    questions: ExamQuestionBankPdfDraft[];
    target?: 'PAPER' | 'QUESTION_BANK';
    teamId?: Id;
  }

  export interface ExamQuestionBankPdfConfirmResult {
    errors: string[];
    paperQuestions: ExamQuestion[];
    questions: ExamQuestionBank[];
    successRows: number;
    target: 'PAPER' | 'QUESTION_BANK';
  }

  export interface ExamTaskPayload {
    companyId: Id;
    departmentId?: Id;
    durationMinutes: number;
    exam: string;
    examDate: string;
    examPersonUserIds?: Id[];
    questions?: ExamQuestion[];
    remark?: string;
    status: TaskStatus;
    teamId?: Id;
  }

  export interface ExamPaper {
    company: string;
    companyId?: Id;
    createdAt: string;
    department: string;
    departmentId?: Id;
    description: string;
    id: Id;
    paperName: string;
    questionCount: number;
    questions: ExamQuestion[];
    team: string;
    teamId?: Id;
    totalScore: number;
    updatedAt: string;
  }

  export interface ExamPaperPayload {
    companyId?: Id;
    departmentId?: Id;
    description: string;
    paperName: string;
    questions: ExamQuestion[];
    teamId?: Id;
  }

  export interface ExamResult {
    code?: string;
    company: string;
    companyId: Id;
    createdAt: string;
    department: string;
    departmentId: Id;
    exam: string;
    examDate: string;
    examPersonName: string;
    examPersonUserId: Id;
    id: Id;
    remark?: string;
    score: number;
    status: ResultStatus;
    statusLabel: string;
    taskId: Id;
    updatedAt: string;
  }

  export interface ExamResultPayload {
    examPersonUserId: Id;
    score: number;
    status: ResultStatus;
    taskId: Id;
  }

  export interface ExamTaskDetail extends ExamTask {
    questions: ExamQuestion[];
    results: ExamResult[];
  }

  export interface ExamResultDetail extends ExamResult {
    questions: ExamQuestion[];
  }

  export interface QuestionImportResult {
    errors: string[];
    questions: ExamQuestion[];
    successRows: number;
  }

  export interface QuestionBankImportResult {
    errors: string[];
    questions: ExamQuestionBank[];
    successRows: number;
  }
}

const basePath = '/pingan/training';

export function getExamTasksApi(params: PinganTrainingExamApi.ListParams) {
  return requestClient.get<
    PinganTrainingExamApi.PageResult<PinganTrainingExamApi.ExamTask>
  >(`${basePath}/exam-tasks`, { params });
}

export function createExamTaskApi(
  payload: PinganTrainingExamApi.ExamTaskPayload,
) {
  return requestClient.post<PinganTrainingExamApi.ExamTask>(
    `${basePath}/exam-tasks`,
    payload,
  );
}

export function deleteExamTaskApi(id: PinganTrainingExamApi.Id) {
  return requestClient.delete(`${basePath}/exam-tasks/${id}`);
}

export function batchDeleteExamTasksApi(ids: PinganTrainingExamApi.Id[]) {
  return requestClient.post(`${basePath}/exam-tasks/batch-delete`, { ids });
}

export function getExamPapersApi(
  params: PinganTrainingExamApi.PaperListParams,
) {
  return requestClient.get<
    PinganTrainingExamApi.PageResult<PinganTrainingExamApi.ExamPaper>
  >(`${basePath}/exam-papers`, { params });
}

export function createExamPaperApi(
  payload: PinganTrainingExamApi.ExamPaperPayload,
) {
  return requestClient.post<PinganTrainingExamApi.ExamPaper>(
    `${basePath}/exam-papers`,
    payload,
  );
}

export function updateExamPaperApi(
  id: PinganTrainingExamApi.Id,
  payload: PinganTrainingExamApi.ExamPaperPayload,
) {
  return requestClient.put<PinganTrainingExamApi.ExamPaper>(
    `${basePath}/exam-papers/${id}`,
    payload,
  );
}

export function deleteExamPaperApi(id: PinganTrainingExamApi.Id) {
  return requestClient.delete(`${basePath}/exam-papers/${id}`);
}

export function batchDeleteExamPapersApi(ids: PinganTrainingExamApi.Id[]) {
  return requestClient.post(`${basePath}/exam-papers/batch-delete`, { ids });
}

export function getExamTaskDetailApi(id: PinganTrainingExamApi.Id) {
  return requestClient.get<PinganTrainingExamApi.ExamTaskDetail>(
    `${basePath}/exam-tasks/${id}`,
  );
}

export function downloadExamTaskQuestionsApi(id: PinganTrainingExamApi.Id) {
  return requestClient.get<Blob>(
    `${basePath}/exam-tasks/${id}/questions/export`,
    {
      responseReturn: 'body',
      responseType: 'blob',
    },
  );
}

export function getExamTaskResultsApi(
  id: PinganTrainingExamApi.Id,
  params?: PinganTrainingExamApi.ListParams,
) {
  return requestClient.get<
    PinganTrainingExamApi.PageResult<PinganTrainingExamApi.ExamResult>
  >(`${basePath}/exam-tasks/${id}/results`, { params });
}

export function getExamResultsApi(params: PinganTrainingExamApi.ListParams) {
  return requestClient.get<
    PinganTrainingExamApi.PageResult<PinganTrainingExamApi.ExamResult>
  >(`${basePath}/exam-results`, { params });
}

export function getExamResultDetailApi(id: PinganTrainingExamApi.Id) {
  return requestClient.get<PinganTrainingExamApi.ExamResultDetail>(
    `${basePath}/exam-results/${id}`,
  );
}

export function createExamResultApi(
  payload: PinganTrainingExamApi.ExamResultPayload,
) {
  return requestClient.post<PinganTrainingExamApi.ExamResult>(
    `${basePath}/exam-results`,
    payload,
  );
}

export function reviewExamResultApi(
  id: PinganTrainingExamApi.Id,
  payload: PinganTrainingExamApi.ExamReviewPayload,
) {
  return requestClient.post<PinganTrainingExamApi.ExamResultDetail>(
    `${basePath}/exam-results/${id}/review`,
    payload,
  );
}

export function deleteExamResultApi(id: PinganTrainingExamApi.Id) {
  return requestClient.delete(`${basePath}/exam-results/${id}`);
}

export function batchDeleteExamResultsApi(ids: PinganTrainingExamApi.Id[]) {
  return requestClient.post(`${basePath}/exam-results/batch-delete`, { ids });
}

export function getExamQuestionBankApi(
  params: PinganTrainingExamApi.QuestionBankListParams,
) {
  return requestClient.get<
    PinganTrainingExamApi.PageResult<PinganTrainingExamApi.ExamQuestionBank>
  >(`${basePath}/exam-question-bank`, { params });
}

export function createExamQuestionBankApi(
  payload: PinganTrainingExamApi.ExamQuestionBankPayload,
) {
  return requestClient.post<PinganTrainingExamApi.ExamQuestionBank>(
    `${basePath}/exam-question-bank`,
    payload,
  );
}

export function updateExamQuestionBankApi(
  id: PinganTrainingExamApi.Id,
  payload: PinganTrainingExamApi.ExamQuestionBankPayload,
) {
  return requestClient.put<PinganTrainingExamApi.ExamQuestionBank>(
    `${basePath}/exam-question-bank/${id}`,
    payload,
  );
}

export function deleteExamQuestionBankApi(id: PinganTrainingExamApi.Id) {
  return requestClient.delete(`${basePath}/exam-question-bank/${id}`);
}

export function batchDeleteExamQuestionBankApi(
  ids: PinganTrainingExamApi.Id[],
) {
  return requestClient.post(`${basePath}/exam-question-bank/batch-delete`, {
    ids,
  });
}

export function downloadExamQuestionBankTemplateApi() {
  return requestClient.get<Blob>(`${basePath}/exam-question-bank/template`, {
    responseReturn: 'body',
    responseType: 'blob',
  });
}

export function downloadExamQuestionBankApi(
  params: PinganTrainingExamApi.QuestionBankListParams,
) {
  return requestClient.get<Blob>(`${basePath}/exam-question-bank/export`, {
    params,
    responseReturn: 'body',
    responseType: 'blob',
  });
}

export function uploadExamQuestionBankWorkbookApi(
  companyId: PinganTrainingExamApi.Id,
  departmentId: PinganTrainingExamApi.Id | undefined,
  file: File,
  teamId?: PinganTrainingExamApi.Id,
) {
  const formData = new FormData();
  formData.append('companyId', String(companyId));
  if (departmentId !== undefined) {
    formData.append('departmentId', String(departmentId));
  }
  if (teamId !== undefined) {
    formData.append('teamId', String(teamId));
  }
  formData.append('file', file);

  return requestClient.post<PinganTrainingExamApi.QuestionBankImportResult>(
    `${basePath}/exam-question-bank/import`,
    formData,
    {
      headers: { 'Content-Type': 'multipart/form-data' },
    },
  );
}

export function previewExamQuestionBankPdfApi(file: File) {
  const formData = new FormData();
  formData.append('file', file);

  return requestClient.post<PinganTrainingExamApi.ExamQuestionBankPdfPreview>(
    `${basePath}/exam-question-bank/pdf/preview`,
    formData,
    {
      headers: { 'Content-Type': 'multipart/form-data' },
    },
  );
}

export function confirmExamQuestionBankPdfApi(
  payload: PinganTrainingExamApi.ExamQuestionBankPdfConfirmPayload,
) {
  return requestClient.post<PinganTrainingExamApi.ExamQuestionBankPdfConfirmResult>(
    `${basePath}/exam-question-bank/pdf/confirm`,
    payload,
  );
}

export function downloadExamQuestionTemplateApi() {
  return requestClient.get<Blob>(`${basePath}/exam-tasks/questions/template`, {
    responseReturn: 'body',
    responseType: 'blob',
  });
}

export function uploadExamQuestionWorkbookApi(file: File) {
  const formData = new FormData();
  formData.append('file', file);

  return requestClient.post<PinganTrainingExamApi.QuestionImportResult>(
    `${basePath}/exam-tasks/questions/import`,
    formData,
    {
      headers: { 'Content-Type': 'multipart/form-data' },
    },
  );
}
