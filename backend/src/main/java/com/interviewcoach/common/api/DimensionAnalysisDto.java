package com.interviewcoach.common.api;

import java.util.List;

public record DimensionAnalysisDto(String targetId, List<DimensionDetailDto> dimensions) {
}
