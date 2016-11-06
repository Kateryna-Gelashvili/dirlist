package org.k.data;

public class PathInfo {
    private String path;
    private PathType type;
    private boolean extractionSupported;

    public PathInfo(String path, PathType type, boolean extractionSupported) {
        this.path = path;
        this.type = type;
        this.extractionSupported = extractionSupported;
    }

    public String getPath() {
        return path;
    }

    public PathType getType() {
        return type;
    }

    public boolean isExtractionSupported() {
        return extractionSupported;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PathInfo)) return false;

        PathInfo pathInfo = (PathInfo) o;

        return path.equals(pathInfo.path) && type == pathInfo.type;
    }

    @Override
    public int hashCode() {
        int result = path.hashCode();
        result = 31 * result + type.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "PathInfo{" +
                "path='" + path + '\'' +
                ", type=" + type +
                '}';
    }
}
