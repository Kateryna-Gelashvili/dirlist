package org.k.data;

public class PathInfo {
    private String path;
    private PathType type;
    private boolean isArchiveType;

    public PathInfo(String path, PathType type, boolean isArchiveType) {
        this.path = path;
        this.type = type;
        this.isArchiveType = isArchiveType;
    }

    public String getPath() {
        return path;
    }

    public PathType getType() {
        return type;
    }

    public boolean isArchiveType() {
        return isArchiveType;
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
