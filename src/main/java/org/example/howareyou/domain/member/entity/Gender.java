package org.example.howareyou.domain.member.entity;

/**
 * 성별을 나타내는 열거형
 */
public enum Gender {
    MALE("남성"),
    FEMALE("여성"),
    OTHER("기타"),
    PREFER_NOT_TO_SAY("선택 안함");

    private final String displayName;

    Gender(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
