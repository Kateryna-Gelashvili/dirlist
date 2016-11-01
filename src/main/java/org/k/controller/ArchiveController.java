package org.k.controller;

import com.google.common.collect.ImmutableMap;

import org.k.service.DirService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ArchiveController {
    private final DirService dirService;

    @Autowired
    public ArchiveController(DirService dirService) {
        this.dirService = dirService;
    }

    @PostMapping("/extract")
    public Object extractArchive(@RequestBody String path) {
        dirService.extractFile(path);
        return ImmutableMap.of("status", "STARTED");
    }
}