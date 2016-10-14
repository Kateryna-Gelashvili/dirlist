package org.k.dto;

import org.k.data.PathType;

public enum PathTypeDto {
    FILE(PathType.FILE), DIRECTORY(PathType.DIRECTORY);

    private final PathType pathType;

    PathTypeDto(PathType pathType) {
        this.pathType = pathType;
    }

    public static PathTypeDto fromPathType(PathType pathType) {
        for (PathTypeDto pathTypeDto : values()) {
            if (pathTypeDto.pathType == pathType) {
                return pathTypeDto;
            }
        }
        return null;
    }
}
