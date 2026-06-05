package com.interviewcoach.assessment.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewcoach.common.api.AssessmentQuestionScoreDto;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.io.IOException;
import java.util.List;

/**
 * JPA 属性转换器，将逐题评分列表与 JSON 字符串互转，用于数据库存储。
 */
@Converter
public class QuestionScoreListConverter implements AttributeConverter<List<AssessmentQuestionScoreDto>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<AssessmentQuestionScoreDto> attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize question scores to JSON", e);
        }
    }

    @Override
    public List<AssessmentQuestionScoreDto> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(dbData, new TypeReference<>() {});
        } catch (IOException e) {
            throw new IllegalStateException("Failed to deserialize question scores from JSON", e);
        }
    }
}
