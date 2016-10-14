package org.k.controller;

import org.apache.commons.lang3.StringUtils;
import org.k.dto.PathInfoDto;
import org.k.service.DirService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
public class ApiController {
    private static final String API = "/api";
    private final DirService dirService;
    private static final Logger logger = LoggerFactory.getLogger(ApiController.class);

    @Inject
    public ApiController(DirService dirService) {
        this.dirService = dirService;
    }

    @GetMapping(API + "/**")
    public Set<PathInfoDto> listContentOfDirectory(HttpServletRequest request) {
        String dirPath = extractDirPath(request.getRequestURI().substring(request.getContextPath().length()));

        return dirService.listPathInfosForDirectory(dirPath).stream()
                .map(PathInfoDto::new).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String extractDirPath(String requestUri) {
        String dirPath;
        try {
            dirPath = URLDecoder.decode(StringUtils.replaceOnce(
                    requestUri, API, ""), StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            logger.debug("Unsupported encoding exception while loading " + StringUtils.replaceOnce(
                    requestUri, API, ""));
            throw new RuntimeException(e);
        }
        return dirPath;
    }
}
