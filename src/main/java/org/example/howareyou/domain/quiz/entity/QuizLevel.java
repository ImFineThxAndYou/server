package org.example.howareyou.domain.quiz.entity;

import java.util.Locale;

public enum QuizLevel {
    A, // A1, A2 → A
    B, // B1, B2 → B
    C; // C1, C2 → C

    public boolean match(String levelRow) {
        if (levelRow == null) {
            return false;
        }
        String lv = levelRow.trim().toLowerCase();
        return switch (this) {
            case A -> lv.equals("a1") || lv.equals("a2") || lv.equals("a");
            case B -> lv.equals("b1") || lv.equals("b2") || lv.equals("b");
            case C -> lv.equals("c1") || lv.equals("c2") || lv.equals("c");
        };
    }

    public static QuizLevel from(String levelRow) {
        if (levelRow == null || levelRow.isBlank()) return null;
        String lv = levelRow.trim().toLowerCase();
        if (lv.equals("a1") || lv.equals("a2") || lv.equals("a")) return A;
        if (lv.equals("b1") || lv.equals("b2") || lv.equals("b")) return B;
        if (lv.equals("c1") || lv.equals("c2") || lv.equals("c")) return C;
        return null;
    }
    /** RequestParam으로 받은 문자열을 QuizLevel로 매핑 (null/빈 문자열이면 null 반환 = 전체랜덤) */
    public static QuizLevel fromParam(String p) {
        if (p == null || p.isBlank()) return null; // 전체 난이도
        String v = p.trim().toUpperCase(Locale.ROOT);
        switch (v) {
            case "A":
            case "A1":
            case "A2":
                return A;
            case "B":
            case "B1":
            case "B2":
                return B;
            case "C":
            case "C1":
            case "C2":
                return C;
            default:
                return null; // 모르면 전체 난이도로 처리
        }
    }
}