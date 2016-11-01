package org.k.data;

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
}
