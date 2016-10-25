package org.k.service;

import com.google.common.collect.ImmutableSet;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

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

    public void extractFile(String path) {
        Path dirPath = getPath(path);
        if (!Files.exists(dirPath)) {
            throw new DirServiceException("File was not found " + dirPath.toString());
        }
        Path destPath = dirPath.getParent();
        if (!Files.exists(destPath)) {
            try {
                Files.createDirectories(destPath);
            } catch (IOException e) {
                throw new UnknownException("Error on creating directory:" + destPath, e);
            }
        }
        String destination = destPath.toString();
        extractZIP(dirPath, destination);
    }

    private void extractZIP(Path file, String destination) {
        try {
            ZipFile zipFile = new ZipFile(file.toFile());
            zipFile.setRunInThread(true);
            zipFile.extractAll(destination);
        } catch (ZipException e) {
            throw new UnknownException("Error on extracting:" + file + " to " + destination, e);
        }
    }
}
