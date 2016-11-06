package org.k.service;

import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.impl.FileVolumeManager;
import com.github.junrar.rarfile.FileHeader;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.k.exception.DirServiceException;
import org.k.exception.ExtractionException;
import org.k.exception.UnknownException;
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

@Service
public class ExtractionService {
    private final DirService dirService;

    @Autowired
    public ExtractionService(DirService dirService) {
        this.dirService = dirService;
    }

    /**
     * extracts supported archive type file by relative path to the same directory
     * @param path relative path
     */
    public void extract(String path){
        String fileExtension = FilenameUtils.getExtension(path);
        if (!ArchiveType.fileHasSupportedType(path)) {
            throw new DirServiceException("Can not extract file:[" + path + "]. Unsupported file type");
        }

        Path file = dirService.getPath(path);
        if (!Files.exists(file)) {
            throw new DirServiceException("File was not found " + file.toString());
        }

        Path destPath = getFirstAvailableExtractionDirectoryName(file);
        try {
            Files.createDirectories(destPath);
        } catch (IOException e) {
            throw new UnknownException("Error on creating directory:" + destPath, e);
        }

        if (ArchiveType.ZIP.getFileExtension().equalsIgnoreCase(fileExtension)) {
            extractZip(file, destPath);
        } else if (ArchiveType.RAR.getFileExtension().equalsIgnoreCase(fileExtension)) {
            extractRar(file, destPath);
        }
    }

    private void extractZip(Path file, Path destDir) {
        try {
            ZipFile zipFile = new ZipFile(file.toFile());
            zipFile.extractAll(destDir.toAbsolutePath().toString());
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

    private Path getFirstAvailableExtractionDirectoryName(Path file) {
        Path originalDestPath = Paths.get(StringUtils
                .substringBeforeLast(file.toAbsolutePath().toString(), "."));
        Path destPath = Paths.get(originalDestPath.toAbsolutePath().toString());
        int i = 1;
        while (Files.exists(destPath)) {
            String end = " (" + i + ")";
            destPath = Paths.get(originalDestPath.toAbsolutePath().toString() + end);
            i++;
        }
        return destPath;
    }

    /**
     * enum of supported file extensions for extraction
     */
    public enum ArchiveType {
        ZIP("zip"),
        RAR("rar");

        private final String fileExtension;

        ArchiveType(String fileExtension) {
            this.fileExtension = fileExtension;
        }

        public String getFileExtension() {
            return fileExtension;
        }

        /**
         * check if the service supports extraction for this type of files
         * @param fileName name of file, relative or absolute
         * @return
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
    }
}
