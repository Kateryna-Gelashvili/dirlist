package org.k.controller;

import org.k.dto.PathInfoDto;
import org.k.exception.DirectoryNotFoundException;
import org.k.exception.NotDirectoryException;
import org.k.service.DirService;
import org.k.util.PathUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

@RestController
public class ListingController {
    private static final String LIST = "/list";

    private final DirService dirService;

    @Autowired
    public ListingController(DirService dirService) {
        this.dirService = dirService;
    }

    @GetMapping(value = LIST + "/**", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Set<PathInfoDto> listContentOfDirectory(HttpServletRequest request) throws IOException {
        String pathParameter = PathUtil.extractPath(LIST, request.getRequestURI()
                .substring(request.getContextPath().length()));

        Optional<Path> pathOptional = dirService.resolveFileOrDirectory(pathParameter);
        if (!pathOptional.isPresent()) {
            throw new DirectoryNotFoundException();
        }

        Path directory = pathOptional.get();
        if (!Files.isDirectory(directory)) {
            throw new NotDirectoryException();
        }

        return dirService.listPathInfosForDirectory(pathParameter).stream()
                .map(PathInfoDto::new)
                .collect(Collectors.toCollection(LinkedHashSet::new));

    }
}