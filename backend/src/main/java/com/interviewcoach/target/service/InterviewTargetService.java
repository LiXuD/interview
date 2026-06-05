package com.interviewcoach.target.service;

import com.interviewcoach.assessment.repository.AssessmentResultRepository;
import com.interviewcoach.assessment.repository.AssessmentSessionRepository;
import com.interviewcoach.agent.entity.InterviewCoachAgent;
import com.interviewcoach.agent.repository.AgentRepository;
import com.interviewcoach.agent.repository.CoachEventRepository;
import com.interviewcoach.agent.entity.CoachEvent;
import com.interviewcoach.agent.service.CoachEventService;
import com.interviewcoach.coachingmemory.repository.CoachingMemoryRepository;
import com.interviewcoach.common.api.InterviewTargetCreateRequest;
import com.interviewcoach.common.api.InterviewTargetDto;
import com.interviewcoach.common.api.InterviewTargetUpdateRequest;
import com.interviewcoach.common.error.TargetNotFoundException;
import com.interviewcoach.jobbrief.repository.JobBriefRepository;
import com.interviewcoach.mockinterview.repository.MockInterviewRepository;
import com.interviewcoach.report.repository.ReportRepository;
import com.interviewcoach.target.entity.InterviewTarget;
import com.interviewcoach.profile.repository.CandidateProfileRepository;
import com.interviewcoach.target.repository.InterviewTargetRepository;
import com.interviewcoach.training.repository.TrainingPlanRepository;
import com.interviewcoach.user.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 目标岗位业务服务，负责目标岗位的创建、查询、更新和级联删除。
 * 删除目标时会级联清理该岗位下的所有关联数据。
 */
@Service
public class InterviewTargetService {

    /** 允许的目标岗位状态值。 */
    private static final Set<String> VALID_STATUSES = Set.of("active", "completed", "archived");

    private final InterviewTargetRepository targetRepository;
    private final CandidateProfileRepository profileRepository;
    private final JobBriefRepository jobBriefRepository;
    private final AssessmentResultRepository assessmentResultRepository;
    private final AssessmentSessionRepository assessmentSessionRepository;
    private final ReportRepository reportRepository;
    private final TrainingPlanRepository trainingPlanRepository;
    private final MockInterviewRepository mockInterviewRepository;
    private final CoachingMemoryRepository coachingMemoryRepository;
    private final AgentRepository agentRepository;
    private final CoachEventRepository coachEventRepository;
    private final CoachEventService coachEventService;

    public InterviewTargetService(InterviewTargetRepository targetRepository,
                                  CandidateProfileRepository profileRepository,
                                  JobBriefRepository jobBriefRepository,
                                  AssessmentResultRepository assessmentResultRepository,
                                  AssessmentSessionRepository assessmentSessionRepository,
                                  ReportRepository reportRepository,
                                  TrainingPlanRepository trainingPlanRepository,
                                  MockInterviewRepository mockInterviewRepository,
                                  CoachingMemoryRepository coachingMemoryRepository,
                                  AgentRepository agentRepository,
                                  CoachEventRepository coachEventRepository,
                                  CoachEventService coachEventService) {
        this.targetRepository = targetRepository;
        this.profileRepository = profileRepository;
        this.jobBriefRepository = jobBriefRepository;
        this.assessmentResultRepository = assessmentResultRepository;
        this.assessmentSessionRepository = assessmentSessionRepository;
        this.reportRepository = reportRepository;
        this.trainingPlanRepository = trainingPlanRepository;
        this.mockInterviewRepository = mockInterviewRepository;
        this.coachingMemoryRepository = coachingMemoryRepository;
        this.agentRepository = agentRepository;
        this.coachEventRepository = coachEventRepository;
        this.coachEventService = coachEventService;
    }

    /**
     * 创建目标岗位并初始化关联的教练 Agent。
     *
     * @param user    当前用户
     * @param request 创建请求，含 title 和 jd
     * @return 创建的目标岗位 DTO
     */
    @Transactional
    public InterviewTargetDto createTarget(User user, InterviewTargetCreateRequest request) {
        // 1. 创建目标岗位实体并持久化
        InterviewTarget target = new InterviewTarget();
        target.setUser(user);
        target.setTitle(request.title());
        target.setJd(request.jd());
        target = targetRepository.save(target);
        // 2. 为该目标岗位初始化教练 Agent
        InterviewCoachAgent agent = new InterviewCoachAgent();
        agent.setUser(user);
        agent.setTarget(target);
        agentRepository.save(agent);
        // 3. 记录教练事件
        coachEventService.recordEvent(user, target.getId(), CoachEvent.TARGET_CREATED, "target", target.getId());
        return toDto(target);
    }

