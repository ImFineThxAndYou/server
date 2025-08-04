package org.example.howareyou.domain.member.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.*;
import org.example.howareyou.domain.member.entity.Category;
import org.example.howareyou.domain.member.entity.MemberProfile;

import java.time.LocalDate;
import java.util.Set;

@Getter
@Builder
@AllArgsConstructor
public class ProfileResponse {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long            memberId;
    private String          nickname;
    private String          avatarUrl;
    private String          statusMessage;
    private Set<Category>   interests;

    /* 로케일 · 사용자 정보 */
    private boolean   completed;
    private String    language;
    private String    timezone;
    private LocalDate birthDate;
    private int       age;
    private String    country;
    private String    region;

    public static ProfileResponse from(MemberProfile p) {
        return ProfileResponse.builder()
                .memberId(p.getId())
                .nickname(p.getNickname())
                .avatarUrl(p.getAvatarUrl())
                .statusMessage(p.getStatusMessage())
                .interests(p.getInterests())
                .completed(p.isCompleted())
                .language(p.getLanguage())
                .timezone(p.getTimezone())
                .birthDate(p.getBirthDate())
                .age(p.getAge())
                .country(p.getCountry())
                .region(p.getRegion())
                .build();
    }
}