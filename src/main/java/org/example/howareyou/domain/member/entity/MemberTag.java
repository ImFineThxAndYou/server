package org.example.howareyou.domain.member.entity;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum MemberTag {
    LANGUAGE_LEARNING,
    TRAVEL,
    CULTURE,
    BUSINESS,
    EDUCATION,
    TECHNOLOGY,
    SPORTS,
    MUSIC,
    FOOD,
    ART,
    SCIENCE,
    HISTORY,
    MOVIES,
    GAMES,
    LITERATURE,
    PHOTOGRAPHY,
    NATURE,
    FITNESS,
    FASHION,
    VOLUNTEERING,
    ANIMALS,
    CARS,
    DIY,
    FINANCE;

    @JsonValue
    public String getValue() {
        return this.name();
    }

    @JsonCreator
    public static MemberTag fromValue(String value) {
        try {
            return MemberTag.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown category: " + value);
        }
    }
}
