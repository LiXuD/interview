package com.interviewcoach.common.error;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.UUID;

/**
 * 全局异常处理器，将业务异常统一转换为包含错误码、消息和 requestId 的 ErrorResponse。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理用户未找到异常，返回 404。
     *
     * @param ex 用户未找到异常
     * @return 包含 USER_NOT_FOUND 错误码的响应
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
        return ResponseEntity.status(404)
                .body(new ErrorResponse("USER_NOT_FOUND", ex.getMessage(), generateRequestId()));
    }

    /**
     * 处理目标岗位未找到异常，返回 404。
     *
     * @param ex 目标岗位未找到异常
     * @return 包含 TARGET_NOT_FOUND 错误码的响应
     */
    @ExceptionHandler(TargetNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTargetNotFound(TargetNotFoundException ex) {
        return ResponseEntity.status(404)
                .body(new ErrorResponse("TARGET_NOT_FOUND", ex.getMessage(), generateRequestId()));
    }

    /**
     * 处理候选人画像未找到异常，返回 404。
     *
     * @param ex 候选人画像未找到异常
     * @return 包含 PROFILE_NOT_FOUND 错误码的响应
     */
    @ExceptionHandler(ProfileNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProfileNotFound(ProfileNotFoundException ex) {
        return ResponseEntity.status(404)
                .body(new ErrorResponse("PROFILE_NOT_FOUND", ex.getMessage(), generateRequestId()));
    }

    /**
     * 处理岗位画像未找到异常，返回 404。
     *
     * @param ex 岗位画像未找到异常
     * @return 包含 JOB_BRIEF_NOT_FOUND 错误码的响应
     */
    @ExceptionHandler(JobBriefNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleJobBriefNotFound(JobBriefNotFoundException ex) {
        return ResponseEntity.status(404)
                .body(new ErrorResponse("JOB_BRIEF_NOT_FOUND", ex.getMessage(), generateRequestId()));
    }

    /**
     * 处理测评会话未找到异常，返回 404。
     *
     * @param ex 测评会话未找到异常
     * @return 包含 ASSESSMENT_NOT_FOUND 错误码的响应
     */
    @ExceptionHandler(AssessmentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAssessmentNotFound(AssessmentNotFoundException ex) {
        return ResponseEntity.status(404)
                .body(new ErrorResponse("ASSESSMENT_NOT_FOUND", ex.getMessage(), generateRequestId()));
    }

    /**
     * 处理复盘报告未找到异常，返回 404。
     *
     * @param ex 复盘报告未找到异常
     * @return 包含 REPORT_NOT_FOUND 错误码的响应
     */
    @ExceptionHandler(ReportNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleReportNotFound(ReportNotFoundException ex) {
        return ResponseEntity.status(404)
                .body(new ErrorResponse("REPORT_NOT_FOUND", ex.getMessage(), generateRequestId()));
    }

    /**
     * 处理训练计划/任务未找到异常，返回 404。
     *
     * @param ex 训练计划/任务未找到异常
     * @return 包含 TRAINING_NOT_FOUND 错误码的响应
     */
    @ExceptionHandler(TrainingNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTrainingNotFound(TrainingNotFoundException ex) {
        return ResponseEntity.status(404)
                .body(new ErrorResponse("TRAINING_NOT_FOUND", ex.getMessage(), generateRequestId()));
    }

    /**
     * 处理模拟面试会话未找到异常，返回 404。
     *
     * @param ex 模拟面试会话未找到异常
     * @return 包含 MOCK_INTERVIEW_NOT_FOUND 错误码的响应
     */
    @ExceptionHandler(MockInterviewNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleMockInterviewNotFound(MockInterviewNotFoundException ex) {
        return ResponseEntity.status(404)
                .body(new ErrorResponse("MOCK_INTERVIEW_NOT_FOUND", ex.getMessage(), generateRequestId()));
    }

    /**
     * 处理 AI Provider 未找到异常，返回 404。
     *
     * @param ex AI Provider 未找到异常
     * @return 包含 AI_PROVIDER_NOT_FOUND 错误码的响应
     */
    @ExceptionHandler(AiProviderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAiProviderNotFound(AiProviderNotFoundException ex) {
        return ResponseEntity.status(404)
                .body(new ErrorResponse("AI_PROVIDER_NOT_FOUND", ex.getMessage(), generateRequestId()));
    }

    /**
     * 处理教练记忆记录未找到异常，返回 404。
     *
     * @param ex 教练记忆记录未找到异常
     * @return 包含 COACHING_MEMORY_NOT_FOUND 错误码的响应
     */
    @ExceptionHandler(CoachingMemoryNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCoachingMemoryNotFound(CoachingMemoryNotFoundException ex) {
        return ResponseEntity.status(404)
                .body(new ErrorResponse("COACHING_MEMORY_NOT_FOUND", ex.getMessage(), generateRequestId()));
    }

    /**
     * 处理 InterviewCoachAgent 未找到异常，返回 404。
     *
     * @param ex Agent 未找到异常
     * @return 包含 AGENT_NOT_FOUND 错误码的响应
     */
    @ExceptionHandler(AgentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAgentNotFound(AgentNotFoundException ex) {
        return ResponseEntity.status(404)
                .body(new ErrorResponse("AGENT_NOT_FOUND", ex.getMessage(), generateRequestId()));
    }

    /**
     * 处理 AI Provider 调用失败异常，返回 502。
     *
     * @param ex AI Provider 调用失败异常
     * @return 包含 AI_PROVIDER_CALL_FAILED 错误码的响应
     */
    @ExceptionHandler(AiProviderCallFailedException.class)
    public ResponseEntity<ErrorResponse> handleAiProviderCallFailed(AiProviderCallFailedException ex) {
        return ResponseEntity.status(502)
                .body(new ErrorResponse("AI_PROVIDER_CALL_FAILED", ex.getMessage(), generateRequestId()));
    }

    /**
     * 处理 AI 结构化输出解析失败异常，返回 400。
     *
     * @param ex AI 解析失败异常
     * @return 包含 AI_PARSE_FAILED 错误码的响应
     */
    @ExceptionHandler(AiParseException.class)
    public ResponseEntity<ErrorResponse> handleAiParseFailed(AiParseException ex) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("AI_PARSE_FAILED", ex.getMessage(), generateRequestId()));
    }

    /**
     * 处理 Apple Sign in 认证失败异常，返回 401。
     *
     * @param ex Apple 认证失败异常
     * @return 包含 APPLE_AUTH_FAILED 错误码的响应
     */
    @ExceptionHandler(AppleAuthFailedException.class)
    public ResponseEntity<ErrorResponse> handleAppleAuthFailed(AppleAuthFailedException ex) {
        return ResponseEntity.status(401)
                .body(new ErrorResponse("APPLE_AUTH_FAILED", ex.getMessage(), generateRequestId()));
    }

    /**
     * 处理非法参数异常，返回 400。
     *
     * @param ex 非法参数异常
     * @return 包含 BAD_REQUEST 错误码的响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("BAD_REQUEST", ex.getMessage(), generateRequestId()));
    }

    /**
     * 兜底处理所有未预期异常，返回 500，不暴露内部错误细节。
     *
     * @param ex 未预期异常
     * @return 包含 INTERNAL_ERROR 错误码的响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        return ResponseEntity.status(500)
                .body(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred", generateRequestId()));
    }

    /**
     * 生成唯一请求追踪 ID。
     */
    private String generateRequestId() {
        return UUID.randomUUID().toString();
    }
}
