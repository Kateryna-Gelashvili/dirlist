package org.k.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PathDto {
    private final String path;

    @JsonCreator
    public PathDto(@JsonProperty("path") String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
