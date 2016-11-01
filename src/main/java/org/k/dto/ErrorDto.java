package org.k.dto;

import com.google.common.base.Preconditions;

@SuppressWarnings("unused")
public class ErrorDto {
    private int code;
    private String message;

    public ErrorDto(int code, String message) {
        Preconditions.checkArgument(code > 0);
        this.code = code;
        this.message = Preconditions.checkNotNull(message);
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
