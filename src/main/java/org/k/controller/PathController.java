package org.k.controller;

import com.google.common.io.ByteStreams;

import org.k.exception.RangeNotSatisfiableException;
import org.k.exception.UnknownException;
import org.k.util.ByteRangeSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

public abstract class PathController {
    private static final Logger logger = LoggerFactory.getLogger(PathController.class);

    ResponseEntity<?> downloadFile(Path file) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_TYPE, extractContentTypeForFile(file));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=" +
                file.getFileName().toString());

        long fileSize = extractFileSize(file);

        Optional<ByteRangeSpec> byteRangeSpecOptional = extractByteRangeSpec(fileSize);
        if (byteRangeSpecOptional.isPresent()) {
            ByteRangeSpec byteRangeSpec = byteRangeSpecOptional.get();
            long numberOfBytes = byteRangeSpec.buildContentLengthHeader();
            headers.setContentLength(numberOfBytes);
            headers.set(HttpHeaders.CONTENT_RANGE, byteRangeSpec.buildContentRangeHeader());

            try {
                RandomAccessFile randomAccessFile = new RandomAccessFile(file.toFile(), "r");
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

        headers.setContentLength(fileSize);

        return new ResponseEntity<>(new FileSystemResource(file.toFile()), headers, HttpStatus.OK);
    }

    private String extractContentTypeForFile(Path file) {
        String contentType;
        try {
            String probedContentType = Files.probeContentType(file);
            contentType = probedContentType != null ? probedContentType :
                    MediaType.APPLICATION_OCTET_STREAM_VALUE;
        } catch (IOException e) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        return contentType;
    }

    private long extractFileSize(Path file) {
        long fileSize;
        try {
            fileSize = Files.size(file);
        } catch (IOException e) {
            throw new UnknownException("Failed to determine file size: " + file.toAbsolutePath());
        }

        return fileSize;
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
}
