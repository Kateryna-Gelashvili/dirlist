package org.k.controller;

import com.google.common.collect.ImmutableMap;

import org.k.service.DirService;
import org.k.service.ExtractionService;
import org.springframework.beans.factory.annotation.Autowired;
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

    @PostMapping("/extract")
    public Object extractArchive(@RequestBody String path) {
        extractionService.extract(path);
        return ImmutableMap.of("status", "STARTED");
    }
}