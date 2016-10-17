package org.k.controller;

import com.google.common.collect.ImmutableMap;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import org.apache.commons.lang3.StringUtils;
import org.k.service.DirService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Optional;

/**
 * The controller that is responsible of the web resource resolution and the file downloads.
 */
@Controller
public class WebController {
    private final ResourceLoader resourceLoader;
    private final DirService dirService;

    private static final Logger logger = LoggerFactory.getLogger(WebController.class);

    private final ResponseEntity<String> pageSourceResponseEntity;

    @Inject
    @SuppressWarnings("CdiInjectionPointsInspection")
    public WebController(ResourceLoader resourceLoader, DirService dirService, ServletContext servletContext)
            throws IOException, TemplateException {
        this.resourceLoader = resourceLoader;
        this.dirService = dirService;
        this.pageSourceResponseEntity = initializePageSource(servletContext.getContextPath());
    }

    private ResponseEntity<String> initializePageSource(String contextPath) throws IOException, TemplateException {
        Configuration configuration = new Configuration(Configuration.VERSION_2_3_23);
        configuration.setClassLoaderForTemplateLoading(WebController.class.getClassLoader(), "template");
        configuration.setDefaultEncoding(StandardCharsets.UTF_8.name());
        configuration.setOutputEncoding(StandardCharsets.UTF_8.name());
        configuration.setWhitespaceStripping(true);
//        configuration.setLocale(Locale.ENGLISH);
        configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        Template template = configuration.getTemplate("dirList.html.ftl", StandardCharsets.UTF_8.name());
        StringWriter sw = new StringWriter();
        template.process(ImmutableMap.of("contextPath", contextPath), sw);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_HTML);
        return new ResponseEntity<>(sw.toString(), headers, HttpStatus.OK);
    }

    @GetMapping("/**")
    @SuppressWarnings("unused")
    public ResponseEntity<?> handle(HttpServletRequest request)
            throws IOException, TemplateException {
        String requestUri = request.getRequestURI().substring(request.getContextPath().length());

        if ("".equals(requestUri) || "/".equals(requestUri)) {
            return pageSourceResponseEntity;
        }

        String resourcePathToLoad = resolveResourcePathToLoad(requestUri);

        Resource resource = resourceLoader.getResource("classpath:/static/" + resourcePathToLoad);
        if (resource.exists()) {
            return new ResponseEntity<>(resource, HttpStatus.OK);
        }

        Optional<File> fileOrDirectoryOptional = dirService.resolveFileOrDirectory(resourcePathToLoad);
        if (fileOrDirectoryOptional.isPresent()) {
            return handleRequestToValidResourcePath(fileOrDirectoryOptional.get(), requestUri);
        }

        Optional<Resource> resourceForSubPageOptional = handleResourcePathForSubPages(resourcePathToLoad);
        if (resourceForSubPageOptional.isPresent()) {
            return new ResponseEntity<>(resourceForSubPageOptional.get(), HttpStatus.OK);
        }

        return new ResponseEntity<>("No such file/directory!", HttpStatus.NOT_FOUND);
    }

    private Optional<Resource> handleResourcePathForSubPages(String resourcePathToLoad) {
        String possibleDirPath = StringUtils.substringBeforeLast(resourcePathToLoad, "/");
        Optional<File> dirOptional = dirService.resolveFileOrDirectory(possibleDirPath);
        if (dirOptional.isPresent()) {
            File dir = dirOptional.get();
            if (dir.isDirectory()) {
                Resource additionalResource =
                        resourceLoader.getResource(
                                "classpath:/static/" + StringUtils.substringAfterLast(resourcePathToLoad, "/")
                        );
                if (additionalResource.exists()) {
                    return Optional.of(additionalResource);
                }
            }
        }
        return Optional.empty();
    }

    private ResponseEntity<?> handleRequestToValidResourcePath(File file, String requestUri)
            throws IOException, TemplateException {
        if (file.isFile()) {
            return handleFileDownload(file);
        }

        // file is a directory
        if (!requestUri.endsWith("/")) {
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(URI.create(requestUri + "/"));
            return new ResponseEntity<>(headers, HttpStatus.MOVED_PERMANENTLY);
        }

        return pageSourceResponseEntity;
    }

    private String resolveResourcePathToLoad(String requestUri) {
        String decodedPath;
        try {
            decodedPath = URLDecoder.decode(requestUri, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            logger.debug("Unsupported encoding exception while loading " + requestUri);
            throw new RuntimeException(e);
        }

        if (decodedPath.startsWith("/")) {
            decodedPath = decodedPath.substring(1);
        }
        return decodedPath;
    }

    private ResponseEntity<?> handleFileDownload(File file) {
        HttpHeaders headers = new HttpHeaders();
        String contentType = extractContentTypeForFile(file);
        Resource res = new FileSystemResource(file);
        headers.set(HttpHeaders.CONTENT_TYPE, contentType);
        headers.setContentLength(file.length());
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=" + file.getName());
        return new ResponseEntity<>(res, headers, HttpStatus.OK);
    }

    private String extractContentTypeForFile(File file) {
        String contentType;
        try {
            String probedContentType = Files.probeContentType(file.toPath());
            contentType = probedContentType != null ? probedContentType : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        } catch (IOException e) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        return contentType;
    }
}
