package com.interviewcoach.auth.service;

import com.interviewcoach.agent.repository.AgentRepository;
import com.interviewcoach.agent.repository.CoachEventRepository;
import com.interviewcoach.ai.repository.AiProviderRepository;
import com.interviewcoach.assessment.repository.AssessmentResultRepository;
import com.interviewcoach.assessment.repository.AssessmentSessionRepository;
import com.interviewcoach.coachingmemory.repository.CoachingMemoryRepository;
import com.interviewcoach.common.api.AppleLoginRequest;
import com.interviewcoach.common.api.LoginRequest;
import com.interviewcoach.common.api.LoginResponse;
import com.interviewcoach.common.api.UserDto;
import com.interviewcoach.common.api.WechatLoginRequest;
import com.interviewcoach.common.error.AppleAuthFailedException;
import com.interviewcoach.common.error.UserNotFoundException;
import com.interviewcoach.common.security.AppleTokenVerifier;
import com.interviewcoach.common.security.JwtTokenProvider;
import com.interviewcoach.common.security.WechatTokenVerifier;
import com.interviewcoach.jobbrief.repository.JobBriefRepository;
import com.interviewcoach.mockinterview.repository.MockInterviewRepository;
import com.interviewcoach.profile.repository.CandidateProfileRepository;
import com.interviewcoach.report.repository.ReportRepository;
import com.interviewcoach.target.repository.InterviewTargetRepository;
import com.interviewcoach.training.repository.TrainingPlanRepository;
import com.interviewcoach.user.entity.User;
import com.interviewcoach.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.beans.factory.annotation.Value;

import java.util.Set;
import java.util.UUID;

