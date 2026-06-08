const request = require('../../utils/request');
const API = require('../../utils/api');

Page({
  data: {
    targetId: '',
    sessionId: '',
    status: 'loading', // 'loading' | 'in_progress' | 'completed'
    messages: [],
    conversationTurns: 0,
    answer: '',
    sending: false,
    report: null,
    scrollToId: '',
    error: ''
  },

  onLoad(options) {
    this.setData({ targetId: options.targetId || '' });
    this.startInterview();
  },

  startInterview() {
    this.setData({ status: 'loading', error: '' });
    request.post(API.MOCK_INTERVIEW_START, { targetId: this.data.targetId })
      .then((session) => {
        const messages = [];
        if (session.currentQuestion) {
          messages.push({ id: 'q0', role: 'assistant', content: session.currentQuestion });
        }
        this.setData({
          sessionId: session.id,
          status: 'in_progress',
          messages,
          conversationTurns: session.conversationTurns || 0
        });
      })
      .catch((err) => {
        this.setData({ error: err.message || '开始面试失败', status: 'in_progress' });
      });
  },

  onAnswerInput(e) {
    this.setData({ answer: e.detail.value });
  },

  onSend() {
    const answer = this.data.answer.trim();
    if (!answer || !this.data.sessionId) return;

    const userMsg = { id: 'u' + Date.now(), role: 'user', content: answer };
    const messages = [...this.data.messages, userMsg];
    this.setData({ messages, answer: '', sending: true, scrollToId: '' });

    request.post(API.MOCK_INTERVIEW_ANSWER(this.data.sessionId), { answer })
      .then((session) => {
        const updated = [...messages];
        if (session.currentQuestion) {
          updated.push({ id: 'q' + Date.now(), role: 'assistant', content: session.currentQuestion });
        }
        this.setData({
          messages: updated,
          conversationTurns: session.conversationTurns || this.data.conversationTurns + 1,
          scrollToId: 'chat-bottom'
        });
      })
      .catch((err) => {
        this.setData({ error: err.message || '提交失败' });
      })
      .finally(() => {
        this.setData({ sending: false });
      });
  },

  onFinish() {
    wx.showModal({
      title: '结束面试',
      content: '确定要结束模拟面试吗？',
      success: (res) => {
        if (res.confirm) {
          this.finishInterview();
        }
      }
    });
  },

  finishInterview() {
    this.setData({ sending: true, error: '' });
    request.post(API.MOCK_INTERVIEW_FINISH(this.data.sessionId))
      .then((report) => {
        this.setData({ status: 'completed', report });
      })
      .catch((err) => {
        this.setData({ error: err.message || '完成面试失败' });
      })
      .finally(() => {
        this.setData({ sending: false });
      });
  },

  goReport() {
    wx.redirectTo({ url: '/pages/report-detail/report-detail?targetId=' + this.data.targetId + '&type=mockInterview' });
  }
});
