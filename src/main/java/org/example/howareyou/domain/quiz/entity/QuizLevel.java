package org.example.howareyou.domain.quiz.entity;

public enum QuizLevel {
    A, // A1,A2 - A
    B, // B1,B2 - B
    C; // C1 - C

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
}
