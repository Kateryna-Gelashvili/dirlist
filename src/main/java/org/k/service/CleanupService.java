package org.k.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;

@Service
public class CleanupService {
    private static final Logger logger = LoggerFactory.getLogger(CleanupService.class);
    private static final Duration THRESHOLD_DURATION = Duration.of(1, ChronoUnit.WEEKS);
    private final DirService dirService;

    @Autowired
    public CleanupService(DirService dirService) {
        this.dirService = dirService;
    }

    @Scheduled(cron = "00 00 06 * * *")
    public void doCleanup() throws IOException {
        logger.debug("Cleaning job started");

        Path tempDir = dirService.getTempDir();

        Instant threshold = Instant.now().minus(THRESHOLD_DURATION);

        int num = 0;
        Iterator<Path> iterator = Files.walk(tempDir.toAbsolutePath()).iterator();
        while (iterator.hasNext()) {
            Path path = iterator.next();

            try {
                if (Files.isRegularFile(path) && Files.getLastModifiedTime(path)
                        .toInstant().isBefore(threshold)) {
                    logger.debug("Attempting to delete {}", path.toAbsolutePath());
                    Files.delete(path);
                    logger.debug("Successfully deleted {}", path.toAbsolutePath());
                    num++;
                }
            } catch (Exception e) {
                logger.error("Failed to delete {}. Message: {}",
                        path.toAbsolutePath(), e.getMessage(), e);
            }

        }
        logger.debug("Cleaning job deleted {} file(s) in total", num);
    }
}