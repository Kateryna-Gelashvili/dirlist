package org.k.controller;

import org.k.exception.FileNotFoundException;
import org.k.exception.UnknownException;
import org.k.service.DirService;
import org.k.util.PathUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

@Controller
public class FileDownloadController extends PathController {
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

        Optional<Path> fileOrDirectoryOptional = dirService.resolveFileOrDirectory(path);
        if (fileOrDirectoryOptional.isPresent()) {
            Path fileOrDirectory = fileOrDirectoryOptional.get();
            if (Files.isRegularFile(fileOrDirectory)) {
                return downloadFile(fileOrDirectory);
            } else if (Files.isDirectory(fileOrDirectory)) {
                HttpHeaders headers = new HttpHeaders();
                headers.set(HttpHeaders.LOCATION,
                        request.getContextPath() + DirectoryDownloadController.DL_DIR
                                + path + ".zip");
                return new ResponseEntity<>(headers, HttpStatus.FOUND);
            } else {
                throw new UnknownException("Not a file or directory: "
                        + fileOrDirectory.toAbsolutePath());
            }
        } else {
            throw new FileNotFoundException();
        }
    }
}
