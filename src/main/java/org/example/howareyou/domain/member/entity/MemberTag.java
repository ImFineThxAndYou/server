package org.example.howareyou.domain.member.entity;

<<<<<<<< HEAD:src/main/java/org/example/howareyou/domain/member/entity/MemberTag.java
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
========
public enum Category { SPORTS, MUSIC, MOVIE, GAME, IT }
>>>>>>>> f3fa66f9ae3bd6e81cb88d41e78a357f6f36c27a:src/main/java/org/example/howareyou/domain/member/entity/Category.java
