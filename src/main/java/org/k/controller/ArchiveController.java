package org.k.controller;

import com.google.common.collect.ImmutableMap;

import org.k.dto.PathDto;
import org.k.service.ExtractionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ArchiveController {
    private final ExtractionService extractionService;

    @Autowired
    public ArchiveController(ExtractionService extractionService) {
        this.extractionService = extractionService;
    }

    @PostMapping(value = "/extract", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Object extractArchive(@RequestBody PathDto pathDto) {
        extractionService.extract(pathDto.getPath());
        return ImmutableMap.of("status", "STARTED");
    }
}