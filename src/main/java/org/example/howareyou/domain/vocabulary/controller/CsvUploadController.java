package org.example.howareyou.domain.vocabulary.controller;

import lombok.RequiredArgsConstructor;
import org.example.howareyou.domain.vocabulary.service.CsvImportService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CsvUploadController {

    private final CsvImportService csvImportService;

//    @Operation(summary = "csv업로드", description = "csv를 mongoDB에 업로드합니다.")
    @PostMapping("/upload-csv")
    public String uploadCsv(@RequestParam String filename) {
        csvImportService.importCsv(filename);
        return "업로드 완료: " + filename;
    }
}
