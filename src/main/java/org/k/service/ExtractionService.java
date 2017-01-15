package org.k.service;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;

import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.impl.FileVolumeManager;
import com.github.junrar.rarfile.FileHeader;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
import com.hazelcast.core.IMap;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.progress.ProgressMonitor;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.k.data.ExtractionProgress;
import org.k.exception.DirServiceException;
import org.k.exception.ExtractionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
public class ExtractionService {
    private static final Logger logger = LoggerFactory.getLogger(ExtractionService.class);

    private final HazelcastInstance hazelcastInstance;
    private final ExecutorService extractionPool = Executors.newFixedThreadPool(10);
    private final ConcurrentMap<String, ExtractionInfo> extractionInfoMap;
    private final IMap<String, ExtractionInfo> finishedExtractionInfoMap;
    private final long extractionStatusExpirationSeconds;

    private final DirService dirService;

    @Autowired
    public ExtractionService(DirService dirService,
                             HazelcastInstance hazelcastInstance,
                             @Value("${extraction.status.expiration.seconds}")
                                     long extractionStatusExpirationSeconds) {
        this.dirService = dirService;
        this.extractionInfoMap = hazelcastInstance.getMap("extractionInfoMap");
        this.finishedExtractionInfoMap =
                hazelcastInstance.getMap("finishedExtractionInfoMap");
        this.hazelcastInstance = hazelcastInstance;
        Preconditions.checkArgument(extractionStatusExpirationSeconds > 0,
                "Extraction expiration seconds should be bigger than 0!");
        this.extractionStatusExpirationSeconds = extractionStatusExpirationSeconds;
    }

    public ExtractionProgress extract(String path) throws IOException {
        if (!ArchiveType.fileHasSupportedType(path)) {
            throw new DirServiceException("Can not extract file:[" + path + "]. Unsupported file type");
        }

        Path file = dirService.getPath(path);
        if (!Files.exists(file)) {
            throw new DirServiceException("File was not found " + file.toString());
        }

        Path destPath = createFirstAvailableExtractionDirectory(file);
        String extractionId = UUID.randomUUID().toString();

        extractionPool.submit(() -> {
            try {
                if (isZipFile(path)) {
                    extractZip(file, destPath);
                } else if (isRarFile(path)) {
                    extractRar(file, destPath);
                }
            } catch (IOException e) {
                throw new ExtractionException("Failed to extract archive.", e);
            } finally {
                ExtractionInfo info = extractionInfoMap.get(extractionId);
                finishedExtractionInfoMap.put(
                        extractionId,
                        info,
                        extractionStatusExpirationSeconds,
                        TimeUnit.SECONDS);
                extractionInfoMap.remove(extractionId);
            }
        });

        long totalSize = calculateTotalSizeOfArchive(file);
        extractionInfoMap.put(extractionId,
                new ExtractionInfo(destPath.toAbsolutePath().toString(), totalSize));

        String destinationPath = dirService.getRootPath().relativize(destPath).toString();
        return new ExtractionProgress(extractionId, totalSize, 0, destinationPath);
    }

    private Path createFirstAvailableExtractionDirectory(Path file) throws IOException {
        String destPathString = getDestinationPathString(file);
        ILock lock = createILockForDestinationPath(destPathString);

        String currentPathString = destPathString;
        try {
            Path destPath = Paths.get(currentPathString);
            int i = 1;
            while (Files.exists(destPath)) {
                String end = " (" + i + ")";
                currentPathString = destPathString + end;
                destPath = Paths.get(currentPathString);
                i++;
            }

            Files.createDirectories(destPath);
            return destPath;
        } finally {
            lock.unlock();
            logger.debug("Unlocked extraction directory name for {}, result: {}",
                    destPathString, currentPathString);
        }

    }

    private String getDestinationPathString(Path file) {
        Path originalDestPath = Paths.get(StringUtils
                .substringBeforeLast(file.toAbsolutePath().toString(), "."));
        return originalDestPath.toAbsolutePath().toString();
    }

    private ILock createILockForDestinationPath(String destPathString) {
        ILock lock = hazelcastInstance.getLock(destPathString);
        try {
            logger.debug("Trying to acquire extraction directory name lock for {}", destPathString);
            Stopwatch stopwatch = Stopwatch.createStarted();
            lock.tryLock(30L, TimeUnit.SECONDS);
            logger.debug("Acquired extraction directory name lock for {} in {}",
                    destPathString, stopwatch);
        } catch (InterruptedException e) {
            throw new ExtractionException("Interrupted while trying to acquire " +
                    "the extraction directory name lock for " + destPathString, e);
        }
        return lock;
    }

    private boolean isZipFile(String fileName) {
        String extension = FilenameUtils.getExtension(fileName);
        return ArchiveType.ZIP.getFileExtension().equalsIgnoreCase(extension);
    }

