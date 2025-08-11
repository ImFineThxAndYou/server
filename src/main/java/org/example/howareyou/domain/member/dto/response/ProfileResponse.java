package org.example.howareyou.domain.member.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.example.howareyou.domain.member.entity.Category;
import org.example.howareyou.domain.member.entity.Member;
import org.example.howareyou.domain.member.entity.MemberProfile;

import java.time.LocalDate;
import java.util.Set;

@Getter
@Builder
@AllArgsConstructor
public class ProfileResponse {

    @JsonSerialize(using = ToStringSerializer.class)
    private String          membername;
    private String          nickname;
    private String          avatarUrl;
    private String          bio;
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
                .membername(p.getMember().getMembername())
                .nickname(p.getNickname())
                .avatarUrl(p.getAvatarUrl())
                .bio(p.getBio())
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

    public static ProfileResponse from(Member m) {
        return ProfileResponse.builder()
                .membername(m.getMembername())
                .nickname(m.getProfile().getNickname())
                .avatarUrl(m.getProfile().getAvatarUrl())
                .bio(m.getProfile().getBio())
                .interests(m.getProfile().getInterests())
                .completed(m.getProfile().isCompleted())
                .language(m.getProfile().getLanguage())
                .timezone(m.getProfile().getTimezone())
                .birthDate(m.getProfile().getBirthDate())
                .age(m.getProfile().getAge())
                .country(m.getProfile().getCountry())
                .region(m.getProfile().getRegion())
                .build();
    }
}