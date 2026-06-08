package com.interviewcoach.common.api;

/**
 * 微信小程序登录请求，包含 wx.login() 获取的授权码。
 *
 * @param code 微信授权码
 */
public record WechatLoginRequest(String code) {
}
