package com.interviewcoach.coachingmemory.controller;

import com.interviewcoach.coachingmemory.service.CoachingMemoryService;
import com.interviewcoach.common.api.CoachingMemoryCorrectionRequest;
import com.interviewcoach.common.api.CoachingMemoryDto;
import com.interviewcoach.common.api.CoachingMemoryImportRequest;
import com.interviewcoach.common.security.SecurityUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * 教练记忆控制器，提供记忆查询、用户纠错和本地记忆导入接口。
 */
@RestController
@RequestMapping("/api/coaching-memories")
public class CoachingMemoryController {

    private final CoachingMemoryService memoryService;

    public CoachingMemoryController(CoachingMemoryService memoryService) {
        this.memoryService = memoryService;
    }

    /**
     * 查询指定目标岗位下的所有教练记忆。
     *
     * @param targetId 目标岗位 ID
     * @return 教练记忆 DTO 列表
     */
    @GetMapping("/target/{targetId}")
    public List<CoachingMemoryDto> getByTarget(@PathVariable UUID targetId) {
        return memoryService.getMemories(targetId, SecurityUtils.currentUser().getId());
    }

    /**
     * 查询单条教练记忆详情。
     *
     * @param id 教练记忆 ID
     * @return 教练记忆 DTO
     */
    @GetMapping("/{id}")
    public CoachingMemoryDto get(@PathVariable UUID id) {
        return memoryService.getMemory(id, SecurityUtils.currentUser().getId());
    }

    /**
     * 用户纠正教练记忆中的某条记录，将来源标记为 corrected 或 rejected。
     *
     * @param id      教练记忆 ID
     * @param request 包含字段名、索引、新内容和来源的纠正请求
     * @return 更新后的教练记忆 DTO
     */
    @PatchMapping("/{id}/corrections")
    public CoachingMemoryDto correctMemory(@PathVariable UUID id,
                                           @RequestBody CoachingMemoryCorrectionRequest request) {
        return memoryService.correctMemoryItem(id, SecurityUtils.currentUser().getId(), request);
    }

    /**
     * 从本地教练记忆归档导入记忆摘要，标记为 inferred 低可信度。
     *
     * @param request 包含目标岗位 ID 和摘要列表的导入请求
     * @return 导入后的教练记忆 DTO
     */
    @PostMapping("/import")
    public CoachingMemoryDto importFromLocalArchive(@RequestBody CoachingMemoryImportRequest request) {
        return memoryService.importFromLocalArchive(
                SecurityUtils.currentUser(), UUID.fromString(request.targetId()), request.summaries());
    }
}
