package org.k.exception;

import java.nio.file.Path;

public class ExtractionException extends RuntimeException {
    private final Path file;
    private final Path targetDir;

    public ExtractionException(Path file, Path targetDir, Throwable cause) {
        super("Error on extraction: File: [" + file.toAbsolutePath() + "]. " +
                "Target Dir [" + targetDir.toAbsolutePath() + "].", cause);
        this.file = file;
        this.targetDir = targetDir;
    }

    public Path getFile() {
        return file;
    }

    public Path getTargetDir() {
        return targetDir;
    }
}
