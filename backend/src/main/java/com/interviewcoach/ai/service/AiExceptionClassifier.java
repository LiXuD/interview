package com.interviewcoach.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;

final class AiExceptionClassifier {

    private AiExceptionClassifier() {
    }

    static boolean hasJsonProcessingCause(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof JsonProcessingException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
