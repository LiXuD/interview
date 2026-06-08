const request = require('../../utils/request');
const API = require('../../utils/api');

Page({
  data: {
    providers: [],
    loading: true,
    error: '',
    showCreate: false,
    form: { name: '', baseUrl: '', apiKey: '', model: '', openaiApiMode: 'chatCompletions' },
    testing: false,
    testResult: '',
    saving: false
  },
  onLoad() { this.loadProviders(); },

  loadProviders() {
    this.setData({ loading: true });
    request.get(API.AI_PROVIDERS)
      .then((data) => { this.setData({ providers: Array.isArray(data) ? data : [] }); })
      .catch((err) => { this.setData({ error: err.message || '加载失败' }); })
      .finally(() => { this.setData({ loading: false }); });
  },

  onSetDefault(e) {
    const id = e.currentTarget.dataset.id;
    request.post(API.AI_PROVIDER_SET_DEFAULT(id))
      .then(() => { wx.showToast({ title: '已设置' }); this.loadProviders(); })
      .catch((err) => { this.setData({ error: err.message || '设置失败' }); });
  },

  onDelete(e) {
    const id = e.currentTarget.dataset.id;
    wx.showModal({
      title: '确认删除',
      content: '删除后 API Key 将被清除'
    }).then((res) => {
      if (res.confirm) {
        request.del(API.AI_PROVIDER_DELETE(id))
          .then(() => { wx.showToast({ title: '已删除' }); this.loadProviders(); })
          .catch((err) => { this.setData({ error: err.message || '删除失败' }); });
      }
    });
  },

  onCreate() {
    this.setData({
      showCreate: true,
      form: { name: '', baseUrl: '', apiKey: '', model: '', openaiApiMode: 'chatCompletions' },
      testResult: '',
      error: ''
    });
  },

  onCloseCreate() {
    this.setData({ showCreate: false, testResult: '' });
  },

  onFormInput(e) {
    const field = e.currentTarget.dataset.field;
    this.setData({ ['form.' + field]: e.detail.value, testResult: '' });
  },

  onModeChange(e) {
    this.setData({ 'form.openaiApiMode': e.detail.value === '0' ? 'chatCompletions' : 'responses', testResult: '' });
  },

  onTestConnection() {
    const { form } = this.data;
    if (!form.baseUrl || !form.apiKey || !form.model) {
      this.setData({ testResult: '请填写 Base URL、API Key 和 Model' });
      return;
    }
    this.setData({ testing: true, testResult: '' });
    request.post(API.AI_PROVIDERS_TEST, {
      name: form.name || 'test',
      baseUrl: form.baseUrl,
      apiKey: form.apiKey,
      model: form.model,
      openaiApiMode: form.openaiApiMode
    })
      .then(() => { this.setData({ testResult: '连接成功' }); })
      .catch((err) => { this.setData({ testResult: '连接失败: ' + (err.message || '未知错误') }); })
      .finally(() => { this.setData({ testing: false }); });
  },

  onSaveProvider() {
    const { form } = this.data;
    if (!form.name || !form.baseUrl || !form.apiKey || !form.model) {
      this.setData({ error: '请填写所有必填字段' });
      return;
    }
    this.setData({ saving: true, error: '' });
    request.post(API.AI_PROVIDERS, {
      name: form.name,
      baseUrl: form.baseUrl,
      apiKey: form.apiKey,
      model: form.model,
      openaiApiMode: form.openaiApiMode
    })
      .then(() => {
        wx.showToast({ title: '已添加' });
        this.setData({ showCreate: false });
        this.loadProviders();
      })
      .catch((err) => { this.setData({ error: err.message || '保存失败' }); })
      .finally(() => { this.setData({ saving: false }); });
  }
});