    /**
     * 获取指定用户所有目标岗位列表。
     *
     * @param userId 用户 ID
     * @return 目标岗位 DTO 列表
     */
    @Transactional(readOnly = true)
    public List<InterviewTargetDto> listTargets(UUID userId) {
        return targetRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * 获取指定目标岗位详情。
     *
     * @param targetId 目标岗位 ID
     * @param userId   用户 ID
     * @return 目标岗位 DTO
     * @throws TargetNotFoundException 目标不存在或不属于该用户
     */
    @Transactional(readOnly = true)
    public InterviewTargetDto getTarget(UUID targetId, UUID userId) {
        InterviewTarget target = targetRepository.findByIdAndUserId(targetId, userId)
                .orElseThrow(() -> new TargetNotFoundException(targetId));
        return toDto(target);
    }

    /**
     * 更新目标岗位信息。
     *
     * @param targetId 目标岗位 ID
     * @param userId   用户 ID
     * @param request  更新请求，title/jd/status 均可选
     * @return 更新后的目标岗位 DTO
     * @throws TargetNotFoundException 目标不存在或不属于该用户
     * @throws IllegalArgumentException status 值不在允许范围内
     */
    @Transactional
    public InterviewTargetDto updateTarget(UUID targetId, UUID userId, InterviewTargetUpdateRequest request) {
        // 1. 查询目标岗位并校验用户归属
        InterviewTarget target = targetRepository.findByIdAndUserId(targetId, userId)
                .orElseThrow(() -> new TargetNotFoundException(targetId));
        // 2. 按需更新字段
        if (request.title() != null) {
            target.setTitle(request.title());
        }
        if (request.jd() != null) {
            target.setJd(request.jd());
        }
        if (request.status() != null) {
            if (!VALID_STATUSES.contains(request.status())) {
                throw new IllegalArgumentException("Invalid status: " + request.status() + ". Must be one of: " + VALID_STATUSES);
            }
            target.setStatus(request.status());
        }
        // 3. 持久化并返回
        target = targetRepository.save(target);
        return toDto(target);
    }

    /**
     * 删除目标岗位并级联清理所有关联数据。
     * 删除顺序：先删子表数据，最后删目标主表，避免外键约束冲突。
     *
     * @param targetId 目标岗位 ID
     * @param userId   用户 ID
     * @throws TargetNotFoundException 目标不存在或不属于该用户
     */
    @Transactional
    public void deleteTarget(UUID targetId, UUID userId) {
        // 1. 校验目标岗位归属
        InterviewTarget target = targetRepository.findByIdAndUserId(targetId, userId)
                .orElseThrow(() -> new TargetNotFoundException(targetId));
        // 2. 级联删除所有关联业务数据
        assessmentResultRepository.deleteBySessionTargetId(targetId);
        assessmentSessionRepository.deleteByTargetId(targetId);
        reportRepository.deleteByTargetId(targetId);
        mockInterviewRepository.deleteByTargetId(targetId);
        trainingPlanRepository.deleteByTargetId(targetId);
        jobBriefRepository.deleteByTargetId(targetId);
        coachEventRepository.deleteByTargetId(targetId);
        agentRepository.deleteByTargetId(targetId);
        coachingMemoryRepository.deleteByTargetId(targetId);
        profileRepository.deleteByTargetId(targetId);
        // 3. 最后删除目标岗位主表
        targetRepository.delete(target);
    }

    /**
     * 将目标岗位实体转换为 DTO。
     *
     * @param target 目标岗位实体
     * @return 目标岗位 DTO
     */
    private InterviewTargetDto toDto(InterviewTarget target) {
        return new InterviewTargetDto(
                target.getId().toString(),
                target.getUser().getId().toString(),
                target.getTitle(),
                target.getJd(),
                target.getStatus(),
                target.getCreatedAt().toString(),
                target.getUpdatedAt().toString()
        );
    }
}
