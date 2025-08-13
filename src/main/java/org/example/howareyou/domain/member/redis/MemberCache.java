package org.example.howareyou.domain.member.redis;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.example.howareyou.domain.member.entity.Category;
import org.example.howareyou.domain.member.entity.Member;
import org.example.howareyou.domain.member.entity.MemberProfile;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
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
            @JsonProperty("interests") Object interests, // Object로 받아서 안전하게 처리
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
        this.interests = parseInterests(interests); // 안전한 파싱
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

    /**
     * interests 필드의 안전한 역직렬화
     * 다양한 형태로 저장된 interests를 Set<Category>로 변환
     */
    @SuppressWarnings("unchecked")
    private Set<Category> parseInterests(Object interests) {
        if (interests == null) {
            return new HashSet<>();
        }
        
        // 이미 Set<Category>인 경우
        if (interests instanceof Set) {
            Set<?> set = (Set<?>) interests;
            if (!set.isEmpty() && set.iterator().next() instanceof Category) {
                return (Set<Category>) interests;
            }
        }
        
        // Set<String> 또는 List<String>인 경우 Category로 변환
        Set<Category> result = new HashSet<>();
        if (interests instanceof Iterable) {
            for (Object item : (Iterable<?>) interests) {
                if (item instanceof String) {
                    try {
                        result.add(Category.valueOf((String) item));
                    } catch (IllegalArgumentException e) {
                        // 알 수 없는 카테고리는 무시
                        continue;
                    }
                } else if (item instanceof Category) {
                    result.add((Category) item);
                }
            }
        }
        
        return result;
    }

    /* factory */
    public static MemberCache from(Member m){
        MemberProfile p = m.getProfile();
        
        // interests를 안전하게 처리 (Lazy Loading 방지)
        Set<Category> interests = null;
        if (p != null && p.getInterests() != null) {
            try {
                // Lazy Loading이 발생할 수 있으므로 새로운 HashSet으로 복사
                interests = new HashSet<>(p.getInterests());
            } catch (Exception e) {
                // Lazy Loading 에러 발생 시 빈 Set으로 처리
                interests = new HashSet<>();
            }
        }
        
        return MemberCache.builder()
                .id(m.getId())
                .membername(m.getMembername())
                .nickname(p != null ? p.getNickname() : null)
                .avatarUrl(p != null ? p.getAvatarUrl() : null)
                .bio(p != null ? p.getBio() : null)
                .interests(interests)
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