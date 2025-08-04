package org.example.howareyou.domain.member.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MembernameRequest(
        @NotBlank @Pattern(regexp="^[a-z0-9_]{3,30}$")
        String membername) {}