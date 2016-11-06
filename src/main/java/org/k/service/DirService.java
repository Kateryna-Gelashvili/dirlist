package org.k.service;

import com.google.common.collect.ImmutableSet;

import org.apache.commons.io.FilenameUtils;
import org.k.data.PathInfo;
import org.k.data.PathType;
import org.k.exception.DirServiceException;
import org.k.exception.DirectoryNotFoundException;
import org.k.exception.NotDirectoryException;
import org.k.exception.UnknownException;
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

@Service
public class DirService {

    private final PropertiesService propertiesService;

    @Autowired
    public DirService(PropertiesService propertiesService) {
        this.propertiesService = propertiesService;
    }

    /**
     * Returns set of files in directory by path.
     * Adds root path to path parameter
     *
     * @param pathString path of directory (without root path)
     * @return set of files PathInfos
     */
    public Set<PathInfo> listPathInfosForDirectory(String pathString) {
        Path rootDirectoryPath = Paths.get(propertiesService.getRootDirectory());
        boolean showHiddenFiles = propertiesService.showHiddenFiles();
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

        if (!Files.exists(dirPath)) {
            throw new DirectoryNotFoundException("Directory " + dirPath.toString() + " is not found!");
        }

        if (!Files.isDirectory(dirPath)) {
            throw new NotDirectoryException(dirPath + " is not a directory!");
        }

        try {
            DirectoryStream.Filter<Path> filter = file -> !pathIsHidden(file);
            DirectoryStream<Path> stream = showHiddenFiles ? Files.newDirectoryStream(dirPath) :
                    Files.newDirectoryStream(dirPath, filter);
            for (Path path : stream) {
                Path relativizedPath = rootDirectoryPath.relativize(path);
                boolean directory = Files.isDirectory(path);
                String relativePathString = relativizedPath.toString();
                String normalizedRelativePathString =
                        FilenameUtils.normalize(directory && !relativePathString.endsWith("/") ?
                                relativePathString + "/" : relativePathString, true);

                pathInfos.add(new PathInfo(normalizedRelativePathString,
                        directory ? PathType.DIRECTORY : PathType.FILE,
                        ExtractionService.ArchiveType.fileHasSupportedType(normalizedRelativePathString)
                ));
            }
        } catch (IOException e) {
            throw new UnknownException("Error on reading directory: " + dirPath, e);
        }

        return ImmutableSet.copyOf(pathInfos);
    }

    /**
     * returns optional of path
     * by creating a file from root path and parameter path
     *
     * @param filePath path to the file without root path
     * @return optional of file, if file does not exists then returns empty optional
     */
    public Optional<Path> resolveFileOrDirectory(String filePath) {
        String rootDirectoryPath = propertiesService.getRootDirectory();
        Path path = Paths.get(rootDirectoryPath + File.separator + filePath);
        if (Files.exists(path)) {
            return Optional.of(path);
        }
        return Optional.empty();
    }

    /**
     * get absolute path. Adds relative path to root path
     * @param pathString relative path
     * @return absolute path
     */
    Path getPath(String pathString) {
        String rootPath = propertiesService.getRootDirectory();
        if (rootPath.isEmpty()) {
            throw new DirServiceException("Directory was not found");
        }
        return Paths.get(rootPath + File.separator + pathString);
    }

    private boolean pathIsHidden(Path path) {
        if (path.getFileName().toString().startsWith(".")) {
            return true;
        }
        try {
            return Files.readAttributes(path, DosFileAttributes.class).isHidden();
        } catch (IOException e) {
            return false;
        }
    }
}
