package org.example.howareyou.domain.member.dto.response;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MemberStatusResponse {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long          memberId;
    private boolean       online;
    private LocalDateTime lastActiveAt;
    private boolean       profileCompleted;
}