/**
 * 首次引导页：3 页滑动介绍，登录后首次打开自动展示。
 */
const storage = require('../../utils/storage');

Page({
  data: {
    currentPage: 0,
    pages: [
      {
        icon: '👋',
        title: 'AI 面试教练',
        desc: '你的专属 AI 面试训练伙伴，帮助你高效准备技术面试。'
      },
      {
        icon: '🔄',
        title: '核心流程',
        desc: '四步完成一次完整面试训练：',
        steps: [
          '创建目标岗位，粘贴 JD',
          '输入简历，AI 生成结构化摘要',
          '生成岗位画像，完成 5 题测评',
          '专项训练 + 模拟面试，查看复盘报告'
        ]
      },
      {
        icon: '🔒',
        title: '隐私保护',
        desc: '你的数据安全是我们的底线：',
        steps: [
          '简历原文仅保存在本地设备',
          'AI 摘要生成后，原文不会落库',
          'API Key 加密存储，永不返回明文',
          '删除账号将清除所有远端数据'
        ]
      }
    ]
  },

  onSwiperChange(e) {
    this.setData({ currentPage: e.detail.current });
  },

  onNext() {
    const { currentPage, pages } = this.data;
    if (currentPage < pages.length - 1) {
      this.setData({ currentPage: currentPage + 1 });
    } else {
      this.finishOnboarding();
    }
  },

  onSkip() {
    this.finishOnboarding();
  },

  finishOnboarding() {
    storage.setOnboardingCompleted();
    wx.reLaunch({ url: storage.ROUTES.TARGETS });
  }
});
