package org.k.controller;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

import org.apache.commons.lang3.StringUtils;
import org.k.exception.RangeNotSatisfiableException;
import org.k.exception.UnknownException;
import org.k.service.DirService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

/**
 * The controller that is responsible of the web resource resolution and the file downloads.
 */
@Controller
public class WebController {
    private static final Logger logger = LoggerFactory.getLogger(WebController.class);
    private final ResourceLoader resourceLoader;
    private final DirService dirService;
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
            return handleRequestToValidResourcePath(fileOrDirectoryOptional.get(), requestUri, request);
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

    private ResponseEntity<?> handleRequestToValidResourcePath(File file, String requestUri,
                                                               HttpServletRequest request)
            throws IOException, TemplateException {
        if (file.isFile()) {
            return handleFileDownload(file, request);
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

    private ResponseEntity<?> handleFileDownload(File file, HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        String contentType = extractContentTypeForFile(file);
        headers.set(HttpHeaders.CONTENT_TYPE, contentType);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=" + file.getName());

        Optional<ByteRangeSpec> byteRangeSpecOptional = extractByteRangeSpec(file.length(), request);

        if (byteRangeSpecOptional.isPresent()) {
            ByteRangeSpec byteRangeSpec = byteRangeSpecOptional.get();
            headers.setContentLength(byteRangeSpec.buildContentLengthHeader());
            headers.set(HttpHeaders.CONTENT_RANGE, byteRangeSpec.buildContentRangeHeader());

            try {
                RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
                long numberOfBytes = byteRangeSpec.buildContentLengthHeader();
                FileChannel channel = randomAccessFile.getChannel()
                        .position(byteRangeSpec.getStart());
                InputStream partialInputStream =
                        ByteStreams.limit(Channels.newInputStream(channel), numberOfBytes);
                return new ResponseEntity<>(
                        new InputStreamResource(new BufferedInputStream(partialInputStream)),
                        headers,
                        HttpStatus.PARTIAL_CONTENT);
            } catch (Exception e) {
                throw new UnknownException("Should never happen!");
            }
        }

        headers.setContentLength(file.length());
        return new ResponseEntity<>(new FileSystemResource(file), headers, HttpStatus.OK);
    }

    private Optional<ByteRangeSpec> extractByteRangeSpec(long contentLength,
                                                         HttpServletRequest request) {
        String rangeHeader = request.getHeader(HttpHeaders.RANGE);
        if (rangeHeader != null) {
            try {
                return Optional.of(ByteRangeSpec.fromHeader(rangeHeader, contentLength));
            } catch (RangeNotSatisfiableException e) {
                throw e;
            } catch (Exception e) {
                logger.warn("Invalid range request: {}", rangeHeader);
            }
        }
        return Optional.empty();
    }

    private String extractContentTypeForFile(File file) {
        String contentType;
        try {
            String probedContentType = Files.probeContentType(file.toPath());
            contentType = probedContentType != null ? probedContentType :
                    MediaType.APPLICATION_OCTET_STREAM_VALUE;
        } catch (IOException e) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        return contentType;
    }

    private static class ByteRangeSpec {
        private static final Pattern headerPattern = Pattern.compile("bytes=([0-9]*)-([0-9]*)");

        private final long start;
        private final long end;
        private final long contentSizeInBytes;

        private ByteRangeSpec(long start, long end, long contentSizeInBytes) {
            Preconditions.checkArgument(start >= 0);
            Preconditions.checkArgument(end >= start);
            Preconditions.checkArgument(contentSizeInBytes >= 0);

            if (end >= contentSizeInBytes) {
                throw new RangeNotSatisfiableException(start + "-" + end);
            }

            this.start = start;
            this.end = end;
            this.contentSizeInBytes = contentSizeInBytes;
        }

        static ByteRangeSpec fromHeader(String header, long contentSizeInBytes) {
            Preconditions.checkNotNull(header);

            Matcher matcher = headerPattern.matcher(header);
            if (matcher.matches()) {
                String startStr = matcher.group(1);
                Long start = startStr != null && !"".equals(startStr) ?
                        Long.parseLong(startStr) : 0L;
                String endStr = matcher.group(2);
                Long end = endStr != null && !"".equals(endStr) ?
                        Long.parseLong(endStr) : contentSizeInBytes - 1;
                return new ByteRangeSpec(start, end, contentSizeInBytes);
            }
            throw new IllegalArgumentException("Invalid range header: " + header);
        }

        long getStart() {
            return start;
        }

        String buildContentRangeHeader() {
            return "bytes " + start + "-" + end + "/" + contentSizeInBytes;
        }

        long buildContentLengthHeader() {
            return end - start + 1;
        }
    }
}
