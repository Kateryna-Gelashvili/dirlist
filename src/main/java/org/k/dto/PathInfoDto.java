package org.k.dto;

import org.k.data.PathInfo;

@SuppressWarnings("unused")
public class PathInfoDto {
    private String path;
    private PathTypeDto type;
    private boolean extractionSupported;

    public PathInfoDto() {
    }

    public PathInfoDto(PathInfo pathInfo) {
        this.path = pathInfo.getPath();
        this.type = PathTypeDto.fromPathType(pathInfo.getType());
        this.extractionSupported = pathInfo.isExtractionSupported();
    }

    public String getPath() {
        return path;
    }

    public PathTypeDto getType() {
        return type;
    }

    public boolean isExtractionSupported() {
        return extractionSupported;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PathInfoDto)) return false;

        PathInfoDto that = (PathInfoDto) o;

        return path.equals(that.path) && type == that.type;

    }

    @Override
    public int hashCode() {
        int result = path.hashCode();
        result = 31 * result + type.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "PathInfoDto{" +
                "path='" + path + '\'' +
                ", type=" + type +
                '}';
    }
}