package org.k.controller;

import org.k.dto.PathInfoDto;
import org.k.exception.DirectoryNotFoundException;
import org.k.exception.NotDirectoryException;
import org.k.service.DirService;
import org.k.util.PathUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping(LIST + "/**")
    public Set<PathInfoDto> listContentOfDirectory(HttpServletRequest request) {
        String dirPath = PathUtil.extractPath(LIST, request.getRequestURI()
                .substring(request.getContextPath().length()));
        Optional<Path> pathOptional = dirService.resolveFileOrDirectory(dirPath);
        if (pathOptional.isPresent()) {
            Path path = pathOptional.get();
            if (Files.isDirectory(path)) {
                return dirService.listPathInfosForDirectory(dirPath).stream()
                        .map(PathInfoDto::new)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
            } else {
                throw new NotDirectoryException();
            }
        } else {
            throw new DirectoryNotFoundException();
        }
    }
}