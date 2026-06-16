package com.interviewcoach.target.controller;

import com.interviewcoach.common.api.InterviewTargetCreateRequest;
import com.interviewcoach.common.api.InterviewTargetDto;
import com.interviewcoach.common.api.InterviewTargetUpdateRequest;
import com.interviewcoach.common.security.SecurityUtils;
import com.interviewcoach.target.service.InterviewTargetService;
import com.interviewcoach.user.entity.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * 目标岗位控制器，提供目标岗位的 CRUD 接口。
 */
@RestController
@RequestMapping("/api/targets")
public class InterviewTargetController {

    private final InterviewTargetService targetService;

    public InterviewTargetController(InterviewTargetService targetService) {
        this.targetService = targetService;
    }

    /**
     * 创建目标岗位。
     *
     * @param request 创建请求，包含 title 和 jd
     * @return 201 Created，返回创建的目标岗位 DTO
     */
    @PostMapping
    public ResponseEntity<InterviewTargetDto> createTarget(
            @Valid @RequestBody InterviewTargetCreateRequest request) {
        User user = SecurityUtils.currentUser();
        InterviewTargetDto dto = targetService.createTarget(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    /**
     * 获取当前用户所有目标岗位列表。
     *
     * @return 目标岗位 DTO 列表，按创建时间倒序
     */
    @GetMapping
    public List<InterviewTargetDto> listTargets() {
        User user = SecurityUtils.currentUser();
        return targetService.listTargets(user.getId());
    }

    /**
     * 获取指定目标岗位详情。
     *
     * @param id 目标岗位 ID
     * @return 目标岗位 DTO
     */
    @GetMapping("/{id}")
    public InterviewTargetDto getTarget(@PathVariable("id") UUID id) {
        User user = SecurityUtils.currentUser();
        return targetService.getTarget(id, user.getId());
    }

    /**
     * 更新目标岗位信息（标题、JD、状态）。
     *
     * @param id      目标岗位 ID
     * @param request 更新请求，title/jd/status 均可选
     * @return 更新后的目标岗位 DTO
     */
    @PatchMapping("/{id}")
    public InterviewTargetDto updateTarget(
            @PathVariable("id") UUID id,
            @RequestBody InterviewTargetUpdateRequest request) {
        User user = SecurityUtils.currentUser();
        return targetService.updateTarget(id, user.getId(), request);
    }

    /**
     * 删除目标岗位及其所有关联数据。
     *
     * @param id 目标岗位 ID
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTarget(@PathVariable("id") UUID id) {
        User user = SecurityUtils.currentUser();
        targetService.deleteTarget(id, user.getId());
        return ResponseEntity.noContent().build();
    }
}
