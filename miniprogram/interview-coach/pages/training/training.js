const request = require('../../utils/request');
const API = require('../../utils/api');

Page({
  data: {
    targetId: '',
    plan: null,
    dayGroups: [],
    loading: true,
    error: '',
    // 答题弹出层
    showAnswer: false,
    activeTask: null,
    taskAnswer: '',
    feedback: null,
    submitting: false
  },

  onLoad(options) {
    this.setData({ targetId: options.targetId || '' });
    this.loadPlan();
  },

  loadPlan() {
    this.setData({ loading: true, error: '' });
    request.get(API.TRAINING_PLAN(this.data.targetId))
      .then((plan) => {
        const tasks = plan.tasks || [];
        // 按 dayIndex 分组
        const groupMap = {};
        tasks.forEach((t) => {
          const day = t.dayIndex || 0;
          if (!groupMap[day]) groupMap[day] = { dayIndex: day, tasks: [] };
          groupMap[day].tasks.push(t);
        });
        const dayGroups = Object.values(groupMap).sort((a, b) => a.dayIndex - b.dayIndex);
        this.setData({ plan, dayGroups });
      })
      .catch((err) => {
        this.setData({ error: err.message || '加载失败' });
      })
      .finally(() => {
        this.setData({ loading: false });
      });
  },

  onTaskTap(e) {
    const task = e.currentTarget.dataset.task;
    this.setData({
      showAnswer: true,
      activeTask: task,
      taskAnswer: '',
      feedback: null,
      error: ''
    });
  },

  closeAnswer() {
    this.setData({ showAnswer: false, activeTask: null, feedback: null });
  },

  onTaskAnswerInput(e) {
    this.setData({ taskAnswer: e.detail.value });
  },

  onSubmitTaskAnswer() {
    const answer = this.data.taskAnswer.trim();
    if (!answer || !this.data.activeTask) return;
    this.setData({ submitting: true, error: '' });
    request.post(API.TRAINING_TASK_ANSWER(this.data.activeTask.id), { answer })
      .then((feedback) => {
        this.setData({ feedback });
        this.loadPlan();
      })
      .catch((err) => {
        this.setData({ error: err.message || '提交失败' });
      })
      .finally(() => {
        this.setData({ submitting: false });
      });
  },

  goMockInterview() {
    wx.redirectTo({ url: '/pages/mock-interview/mock-interview?targetId=' + this.data.targetId });
  }
});
