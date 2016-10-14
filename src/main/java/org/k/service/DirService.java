package org.k.service;

import com.google.common.collect.ImmutableSet;
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
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

@Service
public class DirService {

    @Autowired
    private PropertiesService propertiesService;

    public Set<PathInfo> listPathInfosForDirectory(String pathString) {
        Path rootDirectoryPath = Paths.get(propertiesService.getRootDirectory());
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
                ).thenComparing(PathInfo::getPath)
        );
        Path dirPath = getPath(pathString);

        if (!Files.exists(dirPath)) {
            throw new DirectoryNotFoundException("Directory " + dirPath.toString() + " is not found!");
        }

        if (!Files.isDirectory(dirPath)) {
            throw new NotDirectoryException(dirPath + " is not a directory!");
        }

        try {
            DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath);
            for (Path path : stream) {
                Path relativizedPath = rootDirectoryPath.relativize(path);
                boolean directory = Files.isDirectory(path);
                String relativePathString = relativizedPath.toString();
                String normalizedRelativePathString =
                        directory && !relativePathString.endsWith("/") ?
                                relativePathString + "/" : relativePathString;

                pathInfos.add(new PathInfo(normalizedRelativePathString,
                        directory ? PathType.DIRECTORY : PathType.FILE
                ));
            }
        } catch (IOException e) {
            throw new UnknownException("Error on reading directory: " + dirPath, e);
        }

        return ImmutableSet.copyOf(pathInfos);
    }

    private Path getPath(String pathString) {
        String rootPath = propertiesService.getRootDirectory();
        if (rootPath.isEmpty()) {
            throw new DirServiceException("Directory was not found");
        }
        return Paths.get(rootPath + File.separator + pathString);
    }

    public Optional<File> resolveFileOrDirectory(String filePath) {
        String rootDirectoryPath = propertiesService.getRootDirectory();
        File file = new File(rootDirectoryPath + File.separator + filePath);
        if (file.exists()) {
            return Optional.of(file);
        }
        return Optional.empty();
    }
}
