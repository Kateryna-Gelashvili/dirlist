package org.k.controller;

import com.google.common.io.ByteStreams;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.k.exception.DirectoryNotFoundException;
import org.k.exception.MaxDirectoryDownloadSizeExceededException;
import org.k.exception.NotDirectoryException;
import org.k.service.DirService;
import org.k.service.PropertiesService;
import org.k.util.PathUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.Iterator;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.annotation.PreDestroy;
import javax.servlet.http.HttpServletRequest;

@Controller
public class DirectoryDownloadController extends PathController {
    static final String DL_DIR = "/dl_dir";
    private static final Logger logger = LoggerFactory.getLogger(DirectoryDownloadController.class);
    private static final String EXTENSION = "zip";

    private final DirService dirService;
    private final PropertiesService propertiesService;

    private final Path tempDir;

    @Autowired
    public DirectoryDownloadController(DirService dirService,
                                       PropertiesService propertiesService) throws IOException {
        this.dirService = dirService;
        this.propertiesService = propertiesService;
        this.tempDir = Files.createTempDirectory("dirlist-");
        logger.info("Created temp directory for zipped directory downloads: [{}]",
                tempDir.toAbsolutePath().toString());
    }

    @PreDestroy
    protected void preDestroy() throws IOException {
        String tempDirAbsolutePath = tempDir.toAbsolutePath().toString();
        logger.info("Attempting to delete temporary directory for zipped directory downloads: [{}]",
                tempDirAbsolutePath);

        FileUtils.forceDelete(tempDir.toFile());
        logger.info("Successfully deleted the temp directory [{}]", tempDirAbsolutePath);
    }

    @GetMapping(DL_DIR + "/**")
    public ResponseEntity<?> downloadDirectory() throws IOException {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder
                .currentRequestAttributes()).getRequest();
        String path = PathUtil.extractPath(DL_DIR, request.getRequestURI()
                .substring(request.getContextPath().length()));

        if (!EXTENSION.equalsIgnoreCase(FilenameUtils.getExtension(path))) {
            throw new NotDirectoryException();
        }

        String dirPath = path.substring(0, path.length() - EXTENSION.length() - 1);
        Optional<Path> fileOrDirectoryOptional = dirService.resolveFileOrDirectory(dirPath);
        if (fileOrDirectoryOptional.isPresent()) {
            Path directory = fileOrDirectoryOptional.get();
            if (Files.isDirectory(directory)) {
                validateDirectorySize(directory);
                return downloadZipped(dirPath, directory);
            } else {
                throw new NotDirectoryException();
            }
        } else {
            throw new DirectoryNotFoundException();
        }
    }

    private void validateDirectorySize(Path directory) {
        long sizeOfDirectory = FileUtils.sizeOfDirectory(directory.toFile());
        if (sizeOfDirectory > propertiesService.maxAllowedDirectoryDownloadSize()) {
            throw new MaxDirectoryDownloadSizeExceededException();
        }
    }

    private ResponseEntity<?> downloadZipped(String relativePath, Path directory)
            throws IOException {
        Path targetPath = Paths.get(tempDir + File.separator + relativePath + ".zip");

        if (Files.exists(targetPath)) {
            deleteIfObsolete(directory, targetPath);
        }

        synchronized (targetPath.toAbsolutePath().toString().intern()) {
            logger.info("Acquired lock for downloading of {}",
                    targetPath.toAbsolutePath().toString());
            if (!Files.exists(targetPath)) {
                zipDirectoryToTempDir(directory, targetPath);
            }
            logger.info("Releasing lock for downloading of {}",
                    targetPath.toAbsolutePath().toString());
        }

        return downloadFile(targetPath);
    }

    private void zipDirectoryToTempDir(Path directory, Path targetPath) throws IOException {
        Files.createDirectories(targetPath.getParent());
        try (ZipOutputStream output = new ZipOutputStream(
                new BufferedOutputStream(Files.newOutputStream(targetPath)))) {
            Iterator<Path> iterator = Files.walk(directory)
                    .filter(p -> !Files.isDirectory(p)).iterator();
            while (iterator.hasNext()) {
                Path p = iterator.next();
                output.putNextEntry(new ZipEntry(directory
                        .relativize(p).toString()));
                ByteStreams.copy(Files.newInputStream(p), output);
                output.closeEntry();
            }
            output.finish();
        }
    }

    private void deleteIfObsolete(Path directory, Path targetPath) throws IOException {
        FileTime zipLastModified = Files.getLastModifiedTime(targetPath);
        FileTime originalDirLastModified = Files.getLastModifiedTime(directory);

        boolean shouldBeRecreated =
                originalDirLastModified.toInstant().isAfter(zipLastModified.toInstant());
        if (shouldBeRecreated) {
            Files.delete(targetPath);
        }
    }
}
