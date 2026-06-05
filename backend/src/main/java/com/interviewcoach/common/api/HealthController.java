package com.interviewcoach.common.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 健康检查端点，用于 Walking Skeleton 验证后端服务是否正常运行。
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    /**
     * 返回服务健康状态。
     *
     * @return 包含状态和服务名称的健康检查响应
     */
    @GetMapping
    public HealthResponse health() {
        return new HealthResponse("UP", "interview-coach-backend");
    }
}