/**
 * 认证服务，处理用户登录（Dev / Apple）、获取当前用户和删除账号。
 * 删除账号时级联清理用户所有业务数据。
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final AppleTokenVerifier appleTokenVerifier;
    private final WechatTokenVerifier wechatTokenVerifier;
    private final CandidateProfileRepository profileRepository;
    private final InterviewTargetRepository targetRepository;
    private final JobBriefRepository jobBriefRepository;
    private final AssessmentResultRepository assessmentResultRepository;
    private final AssessmentSessionRepository assessmentSessionRepository;
    private final ReportRepository reportRepository;
    private final TrainingPlanRepository trainingPlanRepository;
    private final MockInterviewRepository mockInterviewRepository;
    private final AiProviderRepository aiProviderRepository;
    private final CoachingMemoryRepository coachingMemoryRepository;
    private final AgentRepository agentRepository;
    private final CoachEventRepository coachEventRepository;
    private final Set<String> adminUsernames;

    public AuthService(UserRepository userRepository, JwtTokenProvider jwtTokenProvider,
                       AppleTokenVerifier appleTokenVerifier, WechatTokenVerifier wechatTokenVerifier,
                       CandidateProfileRepository profileRepository,
                       InterviewTargetRepository targetRepository,
                       JobBriefRepository jobBriefRepository,
                       AssessmentResultRepository assessmentResultRepository,
                       AssessmentSessionRepository assessmentSessionRepository,
                       ReportRepository reportRepository,
                       TrainingPlanRepository trainingPlanRepository,
                       MockInterviewRepository mockInterviewRepository,
                       AiProviderRepository aiProviderRepository,
                       CoachingMemoryRepository coachingMemoryRepository,
                       AgentRepository agentRepository,
                       CoachEventRepository coachEventRepository,
                       @Value("${app.admin.usernames:}") Set<String> adminUsernames) {
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.appleTokenVerifier = appleTokenVerifier;
        this.wechatTokenVerifier = wechatTokenVerifier;
        this.profileRepository = profileRepository;
        this.targetRepository = targetRepository;
        this.jobBriefRepository = jobBriefRepository;
        this.assessmentResultRepository = assessmentResultRepository;
        this.assessmentSessionRepository = assessmentSessionRepository;
        this.reportRepository = reportRepository;
        this.trainingPlanRepository = trainingPlanRepository;
        this.mockInterviewRepository = mockInterviewRepository;
        this.aiProviderRepository = aiProviderRepository;
        this.coachingMemoryRepository = coachingMemoryRepository;
        this.agentRepository = agentRepository;
        this.coachEventRepository = coachEventRepository;
        this.adminUsernames = adminUsernames == null ? Set.of() : adminUsernames;
    }

    /**
     * 开发环境登录：查找或创建用户，签发 JWT Token。
     *
     * @param request 包含 username 的登录请求
     * @return 登录响应，含 token、userId、username
     */
    @Transactional
    public LoginResponse devLogin(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseGet(() -> {
                    try {
                        User newUser = new User();
                        newUser.setUsername(request.username());
                        return userRepository.save(newUser);
                    } catch (DataIntegrityViolationException e) {
                        return userRepository.findByUsername(request.username()).orElseThrow();
                    }
                });
        promoteAdmin(user);
        String token = jwtTokenProvider.generateToken(user.getId());
        return new LoginResponse(token, user.getId().toString(), user.getUsername());
    }

    /**
     * Apple 登录：验证 identityToken，查找或创建用户并签发 JWT。
     *
     * @param request Apple 登录请求，含 identityToken 和 nonce
     * @return 登录响应
     */
    @Transactional
    public LoginResponse appleLogin(AppleLoginRequest request) {
        if (request.identityToken() == null || request.identityToken().isBlank()) {
            throw new AppleAuthFailedException("identityToken is required");
        }
        if (request.nonce() == null || request.nonce().isBlank()) {
            throw new AppleAuthFailedException("nonce is required");
        }
        String appleUserId = appleTokenVerifier.verifyAndGetSub(request.identityToken(), request.nonce());
        return findOrCreateAppleUser(appleUserId, request.fullName());
    }

    /**
     * 根据 Apple 用户标识查找或创建用户，签发 JWT Token。
     * 已有用户直接签发；新用户以 apple_ 前缀用户名创建，用户名冲突时兜底查询。
     *
     * @param appleUserId Apple 用户唯一标识（sub）
     * @param fullName    Apple 返回的用户全名（当前未使用，预留）
     * @return 登录响应，包含 token、userId、username
     */
    @Transactional
    LoginResponse findOrCreateAppleUser(String appleUserId, String fullName) {
        User user = userRepository.findByAppleUserId(appleUserId)
                .orElseGet(() -> {
                    User newUser = new User();
                    String username = "apple_" + appleUserId;
                    try {
                        newUser.setUsername(username);
                        newUser.setAppleUserId(appleUserId);
                        return userRepository.save(newUser);
                    } catch (DataIntegrityViolationException e) {
                        return userRepository.findByAppleUserId(appleUserId).orElseThrow();
                    }
                });
        promoteAdmin(user);
        String token = jwtTokenProvider.generateToken(user.getId());
        return new LoginResponse(token, user.getId().toString(), user.getUsername());
    }

    /**
     * 微信小程序登录：用 code 换取 openId，查找或创建用户并签发 JWT。
     *
     * @param request 微信登录请求，含 wx.login() 获取的 code
     * @return 登录响应
     */
    @Transactional
    public LoginResponse wechatLogin(WechatLoginRequest request) {
        if (request.code() == null || request.code().isBlank()) {
            throw new com.interviewcoach.common.error.WechatAuthFailedException("code is required");
        }
        String openId = wechatTokenVerifier.codeToOpenId(request.code());
        return findOrCreateWechatUser(openId);
    }

    /**
     * 根据微信 openId 查找或创建用户，签发 JWT Token。
     * 已有用户直接签发；新用户以 wechat_ 前缀用户名创建，用户名冲突时兜底查询。
     *
     * @param openId 微信小程序 openId
     * @return 登录响应，包含 token、userId、username
     */
    @Transactional
    LoginResponse findOrCreateWechatUser(String openId) {
        User user = userRepository.findByWechatOpenId(openId)
                .orElseGet(() -> {
                    User newUser = new User();
                    String username = "wechat_" + openId;
                    try {
                        newUser.setUsername(username);
                        newUser.setWechatOpenId(openId);
                        return userRepository.save(newUser);
                    } catch (DataIntegrityViolationException e) {
                        return userRepository.findByWechatOpenId(openId).orElseThrow();
                    }
                });
        promoteAdmin(user);
        String token = jwtTokenProvider.generateToken(user.getId());
        return new LoginResponse(token, user.getId().toString(), user.getUsername());
    }

    /**
     * 根据用户 ID 获取当前用户信息。
     *
     * @param userId 用户 ID
     * @return 用户 DTO
     */
    @Transactional(readOnly = true)
    public UserDto getCurrentUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        return new UserDto(user.getId().toString(), user.getUsername());
    }

    /**
     * 删除用户及其所有关联业务数据（测评、报告、训练、模拟面试、AI Provider 等）。
     * 删除顺序：先删子表数据，最后删用户主表，避免外键约束冲突。
     *
     * @param userId 要删除的用户 ID
     */
    @Transactional
    public void deleteUser(UUID userId) {
        // 1. 删除测评结果（依赖 assessment_session）
        assessmentResultRepository.deleteBySessionUserId(userId);
        // 2. 删除测评会话
        assessmentSessionRepository.deleteByUserId(userId);
        // 3. 删除报告
        reportRepository.deleteByUserId(userId);
        // 4. 删除教练记忆
        coachingMemoryRepository.deleteByUserId(userId);
        // 5. 删除模拟面试
        mockInterviewRepository.deleteByUserId(userId);
        // 6. 删除训练计划
        trainingPlanRepository.deleteByUserId(userId);
        // 7. 删除 AI Provider 配置
        aiProviderRepository.deleteByUserId(userId);
        // 8. 删除岗位画像
        jobBriefRepository.deleteByUserId(userId);
        // 9. 删除教练事件
        coachEventRepository.deleteByUserId(userId);
        // 10. 删除教练 Agent
        agentRepository.deleteByUserId(userId);
        // 11. 删除简历摘要
        profileRepository.deleteByUserId(userId);
        // 12. 删除目标岗位
        targetRepository.deleteByUserId(userId);
        // 13. 最后删除用户主表
        userRepository.deleteById(userId);
    }

    /**
     * 如果用户名在管理员名单中且当前角色不是 ADMIN，则提升为 ADMIN。
     */
    private void promoteAdmin(User user) {
        if (adminUsernames.contains(user.getUsername()) && !"ADMIN".equals(user.getRole())) {
            user.setRole("ADMIN");
            userRepository.save(user);
        }
    }
}
