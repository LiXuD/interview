package com.interviewcoach.aiusage.controller;

import com.interviewcoach.aiusage.service.AiUsageQueryService;
import com.interviewcoach.common.api.AiUsageBreakdownDto;
import com.interviewcoach.common.api.AiUsageDailyPointDto;
import com.interviewcoach.common.api.AiUsageSummaryDto;
import com.interviewcoach.common.security.SecurityUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@RestController
@RequestMapping("/api/ai-usage/me")
public class AiUsageController {

    private final AiUsageQueryService queryService;

    public AiUsageController(AiUsageQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/summary")
    public AiUsageSummaryDto summary(@RequestParam(required = false) String startDate,
                                     @RequestParam(required = false) String endDate) {
        return queryService.summary(SecurityUtils.currentUser().getId(), startOfDay(startDate), endOfDay(endDate));
    }

    @GetMapping("/daily")
    public List<AiUsageDailyPointDto> daily(@RequestParam(required = false) String startDate,
                                            @RequestParam(required = false) String endDate) {
        return queryService.daily(SecurityUtils.currentUser().getId(), startOfDay(startDate), endOfDay(endDate));
    }

    @GetMapping("/by-task")
    public List<AiUsageBreakdownDto> byTask(@RequestParam(required = false) String startDate,
                                            @RequestParam(required = false) String endDate) {
        return queryService.byTask(SecurityUtils.currentUser().getId(), startOfDay(startDate), endOfDay(endDate));
    }

    @GetMapping("/by-model")
    public List<AiUsageBreakdownDto> byModel(@RequestParam(required = false) String startDate,
                                             @RequestParam(required = false) String endDate) {
        return queryService.byModel(SecurityUtils.currentUser().getId(), startOfDay(startDate), endOfDay(endDate));
    }

    @GetMapping("/by-provider")
    public List<AiUsageBreakdownDto> byProvider(@RequestParam(required = false) String startDate,
                                                @RequestParam(required = false) String endDate) {
        return queryService.byProvider(SecurityUtils.currentUser().getId(), startOfDay(startDate), endOfDay(endDate));
    }

    private static Instant startOfDay(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDate.parse(value).atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    private static Instant endOfDay(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDate.parse(value).plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
    }
}
