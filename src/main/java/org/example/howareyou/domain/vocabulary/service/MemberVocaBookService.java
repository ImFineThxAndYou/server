package org.example.howareyou.domain.vocabulary.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.howareyou.domain.chat.service.ChatRoomService;
import org.example.howareyou.domain.member.dto.response.MemberProfileViewForVoca;
import org.example.howareyou.domain.member.service.MemberService;
import org.example.howareyou.domain.vocabulary.document.ChatRoomVocabulary;
import org.example.howareyou.domain.vocabulary.document.MemberVocabulary;
import org.example.howareyou.domain.vocabulary.repository.ChatRoomVocabularyRepository;
import org.example.howareyou.domain.vocabulary.repository.MemberVocabularyRepository;
import org.example.howareyou.global.exception.CustomException;
import org.example.howareyou.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;

/**
 * 사용자별 단어장 생성 서비스
 *
 * 동작 개요
 * 1) 단일 스케줄러가 주기적으로 실행된다.
 * 2) 모든 사용자를 순회하되, "해당 사용자의 타임존(Local time)이 05:00"인 경우에만 처리한다.
 * 3) 그 사용자 타임존 기준 "어제 00:00 ~ 오늘 00:00" 기간(=어제 하루)을 UTC Instant로 변환한다.
 * 4) 그 기간에 생성된 채팅방 단어장(ChatRoomVocabulary) 중,
 *    - 사용자가 참여한 채팅방의 것만 취합하고,
 *    - 사용자의 프로필 언어의 반대(lang)만 필터링해서
 *    - 동일 단어는 frequency를 누적한다.
 * 5) MongoDB user_vocabulary 컬렉션에 저장한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberVocaBookService {

    private final MemberService memberService;
    private final ChatRoomService chatRoomService;
    private final ChatRoomVocabularyRepository chatRoomVocabularyRepository;
    private final MemberVocabularyRepository memberVocabularyRepository;

    /**
     * 한 번에 전체 유저를 불러온다.
     */
    public void runByTimezoneWindow() {
        // 배치용 뷰 조회
        List<MemberProfileViewForVoca> profiles = memberService.findAllActiveProfilesForVoca();
        if (profiles.isEmpty()) {
            log.debug("🟡 처리할 회원이 없습니다.");
            return;
        }

        int processed = 0;

        for (MemberProfileViewForVoca profile : profiles) {
            try {
                // 타임존 기준 '시(hour)가 5'인지 간단 체크 (매시간 실행 전제)
                if (!shouldRunNowHourly(profile.timezone())) continue;

                // 사용자 타임존의 "어제 00:00 ~ 오늘 00:00" → UTC 범위(+ 문서 날짜)
                TimeRange range = resolveYesterdayRangeInTz(profile.timezone());
                String docId = profile.membername() + "_" + range.yesterLocalDate().toString();

                // 중복 생성 방지: 이미 문서 있으면 스킵
                if (memberVocabularyRepository.existsById(docId)) {
                    log.info("⏩ 이미 생성된 문서 스킵: {}", docId);
                    continue;
                }

                log.info("🕔 {} (tz: {}) 사용자 단어장 생성 - {} ~ {}",
                        profile.membername(), profile.timezone(), range.start(), range.end());

                // 사용자 단어장 생성
                generateVocabularyForMember(
                        profile.memberId(),
                        profile.membername(),
                        profile.language(),
                        range.start(),
                        range.end(),
                        docId
                );
                processed++;
            } catch (Exception e) {
                log.error("❌ 사용자 단어장 생성 실패 - member={}", profile.membername(), e);
            }
        }

        if (processed > 0) {
            log.info("✅ 타임존 05시 대상 처리 완료 - {}명", processed);
        }
    }


    /**
     * 외부/스케줄러에서 직접 호출 가능한 API형 메서드 (원하는 시간 범위로 실행)
     */
    public void generateVocabularyForMember(Long memberId,
                                            String membername,
                                            String userLang,
                                            Instant start,
                                            Instant end,
                                            String docId) {
        String targetLang = "ko".equalsIgnoreCase(userLang) ? "en" : "ko";

        // ✅ 사용자 참여 채팅방 UUID 미리 조회 (셋)
        Set<String> myRoomUuids = chatRoomService.getMyChatRoomUuids(memberId);
        if (myRoomUuids.isEmpty()) {
            log.info("ℹ️ 사용자 {} 참여 채팅방 없음 → 스킵", membername);
            return;
        }

        // 기간 내 방 단어장 조회
        List<ChatRoomVocabulary> roomVocabs =
                chatRoomVocabularyRepository.findByChatRoomUuidInAndAnalyzedAtBetween(myRoomUuids,start, end);

        Map<String, MemberVocabulary.MemberWordEntry> wordMap = new HashMap<>();

        for (ChatRoomVocabulary vocab : roomVocabs) {
            String roomUuid = vocab.getChatRoomUuid();
            Instant analyzedAt = vocab.getAnalyzedAt();

            vocab.getWords().stream()
                    .filter(w -> w.getLang().equalsIgnoreCase(targetLang))
                    .forEach(w -> {
                        String key = w.getWord().toLowerCase() + "|" + w.getPos().toLowerCase(); // 단어+품사 기준 병합

                        wordMap.merge(
                                key,
                                MemberVocabulary.MemberWordEntry.builder()
                                        .word(w.getWord())
                                        .meaning(w.getMeaning())
                                        .pos(w.getPos())
                                        .lang(w.getLang())
                                        .level(w.getLevel())
                                        .dictionaryType(w.getDictionaryType())
                                        .chatRoomUuid(roomUuid)
                                        .chatMessageId(new ArrayList<>(w.getMessageIds()))
                                        .example(new ArrayList<>(w.getUsedInMessages()))
                                        .analyzedAt(analyzedAt)
                                        .frequency(1)
                                        .build(),
                                //이미 있던 값과 새 값 병합하는 함수
                                (exist, inc) -> {
                                    // 빈도 합산
                                    exist.setFrequency(exist.getFrequency() + 1);

                                    // 최신 채팅방 정보로 교체
                                    exist.setChatRoomUuid(roomUuid);
                                    exist.setChatMessageId(new ArrayList<>(inc.getChatMessageId())); // 최신 메시지 ID
                                    exist.setExample(new ArrayList<>(inc.getExample()));             // 최신 예문

                                    // 분석 시점 최신값 유지
                                    if (inc.getAnalyzedAt().isAfter(exist.getAnalyzedAt())) {
                                        exist.setAnalyzedAt(inc.getAnalyzedAt());
                                    }
                                    return exist;
                                }
                        );
                    });
        }

        if (wordMap.isEmpty()) {
            log.info("ℹ️ {} 대상 단어 없음 → 스킵", membername);
            return;
        }

//        String docId = membername + "_" + LocalDate.now(ZoneId.of("UTC")).minusDays(1);

        MemberVocabulary doc = MemberVocabulary.builder()
                .id(docId)
                .membername(membername)
                .createdAt(Instant.now())
                .words(new ArrayList<>(wordMap.values()))
                .build();

        memberVocabularyRepository.save(doc);
        log.info("💾 저장 완료: {} [{}개 단어] (docId={})", membername, wordMap.size(), docId);

    }

    /* -------------------- 내부 유틸들 -------------------- */

    /** 매 시간 실행 전제: 분/초는 고려하지 않고 '해당 타임존의 시(hour)가 5인지'만 본다 */
    private boolean shouldRunNowHourly(String timezone) {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of(timezone));
        return now.getHour() == 5;
    }

    /** 어제(사용자 타임존)의 시작/끝 + 문서 ID용 로컬 날짜 */
    private TimeRange resolveYesterdayRangeInTz(String timezone) {
        ZoneId zone = ZoneId.of(timezone);
        ZonedDateTime now = ZonedDateTime.now(zone);
        ZonedDateTime startZdt = now.minusDays(1).toLocalDate().atStartOfDay(zone);
        ZonedDateTime endZdt = startZdt.plusDays(1);
        return new TimeRange(startZdt.toInstant(), endZdt.toInstant(), startZdt.toLocalDate());
    }

    /** 문서 생성 범위/날짜 전달용 */
    private record TimeRange(Instant start, Instant end, LocalDate yesterLocalDate) {}


    /*
    * 사용자 별 단어장 조회용
    * */
    public List<MemberVocabulary> findAll() {
        return memberVocabularyRepository.findAll();
    }
// 멤버 이름별로 단어장
    public List<MemberVocabulary> findByMembername(String membername) {
        return memberVocabularyRepository.findByMembername(membername);
    }
// 멤버 별로 단어장 + 날짜 지정까지
    public MemberVocabulary findByMembernameAndDate(String membername, LocalDate date) {
        String docId = membername + "_" + date.toString();
        return memberVocabularyRepository.findById(docId)
                .orElseThrow(() -> new CustomException(ErrorCode.VOCABULARY_NOT_FOUND));
    }

}