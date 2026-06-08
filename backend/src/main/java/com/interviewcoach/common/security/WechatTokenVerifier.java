package com.interviewcoach.common.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewcoach.common.error.WechatAuthFailedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * 微信小程序登录验证器。
 * 调用微信 code2session 接口，将 wx.login() 获取的 code 换取 openId。
 * sessionKey 不返回客户端、不落库、不写日志。
 */
@Component
public class WechatTokenVerifier {

    private static final String CODE2SESSION_URL = "https://api.weixin.qq.com/sns/jscode2session";

    private final String appId;
    private final String appSecret;
    private final boolean enabled;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public WechatTokenVerifier(
            @Value("${app.wechat.mini-program.app-id:}") String appId,
            @Value("${app.wechat.mini-program.app-secret:}") String appSecret,
            @Value("${app.wechat.login-enabled:false}") boolean enabled,
            ObjectMapper objectMapper) {
        this.appId = appId;
        this.appSecret = appSecret;
        this.enabled = enabled;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * 用微信授权码换取 openId。
     *
     * @param code wx.login() 获取的授权码
     * @return 微信 openId
     * @throws WechatAuthFailedException 验证失败时抛出
     */
    public String codeToOpenId(String code) {
        if (!enabled) {
            throw new WechatAuthFailedException("WeChat login is not enabled");
        }
        if (code == null || code.isBlank()) {
            throw new WechatAuthFailedException("code is required");
        }
        try {
            String url = String.format("%s?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                    CODE2SESSION_URL, appId, appSecret, code);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());

            // 微信返回 errcode != 0 表示失败
            JsonNode errcodeNode = root.get("errcode");
            if (errcodeNode != null && errcodeNode.asInt() != 0) {
                String errmsg = root.has("errmsg") ? root.get("errmsg").asText() : "unknown error";
                throw new WechatAuthFailedException("WeChat code2session failed: " + errmsg);
            }

            JsonNode openidNode = root.get("openid");
            if (openidNode == null || openidNode.asText().isEmpty()) {
                throw new WechatAuthFailedException("WeChat code2session response missing openid");
            }

            // sessionKey 不返回、不落库、不写日志，仅在此方法内使用后丢弃
            return openidNode.asText();
        } catch (WechatAuthFailedException e) {
            throw e;
        } catch (Exception e) {
            throw new WechatAuthFailedException("Failed to call WeChat code2session: " + e.getMessage(), e);
        }
    }
}