    private boolean isRarFile(String fileName) {
        String extension = FilenameUtils.getExtension(fileName);
        return ArchiveType.RAR.getFileExtension().equalsIgnoreCase(extension);
    }

    private ProgressMonitor extractZip(Path file, Path destDir) {
        try {
            ZipFile zipFile = new ZipFile(file.toFile());
            zipFile.extractAll(destDir.toAbsolutePath().toString());
            return zipFile.getProgressMonitor();
        } catch (ZipException e) {
            throw new ExtractionException("Failed to extract archive.", e);
        }
    }

    private void extractRar(Path file, Path destDir) throws IOException {
        Archive archive;
        try {
            archive = new Archive(new FileVolumeManager(file.toFile()));
        } catch (RarException e) {
            throw new ExtractionException("Failed to extract archive.", e);
        }

        FileHeader header;
        while ((header = archive.nextFileHeader()) != null) {
            File newFile = new File(destDir.toAbsolutePath() + File.separator +
                    header.getFileNameString().trim());
            Files.createDirectories(newFile.getParentFile().toPath());
            try (OutputStream os = new BufferedOutputStream(new FileOutputStream(newFile))) {
                archive.extractFile(header, os);
            } catch (RarException e) {
                throw new ExtractionException("Failed to extract archive.", e);
            }
        }
    }

    private long calculateTotalSizeOfArchive(Path archiveFile) throws IOException {
        if (isZipFile(archiveFile.toAbsolutePath().toString())) {
            try {
                return calculateTotalSizeOfZipFile(archiveFile);
            } catch (ZipException e) {
                throw new IllegalArgumentException("Corrupt ZIP file.");
            }
        } else if (isRarFile(archiveFile.toAbsolutePath().toString())) {
            try {
                return calculateTotalSizeOfRarFile(archiveFile);
            } catch (RarException e) {
                throw new IllegalArgumentException("Corrupt RAR file.");
            }
        } else {
            throw new IllegalArgumentException("Archive type not supported.");
        }
    }

    private long calculateTotalSizeOfZipFile(Path archiveFile) throws ZipException {
        ZipFile zipFile = new ZipFile(archiveFile.toFile());
        long totalSize = 0;
        for (Object fileHeader : zipFile.getFileHeaders()) {
            net.lingala.zip4j.model.FileHeader header
                    = (net.lingala.zip4j.model.FileHeader) fileHeader;
            if (header.getZip64ExtendedInfo() != null) {
                totalSize += header.getZip64ExtendedInfo().getUnCompressedSize();
            } else {
                totalSize += header.getUncompressedSize();
            }
        }

        return totalSize;
    }

    private long calculateTotalSizeOfRarFile(Path archiveFile) throws RarException, IOException {
        Archive archive = new Archive(new FileVolumeManager(archiveFile.toFile()));
        long totalSize = 0;
        for (FileHeader fileHeader : archive.getFileHeaders()) {
            totalSize += fileHeader.getFullUnpackSize();
        }

        return totalSize;
    }

    public Optional<ExtractionProgress> getExtractionProgress(String id) {
        ExtractionInfo info = extractionInfoMap.get(id);
        if (info == null) {
            info = finishedExtractionInfoMap.get(id);
            if (info == null) {
                return Optional.empty();
            }
        }

        Path infoPath = Paths.get(info.getDestinationPath());

        // todo avoid going to file system
        long extractedSize = FileUtils.sizeOfDirectory(infoPath.toFile());
        String destinationPath = dirService.getRootPath()
                .relativize(Paths.get(info.getDestinationPath())).toString();
        return Optional.of(new ExtractionProgress(id,
                info.getTotalSize(), extractedSize, destinationPath));
    }

    enum ArchiveType {
        ZIP("zip"),
        RAR("rar");

        private final String fileExtension;

        ArchiveType(String fileExtension) {
            this.fileExtension = fileExtension;
        }

        public static boolean fileHasSupportedType(String fileName) {
            String fileExtension = FilenameUtils.getExtension(fileName);
            for (ArchiveType type : ArchiveType.values()) {
                if (type.getFileExtension().equalsIgnoreCase(fileExtension)) {
                    return true;
                }
            }
            return false;
        }

        public String getFileExtension() {
            return fileExtension;
        }
    }

    private static class ExtractionInfo implements Serializable {
        private final String destinationPath;
        private final long totalSize;

        private ExtractionInfo(String destinationPath, long totalSize) {
            this.destinationPath = Preconditions.checkNotNull(destinationPath);
            Preconditions.checkArgument(totalSize > 0);
            this.totalSize = totalSize;
        }

        String getDestinationPath() {
            return destinationPath;
        }

        long getTotalSize() {
            return totalSize;
        }
    }
}
