package org.example.howareyou.domain.vocabulary.service;

import com.opencsv.bean.CsvToBeanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.howareyou.domain.vocabulary.document.DictionaryData;
import org.example.howareyou.domain.vocabulary.repository.DictionaryDataRepository;
import org.example.howareyou.domain.vocabulary.service.dto.VocabularyCsvRow;
import org.springframework.stereotype.Service;

import java.io.InputStreamReader;
import java.io.Reader;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CsvImportService {

    private final DictionaryDataRepository vocabularyDataRepository;

    public void importCsv(String filename) {
        String path = "/voca/" + filename;

        try (Reader reader = new InputStreamReader(getClass().getResourceAsStream(path))) {
            List<VocabularyCsvRow> rows = new CsvToBeanBuilder<VocabularyCsvRow>(reader)
                    .withType(VocabularyCsvRow.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build()
                    .parse();

            Instant now = Instant.now();

            List<DictionaryData> vocabularyList = rows.stream()
                    .map(row -> DictionaryData.builder()
                            .word(row.getWord())
                            .meaning(row.getMeaning())
                            .pos(row.getPos())
                            .level(row.getLevel())
                            .dictionaryType(row.getDictionaryType())
                            .createdAt(now)  // ✅ 동일 시간 부여
                            .build())
                    .toList();

            vocabularyDataRepository.saveAll(vocabularyList);
            log.info("✅ CSV import 완료: {}건 저장", vocabularyList.size());

        } catch (Exception e) {
            log.error("❌ CSV import 실패", e);
            throw new RuntimeException("CSV import 실패", e);
        }
    }
}