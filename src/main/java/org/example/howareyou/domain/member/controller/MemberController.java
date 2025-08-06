package org.example.howareyou.domain.member.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.howareyou.domain.member.dto.request.FilterRequest;
import org.example.howareyou.domain.member.dto.request.MembernameRequest;
import org.example.howareyou.domain.member.dto.request.ProfileCreateRequest;
import org.example.howareyou.domain.member.dto.response.MembernameResponse;
import org.example.howareyou.domain.member.dto.response.ProfileResponse;
import org.example.howareyou.domain.member.dto.response.MemberStatusResponse;
import org.example.howareyou.domain.member.entity.MemberProfile;
import org.example.howareyou.domain.member.service.MemberService;
import org.example.howareyou.global.security.CustomMemberDetails;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "회원 관리", description = "사용자 계정 및 프로필 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping(
        value = "/api/v1/members",
        produces = MediaType.APPLICATION_JSON_VALUE
)
public class MemberController {

    private final MemberService memberService;

    @Operation(summary = "내 프로필 조회", description = "현재 로그인한 사용자의 프로필 정보를 조회합니다.")
    @GetMapping("/me")
    public ResponseEntity<ProfileResponse> getMyProfile(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomMemberDetails memberDetails) {
        return ResponseEntity.ok(memberService.getMyProfile(memberDetails.getId()));
    }

    @Operation(summary = "프로필 생성/수정", description = "현재 로그인한 사용자의 프로필을 생성하거나 수정합니다.")
    @PutMapping(value = "/me", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProfileResponse> updateMyProfile(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomMemberDetails memberDetails,
            @Valid @RequestBody ProfileCreateRequest request) {
        return ResponseEntity.ok(memberService.updateMyProfile(memberDetails.getId(), request));
    }

    @Operation(summary = "온라인 상태 업데이트", description = "현재 로그인한 사용자의 온라인 상태를 업데이트합니다.")
    @PostMapping("/profiles/me/status")
    public ResponseEntity<Void> setPresence(
            @AuthenticationPrincipal Long memberId,
            @RequestParam boolean online) {
        memberService.updatePresence(memberId, online);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "공개 프로필 조회", description = "특정 사용자의 공개 프로필을 조회합니다.")
    @GetMapping("/{membername}")
    public ResponseEntity<ProfileResponse> getPublicProfile(
            @Parameter(description = "조회할 사용자 ID", required = true, example = "1")
            @PathVariable String membername) {
        return ResponseEntity.ok(memberService.getPublicProfile(membername));
    }

    @Operation(summary = "사용자 상태 조회", description = "사용자의 온라인/오프라인 상태, 마지막 활동 시간, 프로필 완료 여부를 조회합니다.")
    @GetMapping("/{membername}/status")
    public ResponseEntity<MemberStatusResponse> getMemberStatus(
            @Parameter(description = "상태를 조회할 사용자 ID", required = true, example = "1")
            @PathVariable String membername) {
        return ResponseEntity.ok(memberService.getMemberStatus(membername));
    }

    @Operation(summary = "계정 삭제", description = "현재 로그인한 사용자의 계정을 삭제합니다.\n- 소프트 삭제가 적용되어 실제 데이터는 보관되지만 로그인이 불가능합니다.")
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMyAccount(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomMemberDetails memberDetails) {
        memberService.deleteAccount(memberDetails.getId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Membername 중복 확인", description = "입력한 Membername이 이미 존재하는지 확인합니다. (프론트 실시간 AJAX용)")
    @GetMapping("/membername/duplicated")
    public ResponseEntity<Boolean> duplicated(
            @RequestParam String membername) {
        return ResponseEntity.ok(memberService.isMembernameDuplicated(membername));
    }

    @Operation(summary = "Membername 설정", description = "사용자의 Membername을 최초 설정하거나 변경합니다.")
    @PostMapping("/me/membername")
    public ResponseEntity<MembernameResponse> setMembername(
            @AuthenticationPrincipal CustomMemberDetails memberDetails,
            @Valid @RequestBody MembernameRequest request) {
        return ResponseEntity.ok(memberService.setMembername(memberDetails.id(), request));
    }

    @Operation(
            summary     = "같은 관심사를 가진 다른 유저 조회",
            description = """
            로그인한 사용자가 설정한 관심사와 동일한 카테고리를 가진
            다른 활성화된 사용자들의 프로필 목록을 반환합니다.
            """

    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description  = "조회 성공",
                    content      = @Content(
                            mediaType    = "application/json",
                            array        = @ArraySchema(
                                    schema = @Schema(implementation = MemberProfile.class)
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description  = "인증되지 않음",
                    content      = @Content(
                            mediaType = "application/json",
                            schema    = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description  = "서버 에러",
                    content      = @Content(
                            mediaType = "application/json",
                            schema    = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @GetMapping("/me/peers")
    public ResponseEntity<List<MemberProfile>> getPeers(
            @Parameter(
                    name        = "memberDetails",
                    description = "인증된 사용자 정보 (스프링 시큐리티가 주입)",
                    hidden      = true
            )
            @AuthenticationPrincipal CustomMemberDetails memberDetails){
        List<MemberProfile> users = memberService.findOthersWithSameCategories(memberDetails.getId());
        return ResponseEntity.ok().body(users);
    }

    @Operation(
            summary = "관심사 필터 기반 다른 유저 조회",
            description = """
        로그인한 사용자의 관심사를 기준으로,
        필터에 지정된 모든 카테고리를 포함하는 다른 활성화된 유저 프로필 목록을 반환합니다.
      """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = MemberProfile.class))
                    )
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "인증되지 않음",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/me/filter")
    public ResponseEntity<List<MemberProfile>> getFilter(
            @AuthenticationPrincipal CustomMemberDetails memberDetails,
            @RequestBody @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "관심사 필터 요청",
                    required = true,
                    content = @Content(schema = @Schema(implementation = FilterRequest.class))
            )
            @Valid FilterRequest filterRequest
    ) {
        List<MemberProfile> users = memberService.findOthersWithFilter(filterRequest, memberDetails.getId());
        return ResponseEntity.ok(users);
    }
}