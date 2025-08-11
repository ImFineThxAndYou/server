package org.example.howareyou.domain.member.redis;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.example.howareyou.domain.member.entity.Category;
import org.example.howareyou.domain.member.entity.Member;
import org.example.howareyou.domain.member.entity.MemberProfile;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;


@Getter
@Setter
@Builder
public class MemberCache implements Serializable {

    /* ─── 프로필 전문 ─── */
    private Long            id;
    private String          membername;
    private String          nickname;
    private String          avatarUrl;
    private String          bio;
    private Set<Category> interests;
    private boolean         completed;
    private String          language;
    private String          timezone;
    private LocalDate birthDate;
    private int             age;
    private String          country;
    private String          region;

    /* ─── Presence ─── */
    private boolean online;
    private Instant lastActiveAt;   // 캐시 생성(또는 touch) 시각

    // Jackson 역직렬화를 위한 기본 생성자
    public MemberCache() {
    }

    // Jackson 역직렬화를 위한 생성자
    @JsonCreator
    public MemberCache(
            @JsonProperty("id") Long id,
            @JsonProperty("membername") String membername,
            @JsonProperty("nickname") String nickname,
            @JsonProperty("avatarUrl") String avatarUrl,
            @JsonProperty("bio") String bio,
            @JsonProperty("interests") Set<Category> interests,
            @JsonProperty("completed") boolean completed,
            @JsonProperty("language") String language,
            @JsonProperty("timezone") String timezone,
            @JsonProperty("birthDate") LocalDate birthDate,
            @JsonProperty("age") int age,
            @JsonProperty("country") String country,
            @JsonProperty("region") String region,
            @JsonProperty("online") boolean online,
            @JsonProperty("lastActiveAt") Instant lastActiveAt) {
        this.id = id;
        this.membername = membername;
        this.nickname = nickname;
        this.avatarUrl = avatarUrl;
        this.bio = bio;
        this.interests = interests;
        this.completed = completed;
        this.language = language;
        this.timezone = timezone;
        this.birthDate = birthDate;
        this.age = age;
        this.country = country;
        this.region = region;
        this.online = online;
        this.lastActiveAt = lastActiveAt;
    }

    /* factory */
    public static MemberCache from(Member m){
        MemberProfile p = m.getProfile();
        
        return MemberCache.builder()
                .id(m.getId())
                .membername(m.getMembername())
                .nickname(p != null ? p.getNickname() : null)
                .avatarUrl(p != null ? p.getAvatarUrl() : null)
                .bio(p != null ? p.getBio() : null)
                .interests(p != null ? p.getInterests() : null)
                .completed(p != null ? p.isCompleted() : false)
                .language(p != null ? p.getLanguage() : null)
                .timezone(p != null ? p.getTimezone() : null)
                .birthDate(p != null ? p.getBirthDate() : null)
                .age(p != null ? p.getAge() : 0)
                .country(p != null ? p.getCountry() : null)
                .region(p != null ? p.getRegion() : null)
                .online(true)
                .lastActiveAt(Instant.now())
                .build();
    }
        
    public MemberCache touch(){
        this.online       = true;
        this.lastActiveAt = Instant.now();
        return this;
    }
}