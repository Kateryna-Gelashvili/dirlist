package org.k.controller;

import com.google.common.io.ByteStreams;

import org.k.exception.FileNotFoundException;
import org.k.exception.RangeNotSatisfiableException;
import org.k.exception.UnknownException;
import org.k.service.DirService;
import org.k.util.ByteRangeSpec;
import org.k.util.PathUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

@Controller
public class FileDownloadController {
    private static final Logger logger = LoggerFactory.getLogger(FileDownloadController.class);

    private static final String DL = "/dl";

    private final DirService dirService;

    @Autowired
    public FileDownloadController(DirService dirService) {
        this.dirService = dirService;
    }

    @GetMapping(DL + "/**")
    public ResponseEntity<?> downloadFile() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder
                .currentRequestAttributes()).getRequest();
        String path = PathUtil.extractPath(DL, request.getRequestURI()
                .substring(request.getContextPath().length()));

        Optional<File> fileOrDirectoryOptional = dirService.resolveFileOrDirectory(path);
        if (fileOrDirectoryOptional.isPresent()) {
            File file = fileOrDirectoryOptional.get();
            if (file.isFile()) {
                return downloadFile(file);
            } else if (file.isDirectory()) {
                HttpHeaders headers = new HttpHeaders();
                headers.set(HttpHeaders.LOCATION,
                    request.getContextPath() + DirectoryDownloadController.DL_DIR + path + ".zip");
                return new ResponseEntity<>(headers, HttpStatus.FOUND);
            } else {
                throw new UnknownException("Not a file or directory: " + file.getAbsolutePath());
            }
        } else {
            throw new FileNotFoundException();
        }
    }

    private ResponseEntity<?> downloadFile(File file) {
        HttpHeaders headers = new HttpHeaders();
        String contentType = extractContentTypeForFile(file);
        headers.set(HttpHeaders.CONTENT_TYPE, contentType);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=" + file.getName());

        Optional<ByteRangeSpec> byteRangeSpecOptional = extractByteRangeSpec(file.length());
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

    private Optional<ByteRangeSpec> extractByteRangeSpec(long contentLength) {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder
                .currentRequestAttributes()).getRequest();
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
}
