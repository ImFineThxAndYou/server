package org.example.howareyou.domain.vocabulary.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.example.howareyou.domain.chat.service.ChatRoomService;
import org.example.howareyou.domain.member.dto.response.MemberProfileViewForVoca;
import org.example.howareyou.domain.member.service.MemberService;
import org.example.howareyou.domain.vocabulary.document.ChatRoomVocabulary;
import org.example.howareyou.domain.vocabulary.document.MemberVocabulary;
import org.example.howareyou.domain.vocabulary.dto.AggregatedWordEntry;
import org.example.howareyou.domain.vocabulary.repository.ChatRoomVocabularyRepository;
import org.example.howareyou.domain.vocabulary.repository.MemberVocabularyRepository;
import org.example.howareyou.global.exception.CustomException;
import org.example.howareyou.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
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
    private final MongoTemplate mongoTemplate;

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
                                        .build(),
                                //이미 있던 값과 새 값 병합하는 함수
                                (exist, inc) -> {

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

    /* -------------------- 사용자 별 단어장 조회용 -------------------- */

    /*
    * 전체 사용자의 전체 단어장 보기
    * */
    public List<MemberVocabulary> findAll() {
        return memberVocabularyRepository.findAll();
    }

    //가장 최신 단어들만 뽑아서 중복없이 전체 조회
    public Page<AggregatedWordEntry> findLatestUniqueWordsPaged(String membername,
                                                                String lang,
                                                                String pos,
                                                                int page,
                                                                int size) {
        int skip = Math.max(page, 0) * Math.max(size, 1);

        // items
        List<AggregatedWordEntry> items =
                memberVocabularyRepository.findLatestUniqueWords(membername, lang, pos, skip, size);

        // total
        long total = 0L;
        List<MemberVocabularyRepository.CountOnly> cnt =
                memberVocabularyRepository.countLatestUniqueWords(membername, lang, pos);
        if (cnt != null && !cnt.isEmpty() && cnt.get(0).getTotal() != null) {
            total = cnt.get(0).getTotal();
        }

        return new PageImpl<>(items, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "analyzedAt")), total);
    }

    //사용자 + 날짜별
    public Page<MemberVocabulary.MemberWordEntry> findWordsByMemberAndDatePaged(
            String membername,
            LocalDate date,
            int page,
            int size,
            String sortBy,
            String direction
    ) {
        String docId = membername + "_" + date;
        MemberVocabulary doc = memberVocabularyRepository.findById(docId)
                .orElseThrow(() -> new CustomException(ErrorCode.VOCABULARY_NOT_FOUND));

        List<MemberVocabulary.MemberWordEntry> words = new ArrayList<>(doc.getWords());
        if (words.isEmpty()) {
            return new PageImpl<>(List.of(), PageRequest.of(page, size), 0);
        }

        // 정렬
        Comparator<MemberVocabulary.MemberWordEntry> cmp = buildComparator(sortBy);
        if ("desc".equalsIgnoreCase(direction)) cmp = cmp.reversed();
        words.sort(cmp);

        // 페이징 슬라이스
        int from = Math.min(page * size, words.size());
        int to   = Math.min(from + size, words.size());
        List<MemberVocabulary.MemberWordEntry> slice = words.subList(from, to);

        return new PageImpl<>(slice, PageRequest.of(page, size), words.size());
    }

    /* -------------------- 대시보드용 메서드들 -------------------- */

    /**
     * 사용자 ID로 전체 단어 개수 조회 (기간별 필터링 없음)
     */
    public long countTotalWordsByMemberId(Long memberId) {
        try {
            String membername = memberService.findMembernameById(memberId);
            if (membername == null) {
                log.warn("멤버를 찾을 수 없습니다 - memberId: {}", memberId);
                return 0;
            }
            List<MemberVocabularyRepository.CountOnly> result = memberVocabularyRepository.countTotalUniqueWords(membername);
            return result.isEmpty() ? 0 : result.get(0).getTotal();
        } catch (Exception e) {
            log.error("전체 단어 개수 조회 실패 - memberId: {}", memberId, e);
            return 0;
        }
    }

    /**
     * 사용자 ID로 단어 개수 조회 (기간별)
     */
    public long countByMemberIdAndPeriod(Long memberId, LocalDate from, LocalDate to) {
        try {
            String membername = memberService.findMembernameById(memberId);
            if (membername == null) {
                log.warn("멤버를 찾을 수 없습니다 - memberId: {}", memberId);
                return 0;
            }
            List<MemberVocabularyRepository.CountOnly> result = memberVocabularyRepository.countTotalUniqueWordsByPeriod(
                membername, "Asia/Seoul", from.toString(), to.toString()); // Hardcoded timezone for now
            return result.isEmpty() ? 0 : result.get(0).getTotal();
        } catch (Exception e) {
            log.error("기간별 단어 개수 조회 실패 - memberId: {}, from: {}, to: {}", memberId, from, to, e);
            return 0;
        }
    }

    /**
     * 언어별 단어 개수 조회 (기간별)
     */
    public long countByMemberAndLangAndPeriod(Long memberId, String lang, LocalDate from, LocalDate to) {
        try {
            String membername = memberService.findMembernameById(memberId);
            if (membername == null) {
                log.warn("멤버를 찾을 수 없습니다 - memberId: {}", memberId);
                return 0;
            }
            List<MemberVocabularyRepository.CountOnly> result = memberVocabularyRepository.countByMemberAndLangAndPeriod(
                membername, lang, "Asia/Seoul", from.toString(), to.toString());
            return result.isEmpty() ? 0 : result.get(0).getTotal();
        } catch (Exception e) {
            log.error("언어별 단어 개수 조회 실패 - memberId: {}, lang: {}, from: {}, to: {}", memberId, lang, from, to, e);
            return 0;
        }
    }

    /**
     * 품사별 단어 개수 조회 (기간별)
     */
    public long countByMemberAndPosAndPeriod(Long memberId, String pos, LocalDate from, LocalDate to) {
        try {
            String membername = memberService.findMembernameById(memberId);
            if (membername == null) {
                log.warn("멤버를 찾을 수 없습니다 - memberId: {}", memberId);
                return 0;
            }
            List<MemberVocabularyRepository.CountOnly> result = memberVocabularyRepository.countByMemberAndPosAndPeriod(
                membername, pos, "Asia/Seoul", from.toString(), to.toString());
            return result.isEmpty() ? 0 : result.get(0).getTotal();
        } catch (Exception e) {
            log.error("품사별 단어 개수 조회 실패 - memberId: {}, pos: {}, from: {}, to: {}", memberId, pos, from, to, e);
            return 0;
        }
    }

    /**
     * 언어+품사별 단어 개수 조회 (기간별)
     */
    public long countByMemberAndLangAndPosAndPeriod(Long memberId, String lang, String pos, LocalDate from, LocalDate to) {
        try {
            String membername = memberService.findMembernameById(memberId);
            if (membername == null) {
                log.warn("멤버를 찾을 수 없습니다 - memberId: {}", memberId);
                return 0;
            }
            List<MemberVocabularyRepository.CountOnly> result = memberVocabularyRepository.countByMemberAndLangAndPosAndPeriod(
                membername, lang, pos, "Asia/Seoul", from.toString(), to.toString());
            return result.isEmpty() ? 0 : result.get(0).getTotal();
        } catch (Exception e) {
            log.error("언어+품사별 단어 개수 조회 실패 - memberId: {}, lang: {}, pos: {}, from: {}, to: {}", 
                memberId, lang, pos, from, to, e);
            return 0;
        }
    }

    /**
     * 학습 잔디 데이터 조회
     */
    public Map<String, Integer> getLearningGrass(Long memberId, int year, ZoneId zoneId, String period) {
        try {
            String membername = memberService.findMembernameById(memberId);
            if (membername == null) {
                log.warn("멤버를 찾을 수 없습니다 - memberId: {}", memberId);
                return Map.of();
            }
            
            // 기간에 따른 날짜 범위 계산
            LocalDate to = LocalDate.now(zoneId);
            LocalDate from = calculateFromDate(to, period);
            
            List<MemberVocabularyRepository.DailyWordCount> result = memberVocabularyRepository.getDailyWordCountsByPeriod(
                membername, "Asia/Seoul", from.toString(), to.toString());
            
            Map<String, Integer> grass = new HashMap<>();
            for (MemberVocabularyRepository.DailyWordCount daily : result) {
                grass.put(daily.get_id(), daily.getCount());
            }
            
            return grass;
        } catch (Exception e) {
            log.error("학습 잔디 조회 실패 - memberId: {}, year: {}, period: {}", memberId, year, period, e);
            return Map.of();
        }
    }

    /**
     * 단어장 잔디 데이터 조회
     */
    public Map<String, Integer> getVocabularyGrass(Long memberId, int year, ZoneId zoneId, String period) {
        try {
            String membername = memberService.findMembernameById(memberId);
            if (membername == null) {
                log.warn("멤버를 찾을 수 없습니다 - memberId: {}", memberId);
                return Map.of();
            }
            
            // 기간에 따른 날짜 범위 계산
            LocalDate to = LocalDate.now(zoneId);
            LocalDate from = calculateFromDate(to, period);
            
            List<MemberVocabularyRepository.DailyWordCount> result = memberVocabularyRepository.getDailyWordCountsByPeriod(
                membername, "Asia/Seoul", from.toString(), to.toString());
            
            Map<String, Integer> grass = new HashMap<>();
            for (MemberVocabularyRepository.DailyWordCount daily : result) {
                grass.put(daily.get_id(), daily.getCount());
            }
            
            return grass;
        } catch (Exception e) {
            log.error("단어장 잔디 조회 실패 - memberId: {}, year: {}, period: {}", memberId, year, period, e);
            return Map.of();
        }
    }

    /**
     * 기간에 따른 시작 날짜 계산
     */
    private LocalDate calculateFromDate(LocalDate to, String period) {
        return switch (period) {
            case "week" -> to.minusWeeks(1);
            case "month" -> to.minusMonths(1);
            default -> to.minusWeeks(1); // 기본값은 주간
        };
    }

    private Comparator<MemberVocabulary.MemberWordEntry> buildComparator(String sortBy) {
        // 허용 필드: word|analyzedAt (기본 analyzedAt)
        return switch (sortBy == null ? "" : sortBy) {
            case "word"      -> Comparator.comparing(w -> safeLower(w.getWord()));
            case "analyzedAt", "" -> Comparator.comparing(MemberVocabulary.MemberWordEntry::getAnalyzedAt,
                    Comparator.nullsLast(Comparator.naturalOrder()));
            default          -> Comparator.comparing(MemberVocabulary.MemberWordEntry::getAnalyzedAt,
                    Comparator.nullsLast(Comparator.naturalOrder()));
        };
    }
    private String safeLower(String s) { return s == null ? "" : s.toLowerCase(); }


    //난이도별 조회
    public Page<AggregatedWordEntry> findLatestUniqueWordsByLevelPaged(
            String membername,
            String lang,
            String pos,
            String level,
            int page,
            int size
    ) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, size);
        int skip = safePage * safeSize;

        // items
        List<AggregatedWordEntry> items =
                memberVocabularyRepository.findLatestUniqueWordsByLevel(membername, lang, pos, level, skip, safeSize);

        // total
        long total = 0L;
        List<MemberVocabularyRepository.CountOnly> cnt =
                memberVocabularyRepository.countLatestUniqueWordsByLevel(membername, lang, pos, level);
        if (cnt != null && !cnt.isEmpty() && cnt.get(0).getTotal() != null) {
            total = cnt.get(0).getTotal();
        }

        return new PageImpl<>(
                items,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "analyzedAt")),
                total
        );
    }

}