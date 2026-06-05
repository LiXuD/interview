package com.interviewcoach.common.api;

/**
 * 健康检查响应 DTO。
 *
 * @param status  服务状态，如 "UP"
 * @param service 服务名称
 */
public record HealthResponse(String status, String service) {
}
