package org.k.controller;

import org.k.data.ExtractionProgress;
import org.k.dto.ExtractionProgressDto;
import org.k.dto.PathDto;
import org.k.service.ExtractionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class ExtractionController {
    private final ExtractionService extractionService;

    @Autowired
    public ExtractionController(ExtractionService extractionService) {
        this.extractionService = extractionService;
    }

    @PostMapping(value = "/extract", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ExtractionProgressDto extractFiles(@RequestBody PathDto pathDto) throws IOException {
        ExtractionProgress progress = extractionService.extract(pathDto.getPath());
        return extractionProgressToDto(progress);
    }

    @GetMapping(value = "/extractionProgress/{id}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<ExtractionProgressDto> getExtractionProgress(
            @PathVariable("id") String id) {
        return extractionService.getExtractionProgress(id)
                .map(this::extractionProgressToDto)
                .map(extractionProgressDto ->
                        new ResponseEntity<>(extractionProgressDto, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    private ExtractionProgressDto extractionProgressToDto(ExtractionProgress progress) {
        return new ExtractionProgressDto(progress.getId(),
                progress.getTotalSize(),
                progress.getExtractedSize(),
                progress.getDestinationPath());
    }
}