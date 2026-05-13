import api from './axios';

export const assistantApi = {
  explainBalance: (focusGroupId) =>
    api.post('/assistant/explain-balance', { focusGroupId }),

  expenseQnA: (question, searchQuery, groupId, chartSummary) =>
    api.post('/assistant/expense-qna', { question, searchQuery, groupId, chartSummary }),

  expenseDraft: (utterance, defaultGroupId, defaultCurrency) =>
    api.post('/assistant/expense-draft', { utterance, defaultGroupId, defaultCurrency }),

  suggestCategory: (description, merchant) =>
    api.post('/assistant/suggest-category', { description, merchant }),

  settlementBriefing: (groupId) =>
    api.post('/assistant/settlement-briefing', { groupId }),

  activityNarrative: (scope, groupId, pageSize) =>
    api.post('/assistant/activity-narrative', { scope, groupId, pageSize }),

  whatIf: (excludeCategories, excludeExpenseIds, groupId) =>
    api.post('/assistant/what-if', { excludeCategories, excludeExpenseIds, groupId }),

  chartCaption: (chartKind, series) =>
    api.post('/assistant/chart-caption', { chartKind, series }),

  coherenceScan: (groupId) =>
    api.get('/assistant/coherence-scan', { params: { groupId } }),

  groupDigest: (groupId) =>
    api.post('/assistant/group-digest', { groupId }),
};
