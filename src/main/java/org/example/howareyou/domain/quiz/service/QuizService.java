package org.example.howareyou.domain.quiz.service;

import org.example.howareyou.domain.quiz.dto.submit.SubmitResponse;

import java.util.List;

public interface QuizService {
    SubmitResponse gradeQuiz(String QuizeUUID, List<Integer> selectedIndices);
}
