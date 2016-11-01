package org.k.controller;

import com.google.common.io.ByteStreams;

import org.apache.commons.io.FilenameUtils;
import org.k.exception.DirectoryNotFoundException;
import org.k.exception.NotDirectoryException;
import org.k.exception.UnknownException;
import org.k.service.DirService;
import org.k.util.PathUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
public class DirectoryDownloadController {
    static final String DL_DIR = "/dl_dir";

    private static final String EXTENSION = "zip";

    private final DirService dirService;

    @Autowired
    public DirectoryDownloadController(DirService dirService) {
        this.dirService = dirService;
    }

    @GetMapping(DL_DIR + "/**")
    public ResponseEntity<?> downloadDirectory() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder
                .currentRequestAttributes()).getRequest();
        String path = PathUtil.extractPath(DL_DIR, request.getRequestURI()
                .substring(request.getContextPath().length()));

        if (!EXTENSION.equalsIgnoreCase(FilenameUtils.getExtension(path))) {
            throw new NotDirectoryException();
        }

        String dirPath = path.substring(0, path.length() - EXTENSION.length() - 1);
        Optional<File> fileOrDirectoryOptional = dirService.resolveFileOrDirectory(dirPath);
        if (fileOrDirectoryOptional.isPresent()) {
            File file = fileOrDirectoryOptional.get();
            if (file.isDirectory()) {
                return downloadZipped(file);
            } else {
                throw new NotDirectoryException();
            }
        } else {
            throw new DirectoryNotFoundException();
        }
    }

    private ResponseEntity<?> downloadZipped(File file) {
        HttpServletResponse response = ((ServletRequestAttributes) RequestContextHolder
                .currentRequestAttributes()).getResponse();
        response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);
        try (ZipOutputStream output = new ZipOutputStream(response.getOutputStream())) {
            Path dirPath = file.toPath();
            Files.walk(dirPath)
                    .filter(p -> !Files.isDirectory(p))
                    .forEach(p -> {
                        try {
                            output.putNextEntry(new ZipEntry(dirPath.relativize(p).toString()));
                            ByteStreams.copy(Files.newInputStream(p), output);
                            output.closeEntry();
                        } catch (IOException e) {
                            throw new UnknownException("Error while adding zip entry " +
                                    "from the directory: " + file.getAbsolutePath());
                        }
                    });
            output.finish();
        } catch (IOException e) {
            throw new UnknownException("Error while walking the directory: "
                    + file.getAbsolutePath());
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
