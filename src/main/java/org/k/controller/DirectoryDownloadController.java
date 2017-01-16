package org.k.controller;

import com.google.common.base.Stopwatch;
import com.google.common.io.ByteStreams;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.k.exception.DirectoryNotFoundException;
import org.k.exception.MaxDirectoryDownloadSizeExceededException;
import org.k.exception.NotDirectoryException;
import org.k.exception.UnknownException;
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
import java.time.Instant;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletRequest;

@Controller
public class DirectoryDownloadController extends PathController {
    static final String DL_DIR = "/dl_dir";
    private static final Logger logger = LoggerFactory.getLogger(DirectoryDownloadController.class);
    private static final String ZIP_EXTENSION = "zip";

    private final DirService dirService;
    private final PropertiesService propertiesService;
    private final HazelcastInstance hazelcastInstance;


    @Autowired
    public DirectoryDownloadController(DirService dirService,
                                       PropertiesService propertiesService,
                                       HazelcastInstance hazelcastInstance) throws IOException {
        this.dirService = dirService;
        this.propertiesService = propertiesService;
        this.hazelcastInstance = hazelcastInstance;
    }

    @GetMapping(DL_DIR + "/**")
    public ResponseEntity<?> downloadDirectory() throws IOException {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder
                .currentRequestAttributes()).getRequest();
        String pathParameter = PathUtil.extractPath(DL_DIR, request.getRequestURI()
                .substring(request.getContextPath().length()));

        checkZipExtension(pathParameter);
        String relativePath = getPathWithoutZipExtension(pathParameter);

        Optional<Path> pathOptional = dirService.resolveFileOrDirectory(relativePath);
        if (!pathOptional.isPresent()) {
            throw new DirectoryNotFoundException();
        }

        Path directory = pathOptional.get();
        if (!Files.isDirectory(directory)) {
            throw new NotDirectoryException();
        }

        validateDirectorySize(directory);
        return downloadZipped(relativePath, directory);
    }

    private void checkZipExtension(String pathParameter) {
        if (!ZIP_EXTENSION.equalsIgnoreCase(FilenameUtils.getExtension(pathParameter))) {
            throw new NotDirectoryException();
        }
    }

    private String getPathWithoutZipExtension(String path) {
        return path.substring(0, path.length() - ZIP_EXTENSION.length() - 1);
    }

    private void validateDirectorySize(Path directory) {
        long sizeOfDirectory = FileUtils.sizeOfDirectory(directory.toFile());
        if (sizeOfDirectory > propertiesService.maxAllowedDirectoryDownloadSize()) {
            throw new MaxDirectoryDownloadSizeExceededException();
        }
    }

    private ResponseEntity<?> downloadZipped(String relativePath, Path directory)
            throws IOException {
        Path targetPath = getTargetDirectoryPath(relativePath);
        String targetPathString = targetPath.toAbsolutePath().toString();

        ILock lock = createLockForTargetPath(targetPathString);

        try {
            if (Files.exists(targetPath)) {
                deleteIfObsolete(directory, targetPath);
            }

            if (Files.exists(targetPath)) {
                updateLastModifiedDateOfTargetPath(targetPath);
            } else {
                zipDirectoryToTempDir(directory, targetPath);
            }
        } finally {
            lock.unlock();
            logger.debug("Unlocked zipped directory download for {}", targetPathString);
        }

        return downloadFile(targetPath);
    }

    private Path getTargetDirectoryPath(String relativePath) {
        return Paths.get(dirService.getTempDir() +
                File.separator + relativePath + ".zip");
    }

    private ILock createLockForTargetPath(String targetPathString) {
        ILock lock = hazelcastInstance.getLock(targetPathString);
        try {
            logger.debug("Trying to acquire zipped directory download lock for {}", targetPathString);
            Stopwatch stopwatch = Stopwatch.createStarted();
            lock.tryLock(1L, TimeUnit.MINUTES);
            logger.debug("Acquired zipped directory download lock for {} in {}",
                    targetPathString, stopwatch);
        } catch (InterruptedException e) {
            throw new UnknownException("Interrupted while trying to acquire the " +
                    "zipped directory download lock for " + targetPathString, e);
        }
        return lock;
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

    private void updateLastModifiedDateOfTargetPath(Path targetPath) throws IOException {
        Files.setLastModifiedTime(targetPath.toAbsolutePath(),
                FileTime.from(Instant.now()));
    }
}
