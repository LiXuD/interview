const request = require('../../utils/request');
const API = require('../../utils/api');

Page({
  data: {
    targetId: '',
    plan: null,
    dayGroups: [],
    loading: true,
    generating: false,
    error: '',
    // 答题弹出层
    showAnswer: false,
    activeTask: null,
    taskAnswer: '',
    feedback: null,
    submitting: false,
    // 自适应训练
    adaptiveMode: false,
    adaptiveSessionId: '',
    adaptiveQuestion: '',
    adaptiveHistory: []
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
        if (err.statusCode === 404) {
          this.setData({ plan: null, dayGroups: [] });
        } else {
          this.setData({ error: err.message || '加载失败' });
        }
      })
      .finally(() => {
        this.setData({ loading: false });
      });
  },

  onGeneratePlan() {
    this.setData({ generating: true, error: '' });
    request.post(API.TRAINING_PLAN_GENERATE, { targetId: this.data.targetId })
      .then(() => {
        wx.showToast({ title: '训练计划已生成' });
        this.loadPlan();
      })
      .catch((err) => {
        this.setData({ error: err.message || '生成失败' });
      })
      .finally(() => {
        this.setData({ generating: false });
      });
  },

  onTaskTap(e) {
    const task = e.currentTarget.dataset.task;
    this.setData({
      showAnswer: true,
      activeTask: task,
      taskAnswer: '',
      feedback: null,
      adaptiveMode: false,
      adaptiveSessionId: '',
      adaptiveQuestion: '',
      adaptiveHistory: [],
      error: ''
    });
  },

  onStartAdaptive(e) {
    const task = e.currentTarget.dataset.task;
    this.setData({
      showAnswer: true,
      activeTask: task,
      taskAnswer: '',
      feedback: null,
      adaptiveMode: true,
      adaptiveSessionId: '',
      adaptiveQuestion: '',
      adaptiveHistory: [],
      submitting: true,
      error: ''
    });
    request.post(API.ADAPTIVE_SESSION_START(task.id))
      .then((session) => {
        this.setData({
          adaptiveSessionId: session.id,
          adaptiveQuestion: session.currentQuestion || '请开始回答',
          submitting: false
        });
      })
      .catch((err) => {
        this.setData({ error: err.message || '启动自适应训练失败', submitting: false });
      });
  },

  closeAnswer() {
    this.setData({ showAnswer: false, activeTask: null, feedback: null, adaptiveMode: false, adaptiveSessionId: '' });
  },

  onTaskAnswerInput(e) {
    this.setData({ taskAnswer: e.detail.value });
  },

  onSubmitTaskAnswer() {
    const answer = this.data.taskAnswer.trim();
    if (!answer || !this.data.activeTask) return;
    this.setData({ submitting: true, error: '' });

    if (this.data.adaptiveMode && this.data.adaptiveSessionId) {
      this.submitAdaptiveAnswer(answer);
    } else {
      this.submitRegularAnswer(answer);
    }
  },

  submitRegularAnswer(answer) {
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

  submitAdaptiveAnswer(answer) {
    const history = [...this.data.adaptiveHistory, { role: 'user', content: answer }];
    request.post(API.ADAPTIVE_SESSION_ANSWER(this.data.adaptiveSessionId), { answer })
      .then((resp) => {
        if (resp.status === 'completed' || resp.finished) {
          // 自适应训练完成
          const finalHistory = [...history, { role: 'assistant', content: '训练完成' }];
          this.setData({
            feedback: {
              score: resp.score || 0,
              feedback: resp.feedback || '训练已完成',
              problems: resp.problems || [],
              rewrittenAnswer: resp.rewrittenAnswer || ''
            },
            adaptiveHistory: finalHistory,
            taskAnswer: ''
          });
          this.loadPlan();
        } else {
          // 还有追问
          const newHistory = [...history, { role: 'assistant', content: resp.currentQuestion || '' }];
          this.setData({
            adaptiveQuestion: resp.currentQuestion || '',
            adaptiveHistory: newHistory,
            taskAnswer: ''
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

  goMockInterview() {
    wx.redirectTo({ url: '/pages/mock-interview/mock-interview?targetId=' + this.data.targetId });
  }
});
