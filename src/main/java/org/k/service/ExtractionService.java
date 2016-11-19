package org.k.service;

import com.google.common.base.Preconditions;

import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.impl.FileVolumeManager;
import com.github.junrar.rarfile.FileHeader;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.progress.ProgressMonitor;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.k.data.ExtractionProgress;
import org.k.exception.DirServiceException;
import org.k.exception.ExtractionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class ExtractionService {
    private final DirService dirService;

    private final ExecutorService extractionPool = Executors.newFixedThreadPool(10);

    private final ConcurrentMap<String, ExtractionInfo> extractionInfoMap
            = new ConcurrentHashMap<>();

    @Autowired
    public ExtractionService(DirService dirService) {
        this.dirService = dirService;
    }

    /**
     * extracts supported archive type file by relative path to the same directory
     *
     * @param path relative path
     */
    public ExtractionProgress extract(String path) throws IOException {
        String fileExtension = FilenameUtils.getExtension(path);
        if (!ArchiveType.fileHasSupportedType(path)) {
            throw new DirServiceException("Can not extract file:[" + path + "]. Unsupported file type");
        }

        Path file = dirService.getPath(path);
        if (!Files.exists(file)) {
            throw new DirServiceException("File was not found " + file.toString());
        }

        Path destPath = createFirstAvailableExtractionDirectory(file);

        String extractionId = UUID.randomUUID().toString();
        long totalSize = calculateTotalSizeOfArchive(file);

        extractionPool.submit(() -> {
            try {
                if (ArchiveType.ZIP.getFileExtension().equalsIgnoreCase(fileExtension)) {
                    extractZip(file, destPath);
                } else if (ArchiveType.RAR.getFileExtension().equalsIgnoreCase(fileExtension)) {
                    extractRar(file, destPath);
                }
            } finally {
                extractionInfoMap.remove(extractionId);
            }

        });

        extractionInfoMap.put(extractionId, new ExtractionInfo(destPath, totalSize));
        return new ExtractionProgress(extractionId, totalSize, 0);
    }

    /**
     * Gets the extraction progress of the running extraction job, if exists.
     *
     * @param id the id of the extraction job
     * @return the extraction progress
     */
    public Optional<ExtractionProgress> getExtractionProgress(String id) {
        ExtractionInfo info = extractionInfoMap.get(id);
        if (info == null) {
            return Optional.empty();
        }

        // todo avoid going to file system
        long extractedSize = FileUtils.sizeOfDirectory(info.getDestinationPath().toFile());
        return Optional.of(new ExtractionProgress(id, info.getTotalSize(), extractedSize));
    }

    private long calculateTotalSizeOfArchive(Path archiveFile) throws IOException {
        String extension = FilenameUtils.getExtension(archiveFile.toAbsolutePath().toString());
        if (ArchiveType.ZIP.getFileExtension().equalsIgnoreCase(extension)) {
            try {
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
            } catch (ZipException e) {
                throw new IllegalArgumentException("Corrupt ZIP file.");
            }
        } else if (ArchiveType.RAR.getFileExtension().equalsIgnoreCase(extension)) {
            try {
                Archive archive = new Archive(new FileVolumeManager(archiveFile.toFile()));
                long totalSize = 0;
                for (FileHeader fileHeader : archive.getFileHeaders()) {
                    totalSize += fileHeader.getFullUnpackSize();
                }

                return totalSize;
            } catch (RarException e) {
                throw new IllegalArgumentException("Corrupt RAR file.");
            }
        } else {
            throw new IllegalArgumentException("Archive type not supported.");
        }
    }

    private ProgressMonitor extractZip(Path file, Path destDir) {
        try {
            ZipFile zipFile = new ZipFile(file.toFile());
            zipFile.extractAll(destDir.toAbsolutePath().toString());
            return zipFile.getProgressMonitor();
        } catch (ZipException e) {
            throw new ExtractionException(file, destDir, e);
        }
    }

    private void extractRar(Path file, Path destDir) {
        Archive archive;
        try {
            archive = new Archive(new FileVolumeManager(file.toFile()));
        } catch (RarException | IOException e) {
            throw new ExtractionException(file, destDir, e);
        }

        FileHeader header;
        while ((header = archive.nextFileHeader()) != null) {
            File newFile = new File(destDir.toAbsolutePath() + File.separator +
                    header.getFileNameString().trim());
            try (OutputStream os = new BufferedOutputStream(new FileOutputStream(newFile))) {
                archive.extractFile(header, os);
            } catch (RarException | IOException e) {
                throw new ExtractionException(file, destDir, e);
            }
        }
    }

    private Path createFirstAvailableExtractionDirectory(Path file) throws IOException {
        Path originalDestPath = Paths.get(StringUtils
                .substringBeforeLast(file.toAbsolutePath().toString(), "."));
        String destPathString = originalDestPath.toAbsolutePath().toString();

        synchronized (destPathString.intern()) {
            Path destPath = Paths.get(destPathString);
            int i = 1;
            while (Files.exists(destPath)) {
                String end = " (" + i + ")";
                destPath = Paths.get(destPathString + end);
                i++;
            }

            Files.createDirectories(destPath);
            return destPath;
        }
    }

    /**
     * enum of supported file extensions for extraction
     */
    enum ArchiveType {
        ZIP("zip"),
        RAR("rar");

        private final String fileExtension;

        ArchiveType(String fileExtension) {
            this.fileExtension = fileExtension;
        }

        /**
         * check if the service supports extraction for this type of files
         *
         * @param fileName name of file, relative or absolute
         */
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

    private static class ExtractionInfo {
        private final Path destinationPath;
        private final long totalSize;

        private ExtractionInfo(Path destinationPath, long totalSize) {
            this.destinationPath = Preconditions.checkNotNull(destinationPath);
            Preconditions.checkArgument(totalSize > 0);
            this.totalSize = totalSize;
        }

        Path getDestinationPath() {
            return destinationPath;
        }

        long getTotalSize() {
            return totalSize;
        }
    }
}
