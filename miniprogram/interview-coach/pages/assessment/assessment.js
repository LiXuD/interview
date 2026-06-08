const request = require('../../utils/request');
const API = require('../../utils/api');

Page({
  data: {
    targetId: '',
    sessionId: '',
    status: 'loading', // 'loading' | 'in_progress' | 'completed'
    questionIndex: 0,
    totalQuestions: 5,
    currentQuestion: {},
    questions: [],
    answer: '',
    result: null,
    submitting: false,
    error: ''
  },

  onLoad(options) {
    this.setData({ targetId: options.targetId || '' });
    this.startAssessment();
  },

  startAssessment() {
    this.setData({ status: 'loading', error: '' });
    request.post(API.ASSESSMENT_START, { targetId: this.data.targetId })
      .then((session) => {
        const currentQuestion = session.currentQuestion || (session.questions && session.questions[0]) || {};
        this.setData({
          sessionId: session.id,
          status: 'in_progress',
          questionIndex: session.questionIndex || 0,
          totalQuestions: session.totalQuestions || 5,
          currentQuestion,
          questions: session.questions || []
        });
      })
      .catch((err) => {
        this.setData({ error: err.message || '开始测评失败', status: 'in_progress' });
      });
  },

  onAnswerInput(e) {
    this.setData({ answer: e.detail.value, error: '' });
  },

  onSubmitAnswer() {
    const answer = this.data.answer.trim();
    if (!answer) return;
    this.setData({ submitting: true, error: '' });

    request.post(API.ASSESSMENT_ANSWER(this.data.sessionId), { answer })
      .then((session) => {
        const nextIndex = this.data.questionIndex + 1;
        if (nextIndex >= this.data.totalQuestions) {
          this.finishAssessment();
        } else {
          const currentQuestion = session.currentQuestion || this.data.questions[nextIndex] || {};
          this.setData({
            questionIndex: nextIndex,
            currentQuestion,
            answer: ''
          });
        }
      })
      .catch((err) => {
        this.setData({ error: err.message || '提交失败' });
      })
      .finally(() => {
        this.setData({ submitting: false });
      });
  },

  finishAssessment() {
    this.setData({ submitting: true });
    request.post(API.ASSESSMENT_FINISH(this.data.sessionId))
      .then((result) => {
        this.setData({ status: 'completed', result });
      })
      .catch((err) => {
        this.setData({ error: err.message || '完成测评失败' });
      })
      .finally(() => {
        this.setData({ submitting: false });
      });
  },

  goTraining() {
    wx.redirectTo({ url: '/pages/training/training?targetId=' + this.data.targetId });
  }
});
