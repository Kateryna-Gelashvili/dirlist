package org.k.service;

import com.google.common.collect.ImmutableSet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.SystemUtils;
import org.k.data.PathInfo;
import org.k.data.PathType;
import org.k.exception.DirServiceException;
import org.k.exception.DirectoryNotFoundException;
import org.k.exception.NotDirectoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.DosFileAttributes;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.PreDestroy;

@Service
public class DirService {
    private static final Logger logger = LoggerFactory.getLogger(DirService.class);

    private final Path tempDir;

    private final PropertiesService propertiesService;

    @Autowired
    public DirService(PropertiesService propertiesService) throws IOException {
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

    public Set<PathInfo> listPathInfosForDirectory(String pathString) throws IOException {
        Set<PathInfo> pathInfos = new TreeSet<>(
                Comparator.comparing(
                        (PathInfo pathInfo) -> {
                            PathType type = pathInfo.getType();
                            switch (type) {
                                case DIRECTORY:
                                    return -1;
                                case FILE:
                                    return 1;
                                default:
                                    throw new IllegalArgumentException("Invalid type: " + type);
                            }
                        }
                ).thenComparing((p1, p2) -> p1.getPath().compareToIgnoreCase(p2.getPath()))
        );
        Path dirPath = getPath(pathString);

        checkIfPathIsExistingDirectory(dirPath);

        DirectoryStream<Path> stream = getStreamOfPaths(dirPath);
        for (Path path : stream) {
            String normalizedRelativePathString = getNormalizedRelativePathString(path);
            pathInfos.add(new PathInfo(normalizedRelativePathString,
                    Files.isDirectory(path) ? PathType.DIRECTORY : PathType.FILE,
                    ExtractionService.ArchiveType.fileHasSupportedType(normalizedRelativePathString)
            ));
        }

        return ImmutableSet.copyOf(pathInfos);
    }

    Path getPath(String pathString) {
        String rootPath = PropertiesService.ROOT_DIRECTORY;
        if (rootPath.isEmpty()) {
            throw new DirServiceException("Directory was not found");
        }
        return Paths.get(rootPath + File.separator + pathString);
    }

    private void checkIfPathIsExistingDirectory(Path dirPath) {
        if (!Files.exists(dirPath)) {
            throw new DirectoryNotFoundException("Directory " +
                    dirPath.toString() + " is not found!");
        }

        if (!Files.isDirectory(dirPath)) {
            throw new NotDirectoryException(dirPath + " is not a directory!");
        }
    }

    private DirectoryStream<Path> getStreamOfPaths(Path dirPath) throws IOException {
        DirectoryStream.Filter<Path> filter = file -> !pathIsHidden(file);
        boolean showHiddenFiles = propertiesService.showHiddenFiles();
        return showHiddenFiles ? Files.newDirectoryStream(dirPath) :
                Files.newDirectoryStream(dirPath, filter);
    }

    private boolean pathIsHidden(Path path) throws IOException {
        return path.getFileName().toString().startsWith(".") ||
                SystemUtils.IS_OS_WINDOWS &&
                        Files.readAttributes(path, DosFileAttributes.class).isHidden();
    }

    private String getNormalizedRelativePathString(Path path) {
        Path rootDirectoryPath = Paths.get(PropertiesService.ROOT_DIRECTORY);
        String relativePathString = rootDirectoryPath.relativize(path).toString();
        return FilenameUtils.normalize(Files.isDirectory(path) && !relativePathString.endsWith("/") ?
                relativePathString + "/" : relativePathString, true);
    }

    public Optional<Path> resolveFileOrDirectory(String filePath) {
        String rootDirectoryPath = PropertiesService.ROOT_DIRECTORY;
        Path path = Paths.get(rootDirectoryPath + File.separator + filePath);
        if (Files.exists(path)) {
            return Optional.of(path);
        }
        return Optional.empty();
    }

    public Path getTempDir() {
        return tempDir;
    }

    public Path getRootPath() {
        return Paths.get(PropertiesService.ROOT_DIRECTORY);
    }
}